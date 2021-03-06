package ru.skyeng.skyenglogin.loginModule;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import ru.skyeng.skyenglogin.R;
import ru.skyeng.skyenglogin.application.SEApplication;
import ru.skyeng.skyenglogin.network.interfaces.SENetworkCallback;
import ru.skyeng.skyenglogin.utility.FacadCommon;

import static ru.skyeng.skyenglogin.network.authorization.SEAuthorizationServer.TYPE_TEMP_PASSWORD;

public class LoginCodeActivity extends AppCompatActivity implements View.OnClickListener, SENetworkCallback<String> {

    public static final String KEY_PHONE = "phone";
    public static final String KEY_EMAIL = "email";
    private static final String KEY_TIMER_COUNT = "timerCount";
    private static int TIMER_COUNT = 60000;
    private EditText mCodeEditText;
    private Button mLoginButton;
    private Button mResendCodeButton;
    private String mTempPassword;
    private String mEmail;
    private ProgressDialog mProgressDialog;
    private CoordinatorLayout coordinatorLayout;
    private static int NOTIFICATION_ID = 0;

    private CountDownTimer countDownTimer = new CountDownTimer(TIMER_COUNT, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            mResendCodeButton.setText(String.format(getResources().getString(R.string.waiting_resend_code), String.valueOf(millisUntilFinished / 1000)));
            TIMER_COUNT = (int) (millisUntilFinished);
        }

        @Override
        public void onFinish() {
            String text = getResources().getString(R.string.waiting_resend_code);
            mResendCodeButton.setText(text.substring(0, 20));
            mResendCodeButton.setEnabled(true);
            mResendCodeButton.setTextColor(ContextCompat.getColor(LoginCodeActivity.this, R.color.colorAccent));
        }
    };

    @Inject
    protected LoginController mController;

    @Inject
    public void setLoginModel(LoginModel model) {
        model.setContext(this);
        model.setDelegate(this);
        model.setServer(((SEApplication) getApplicationContext()).getServer());
        mController.setModel(model);
    }

    public static Intent receiveIntent(Context context, String password, String email) {
        Intent intent = new Intent(context, LoginCodeActivity.class);
        intent.putExtra(KEY_PHONE, password);
        intent.putExtra(KEY_EMAIL, email);
        return intent;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_TIMER_COUNT, TIMER_COUNT);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((SEApplication) getApplication()).getLoginCodeDiComponent().inject(this);
        setContentView(R.layout.activity_login_code);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Войти");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        mTempPassword = intent.getStringExtra(KEY_PHONE);
        mEmail = intent.getStringExtra(KEY_EMAIL);
        iniViews();

        if (savedInstanceState != null) {
            TIMER_COUNT = savedInstanceState.getInt(KEY_TIMER_COUNT);
        }
        countDownTimer.start();
    }

    private void iniViews() {
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        TextView mCodeText = (TextView) findViewById(R.id.code_text);
        mCodeText.append(" "+ mTempPassword +")");
        mCodeEditText = (EditText) findViewById(R.id.login_code);
        mCodeEditText.getBackground().mutate().setColorFilter(getResources().getColor(R.color.colorTextMain), PorterDuff.Mode.SRC_ATOP);
        mCodeEditText.addTextChangedListener(textWatcher);
        mLoginButton = (Button) findViewById(R.id.button_login);
        mResendCodeButton = (Button) findViewById(R.id.code_resend_button);
        mResendCodeButton.setEnabled(false);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.dialog_message_code_login));
        mLoginButton.setOnClickListener(this);
        mResendCodeButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        FacadCommon.hideKeyboard(this);
        if(v.getId()==R.id.code_resend_button){
            mResendCodeButton.setEnabled(false);
            TIMER_COUNT = 60000;
            countDownTimer.start();
            mController.getOneTimePassword(mEmail);
        }else if(v.getId()==R.id.button_login){
            mProgressDialog.show();
            mController.authorize(mEmail, mCodeEditText.getText().toString());
        }
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable editable) {
            if (editable.toString().length() == 0) {
                mLoginButton.setEnabled(false);
            } else {
                mLoginButton.setEnabled(true);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }
    };

    @Override
    public void onSuccess(String result, int requestType) {
        if(requestType==TYPE_TEMP_PASSWORD){
            FacadCommon.createNotification(result.split("/")[0], this, LoginCodeActivity.class, NOTIFICATION_ID);
        }else{
            mProgressDialog.hide();
            Intent intent = new Intent(this, LogoutActivity.class);
            startActivity(intent);
        }

    }

    @Override
    public void onError(Throwable throwable) {
        mProgressDialog.show();
        Snackbar.make(coordinatorLayout, throwable.getMessage(), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

package com.cars.material.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cars.material.R;
import com.cars.material.application.MaterialApplication;
import com.cars.material.base.BaseActivity;
import com.cars.material.bean.LocationBean;
import com.cars.material.bean.LoginInfo;
import com.cars.material.bean.ParamsInfo;
import com.cars.material.custom.CommonDialog;
import com.cars.material.net.OkHttpException;
import com.cars.material.net.RequestMode;
import com.cars.material.net.RequestParams;
import com.cars.material.net.RequestParamsFactory;
import com.cars.material.net.RequestUrlManager;
import com.cars.material.net.ResponseCallback;
import com.cars.material.utils.ImageUtils;
import com.cars.material.utils.Sm2Utils;
import com.cars.material.utils.SpUtils;
import com.cars.material.utils.ToastUtils;
import com.google.gson.Gson;

import java.util.List;

import static com.cars.material.application.MaterialApplication.getContext;

public class LoginActivity extends BaseActivity implements View.OnClickListener, ResponseCallback {

    private static final int GET_VERIFICATION_CODE = 0;
    private static final int GET_LOGIN_INFO = 1;
    private static final int GET_SMS_LOGIN_INFO = 2;
    private static final int GET_SMS_VERIFICATION_CODE = 3;

    private static final int REQUEST_PERMISSIONS = 0;          //获取权限

    private LinearLayout mLlUserLogin;
    private ImageView mIvBg;
    private EditText mEtUser;
    private EditText mEtPass;
    private EditText mEtCode;
    private ImageView mIvSee;
    private ImageView mIvVerification;
    private RelativeLayout mRlUserBottom;
    private RelativeLayout mRlSmsBottom;
    private TextView mTvChange;
    private TextView mTvSmsChange;
    private LinearLayout mLlCheckBox;
    private ImageView mIvCheckBox;

    private LinearLayout mLlSmsLogin;
    private EditText mEtSmsUser;
    private EditText mEtSmsCode;
    private TextView mTvSmsVerification;

    private TextView mTvLogin;
    private TextView mTvRegister;

    private String mImageBase;
    private String mRandomKey;

    private CountDownTimer mTimer;
    private LoginInfo mLoginInfo;
    private boolean mShowPassWord = false;
    private boolean mSavePassWord = false;
    private String mAndroidId;

    private Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case GET_VERIFICATION_CODE:
                    Bitmap bitmap = ImageUtils.base64ToImage(mImageBase);
                    mIvVerification.setImageBitmap(bitmap);
                    break;
                case GET_LOGIN_INFO:
                    startLogin();
                    break;
                case GET_SMS_LOGIN_INFO:
                    startLogin();
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (!this.isTaskRoot()) { // 当前类不是该Task的根部，那么之前启动
            Intent intent = getIntent();
            if (intent != null) {
                String action = intent.getAction();
                if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) { // 当前类是从桌面启动的
                    finish(); // finish掉该类，直接打开该Task中现存的Activity
                    return;
                }
            }
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏
            setContentView(R.layout.activity_login_horizon);
        } else {
            // 竖屏
            setContentView(R.layout.activity_login_vertical);
        }

        initResourceId();
        initListener();
        initControlSetting();
        initData();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏
            setContentView(R.layout.activity_login_horizon);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 竖屏
            setContentView(R.layout.activity_login_vertical);
        }
        initResourceId();
        initListener();
        initData();
        initControlSetting();
    }

    private void initResourceId() {
        mLlUserLogin = findViewById(R.id.ll_user_login);
        mIvBg = findViewById(R.id.iv_bg);
        mEtUser = findViewById(R.id.et_user);
        mEtPass = findViewById(R.id.et_pass);
        mEtCode = findViewById(R.id.et_code);
        mIvSee = findViewById(R.id.iv_see);
        mIvVerification = findViewById(R.id.iv_verification);

        mLlSmsLogin = findViewById(R.id.ll_sms_login);
        mEtSmsUser = findViewById(R.id.et_sms_user);
        mEtSmsCode = findViewById(R.id.et_sms_code);
        mTvSmsVerification = findViewById(R.id.tv_sms_verification);

        mTvLogin = findViewById(R.id.tv_login);
        mTvRegister = findViewById(R.id.tv_register);

        mRlUserBottom = findViewById(R.id.rl_user_bottom);
        mRlSmsBottom = findViewById(R.id.rl_sms_bottom);
        mTvChange = findViewById(R.id.tv_change);
        mTvSmsChange = findViewById(R.id.tv_sms_change);
        mLlCheckBox = findViewById(R.id.ll_checkbox);
        mIvCheckBox = findViewById(R.id.iv_checkbox);
    }

    private void initListener() {
        mIvVerification.setOnClickListener(this);
        mTvSmsVerification.setOnClickListener(this);
        mTvChange.setOnClickListener(this);
        mTvLogin.setOnClickListener(this);
        mTvRegister.setOnClickListener(this);
        mTvSmsChange.setOnClickListener(this);
        mIvSee.setOnClickListener(this);
        mLlCheckBox.setOnClickListener(this);
    }

    private void initControlSetting() {
        if (SpUtils.getBoolean(getContext(), SpUtils.SAVE_PASS_WORD, false)) {
            mEtUser.setText(SpUtils.getString(getContext(), SpUtils.USER_NAME, ""));
            mEtPass.setText(SpUtils.getString(getContext(), SpUtils.PASS_WORD, ""));
            mIvCheckBox.setImageResource(R.mipmap.icon_checkbox_checked);
            mEtUser.setSelection(mEtUser.getText().toString().trim().length());
            mSavePassWord = true;
        }
        getAndroidId();
    }

    private void initData() {
        getVerificationCode();
    }

    private void getAndroidId() {
        mAndroidId = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private void getVerificationCode() {
        showLoadingDialog();

        mRandomKey = System.currentTimeMillis() + "";
        RequestParams mParams = RequestParamsFactory.getInstance().getDefaultParams();
        RequestMode.getRequest(GET_VERIFICATION_CODE, RequestUrlManager.GET_VERIFICATION_CODE + "/" + mRandomKey,
                mParams, this, null, false);
    }

    private void loginRequest() {
        showLoadingDialog();

        ParamsInfo paramsInfo = new ParamsInfo();
        paramsInfo.setUsername(mEtUser.getText().toString().trim());
        paramsInfo.setPassword(mEtPass.getText().toString().trim());
        paramsInfo.setCheckCode(mEtCode.getText().toString().trim());
        paramsInfo.setCheckKey(mRandomKey);
        paramsInfo.setDeviceNum(mAndroidId);
        Gson gson = new Gson();
        String jsonString = gson.toJson(paramsInfo);
        String params = Sm2Utils.encrypt(MaterialApplication.SECRETKEY, jsonString);

        RequestParams mParams = RequestParamsFactory.getInstance().getLoginParams(params);
        RequestMode.postRequest(GET_LOGIN_INFO, RequestUrlManager.GET_LOGIN_INFO, mParams, this, LoginInfo.class, false);
    }

    private void getSmsVerificationCode() {
        showLoadingDialog();
        mRandomKey = System.currentTimeMillis() + "";
        RequestParams mParams = RequestParamsFactory.getInstance().getSmsVerificationCode(mEtSmsUser.getText().toString().trim());
        RequestMode.postRequestSm4(GET_SMS_VERIFICATION_CODE, RequestUrlManager.GET_SMS_VERIFICATION_CODE,
                mParams, this, null, false);
    }

    private void smsLoginRequest() {
        showLoadingDialog();

        RequestParams mParams = RequestParamsFactory.getInstance().getSmsLoginParams(mEtSmsUser.getText().toString().trim(),
                mEtSmsCode.getText().toString().trim(), mAndroidId);
        RequestMode.postRequestSm4(GET_SMS_LOGIN_INFO, RequestUrlManager.GET_SMS_LOGIN_INFO, mParams, this, LoginInfo.class, false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_verification:
                getVerificationCode();
                break;
            case R.id.tv_login:
                startToMain();
                break;
            case R.id.tv_register:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("授权码");
                builder.setMessage(mAndroidId);
                builder.show();
                break;
            case R.id.tv_sms_verification:
                String phone = mEtSmsUser.getText().toString().trim();
                if (TextUtils.isEmpty(phone)) {
                    ToastUtils.showToast(LoginActivity.this, "请输入手机号");
                    return;
                }
                getSmsVerificationCode();
                break;
            case R.id.tv_change:
                mLlUserLogin.setVisibility(View.GONE);
                mLlSmsLogin.setVisibility(View.VISIBLE);
                mRlUserBottom.setVisibility(View.GONE);
                mRlSmsBottom.setVisibility(View.VISIBLE);
                break;
            case R.id.tv_sms_change:
                mLlUserLogin.setVisibility(View.VISIBLE);
                mLlSmsLogin.setVisibility(View.GONE);
                mRlUserBottom.setVisibility(View.VISIBLE);
                mRlSmsBottom.setVisibility(View.GONE);

                mEtSmsUser.setText("");
                mEtSmsCode.setText("");
                break;
            case R.id.ll_checkbox:
                if (mSavePassWord) {
                    mIvCheckBox.setImageResource(R.mipmap.icon_checkbox_normal);
                } else {
                    mIvCheckBox.setImageResource(R.mipmap.icon_checkbox_checked);
                }
                mSavePassWord = !mSavePassWord;
                break;
            case R.id.iv_see:
                if (mShowPassWord) {
                    mEtPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    mIvSee.setImageResource(R.mipmap.icon_eye_close);
                } else {
                    mEtPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    mIvSee.setImageResource(R.mipmap.icon_eye_open);
                }
                mShowPassWord = !mShowPassWord;
                mEtPass.setSelection(mEtPass.getText().toString().length());
                break;
        }
    }

    private void initTimer() {
        if (mTimer == null) {
            mTimer = new CountDownTimer(60000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    mTvSmsVerification.setEnabled(false);
                    mTvSmsVerification.setText(millisUntilFinished / 1000 + "秒后重新获取");
                }

                @Override
                public void onFinish() {
                    mTvSmsVerification.setEnabled(true);
                    mTvSmsVerification.setText("获取验证码");
                }
            };
        }
        mTimer.start();
    }

    private void startToMain() {
        if (mTvChange.getText().toString().trim().equals("使用短信登录 >")) {
            if (TextUtils.isEmpty(mEtUser.getText().toString().trim())) {
                ToastUtils.showToast(this, "请输入用户名");
                return;
            }
            if (TextUtils.isEmpty(mEtPass.getText().toString().trim())) {
                ToastUtils.showToast(this, "请输入密码");
                return;
            }
            if (TextUtils.isEmpty(mEtCode.getText().toString().trim())) {
                ToastUtils.showToast(this, "请输入验证码");
                return;
            }
            loginRequest();
        } else {
            if (TextUtils.isEmpty(mEtSmsUser.getText().toString().trim())) {
                ToastUtils.showToast(this, "请输入手机号");
                return;
            }
            if (TextUtils.isEmpty(mEtSmsCode.getText().toString().trim())) {
                ToastUtils.showToast(this, "请输入验证码");
                return;
            }
            smsLoginRequest();
        }
    }

    private void startLogin() {
        SpUtils.putString(getContext(), SpUtils.USER_NAME, mLoginInfo.getUsername());
        SpUtils.putString(getContext(), SpUtils.PASS_WORD, mEtPass.getText().toString().trim());
        SpUtils.putBoolean(getContext(), SpUtils.SAVE_PASS_WORD, mSavePassWord);
        SpUtils.putString(getContext(), SpUtils.TOKEN, mLoginInfo.getToken());

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onSuccess(int requestId, Object responseObj) {
        hideLoadingDialog();
        switch (requestId) {
            case GET_VERIFICATION_CODE:
                mImageBase = (String) responseObj;

                Message msg = Message.obtain();
                msg.what = GET_VERIFICATION_CODE;
                mHandler.sendMessage(msg);
                break;
            case GET_LOGIN_INFO:
                mLoginInfo = (LoginInfo) responseObj;

                Message msgLogin = Message.obtain();
                msgLogin.what = GET_LOGIN_INFO;
                mHandler.sendMessage(msgLogin);
                break;
            case GET_SMS_VERIFICATION_CODE:
//                String smsCode = (String) responseObj;
//                mEtSmsCode.setText(smsCode);

                initTimer();
                ToastUtils.showToast(LoginActivity.this, "验证码已发送");
                break;
            case GET_SMS_LOGIN_INFO:
                mLoginInfo = (LoginInfo) responseObj;

                Message msgSmsLogin = Message.obtain();
                msgSmsLogin.what = GET_SMS_LOGIN_INFO;
                mHandler.sendMessage(msgSmsLogin);
                break;
        }
    }

    @Override
    public void onFailure(int requestId, OkHttpException failuer) {
        hideLoadingDialog();
        switch (requestId) {
            case GET_VERIFICATION_CODE:
                ToastUtils.showToast(this, failuer.getEmsg());
                break;
            case GET_LOGIN_INFO:
                ToastUtils.showToast(this, failuer.getEmsg());
                if ("登录失败：验证码错误！".equals(failuer.getEmsg())) {
                    getVerificationCode();
                }
                break;
            case GET_SMS_LOGIN_INFO:
                ToastUtils.showToast(this, failuer.getEmsg());
                break;
            case GET_SMS_VERIFICATION_CODE:
                ToastUtils.showToast(this, failuer.getEmsg());
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RequestMode.cancelTag(RequestUrlManager.GET_VERIFICATION_CODE,
                RequestUrlManager.GET_LOGIN_INFO,
                RequestUrlManager.GET_SMS_LOGIN_INFO,
                RequestUrlManager.GET_SMS_VERIFICATION_CODE);

        if (mTimer != null) {
            mTimer.cancel();
        }
    }
}

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
import com.cars.material.bean.AuthorizationInfo;
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
    private static final int GET_AUTHORIZATION_DETAIL = 4;

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
    private View mTvRegisterApply;
    private View mTvRegisterApproved;
    private View mTvRegisterPending;
    private View mTvRegisterRejected;
    private View mTvRegisterClosed;

    private String mImageBase;
    private String mRandomKey;

    private CountDownTimer mTimer;
    private LoginInfo mLoginInfo;
    private AuthorizationInfo mAuthorizationInfo;
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
                case GET_AUTHORIZATION_DETAIL:
                    updateAuthorizationButton();
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

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到登录页面时重新获取授权状态，确保状态是最新的
        // 这样当H5调用退出登录时，授权按钮会被正确更新
        getAuthorizationDetail();
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
        mTvRegisterApply = findViewById(R.id.tv_register_apply);
        mTvRegisterApproved = findViewById(R.id.tv_register_approved);
        mTvRegisterPending = findViewById(R.id.tv_register_pending);
        mTvRegisterRejected = findViewById(R.id.tv_register_rejected);
        mTvRegisterClosed = findViewById(R.id.tv_register_closed);

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
        mTvRegisterApply.setOnClickListener(this);
        mTvRegisterApproved.setOnClickListener(this);
        mTvRegisterPending.setOnClickListener(this);
        mTvRegisterRejected.setOnClickListener(this);
        mTvRegisterClosed.setOnClickListener(this);
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
        // 进入页面时自动获取授权状态
        getAuthorizationDetail();
    }



    private void getAndroidId() {
        mAndroidId = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        android.util.Log.d("LoginActivity", "获取到的设备号: " + mAndroidId);
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

    private void getAuthorizationDetail() {
        // 检查设备号是否为空
        if (mAndroidId == null || mAndroidId.isEmpty()) {
            android.util.Log.w("LoginActivity", "设备号为空，无法请求授权详情");
            return;
        }

        showLoadingDialog();

        // 创建请求参数，不加密，直接传递设备号
        RequestParams mParams = RequestParamsFactory.getInstance().getDefaultParams();
        mParams.put("deviceNum", mAndroidId);

        // 添加调试日志
        android.util.Log.d("LoginActivity", "开始请求授权详情，设备号: " + mAndroidId);
        android.util.Log.d("LoginActivity", "请求URL: " + RequestUrlManager.HOST + RequestUrlManager.GET_AUTHORIZATION_DETAIL);

        RequestMode.getRequestWithHeadersPlain(GET_AUTHORIZATION_DETAIL, RequestUrlManager.GET_AUTHORIZATION_DETAIL, mParams, this, AuthorizationInfo.class, false);
    }

    private void updateAuthorizationButton() {
        android.util.Log.d("LoginActivity", "开始更新授权按钮");
        if (mAuthorizationInfo == null) {
            android.util.Log.d("LoginActivity", "授权信息为空，返回");
            return;
        }

        android.util.Log.d("LoginActivity", "授权信息: " + new Gson().toJson(mAuthorizationInfo));

        // 详细检查授权信息
        if (mAuthorizationInfo.getData() == null) {
            android.util.Log.d("LoginActivity", "data为null");
        } else {
            android.util.Log.d("LoginActivity", "data不为null");
            android.util.Log.d("LoginActivity", "data.getStatus(): " + mAuthorizationInfo.getData().getStatus());
            android.util.Log.d("LoginActivity", "data.hasStatus(): " + mAuthorizationInfo.getData().hasStatus());
        }
        android.util.Log.d("LoginActivity", "needApply(): " + mAuthorizationInfo.needApply());

        // 先隐藏所有按钮
        android.util.Log.d("LoginActivity", "隐藏所有授权按钮");
        mTvRegisterApply.setVisibility(View.GONE);
        mTvRegisterApproved.setVisibility(View.GONE);
        mTvRegisterPending.setVisibility(View.GONE);
        mTvRegisterRejected.setVisibility(View.GONE);
        mTvRegisterClosed.setVisibility(View.GONE);

        // 根据授权状态显示对应按钮
        if (mAuthorizationInfo.needApply()) {
            // 需要申请授权
            android.util.Log.d("LoginActivity", "needApply()=true，显示申请授权按钮");
            mTvRegisterApply.setVisibility(View.VISIBLE);
        } else {
            AuthorizationInfo.AuthorizationData data = mAuthorizationInfo.getData();
            android.util.Log.d("LoginActivity", "needApply()=false，有授权数据，status: " + data.getStatus());

            // 根据状态显示对应按钮
            switch (data.getStatus()) {
                case "1": // 已通过授权
                    android.util.Log.d("LoginActivity", "显示已通过授权按钮");
                    mTvRegisterApproved.setVisibility(View.VISIBLE);
                    break;
                case "2": // 正在授权中
                    android.util.Log.d("LoginActivity", "显示正在授权中按钮");
                    mTvRegisterPending.setVisibility(View.VISIBLE);
                    break;
                case "3": // 已驳回授权
                    android.util.Log.d("LoginActivity", "显示已驳回授权按钮");
                    mTvRegisterRejected.setVisibility(View.VISIBLE);
                    break;
                case "0": // 已关闭授权
                    android.util.Log.d("LoginActivity", "显示已关闭授权按钮");
                    mTvRegisterClosed.setVisibility(View.VISIBLE);
                    break;
                default:
                    // 默认申请状态
                    android.util.Log.d("LoginActivity", "默认显示申请授权按钮");
                    mTvRegisterApply.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    private void jumpToH5Page() {
        android.util.Log.d("LoginActivity", "开始跳转H5页面");
        android.util.Log.d("LoginActivity", "当前设备号: " + mAndroidId);

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);

        // 构建H5页面URL
        String baseUrl = RequestUrlManager.MOBILE_HOST + "deviceDetail";
        String url = baseUrl;

        // 确保设备号不为空
        if (mAndroidId == null || mAndroidId.isEmpty()) {
            android.util.Log.w("LoginActivity", "设备号为空，重新获取");
            getAndroidId();
        }

        // 如果有授权状态，添加status参数
        if (mAuthorizationInfo != null && !mAuthorizationInfo.needApply()) {
            String status = mAuthorizationInfo.getStatusForH5();
            if (status != null && !status.isEmpty()) {
                url = baseUrl + "/?status=" + status + "&deviceNum=" + mAndroidId;
            } else {
                url = baseUrl + "/?deviceNum=" + mAndroidId;
            }
        } else {
            // 没有授权状态时，也传递设备号
            url = baseUrl + "/?deviceNum=" + mAndroidId;
        }

        android.util.Log.d("LoginActivity", "跳转URL: " + url);

        // 通过Intent传递URL给MainActivity
        intent.putExtra("h5_url", url);
        startActivity(intent);
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
            case R.id.tv_register_apply:
            case R.id.tv_register_approved:
            case R.id.tv_register_pending:
            case R.id.tv_register_rejected:
            case R.id.tv_register_closed:
                // 直接跳转H5页面
                jumpToH5Page();
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
            case GET_AUTHORIZATION_DETAIL:
                android.util.Log.d("LoginActivity", "授权详情接口调用成功");
                android.util.Log.d("LoginActivity", "原始响应对象类型: " + responseObj.getClass().getName());

                mAuthorizationInfo = (AuthorizationInfo) responseObj;

                if (mAuthorizationInfo != null) {
                    android.util.Log.d("LoginActivity", "解析后的授权信息: " + new Gson().toJson(mAuthorizationInfo));

                    // 详细检查解析结果
                    if (mAuthorizationInfo.getData() == null) {
                        android.util.Log.d("LoginActivity", "解析结果: data为null");
                    } else {
                        android.util.Log.d("LoginActivity", "解析结果: data不为null");
                        android.util.Log.d("LoginActivity", "解析结果: status = " + mAuthorizationInfo.getData().getStatus());
                        android.util.Log.d("LoginActivity", "解析结果: hasStatus = " + mAuthorizationInfo.getData().hasStatus());
                        android.util.Log.d("LoginActivity", "解析结果: needApply = " + mAuthorizationInfo.needApply());
                    }
                } else {
                    android.util.Log.d("LoginActivity", "授权信息解析为空");
                }

                Message msgAuth = Message.obtain();
                msgAuth.what = GET_AUTHORIZATION_DETAIL;
                mHandler.sendMessage(msgAuth);
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
            case GET_AUTHORIZATION_DETAIL:
                android.util.Log.e("LoginActivity", "授权详情接口调用失败: " + failuer.getEmsg());
                ToastUtils.showToast(this, "获取授权详情失败：" + failuer.getEmsg());
                // 接口失败时，保持默认的申请授权状态
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

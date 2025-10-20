package com.cars.material.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cars.material.R;
import com.cars.material.base.BaseActivity;
import com.cars.material.bean.LocationBean;
import com.cars.material.custom.ProgressWebView;
import com.cars.material.custom.WaterMarBg;
import com.cars.material.manager.AppManager;
import com.cars.material.net.RequestUrlManager;
import com.cars.material.utils.DownloadUtil;
import com.cars.material.utils.SpUtils;
import com.cars.material.utils.ToastUtils;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cn.hutool.json.JSON;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_PERMISSIONS = 0;          //获取权限
    private static final int SELECT_FILE_REQUEST_CODE = 1;          //选择文件

    private ProgressWebView mWebView;
    private RelativeLayout mRlBack;
    private RelativeLayout mRlClose;
    private RelativeLayout mRlHome;
    private TextView mTvTitle;
    private boolean isExit = false;
    private String[] mFilterMenu = {"供应商首页", "施工单位首页", "监理首页","设备使用授权"};
    private View mWatermarkView;
    private String mFileName;
    private String mFilePath;
    private String mPermissionType;

    private ValueCallback<Uri[]> mFilePathCallback;
    private String mBase64Data;

    private LocationManager locationManager;// 位置管理类
    private String provider;// 位置提供器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean useFullScreen = getIntent().getBooleanExtra("use_full_screen_layout", false);

        if (useFullScreen) {
            setContentView(R.layout.full_activity_main);
            mWebView = findViewById(R.id.web_view);
            mWatermarkView = findViewById(R.id.watermark);
        } else {
            setContentView(R.layout.activity_main);
            mWebView = findViewById(R.id.web_view);
            mRlBack = findViewById(R.id.rl_back);
            mRlHome = findViewById(R.id.rl_home);
            mRlClose = findViewById(R.id.rl_close);
            mTvTitle = findViewById(R.id.tv_title);
            mWatermarkView = findViewById(R.id.watermark);

            mRlBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mWebView.canGoBack()) {
                        mWebView.goBack();
                    }
                }
            });
            mRlHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mWebView.evaluateJavascript("window.backToHomeClick();", null);
                }
            });
        }

        WebSettings settings = mWebView.getSettings();
        settings.setSupportZoom(true);
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setDomStorageEnabled(true);

        // 检查是否有传递的H5 URL
        String h5Url = getIntent().getStringExtra("h5_url");
        if (h5Url != null && !h5Url.isEmpty()) {
            // 加载指定的H5页面
            mWebView.loadUrl(h5Url);
        } else {
            // 加载默认首页
            mWebView.loadUrl(RequestUrlManager.MOBILE_HOST
                    + "?TokenKey=" + SpUtils.getString(this, SpUtils.TOKEN, ""));
//            mWebView.loadUrl("http://10.59.248.155:9527/"
//                    + "?TokenKey=" + SpUtils.getString(this, SpUtils.TOKEN, ""));
        }
        mWebView.setWebViewClient(new WebViewClient());

        mWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    mWebView.hideProgress();
                } else {
                    mWebView.setProgress(newProgress);
                }
                super.onProgressChanged(view, newProgress);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                mFilePathCallback = filePathCallback;

                String[] permissions = new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,};
                mPermissionType = "upload";
                initPermission(permissions);
                return true;
            }

        });
        mWebView.addJavascriptInterface(new WebAppInterface(this), "Android");
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context context) {
            mContext = context;
        }

        @JavascriptInterface
        public void sendMenuTitle(final String title) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvTitle.setText(title);

                    if (checkMenu(title)) {
                        mRlBack.setVisibility(View.GONE);
                        mRlHome.setVisibility(View.GONE);
                    } else {
                        mRlBack.setVisibility(View.VISIBLE);
                        mRlHome.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        @JavascriptInterface
        public void startToLogin() {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        @JavascriptInterface
        public void setWaterMark(String nickName) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();
            String formattedDate = formatter.format(date);
            String waterMark = nickName + " " + formattedDate;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWatermarkView.setBackground(new WaterMarBg(waterMark));
                }
            });
        }

        @JavascriptInterface
        public void fileDownLoad(String fileName, String filePath) {
            mFileName = fileName;
            mFilePath = RequestUrlManager.HOST + "/blcd-base/minio/download?filePath="
                    + filePath
                    + "&fileName="
                    + fileName;

            String[] permissions = new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,};
            mPermissionType = "download";
            initPermission(permissions);
        }

        @JavascriptInterface
        public void getBase64FromBlobData(String base64Data, String fileName) {
            mFileName = fileName;
            mBase64Data = base64Data;

            String[] permissions = new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,};
            mPermissionType = "downloadBlob";
            initPermission(permissions);
        }

        @JavascriptInterface
        public void initLocationPermission() {
            String[] permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,};
            mPermissionType = "location";
            initPermission(permissions);
        }

        @JavascriptInterface
        public void backToAndroidHome() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 加载默认首页
                    mWebView.loadUrl(RequestUrlManager.MOBILE_HOST
                            + "?TokenKey=" + SpUtils.getString(MainActivity.this, SpUtils.TOKEN, ""));
                }
            });
        }
    }

    /**
     * 转换成file
     */
    private void convertToGifAndProcess() {
        File gifFile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS) + "/" + mFileName);
        saveFileToPath(mBase64Data, gifFile);
    }

    /**
     * 保存文件
     *
     * @param base64
     * @param gifFilePath
     */
    private void saveFileToPath(String base64, File gifFilePath) {
        try {
            byte[] fileBytes = Base64.decode(base64.replaceFirst(
                    "data:application/octet-stream;base64,", ""), 0);
            FileOutputStream os = new FileOutputStream(gifFilePath, false);
            os.write(fileBytes);
            os.flush();
            os.close();

            Toast.makeText(MainActivity.this, "文件路径：" + gifFilePath, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取权限
     *
     * @param permissions
     */
    private void initPermission(String[] permissions) {
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        } else {
            if (mPermissionType.equals("upload")) {
                selectFile();
            } else if (mPermissionType.equals("download")) {
                downLoadFile();
            } else if (mPermissionType.equals("downloadBlob")) {
                convertToGifAndProcess();
            } else if (mPermissionType.equals("location")) {
                getLocation();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //有权限没有通过
        boolean hasPermissionDismiss = false;
        if (REQUEST_PERMISSIONS == requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;
                    break;
                }
            }
        }
        //权限已经都通过了，可以将程序继续打开了
        if (!hasPermissionDismiss) {
            if (mPermissionType.equals("upload")) {
                selectFile();
            } else if (mPermissionType.equals("download")) {
                downLoadFile();
            } else if (mPermissionType.equals("downloadBlob")) {
                convertToGifAndProcess();
            } else if (mPermissionType.equals("location")) {
                getLocation();
            }
        } else {
            if (mPermissionType.equals("location")) {
                mWebView.evaluateJavascript("window.getLocationPermission();", null);
            }
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = null;
        }
    }

    private void getLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        List<String> providerList = locationManager.getProviders(true);
        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            //优先使用gps
            provider = LocationManager.GPS_PROVIDER;
        } else if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else {
            // 没有可用的位置提供器
            ToastUtils.showToast(MainActivity.this, "获取定位失败");
            return;
        }
        @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
//            String currentLocation = "当前的经度是：" + location.getLongitude() + ",\n"
//                    + "当前的纬度是：" + location.getLatitude();
//            ToastUtils.showToast(MainActivity.this, currentLocation);

            // 显示当前设备的位置信息
            LocationBean locationBean = new LocationBean(location.getLongitude(),location.getLatitude());
            Gson gson = new Gson();
            String result = gson.toJson(locationBean);

            mWebView.evaluateJavascript("window.getLocationPermission(" + result + ");", null);
        } else {
            ToastUtils.showToast(MainActivity.this, "获取定位失败");
        }
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "ChooseFile"), SELECT_FILE_REQUEST_CODE);
    }

    private void downLoadFile() {
        showLoadingDialog();

        DownloadUtil.get().download(MainActivity.this, mFilePath, getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), mFileName, new DownloadUtil.OnDownloadListener() {
            @Override
            public void onDownloadSuccess(File file) {
                hideLoadingDialog();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "文件路径：" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onDownloading(int progress) {

            }

            @Override
            public void onDownloadFailed(Exception e) {
                hideLoadingDialog();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtils.showToast(MainActivity.this, "下载异常");
                    }
                });
            }
        });
    }

    private boolean checkMenu(String title) {
        for (String str : mFilterMenu) {
            if (str.equals(title)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_FILE_REQUEST_CODE) {
            if (data != null) {
                Uri uri = data.getData();
                Uri[] results = new Uri[]{uri};
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(results);
                }
                mFilePathCallback = null;
            } else {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = null;
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isExit) {
                isExit = true;
                ToastUtils.showToast(MainActivity.this, "再次点击退出程序");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isExit = false;
                    }
                }, 3000);
                return true;
            } else {
                AppManager.getAppManager().finishAllActivity();
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWebView.onPause();
        // 保存WebView状态到Bundle或SharedPreferences等
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
        // 从保存的位置恢复WebView状态
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

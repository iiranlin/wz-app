
package com.cars.material.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
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

public class MainActivity extends BaseActivity {

    private static final int REQUEST_PERMISSIONS = 0;          //获取权限
    private static final int SELECT_FILE_REQUEST_CODE = 1;          //选择文件
    // 跳转到“安装未知应用”设置页面的请求码
    private static final int INSTALL_PERMISSION_REQUEST_CODE = 1002;

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

    // --- App更新相关变量 ---
    private DownloadManager mDownloadManager;
    private long mDownloadId; // 下载任务的唯一ID
    private String mPendingDownloadUrl; // 用于保存请求权限前待下载的URL

    private WebAppInterface mWebInterface;

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
                    handleBackAction();
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

        String h5Url = getIntent().getStringExtra("h5_url");
        if (h5Url != null && !h5Url.isEmpty()) {
            mWebView.loadUrl(h5Url);
        } else {
            mWebView.loadUrl(RequestUrlManager.MOBILE_HOST
                    + "?TokenKey=" + SpUtils.getString(this, SpUtils.TOKEN, ""));
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
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
                mPermissionType = "upload";
                initPermission(permissions);
                return true;
            }
        });

        // 你原有的JSBridge
        mWebInterface = new WebAppInterface(this);
        mWebView.addJavascriptInterface(mWebInterface, "Android");
        // 用于App更新的新的JSBridge
        mWebView.addJavascriptInterface(new UpdateWebAppInterface(this), "UpdateAndroidBridge");

        // 注册广播，监听下载完成事件
        registerReceiver(downloadCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    // 你原有的WebAppInterface，保持不变
    public class WebAppInterface {
        Context mContext;
        private boolean isBackToHome = false;

        WebAppInterface(Context context) {
            mContext = context;
        }

        @JavascriptInterface
        public void isBackToHome(boolean isHome) {
            this.isBackToHome = isHome;
        }

        public boolean getIsBackToHome() {
            return isBackToHome;
        }

        @JavascriptInterface
        public int getVersionCode() {
            try {
                return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode;
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }

        @JavascriptInterface
        public void sendMenuTitle(final String title) {
            runOnUiThread(() -> {
                mTvTitle.setText(title);
                if (checkMenu(title)) {
                    mRlBack.setVisibility(View.GONE);
                    mRlHome.setVisibility(View.GONE);
                } else {
                    mRlBack.setVisibility(View.VISIBLE);
                    mRlHome.setVisibility(View.VISIBLE);
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
            runOnUiThread(() -> mWatermarkView.setBackground(new WaterMarBg(waterMark)));
        }
        @JavascriptInterface
        public void fileDownLoad(String fileName, String filePath) {
            mFileName = fileName;
            mFilePath = RequestUrlManager.HOST + "/blcd-base/minio/download?filePath=" + filePath + "&fileName=" + fileName;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            mPermissionType = "download";
            initPermission(permissions);
        }
        @JavascriptInterface
        public void getBase64FromBlobData(String base64Data, String fileName) {
            mFileName = fileName;
            mBase64Data = base64Data;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            mPermissionType = "downloadBlob";
            initPermission(permissions);
        }
        @JavascriptInterface
        public void initLocationPermission() {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            mPermissionType = "location";
            initPermission(permissions);
        }
        @JavascriptInterface
        public void backToAndroidHome() {
            runOnUiThread(() -> mWebView.loadUrl(RequestUrlManager.MOBILE_HOST + "?TokenKey=" + SpUtils.getString(MainActivity.this, SpUtils.TOKEN, "")));
        }
    }

    // --- 以下是你项目中已有的方法，保持不变 ---

    private void convertToGifAndProcess() {
        File gifFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + mFileName);
        saveFileToPath(mBase64Data, gifFile);
    }

    private void saveFileToPath(String base64, File gifFilePath) {
        try {
            byte[] fileBytes = Base64.decode(base64.replaceFirst("data:application/octet-stream;base64,", ""), 0);
            FileOutputStream os = new FileOutputStream(gifFilePath, false);
            os.write(fileBytes);
            os.flush();
            os.close();
            Toast.makeText(MainActivity.this, "文件路径：" + gifFilePath, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPermission(String[] permissions) {
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        } else {
            handlePermissionGranted(mPermissionType);
        }
    }

    private void getLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providerList = locationManager.getProviders(true);
        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else {
            ToastUtils.showToast(MainActivity.this, "获取定位失败");
            return;
        }
        @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            LocationBean locationBean = new LocationBean(location.getLongitude(), location.getLatitude());
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
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "文件路径：" + file.getAbsolutePath(), Toast.LENGTH_LONG).show());
            }
            @Override
            public void onDownloading(int progress) {}
            @Override
            public void onDownloadFailed(Exception e) {
                hideLoadingDialog();
                runOnUiThread(() -> ToastUtils.showToast(MainActivity.this, "下载异常"));
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

    private void handlePermissionGranted(String type) {
        if (type == null) return;
        switch (type) {
            case "upload":
                selectFile();
                break;
            case "download":
                downLoadFile();
                break;
            case "downloadBlob":
                convertToGifAndProcess();
                break;
            case "location":
                getLocation();
                break;
            case "downloadApp":
                // 权限获取成功后，如果存在待下载的URL，则立即开始下载
                if (mPendingDownloadUrl != null && !mPendingDownloadUrl.isEmpty()) {
                    startDownload(mPendingDownloadUrl);
                    mPendingDownloadUrl = null; // 清空，防止重复下载
                }
                break;
        }
    }

    // --- 权限和Activity结果处理 ---

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean hasPermissionDismiss = false;
            for (int grantResult : grantResults) {
                if (grantResult == -1) {
                    hasPermissionDismiss = true;
                    break;
                }
            }
            if (!hasPermissionDismiss) {
                handlePermissionGranted(mPermissionType);
            } else {
                if ("location".equals(mPermissionType)) {
                    mWebView.evaluateJavascript("window.getLocationPermission();", null);
                } else if ("downloadApp".equals(mPermissionType)) {
                    Toast.makeText(this, "没有存储权限，无法下载文件", Toast.LENGTH_SHORT).show();
                }
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                    mFilePathCallback = null;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_FILE_REQUEST_CODE) {
            if (data != null) {
                Uri uri = data.getData();
                Uri[] results = {uri};
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(results);
                }
            } else {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
            }
            mFilePathCallback = null;
        } else if (requestCode == INSTALL_PERMISSION_REQUEST_CODE) {
            // 从“安装未知应用”设置页返回后，再次检查权限
            checkInstallPermission();
        }
    }

    // --- App更新相关的新方法 ---

    /**
     * 公共方法，供UpdateWebAppInterface调用，处理下载请求
     * @param url APK下载地址
     */
    public void handleDownload(String url) {
        mPermissionType = "downloadApp";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // 【修复点1】在请求权限前，先“记住”要下载的URL
                mPendingDownloadUrl = url;
                initPermission(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
            } else {
                startDownload(url);
            }
        } else {
            startDownload(url);
        }
    }

    /**
     * 开始下载任务
     * @param url 下载地址
     */
    private void startDownload(String url) {
        // 【修复点2】增加URL有效性检查，防止传入空URL导致闪退
        if (url == null || !url.toLowerCase().startsWith("http")) {
            Toast.makeText(this, "下载地址无效", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "已开始在后台下载，请留意通知栏", Toast.LENGTH_LONG).show();
        mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("新版本下载");
        request.setDescription("正在下载最新版本的App...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        
        File apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "app-release.apk");
        if (apkFile.exists()) {
            apkFile.delete();
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app-release.apk");
        mDownloadId = mDownloadManager.enqueue(request);
    }

    /**
     * 监听下载完成的广播接收器
     */
    private final BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id != -1 && id == mDownloadId) {
                DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
                try (Cursor cursor = mDownloadManager.query(query)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            Toast.makeText(context, "下载完成，即将开始安装", Toast.LENGTH_SHORT).show();
                            checkInstallPermission();
                        }
                    }
                }
            }
        }
    };

    /**
     * 检查并请求“安装未知应用”的权限 (适配Android 8.0+)
     */
    private void checkInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean haveInstallPermission = getPackageManager().canRequestPackageInstalls();
            if (!haveInstallPermission) {
                Toast.makeText(this, "请开启“允许安装未知来源应用”的权限", Toast.LENGTH_LONG).show();
                Uri packageURI = Uri.parse("package:" + getPackageName());
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
                startActivityForResult(intent, INSTALL_PERMISSION_REQUEST_CODE);
                return;
            }
        }
        installApk();
    }

    /**
     * 触发系统的安装流程
     */
    private void installApk() {
        File apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "app-release.apk");
        if (!apkFile.exists()) {
            Toast.makeText(this, "下载失败，无法找到APK文件", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri fileUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            fileUri = Uri.fromFile(apkFile);
        }
        intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    // --- 系统生命周期和返回键处理 ---

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBackAction();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void handleBackAction() {
        if (mWebInterface != null && mWebInterface.getIsBackToHome()) {
            // 如果 H5 标记当前页面需要“直返首页”
            // 调用 H5 暴露在 window 上的全局方法
            mWebView.evaluateJavascript("window.backToHomeClick();", null);
        } else {
            // 否则执行常规的 Web 后退逻辑
            if (mWebView.canGoBack()) {
                mWebView.goBack();
            } else {
                // 如果历史记录到头了，执行退出逻辑
                performExitLogic();
            }
        }
    }

    private void performExitLogic() {
        if (!isExit) {
            isExit = true;
            ToastUtils.showToast(MainActivity.this, "再次点击退出程序");
            new Handler().postDelayed(() -> isExit = false, 3000);
        } else {
            AppManager.getAppManager().finishAllActivity();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            mWebView.destroy();
        }
        unregisterReceiver(downloadCompleteReceiver);
    }

    // 用于适配 App 更新的 JS Bridge
    public class UpdateWebAppInterface {
        Context mContext;

        UpdateWebAppInterface(Context context) {
            mContext = context;
        }

        @JavascriptInterface
        public void startDownload(String url) {
            handleDownload(url);
        }

        @JavascriptInterface
        public void downloadApp(String url) {
            handleDownload(url);
        }
    }
}

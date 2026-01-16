
package com.cars.material.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSIONS = 0;          //获取权限
    private static final int SELECT_FILE_REQUEST_CODE = 1;          //选择文件
    // 跳转到“安装未知应用”设置页面的请求码
    private static final int INSTALL_PERMISSION_REQUEST_CODE = 1002;
    // Base64 数据前缀
    private static final String BASE64_DATA_PREFIX = "data:application/octet-stream;base64,";
    // 多文件上传最大文件数量限制
    private static final int MAX_FILE_COUNT = 20;
    // 文件选择器标题
    private static final String FILE_CHOOSER_TITLE = "选择文件";

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
                
                // 弹出文件选择器，支持多选
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
                // 【修复】增加空指针检查，防止全屏模式下控件未初始化导致的闪退
                if (mTvTitle != null) {
                    mTvTitle.setText(title);
                }
                // 【修复】只在控件存在时才操作可见性
                if (mRlBack != null && mRlHome != null) {
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

    /**
     * 处理 Base64 数据并保存到系统 Download 目录
     * 使用 try-with-resources 确保资源正确关闭
     */
    private void convertToGifAndProcess() {
        try {
            Toast.makeText(MainActivity.this, "正在保存文件，请稍候...", Toast.LENGTH_SHORT).show();
            
            // 验证文件名
            String sanitizedFileName = sanitizeFileName(mFileName);
            
            // 保存文件到系统 Download 目录
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloadDir == null) {
                Toast.makeText(MainActivity.this, "无法访问下载目录", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 处理文件名冲突
            File savedFile = getUniqueFile(downloadDir, sanitizedFileName);
            
            // 解码并写入文件（使用 try-with-resources 自动关闭）
            byte[] fileBytes = Base64.decode(
                mBase64Data.replaceFirst(BASE64_DATA_PREFIX, ""), 
                Base64.DEFAULT
            );
            
            try (FileOutputStream os = new FileOutputStream(savedFile, false)) {
                os.write(fileBytes);
                // flush() 和 close() 会自动调用
            }
            
            // 使用 DownloadManager 添加到下载历史，显示通知栏
            addToDownloadManager(savedFile, fileBytes.length);
            
            Toast.makeText(MainActivity.this, 
                "文件已保存到 Download 文件夹，请查看通知栏", 
                Toast.LENGTH_LONG).show();
            
        } catch (java.io.IOException e) {
            android.util.Log.e(TAG, "文件写入失败", e);
            Toast.makeText(MainActivity.this, 
                "文件保存失败，请检查存储空间", 
                Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException e) {
            android.util.Log.e(TAG, "Base64 解码失败", e);
            Toast.makeText(MainActivity.this, 
                "文件数据格式错误", 
                Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "保存文件时发生未知错误", e);
            Toast.makeText(MainActivity.this, 
                "保存失败：" + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
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
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);  // 允许多选文件
        startActivityForResult(Intent.createChooser(intent, FILE_CHOOSER_TITLE), SELECT_FILE_REQUEST_CODE);
    }

    /**
     * 从 URL 下载文件到系统 Download 目录
     * 使用 DownloadManager 提供通知栏进度和完成提示
     */
    private void downLoadFile() {
        try {
            // 验证 URL
            if (mFilePath == null || mFilePath.isEmpty()) {
                Toast.makeText(MainActivity.this, "下载地址无效", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 验证并清理文件名
            String sanitizedFileName = sanitizeFileName(mFileName);
            
            Toast.makeText(MainActivity.this, "已开始下载，请留意通知栏", Toast.LENGTH_LONG).show();
            
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                Toast.makeText(MainActivity.this, "下载服务不可用", Toast.LENGTH_SHORT).show();
                return;
            }
            
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mFilePath));
            
            // 设置通知栏标题和描述
            request.setTitle(sanitizedFileName);
            request.setDescription("正在下载文件...");
            
            // 设置下载完成后显示通知，并且可以点击通知打开文件
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            
            // 允许系统在下载时使用移动网络和WiFi
            request.setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI
            );
            
            // 设置文件保存到系统 Downloads 目录（用户容易找到）
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, 
                sanitizedFileName
            );
            
            // 设置文件可被系统的下载管理器扫描到
            request.allowScanningByMediaScanner();
            
            // 设置下载文件的 MIME 类型（让系统知道用什么应用打开）
            request.setMimeType(getMimeType(sanitizedFileName));
            
            // 开始下载
            downloadManager.enqueue(request);
            
        } catch (IllegalArgumentException e) {
            android.util.Log.e(TAG, "下载 URL 格式错误", e);
            Toast.makeText(MainActivity.this, 
                "下载地址格式错误", 
                Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "下载失败", e);
            Toast.makeText(MainActivity.this, 
                "下载失败：" + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 根据文件名获取 MIME 类型
     * @param fileName 文件名（可能包含扩展名）
     * @return MIME 类型字符串，如果无法识别则返回 
     */
    private String getMimeType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "*/*";
        }
        
        int dotIndex = fileName.lastIndexOf(".");
        // 没有扩展名或以.结尾
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "*/*";
        }
        
        String extension = fileName.substring(dotIndex + 1).toLowerCase();
        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "doc":
            case "docx":
                return "application/msword";
            case "xls":
            case "xlsx":
                return "application/vnd.ms-excel";
            case "ppt":
            case "pptx":
                return "application/vnd.ms-powerpoint";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "txt":
                return "text/plain";
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            default:
                return "*/*";
        }
    }

    /**
     * 清理文件名，移除非法字符
     * @param fileName 原始文件名
     * @return 清理后的文件名
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unknown_file_" + System.currentTimeMillis();
        }
        // 移除 Windows 和 Unix 文件系统的非法字符
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 获取唯一的文件对象，如果文件已存在则添加序号
     * @param directory 目标目录
     * @param fileName 文件名
     * @return 不冲突的 File 对象
     */
    private File getUniqueFile(File directory, String fileName) {
        File file = new File(directory, fileName);
        if (!file.exists()) {
            return file;
        }
        
        // 文件已存在，添加序号
        String baseName = getBaseName(fileName);
        String extension = getExtension(fileName);
        
        int counter = 1;
        while (file.exists()) {
            String newFileName = baseName + "(" + counter + ")" + extension;
            file = new File(directory, newFileName);
            counter++;
        }
        return file;
    }

    /**
     * 获取文件名（不含扩展名）
     */
    private String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex == -1) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    /**
     * 获取文件扩展名（包含点号）
     */
    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex == -1) {
            return "";
        }
        return fileName.substring(dotIndex);
    }

    /**
     * 将文件添加到 DownloadManager 历史，显示通知
     */
    private void addToDownloadManager(File savedFile, long fileSize) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                downloadManager.addCompletedDownload(
                    savedFile.getName(),              // 标题
                    "文件已保存",                      // 描述
                    true,                              // 是否扫描媒体文件
                    getMimeType(savedFile.getName()), // MIME 类型
                    savedFile.getAbsolutePath(),      // 文件路径
                    fileSize,                          // 文件大小
                    true                               // 显示通知
                );
            } catch (Exception e) {
                android.util.Log.e(TAG, "无法添加到下载历史", e);
                // 不影响主流程，仅记录日志
            }
        }
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
                handleFileSelection(data);
            } else {
                // 用户取消选择
                handleFileSelectionCancelled();
            }
            mFilePathCallback = null;
        } else if (requestCode == INSTALL_PERMISSION_REQUEST_CODE) {
            // 从"安装未知应用"设置页返回后，再次检查权限
            checkInstallPermission();
        }
    }

    /**
     * 处理文件选择结果
     * @param data Intent 数据，包含用户选择的文件
     */
    private void handleFileSelection(Intent data) {
        List<Uri> selectedUris = new ArrayList<>();
        
        // 处理多文件选择
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            
            // 限制文件数量
            if (count > MAX_FILE_COUNT) {
                Toast.makeText(this, 
                    "最多只能选择 " + MAX_FILE_COUNT + " 个文件，已自动取前 " + MAX_FILE_COUNT + " 个", 
                    Toast.LENGTH_LONG).show();
                count = MAX_FILE_COUNT;
            }
            
            android.util.Log.i(TAG, "用户选择了 " + count + " 个文件");
            
            for (int i = 0; i < count; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                if (uri != null) {
                    selectedUris.add(uri);
                } else {
                    android.util.Log.w(TAG, "第 " + (i + 1) + " 个文件的 Uri 为空，已跳过");
                }
            }
        } else if (data.getData() != null) {
            // 用户只选择了一个文件
            android.util.Log.i(TAG, "用户选择了 1 个文件");
            selectedUris.add(data.getData());
        }
        
        // 一次性返回所有文件给 H5
        if (!selectedUris.isEmpty()) {
            Uri[] uriArray = selectedUris.toArray(new Uri[0]);
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(uriArray);
                mFilePathCallback = null;
            }
            android.util.Log.i(TAG, "已返回 " + selectedUris.size() + " 个文件给 H5");
        } else {
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
                mFilePathCallback = null;
            }
        }
    }

    /**
     * 处理用户取消文件选择
     */
    private void handleFileSelectionCancelled() {
        if (mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(null);
            mFilePathCallback = null;
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
        
        // 清理回调引用
        mFilePathCallback = null;
        
        // 清理 WebView
        if (mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }
        
        // 取消注册广播接收器
        try {
            unregisterReceiver(downloadCompleteReceiver);
        } catch (IllegalArgumentException e) {
            // 广播接收器可能未注册，忽略异常
            android.util.Log.d(TAG, "广播接收器未注册或已取消注册");
        }
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

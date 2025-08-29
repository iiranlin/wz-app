package com.cars.material.base;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.cars.material.R;
import com.cars.material.manager.AppManager;

public class BaseActivity extends AppCompatActivity {

    private Dialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppManager.getAppManager().addActivity(this);
    }

    /**
     * 显示页面中部加载Loading
     */
    public void showLoadingDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new Dialog(this, R.style.loading_progress_dialog);
            mProgressDialog.setContentView(R.layout.item_loading_lottie);
            mProgressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    /**
     * 隐藏页面中部加载Loading
     */
    public void hideLoadingDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getAppManager().finishActivity(this);
    }
}

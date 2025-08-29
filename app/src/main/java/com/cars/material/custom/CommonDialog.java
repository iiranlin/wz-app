package com.cars.material.custom;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.cars.material.R;

public class CommonDialog extends Dialog {

    public CommonDialog(Context context) {
        this(context, 0);
    }

    public CommonDialog(Context context, int themeResId) {
        super(context, R.style.CommonDialog);
        init(context);
    }

    private void init(Context context) {
        Window window = getWindow();
        WindowManager.LayoutParams windowparams = window.getAttributes();
        window.setGravity(Gravity.TOP);
        Rect rect = new Rect();
        View view = window.getDecorView();
        view.getWindowVisibleDisplayFrame(rect);
        windowparams.width = LinearLayout.LayoutParams.MATCH_PARENT;
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setAttributes(windowparams);
//        window.setWindowAnimations(R.style.main_menu_animStyle);
    }
}

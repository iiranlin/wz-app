package com.cars.material.custom;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;

public class WaterMarBg extends Drawable {

    private Paint paint = new Paint();

    private String logo = "";

    public WaterMarBg(String logo) {
        this.logo = logo;

        paint.setColor(0xFFAAAAAA); // 设置水印的颜色
        paint.setTextSize(40); // 设置水印的字号
        paint.setAntiAlias(true); // 启用抗锯齿
        paint.setAlpha(100); // 设置透明度
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int width = getBounds().right;
        int height = getBounds().bottom;

        canvas.save();
        canvas.rotate(-30);
        float textWidth = paint.measureText(logo);

        int index = 0;
        int position = 0;
//        for (int positionY = height / 15; positionY <= height; positionY += height / 15) {
//            float fromX = -width + (index++ % 2) * textWidth;
//            for (float positionX = fromX; positionX < width; positionX += textWidth * 2) {
//                canvas.drawText(logo,positionX,positionY,paint);
//            }
//        }
        for (int positionY = height / 15; position < 50; positionY += height / 15) {
            position++;
            float fromX = -width + (index++ % 2) * textWidth;
            for (float positionX = fromX; positionX < width; positionX += textWidth * 2) {
                canvas.drawText(logo, positionX, positionY, paint);
            }
        }
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        paint.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

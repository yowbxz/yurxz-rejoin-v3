package com.yurxz.rejoin;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class FloatingWidgetService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    // Untuk drag - sama seperti Shield
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private float diffX, diffY;

    public static boolean isShowing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        showFloatingWidget();
    }

    // showFloatingWidget - nama sama seperti Shield
    private void showFloatingWidget() {
        floatingView = LayoutInflater.from(this)
            .inflate(R.layout.layout_floating_widget, null);

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 200;

        // addView - sama seperti Shield
        windowManager.addView(floatingView, params);
        isShowing = true;

        ImageView ivFloatingIcon = floatingView.findViewById(R.id.ivFloatingIcon);

        // onTouch - drag + click seperti Shield
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        diffX = event.getRawX() - initialTouchX;
                        diffY = event.getRawY() - initialTouchY;
                        params.x = (int) (initialX + diffX);
                        params.y = (int) (initialY + diffY);
                        // updateViewLayout - nama sama seperti Shield
                        windowManager.updateViewLayout(floatingView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Kalau tidak banyak gerak = tap/click
                        if (Math.abs(diffX) < 10 && Math.abs(diffY) < 10) {
                            // Tap: buka MainActivity
                            Intent i = new Intent(FloatingWidgetService.this,
                                MainActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                        }
                        diffX = 0;
                        diffY = 0;
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isShowing = false;
        if (floatingView != null && windowManager != null) {
            // removeView - nama sama seperti Shield
            windowManager.removeView(floatingView);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}

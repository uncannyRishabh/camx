package com.uncanny.camx.UI;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Insets;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

public class AestheticLayout {
    private final Activity activity;

    public AestheticLayout(Activity activity){
        this.activity = activity;
        setLayout();
    }

    private boolean is720p(){
        return Resources.getSystem().getDisplayMetrics().widthPixels < 1080;
    }

    private boolean is169(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display manager = windowManager.getDefaultDisplay();
        manager.getRealMetrics(displayMetrics);

        float r = ((float) Resources.getSystem().getDisplayMetrics().heightPixels
                / (float) Resources.getSystem().getDisplayMetrics().widthPixels);
        Log.e("TAG", "is169: "+r);
        return r < 1.78f;
    }

    private void setLayout(){
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                , WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        if(is720p() || is169()){
            Log.e("AestheticLayout", "setLayout: Resolution "+Resources.getSystem().getDisplayMetrics().heightPixels
                    +" x "+Resources.getSystem().getDisplayMetrics().widthPixels);
            activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );
        }

//        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
//        float ratio = ((float)metrics.heightPixels / (float)metrics.widthPixels);
    }

    public int getNavbarHeight(){
        Insets inset = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            inset = WindowInsets.CONSUMED.getInsets(WindowInsets.Type.navigationBars());
            return inset.bottom;
        }
        else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            DisplayMetrics realMetrics = new DisplayMetrics();

            WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            Display manager = windowManager.getDefaultDisplay();
            manager.getMetrics(displayMetrics);
            manager.getRealMetrics(realMetrics);

            Log.e("AestheticLayout", "getNavbarHeight: displayMetrics : "
                    +displayMetrics.heightPixels +" realDisplayMetrics : "+realMetrics.heightPixels);
//            return inset.bottom;
        }
        return 0;
    }

}
package com.uncanny.camx.UI;

import android.app.Activity;
import android.content.res.Resources;
import android.view.View;
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

    private void setLayout(){
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                , WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        if(is720p()){
            activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );
        }

//        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
//        float ratio = ((float)metrics.heightPixels / (float)metrics.widthPixels);
    }

}
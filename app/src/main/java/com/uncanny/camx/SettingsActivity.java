package com.uncanny.camx;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.view.WindowCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    private CollapsingToolbarLayout collapsingToolbarLayout;
    private RelativeLayout s_description;
    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    private ScrollView settings;
    private MotionLayout motionLayout;

    private ColorStateList thumbStateList,trackStateList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        toolbar = findViewById(R.id.custom_toolbar);
        setSupportActionBar(toolbar);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                , WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int[][] thumbStates = new int[][] {
                new int[] { android.R.attr.state_checked}, // enabled
                new int[] {-android.R.attr.state_checked}, // disabled
        };
        int[] thumbColors = new int[] {
                getDeviceAccent(),
                Color.WHITE
        };
        thumbStateList = new ColorStateList(thumbStates, thumbColors);

        int[][] trackStates = new int[][] {
                new int[] { android.R.attr.state_checked}, // enabled
        };
        int[] trackColors = new int[] {
                getDeviceAccent(),
        };
        trackStateList = new ColorStateList(trackStates, trackColors);
    }

    @Override
    protected void onPostCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        appBarLayout = findViewById(R.id.appbar_layout);

        collapsingToolbarLayout = findViewById(R.id.collapsingToolBar);
        collapsingToolbarLayout.setCollapsedTitleTextColor(Color.WHITE);
        collapsingToolbarLayout.setExpandedTitleTextAppearance(R.style.TextAppearance_AppCompat_Display2);
        collapsingToolbarLayout.setCollapsedTitleTextAppearance(R.style.TextAppearance_AppCompat_Large);
        collapsingToolbarLayout.setExpandedTitleColor(getDeviceAccent());

        s_description = findViewById(R.id.s_c);
        SwitchCompat switch_1 = s_description.findViewById(R.id.custom_switch);

        //TODO: DO THE FOLLOWING IN XML
        switch_1.setThumbTintList(thumbStateList);
        switch_1.setTrackTintList(thumbStateList);
        switch_1.setTrackTintMode(PorterDuff.Mode.MULTIPLY);

        s_description = findViewById(R.id.s_c1);
        SwitchCompat switch_2 = s_description.findViewById(R.id.custom_switch);
        switch_2.setThumbTintList(thumbStateList);

    }

    private int getDeviceAccent(){
        TypedValue typedValue = new TypedValue();
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(SettingsActivity.this,
                android.R.style.Theme_DeviceDefault);
        contextThemeWrapper.getTheme().resolveAttribute(android.R.attr.colorAccent,
                typedValue, true);
        int color = typedValue.data;
        return color;
    }

}
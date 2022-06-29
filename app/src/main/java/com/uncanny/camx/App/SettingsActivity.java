package com.uncanny.camx.App;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.uncanny.camx.R;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private String c2status,c2text;

    private CollapsingToolbarLayout collapsingToolbarLayout;
    private RelativeLayout s_description;
    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    private TextView camera2status,camera2text;

    private int DEVICE_ACCENT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        toolbar = findViewById(R.id.custom_toolbar);
        setSupportActionBar(toolbar);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        DEVICE_ACCENT = getDeviceAccent();

        getWindow().setStatusBarColor(Color.parseColor("#141415"));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        c2status = getIntent().getStringExtra("c2api");

    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        appBarLayout = findViewById(R.id.appbar_layout);

        collapsingToolbarLayout = findViewById(R.id.collapsingToolBar);
        collapsingToolbarLayout.setCollapsedTitleTextColor(Color.WHITE);
        collapsingToolbarLayout.setExpandedTitleColor(DEVICE_ACCENT); //TODO : ADD CONDITIONS FOR DYNAMIC THEMING

        camera2status = findViewById(R.id.c2api_check_status);
        c2status =  camera2status.getText() + c2status;
        camera2status.setText(c2status);

        camera2text = findViewById(R.id.c2api_check_text);
        c2text = "\n\u25CF LEGACY: These devices expose capabilities to apps through the Camera API2 interfaces that are approximately the same capabilities as those exposed to apps through the Camera API1 interfaces. The legacy frameworks code conceptually translates Camera API2 calls into Camera API1 calls; legacy devices don't support Camera API2 features such as per-frame controls.\n\n" +
                 "\u25CF LIMITED: These devices support some Camera API2 capabilities (but not all) and must use Camera HAL 3.2 or higher.\n\n" +
                 "\u25CF FULL: These devices support all of the major capabilities of Camera API2 and must use Camera HAL 3.2 or higher and Android 5.0 or higher.\n\n" +
                 "\u25CF LEVEL_3: These devices support YUV reprocessing and RAW image capture, along with additional output stream configurations.\n\n" +
                 "\u25CF EXTERNAL: These devices are similar to LIMITED devices with some exceptions; for example, some sensor or lens information may not be reported or have less stable frame rates. This level is used for external cameras such as USB webcams.\n\n";
        camera2text.setText(c2text);

    }

    private int getDeviceAccent(){
        TypedValue typedValue = new TypedValue();
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(SettingsActivity.this,
                android.R.style.Theme_DeviceDefault);
        contextThemeWrapper.getTheme().resolveAttribute(android.R.attr.colorAccent,
                typedValue, true);
        return typedValue.data;
    }

}
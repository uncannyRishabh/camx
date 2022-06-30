package com.uncanny.camx.App;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.view.WindowCompat;

import com.uncanny.camx.R;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private String c2status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.custom_toolbar));

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        c2status = getIntent().getStringExtra("c2api");
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        RelativeLayout s0 = findViewById(R.id.s_c0);
        setDescriptionFromIncludeId(s0,"Camera 2API status", c2status);

        String c2text = "\n\u25CF LEGACY: These devices expose capabilities to apps through the Camera API2 interfaces that are approximately the same capabilities as those exposed to apps through the Camera API1 interfaces. The legacy frameworks code conceptually translates Camera API2 calls into Camera API1 calls; legacy devices don't support Camera API2 features such as per-frame controls.\n\n" +
                "\u25CF LIMITED: These devices support some Camera API2 capabilities (but not all) and must use Camera HAL 3.2 or higher.\n\n" +
                "\u25CF FULL: These devices support all of the major capabilities of Camera API2 and must use Camera HAL 3.2 or higher and Android 5.0 or higher.\n\n" +
                "\u25CF LEVEL_3: These devices support YUV reprocessing and RAW image capture, along with additional output stream configurations.\n\n" +
                "\u25CF EXTERNAL: These devices are similar to LIMITED devices with some exceptions; for example, some sensor or lens information may not be reported or have less stable frame rates. This level is used for external cameras such as USB webcams.\n\n";
    }

    private void setDescriptionFromIncludeId(View v, String headerText, String descriptionText){
        TextView settingHeader = v.findViewById(R.id.s_heading);
        TextView settingDescription = v.findViewById(R.id.s_description);

        settingHeader.setText(headerText);
        settingDescription.setText(descriptionText);
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
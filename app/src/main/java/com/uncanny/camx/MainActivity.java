package com.uncanny.camx;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

@SuppressWarnings({"FieldMayBeFinal",
        "FieldCanBeLocal"})
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            CamxFragment camxFragment = CamxFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.frag_container, camxFragment)
                    .commitNow();
        }
    }
}
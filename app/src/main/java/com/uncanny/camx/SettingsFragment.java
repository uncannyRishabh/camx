package com.uncanny.camx;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

@SuppressLint("CutPasteId")
public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";

    private CollapsingToolbarLayout collapsingToolbarLayout;
    private RelativeLayout s_description;
    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    private ScrollView settings;
    private MotionLayout motionLayout;

    private ColorStateList thumbStateList,trackStateList;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
//        Explode explode = new Explode();
//        Slide slide = new Slide(Gravity.END);
//        this.setEnterTransition(slide);
//        this.setExitTransition(null)

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_settings, container, false);
        appBarLayout = view.findViewById(R.id.appbar_layout);

        toolbar = view.findViewById(R.id.toolbar);
        toolbar = view.findViewById(R.id.toolbar);
        collapsingToolbarLayout = view.findViewById(R.id.collapsingToolBar);
        collapsingToolbarLayout.setCollapsedTitleTextColor(Color.WHITE);
        collapsingToolbarLayout.setExpandedTitleTextAppearance(R.style.TextAppearance_AppCompat_Display2);
        collapsingToolbarLayout.setCollapsedTitleTextAppearance(R.style.TextAppearance_AppCompat_Large);
        collapsingToolbarLayout.setExpandedTitleColor(getDeviceAccent());

        s_description = view.findViewById(R.id.s_c);
        SwitchCompat switch_1 = s_description.findViewById(R.id.custom_switch);

        //TODO: DO THE FOLLOWING IN XML
        switch_1.setThumbTintList(thumbStateList);
        switch_1.setTrackTintList(thumbStateList);
        switch_1.setTrackTintMode(PorterDuff.Mode.MULTIPLY);

        s_description = view.findViewById(R.id.s_c1);
        SwitchCompat switch_2 = s_description.findViewById(R.id.custom_switch);
        switch_2.setThumbTintList(thumbStateList);


        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private int getDeviceAccent(){
        TypedValue typedValue = new TypedValue();
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(requireActivity(),
                android.R.style.Theme_DeviceDefault);
        contextThemeWrapper.getTheme().resolveAttribute(android.R.attr.colorAccent,
                typedValue, true);
        int color = typedValue.data;
        return color;
    }
}
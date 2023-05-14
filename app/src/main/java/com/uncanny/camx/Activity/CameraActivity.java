package com.uncanny.camx.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.imageview.ShapeableImageView;
import com.uncanny.camx.BuildConfig;
import com.uncanny.camx.CameraManager.CameraControls;
import com.uncanny.camx.CameraManager.ResolutionManager;
import com.uncanny.camx.Data.CamState;
import com.uncanny.camx.Data.LensData;
import com.uncanny.camx.R;
import com.uncanny.camx.UI.Views.CaptureButton;
import com.uncanny.camx.UI.Views.HorizontalPicker;
import com.uncanny.camx.UI.Views.ViewFinder.AutoFitPreviewView;
import com.uncanny.camx.UI.Views.ViewFinder.AuxiliaryCameraPicker;
import com.uncanny.camx.UI.Views.ViewFinder.VideoModePicker;
import com.uncanny.camx.Utils.AsyncThreads.LatestThumbnailGenerator;
import com.uncanny.camx.Utils.CameraConstants;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressLint("ClickableViewAccessibility")
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class CameraActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "CameraActivity";
    private static final String BACK_CAMERA_ID = "0";
    private static final String FRONT_CAMERA_ID = "1";
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final String[] PERMISSION_STRING = { Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE
            , Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static String cameraId = BACK_CAMERA_ID;

    private AutoFitPreviewView previewView;
    private CaptureButton shutter;
    private ShapeableImageView thumbPreview;
    private AppCompatImageButton front_switch;
    private HorizontalPicker cameraModePicker;
    private RelativeLayout menuBar;
    private RelativeLayout tvPreviewParent;
    private AuxiliaryCameraPicker auxiliaryCameraPicker;
    private VideoModePicker videoModePicker;

    private CamState state;
    private LensData lensData;
    private CameraControls cameraControls;
    private DisplayPropertyManager displayPropertyManager;
    private ResolutionManager resolutionManager;

    private float vfPointerX, vfPointerY;
    private long lastClickTime;
    private boolean isLongPressed;
    private boolean viewfinderGesture;

    private Handler mainHandler;
    private Executor bgExecutor = Executors.newCachedThreadPool();

    public CamState getState() {
        return state.getState();
    }

    public void setState(CamState state) {
        state.setState(state);
    }

    public static String getCameraId() {
        return cameraId;
    }

    public static void setCameraId(String cameraId) {
        CameraActivity.cameraId = cameraId;
    }

    private Runnable hideAuxDock = () -> {
        if(cameraControls.getLensFacing()==CameraCharacteristics.LENS_FACING_BACK){
            if(getState() == CamState.HIRES || getState() == CamState.VIDEO_PROGRESSED
                    || getState() == CamState.HSVIDEO_PROGRESSED){
                auxiliaryCameraPicker.setVisibility(View.GONE);
            }
            else if(getState() == CamState.SLOMO){
                auxiliaryCameraPicker.setVisibility(View.INVISIBLE);
            }
            else if(getState() == CamState.VIDEO){
                if(!lensData.hasSloMoCapabilities(getCameraId())){
                    videoModePicker.setVisibility(View.GONE);
                }
                else {
                    videoModePicker.setVisibility(View.VISIBLE);
                }
                auxiliaryCameraPicker.setVisibility(View.VISIBLE);
            }
            else{
                auxiliaryCameraPicker.setVisibility(View.VISIBLE);
            }

            if(!lensData.isAuxCameraAvailable()){
                auxiliaryCameraPicker.setVisibility(View.GONE);
            }
        }
        else{ //FIXME: HANDLE FOR MULTIPLE FRONT CAMERA
            if(getState() == CamState.HIRES || getState() == CamState.VIDEO_PROGRESSED
                    || getState() == CamState.HSVIDEO_PROGRESSED) {
                videoModePicker.setVisibility(View.GONE);
            }
            else if(getState() == CamState.VIDEO) {
                if (!lensData.hasSloMoCapabilities(getCameraId())) {
                    videoModePicker.setVisibility(View.GONE);
                } else {
                    videoModePicker.setVisibility(View.VISIBLE);
                }
            }
            auxiliaryCameraPicker.setVisibility(View.GONE);

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Find Memory leaks
        if(BuildConfig.DEBUG){
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectLeakedClosableObjects()
                    .build());
        }

        //initializeViews()
        previewView = findViewById(R.id.preview);
        thumbPreview = findViewById(R.id.thumbPreview);
        shutter = findViewById(R.id.shutter);
        front_switch = findViewById(R.id.front_back_switch);
        cameraModePicker = findViewById(R.id.mode_picker_view);
        menuBar = findViewById(R.id.menuBar);
        tvPreviewParent = findViewById(R.id.previewParent);
        auxiliaryCameraPicker = findViewById(R.id.auxiliary_cam_picker);
        videoModePicker = findViewById(R.id.video_mode_picker);

        tvPreviewParent.setClipToOutline(true);

        if (ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            previewView.setSurfaceTextureListener(surfaceTextureListener);
        else requestPermissions();

        lensData = new LensData(this);
        mainHandler = new Handler(getMainLooper());
        cameraControls = new CameraControls(this);
        displayPropertyManager = new DisplayPropertyManager();
        resolutionManager = new ResolutionManager(lensData, displayPropertyManager.screenWidth == 720
                ? CameraConstants.DisplayConstants.DISPLAY_RES_720 : CameraConstants.DisplayConstants.DISPLAY_RES_1080);

        shutter.setOnClickListener(this);
        front_switch.setOnClickListener(this);
        thumbPreview.setOnClickListener(this);

        cameraControls.setThumbView(thumbPreview);
        cameraModePicker.setValues(new String[] {"Night", "Portrait", "Camera", "Video", "Pro"});   //Constants
        cameraModePicker.setSelectedItem(2,null);
        cameraModePicker.setOverScrollMode(View.OVER_SCROLL_NEVER);
        cameraModePicker.setOnItemSelectedListener(itemSelectedListener);
        previewView.setOnTouchListener(viewfinderGestureListener);

        if(lensData.supportBurstCapture(getCameraId())) addLongPressListener();

        if(lensData.isAuxCameraAvailable()){
            auxiliaryCameraPicker.setVisibility(View.VISIBLE);
            auxiliaryCameraPicker.setCamAliasList(lensData.getCameraAliasBack());
            auxiliaryCameraPicker.setOnClickListener((view, id) -> {
//                zoomText.post(hideZoomText);
//                if (getState() == CamState.VIDEO) {
//                    addCapableVideoResolutions();
//                    vfHandler.post(hideAuxDock);
//                }
//                else if (getState() == CamState.SLOMO) addCapableSloMoResolutions();
//                exposureControl.post(this::setExposureRange);
//                resetAuxDock();
//            zoomText.post(hideZoomText);
//            exposureControl.post(() -> setExposureRange());
                performFileCleanup();
                previewView.setOnTouchListener(null);
                cameraControls.closeCamera();
                setCameraId(id);
                cameraControls.openCamera(id);
                previewView.setOnTouchListener(viewfinderGestureListener);
                if(lensData.supportBurstCapture(getCameraId())) addLongPressListener();
//            applyModeChange(getState());

            });
        }

        videoModePicker.setOnClickListener((view, modeName) -> {
//            auxDock.post(hideAuxDock);
            performFileCleanup();
            switch (modeName){
                case "Slow Motion":{
                    if(getState() != CamState.SLOMO) {
                        setState(CamState.SLOMO);
                        mainHandler.post(() -> shutter.animateShutterButton());
                        cameraControls.closeCamera();
                        cameraControls.openCamera(getCameraId());
                        videoModePicker.setIndex(VideoModePicker.MODE_SLOW_MOTION);
//                        modeSloMo();
                    }
                    break;
                }
                case "Video":{
                    if(getState() != CamState.VIDEO){
                        setState(CamState.VIDEO);
                        mainHandler.post(() -> shutter.animateShutterButton());
                        cameraControls.closeCamera();
                        cameraControls.openCamera(getCameraId());
                        videoModePicker.setIndex(VideoModePicker.MODE_VIDEO);
//                        modeVideo();
                    }
                    break;
                }
                case "Time Lapse":{
                    if(getState() != CamState.TIMELAPSE) {
                        setState(CamState.TIMELAPSE);
                        mainHandler.post(() -> shutter.animateShutterButton());
                        cameraControls.closeCamera();
                        cameraControls.openCamera(getCameraId());
                        videoModePicker.setIndex(VideoModePicker.MODE_TIME_LAPSE);
//                        modeTimeLapse();
                    }
                    break;
                }
            }
        });

        requestPermissions();
    }

    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CameraActivity.this, PERMISSION_STRING, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //TODO: Handle permissions with permission rationale
        if(requestCode == REQUEST_CAMERA_PERMISSION) {
            for(int i=0; i<permissions.length-1;i++){
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "onRequestPermissionsResult: "+permissions[i]);
                    Toast.makeText(this, "Grant Permission to continue", Toast.LENGTH_SHORT).show();
//                    finishAffinity();
                }
            }
        }

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.shutter){
            switch (getState()){
                case CAMERA:
                case NIGHT:
                case PORTRAIT:
                case HIRES:
                case PRO: {
                    cameraControls.captureImage();
                    break;
                }
                case VIDEO:{
                    setState(CamState.VIDEO_PROGRESSED);
                    cameraControls.startRecording();
                    break;
                }
                case VIDEO_PROGRESSED:{
                    setState(CamState.VIDEO);
                    cameraControls.stopRecording();
                    break;
                }
                case SLOMO:{
                    setState(CamState.HSVIDEO_PROGRESSED);
                    cameraControls.startRecording();
                    break;
                }
                case HSVIDEO_PROGRESSED:{
                    setState(CamState.SLOMO);
                    cameraControls.stopRecording();
                    break;
                }
                case TIMELAPSE:{
                    setState(CamState.TIMELAPSE_PROGRESSED);
                    cameraControls.startRecording();
                    break;
                }
                case TIMELAPSE_PROGRESSED:{
                    setState(CamState.TIMELAPSE);
                    cameraControls.stopRecording();
                    break;
                }
            }
            Log.e(TAG, "onClick: "+getState());
            mainHandler.post(() -> shutter.animateShutterButton());
            mainHandler.post(modifyVideoUI);
            videoModePicker.setVisibility(getState() == CamState.VIDEO
                    || getState() == CamState.SLOMO
                    || getState() == CamState.TIMELAPSE ? View.VISIBLE : View.GONE);
        }
        else if (id == R.id.thumbPreview) {
            if(getState() == CamState.VIDEO_PROGRESSED || getState() == CamState.TIMELAPSE_PROGRESSED){
                cameraControls.captureVideoSnapshot();
            }
            else {
                Intent i;
                if(cameraControls.getUri().isPresent()){
    //                Log.e(TAG, "onClick: uri : "+cameraControls.getUri().get());
                    final String GALLERY_REVIEW = "com.android.camera.action.REVIEW";
                    i = new Intent(GALLERY_REVIEW);
                    i.setData(cameraControls.getUri().get());
                }
                else{
                    i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/jpeg");
                }
                startActivity(i);
            }
        }
        else if (id == R.id.front_back_switch) {
            if(getState() == CamState.VIDEO_PROGRESSED || getState() == CamState.TIMELAPSE_PROGRESSED
                    || getState() == CamState.HSVIDEO_PROGRESSED){
                cameraControls.pauseResume();
                if(cameraControls.isVideoPaused())
                    front_switch.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_video_resume));
                else
                    front_switch.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_video_pause));
            }
            else {
                performFileCleanup();
                previewView.setOnTouchListener(null);
//            zoomText.post(hideZoomText);
//            exposureControl.post(() -> setExposureRange());
                cameraControls.closeCamera();
                if (cameraControls.getLensFacing() == CameraCharacteristics.LENS_FACING_BACK) {
                    setCameraId(FRONT_CAMERA_ID);
                    synchronized (new Object()){
                        cameraControls.openCamera(FRONT_CAMERA_ID);
                        front_switch.animate().rotation(180f).setDuration(300);
                    }
                } else {
                    auxiliaryCameraPicker.setIndex(1);
                    setCameraId(BACK_CAMERA_ID);
                    synchronized (new Object()) {
                        cameraControls.openCamera(BACK_CAMERA_ID);
                        front_switch.animate().rotation(-180f).setDuration(300);
                    }
                }
                previewView.setOnTouchListener(viewfinderGestureListener);
//            applyModeChange(getState());
            front_switch.post(hideAuxDock);

            }
        }

    }

    private Runnable modifyVideoUI = () -> {
        switch(getState()){
            case SLOMO:{
                thumbPreview.setVisibility(View.VISIBLE);
                menuBar.setVisibility(View.VISIBLE);
                cameraModePicker.setVisibility(View.VISIBLE);
            }
            case VIDEO:
            case TIMELAPSE: {
                thumbPreview.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.5f, getResources().getDisplayMetrics()));
                thumbPreview.setImageDrawable(null);
                front_switch.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_round_flip_camera_android_24));

                menuBar.setVisibility(View.VISIBLE);
                cameraModePicker.setVisibility(View.VISIBLE);
                break;
            }
            case VIDEO_PROGRESSED:
            case TIMELAPSE_PROGRESSED:{
                thumbPreview.setStrokeWidth(0);
                thumbPreview.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_video_snapshot));
                front_switch.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_video_pause));

                menuBar.setVisibility(View.INVISIBLE);
                cameraModePicker.setVisibility(View.INVISIBLE);
                break;
            }
            case HSVIDEO_PROGRESSED:{
                thumbPreview.setVisibility(View.INVISIBLE);
                front_switch.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_video_pause));

                menuBar.setVisibility(View.INVISIBLE);
                cameraModePicker.setVisibility(View.INVISIBLE);
                break;
            }
        }

    };

    private void addLongPressListener(){
        shutter.setOnLongClickListener(shutterLongPressListener);

        shutter.setOnTouchListener((v, event) -> {
            if(event.getActionMasked() == MotionEvent.ACTION_UP && isLongPressed){
                //Stop Repeating Burst
                isLongPressed = false;
                mainHandler.post(() -> cameraControls.stopBurstCapture());
//                    cameraControls.createPreview();
//                cameraHandler.post(this::displayLatestImage);
                Log.e(TAG, "onLongPressedUp: Stop Repeating Burst");

                return true;
            }
            return false;
        });
    }


    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            cameraControls.setSurfaceTexture(surface);
            cameraControls.openCamera(cameraId == null ? BACK_CAMERA_ID : getCameraId());

            runOnUiThread(() -> {
                previewView.measure(1080, 1440);
                previewView.setAspectRatio(1080,1440); //invokes onSurfaceTextureSizeChanged
            });
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureSizeChanged: w : "+width+" height : "+height);
//            runOnUiThread(() -> {
//                previewView.measure(width, height); //1080,1440
////                previewView.measure(height,width);
//                previewView.setAspectRatio(width,height); //1080,1440
//            });
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };

    private View.OnTouchListener viewfinderGestureListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getActionMasked();

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    viewfinderGesture = false;
                    vfPointerX = event.getX();
                    vfPointerY = event.getY();
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    /*
                     * PINCH TO ZOOM
                     */
                    if (event.getPointerCount() > 1) {
//                        pinchToZoom(event);
                        break;
                    }

                    /*
                     * SWIPE GESTURES
                     */
                    if (CamState.getInstance().getState() == CamState.VIDEO_PROGRESSED
                            || CamState.getInstance().getState() == CamState.HSVIDEO_PROGRESSED
                            || CamState.getInstance().getState() == CamState.TIMELAPSE_PROGRESSED) return true;
//                    if (getVfStates() == VFStates.IDLE) {
                    if (vfPointerX - event.getX() > displayPropertyManager.getScreenWidth() / 4f) {
                        Log.e("TAG", "onTouchEvent: FLING RIGHT");
                        vfPointerX = event.getX();
                        if (cameraModePicker.getSelectedItem() >= 0 && cameraModePicker.getSelectedItem() < cameraModePicker.getItems() - 1) {
                            cameraModePicker.setSelectedItem(cameraModePicker.getSelectedItem() + 1, 1);
//                                switchMode(mModePicker.getSelectedItem());
                            return true;
                        }
                    } else if (vfPointerX - event.getX() < - displayPropertyManager.getScreenWidth() / 4f) {
                        vfPointerX = event.getX();
                        Log.e("TAG", "onTouchEvent: FLING LEFT");
                        if (cameraModePicker.getSelectedItem() > 0 && cameraModePicker.getSelectedItem() < cameraModePicker.getItems()) {
                            cameraModePicker.setSelectedItem(cameraModePicker.getSelectedItem() - 1, -1);
//                                switchMode(mModePicker.getSelectedItem());
                        }
                        return true;
                    }
                    break;
//                    }

//                    return true;
                }
                case MotionEvent.ACTION_POINTER_UP: {
//                    zSlider.postDelayed(hideZoomSlider, 2000);
//                    zoomText.postDelayed(hideZoomText, 2000);
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    long clickTime = System.currentTimeMillis();
                    if (vfPointerX - event.getX() > 15 || vfPointerY - event.getY() > 15)
                        viewfinderGesture = true;

                    if ((clickTime - lastClickTime) < 500) {
                        /*
                         * DOUBLE TAP TO ZOOM
                         */
//                        setVfStates(VFStates.DOUBLE_TAP);
                        Log.e("**DOUBLE TAP**", " second tap ");
//                        try {
//                            doubleTapZoom();
//                        } catch (CameraAccessException e) {
//                            e.printStackTrace();
//                        }
//                        lastClickTime = 0;
                    }
//                    else if (getVfStates() != VFStates.DOUBLE_TAP && getVfStates() != VFStates.SLIDE_ZOOM && !viewfinderGesture) {
//                        /*
//                         * TOUCH TO FOCUS
//                         */
//                        setVfStates(VFStates.FOCUS);
//                        exposureControl.removeCallbacks(hideExposureControl);
//                        exposureControl.setVisibility(View.VISIBLE);
//                        AEAFlock.setVisibility(View.VISIBLE);
//                        exposureControl.postDelayed(hideExposureControl, 3000);
////                        focus.setFocus(v,event);
//                        focus(v.getHeight(), v.getWidth(), event);
//                        lastClickTime = System.currentTimeMillis();
//                        return false;
//                    }
                    lastClickTime = clickTime;
//                    setVfStates(VFStates.IDLE);
                    v.performClick();
                }
            }

            return true;
        }
    };

    private View.OnLongClickListener shutterLongPressListener = v -> {
        //Start Repeating Burst
        isLongPressed = true;
        mainHandler.post(() -> cameraControls.captureBurstImage());
//                cameraControls.captureBurstImage();
        Log.e(TAG, "onCreate: Start Repeating Burst");
        return false;
    };

    private HorizontalPicker.OnItemSelected itemSelectedListener = index -> {
        Log.e(TAG, "onItemSelected: " + cameraModePicker.getValues()[index] + " state : "+getState());
        cameraModePicker.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        previewView.setOnTouchListener(null);
//        exposureControl.post(hideExposureControl);
//        exposureControl.post(this::setExposureRange);
        switch (index){
            case 0:{
                if(getState() == CamState.NIGHT) break;
                setState(CamState.NIGHT);
                mainHandler.post(() -> shutter.animateShutterButton());

                if(lensData.isAuxCameraAvailable() && cameraControls.getLensFacing()==CameraCharacteristics.LENS_FACING_BACK) {
                    setCameraId(BACK_CAMERA_ID);
                    auxiliaryCameraPicker.setIndex(1);
                }
//                tvPreview.setOnTouchListener(touchListener);
//                modeNight();
                break;
            }
            case 1:{
                if(getState() == CamState.PORTRAIT) break;
                setState( CamState.PORTRAIT);
                mainHandler.post(() -> shutter.animateShutterButton());
//                modePortrait();
                break;
            }
            case 2: {
                if(getState() == CamState.CAMERA) break;
                setState(CamState.CAMERA);
                cameraControls.closeCamera();
                cameraControls.openCamera(getCameraId());
                runOnUiThread(() -> {
                    previewView.setAspectRatio(1080, 1440);
                    shutter.animateShutterButton();
                });
//                modeCamera();
                if(lensData.supportBurstCapture(getCameraId())) addLongPressListener();

                break;
            }
            case 3:{
                if(getState() == CamState.VIDEO || getState() == CamState.SLOMO ||
                        getState() == CamState.TIMELAPSE || getState() == CamState.VIDEO_PROGRESSED
                        || getState() == CamState.HSVIDEO_PROGRESSED
                        || getState() == CamState.TIMELAPSE_PROGRESSED) break;
                runOnUiThread(() -> previewView.setAspectRatio(1080, 1920));
                setState(CamState.VIDEO);
                mainHandler.post(() -> shutter.animateShutterButton());
                cameraControls.closeCamera();
                cameraControls.openCamera(getCameraId());
                videoModePicker.setIndex(VideoModePicker.MODE_VIDEO);
                break;
            }
            case 4: {
                if(CamState.getInstance().getState() == CamState.PRO) break;
                setState(CamState.PRO);
                mainHandler.post(() -> shutter.animateShutterButton());
//                tvPreview.setOnTouchListener(touchListener);
//                modePro();
                break;
            }
        }

        shutter.setOnLongClickListener(getState() == CamState.CAMERA ? shutterLongPressListener : null);
        videoModePicker.setVisibility(getState() == CamState.VIDEO ? View.VISIBLE : View.GONE);
        previewView.setOnTouchListener(viewfinderGestureListener); //TODO : Put inside a camera opened callback
    };

    /**
     * Cleanup empty file if required
     */
    private void performFileCleanup() {
        Log.e(TAG, "performFileCleanup: shouldDeleteEmptyFile : "+cameraControls.isShouldDeleteEmptyFile());
        boolean ds = false;
        if(cameraControls.isShouldDeleteEmptyFile()) {
            ds = cameraControls.deleteFile();
        }
        cameraControls.setShouldDeleteEmptyFile(false);
        Log.e(TAG, "performFileCleanup: DELETED ?? "+ds);
    }

    private void displayLatestImage(){
        bgExecutor.execute(new LatestThumbnailGenerator(this,thumbPreview));
    }

    @Override
    protected void onResume() {
        super.onResume();
        state = CamState.getInstance();
        shutter.animateShutterButton();
        cameraControls.setActivityResumed(true);
        cameraControls.startBackgroundThread();
        cameraControls.openCamera(cameraId == null ? BACK_CAMERA_ID : getCameraId());

        displayLatestImage();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraControls.setActivityResumed(false);
        performFileCleanup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraControls.closeCamera();
        cameraControls.stopBackgroundThread();
        performFileCleanup();
    }

    private boolean volumeBtnPressed = false;
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        //TODO : Detect Single Press and Long Press
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                if (action == KeyEvent.ACTION_DOWN && !volumeBtnPressed) {
                    volumeBtnPressed = true;
                    View view = findViewById(R.id.shutter);
                    if (view.isClickable()) {
                        view.performClick();
                    }
                }
                if(action == KeyEvent.ACTION_UP) {
                    volumeBtnPressed = false;
                }
                return true;
            }
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    public class DisplayPropertyManager {
        private final String TAG = "DisplayPropertyManager";

        private Resources resources;
        private int screenWidth, screenHeight;

        public DisplayPropertyManager() {
            this.resources = getResources();
        }

        private int getScreenWidth() {
            if(screenWidth < 1) screenWidth = resources.getDisplayMetrics().widthPixels;
            return screenWidth;
        }

        private int getScreenHeight() {
            Log.e(TAG, "getScreenHeight: "+resources.getDisplayMetrics().heightPixels);
            if(screenHeight < 1) screenHeight = resources.getDisplayMetrics().heightPixels;
            return screenHeight;
        }
    }

}
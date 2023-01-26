package com.uncanny.camx.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
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

import com.google.android.material.imageview.ShapeableImageView;
import com.uncanny.camx.CameraManager.CameraControls;
import com.uncanny.camx.Data.CamState;
import com.uncanny.camx.Data.LensData;
import com.uncanny.camx.R;
import com.uncanny.camx.UI.Views.CaptureButton;
import com.uncanny.camx.UI.Views.HorizontalPicker;
import com.uncanny.camx.UI.Views.ViewFinder.AutoFitPreviewView;
import com.uncanny.camx.Utils.AsyncThreads.LatestThumbnailGenerator;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

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
    private HorizontalPicker modePicker;
    private RelativeLayout tvPreviewParent;

    private LensData lensData;
    private CameraControls cameraControls;
    private LatestThumbnailGenerator ltg;
    private CamState state;

    private int screenWidth, screenHeight;
    private float vfPointerX, vfPointerY;
    private long lastClickTime;
    private boolean isLongPressed;
    private boolean viewfinderGesture;

    private Handler backgroundHandler;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

//        initializeViews()
        previewView = findViewById(R.id.preview);
        thumbPreview = findViewById(R.id.thumbPreview);
        shutter = findViewById(R.id.shutter);
        front_switch = findViewById(R.id.front_back_switch);
        modePicker = findViewById(R.id.mode_picker_view);
        tvPreviewParent = findViewById(R.id.previewParent);

        tvPreviewParent.setClipToOutline(true);


        if (ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            previewView.setSurfaceTextureListener(surfaceTextureListener);
        else requestPermissions();

        lensData = new LensData(this);
        backgroundHandler = new Handler(getMainLooper());
        cameraControls = new CameraControls(this);
        cameraControls.setThumbView(thumbPreview);

        shutter.setOnClickListener(this);
        front_switch.setOnClickListener(this);
        thumbPreview.setOnClickListener(this);

        modePicker.setValues(new String[] {"Night", "Portrait", "Camera", "Video", "Pro"});
        modePicker.setSelectedItem(2,null);
        modePicker.setOverScrollMode(View.OVER_SCROLL_NEVER);
        modePicker.setOnItemSelectedListener(itemSelectedListener);
        previewView.setOnTouchListener(viewfinderGestureListener);

        if(lensData.supportBurstCapture(getCameraId())){
            shutter.setOnLongClickListener(shutterLongPressListener);

            shutter.setOnTouchListener((v, event) -> {
                if(event.getActionMasked() == MotionEvent.ACTION_UP && isLongPressed){
                    //Stop Repeating Burst
                    isLongPressed = false;
                    backgroundHandler.post(() -> cameraControls.createPreview());
//                    cameraControls.createPreview();
//                cameraHandler.post(this::displayLatestImage);
                    Log.e(TAG, "onLongPressedUp: Stop Repeating Burst");

                    return true;
                }
                return false;
            });
        }

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
                    finishAffinity();
                }
            }
        }

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.shutter){
            Log.e(TAG, "onClick: "+getState());
            switch (getState()){
                case CAMERA:{
                    cameraControls.captureImage();
                    break;
                }
                case VIDEO:{
                    cameraControls.startRecording();
                    setState(CamState.VIDEO_PROGRESSED);
                    break;
                }
                case VIDEO_PROGRESSED:{
                    cameraControls.stopRecording();
                    setState(CamState.VIDEO);
                    break;
                }
            }
        }
        else if (id == R.id.thumbPreview) {
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
        else if (id == R.id.front_back_switch) {
            performFileCleanup();
            previewView.setOnTouchListener(null);
//            zoomText.post(hideZoomText);
//            exposureControl.post(() -> setExposureRange());
            cameraControls.closeCamera();
            if (cameraControls.getLensFacing()== CameraCharacteristics.LENS_FACING_BACK) {
                setCameraId(FRONT_CAMERA_ID);
                if(CamState.getInstance().getState() != CamState.PORTRAIT) cameraControls.openCamera(FRONT_CAMERA_ID);
                front_switch.animate().rotation(180f).setDuration(300);
            } else {
//                auxDock.setIndex(1);
                setCameraId(BACK_CAMERA_ID);
                if(CamState.getInstance().getState() != CamState.PORTRAIT) cameraControls.openCamera(BACK_CAMERA_ID);
                front_switch.animate().rotation(-180f).setDuration(300);

            }
//            applyModeChange(getState());
//            front_switch.post(hideAuxDock);
        }

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
                    if (vfPointerX - event.getX() > getScreenWidth() / 4f) {
                        Log.e("TAG", "onTouchEvent: FLING RIGHT");
                        vfPointerX = event.getX();
                        if (modePicker.getSelectedItem() >= 0 && modePicker.getSelectedItem() < modePicker.getItems() - 1) {
                            modePicker.setSelectedItem(modePicker.getSelectedItem() + 1, 1);
//                                switchMode(mModePicker.getSelectedItem());
                            return true;
                        }
                    } else if (vfPointerX - event.getX() < -getScreenWidth() / 4f) {
                        Log.e("TAG", "onTouchEvent: FLING LEFT");
                        vfPointerX = event.getX();
                        if (modePicker.getSelectedItem() > 0 && modePicker.getSelectedItem() < modePicker.getItems()) {
                            modePicker.setSelectedItem(modePicker.getSelectedItem() - 1, -1);
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
        backgroundHandler.post(() -> cameraControls.captureBurstImage());
//                cameraControls.captureBurstImage();
        Log.e(TAG, "onCreate: Start Repeating Burst");
        return false;
    };

    private HorizontalPicker.OnItemSelected itemSelectedListener = index -> {
        Log.e(TAG, "onItemSelected: " + modePicker.getValues()[index]);
        modePicker.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
//        exposureControl.post(hideExposureControl);
        previewView.setOnTouchListener(null);
        shutter.setOnLongClickListener(getState() == CamState.CAMERA ? shutterLongPressListener : null);
//        exposureControl.post(this::setExposureRange);

        switch (index){
            case 0:{
                if(getState() == CamState.NIGHT) break;
                setState(CamState.NIGHT);
//                tvPreview.setOnTouchListener(touchListener);
//                modeNight();
                break;
            }
            case 1:{
                if(getState() == CamState.PORTRAIT) break;
                setState( CamState.PORTRAIT);
//                modePortrait();
                break;
            }
            case 2:{
                if(getState() == CamState.CAMERA) break;
                setState(CamState.CAMERA);
//                modeCamera();
                break;
            }
            case 3:{
                if(getState() == CamState.VIDEO || getState() == CamState.SLOMO ||
                        getState() == CamState.TIMELAPSE || getState() == CamState.VIDEO_PROGRESSED
                        || getState() == CamState.HSVIDEO_PROGRESSED
                        || getState() == CamState.TIMELAPSE_PROGRESSED) break;
                runOnUiThread(() -> {
//                    previewView.measure(1080, 1920);
                    previewView.setAspectRatio(1080,1920);
                });
                setState(CamState.VIDEO);
                cameraControls.openCamera(getCameraId());
                break;
            }
            case 4: {
                if(CamState.getInstance().getState() == CamState.PRO) break;
                setState(CamState.PRO);
//                tvPreview.setOnTouchListener(touchListener);
//                modePro();
                break;
            }
        }

        previewView.setOnTouchListener(viewfinderGestureListener); //TODO : Put inside a camera opened callback
    };


    private int getScreenWidth() {
        if(screenWidth < 1) screenWidth = getResources().getDisplayMetrics().widthPixels;
        return screenWidth;
    }

    private int getScreenHeight() {
        Log.e(TAG, "getScreenHeight: "+getResources().getDisplayMetrics().heightPixels);
        if(screenHeight < 1) screenHeight = getResources().getDisplayMetrics().heightPixels;
        return screenHeight;
    }

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
        Completable.fromRunnable(ltg = new LatestThumbnailGenerator(this))
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .andThen(Completable.fromRunnable(() -> {
                    thumbPreview.setImageBitmap(ltg.getBitmap());
                    Log.e(TAG, "displayLatestMediaThumbnailFromGallery: Updated Thumbnail");
                })).subscribe();
    }

    @Override
    protected void onResume() {
        super.onResume();
        state = CamState.getInstance();
        cameraControls.setResumed(true);
        cameraControls.startBackgroundThread();
        cameraControls.openCamera(cameraId == null ? BACK_CAMERA_ID : getCameraId());

        displayLatestImage();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraControls.setResumed(false);
        performFileCleanup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraControls.closeCamera();
        cameraControls.stopBackgroundThread();
        performFileCleanup();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    View view = findViewById(R.id.shutter);
                    if (view.isClickable()) {
                        view.performClick();
                    }
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

}
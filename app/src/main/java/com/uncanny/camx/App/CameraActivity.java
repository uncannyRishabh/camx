 package com.uncanny.camx.App;

 import android.Manifest;
 import android.animation.LayoutTransition;
 import android.annotation.SuppressLint;
 import android.content.Context;
 import android.content.Intent;
 import android.content.pm.PackageManager;
 import android.graphics.Color;
 import android.graphics.ImageFormat;
 import android.graphics.PorterDuff;
 import android.graphics.Rect;
 import android.graphics.SurfaceTexture;
 import android.hardware.camera2.CameraAccessException;
 import android.hardware.camera2.CameraCaptureSession;
 import android.hardware.camera2.CameraCharacteristics;
 import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
 import android.hardware.camera2.CameraDevice;
 import android.hardware.camera2.CameraManager;
 import android.hardware.camera2.CaptureFailure;
 import android.hardware.camera2.CaptureRequest;
 import android.hardware.camera2.CaptureResult;
 import android.hardware.camera2.TotalCaptureResult;
 import android.hardware.camera2.params.MeteringRectangle;
 import android.hardware.camera2.params.StreamConfigurationMap;
 import android.media.CamcorderProfile;
 import android.media.ImageReader;
 import android.media.MediaActionSound;
 import android.media.MediaRecorder;
 import android.os.Build;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.HandlerThread;
 import android.os.Looper;
 import android.os.SystemClock;
 import android.provider.MediaStore;
 import android.util.Log;
 import android.util.Pair;
 import android.util.Range;
 import android.util.Size;
 import android.util.SparseIntArray;
 import android.util.TypedValue;
 import android.view.Gravity;
 import android.view.HapticFeedbackConstants;
 import android.view.KeyEvent;
 import android.view.MotionEvent;
 import android.view.Surface;
 import android.view.TextureView;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.WindowManager;
 import android.view.animation.AccelerateInterpolator;
 import android.view.animation.DecelerateInterpolator;
 import android.widget.ImageButton;
 import android.widget.LinearLayout;
 import android.widget.RelativeLayout;
 import android.widget.TextView;
 import android.widget.Toast;

 import androidx.annotation.NonNull;
 import androidx.annotation.Nullable;
 import androidx.appcompat.app.AppCompatActivity;
 import androidx.appcompat.widget.AppCompatImageButton;
 import androidx.core.app.ActivityCompat;
 import androidx.core.content.ContextCompat;
 import androidx.core.content.res.ResourcesCompat;
 import androidx.core.os.HandlerCompat;

 import com.google.android.material.chip.Chip;
 import com.google.android.material.imageview.ShapeableImageView;
 import com.google.android.material.slider.Slider;
 import com.google.android.material.textview.MaterialTextView;
 import com.uncanny.camx.Control.FocusControls;
 import com.uncanny.camx.Control.ZoomControls;
 import com.uncanny.camx.Data.LensData;
 import com.uncanny.camx.R;
 import com.uncanny.camx.UI.AestheticLayout;
 import com.uncanny.camx.UI.Views.CaptureButton;
 import com.uncanny.camx.UI.Views.GestureBar;
 import com.uncanny.camx.UI.Views.HorizontalPicker;
 import com.uncanny.camx.UI.Views.ResolutionSelector;
 import com.uncanny.camx.UI.Views.UncannyChronometer;
 import com.uncanny.camx.UI.Views.ViewFinder.AutoFitPreviewView;
 import com.uncanny.camx.UI.Views.ViewFinder.FocusCircle;
 import com.uncanny.camx.UI.Views.ViewFinder.Grids;
 import com.uncanny.camx.UI.Views.ViewFinder.VerticalSlider;
 import com.uncanny.camx.UI.Views.ViewFinder.VideoMode;
 import com.uncanny.camx.Utils.AsyncThreads.ImageDecoderThread;
 import com.uncanny.camx.Utils.AsyncThreads.ImageSaverThread;
 import com.uncanny.camx.Utils.AsyncThreads.SerialExecutor;
 import com.uncanny.camx.Utils.CompareSizeByArea;

 import java.io.File;
 import java.io.IOException;
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Objects;
 import java.util.Vector;
 import java.util.concurrent.Executor;
 import java.util.concurrent.Executors;

 import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
 import io.reactivex.rxjava3.core.Completable;
 import io.reactivex.rxjava3.core.CompletableObserver;
 import io.reactivex.rxjava3.disposables.Disposable;
 import io.reactivex.rxjava3.schedulers.Schedulers;

 @SuppressWarnings({"FieldMayBeFinal",
        "FieldCanBeLocal"})
public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private final String BACK_CAMERA_ID = "0";
    private final String FRONT_CAMERA_ID = "1";
    private static final int REQUEST_PERMISSIONS = 200;
    private static final String[] PERMISSION_STRING = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE
            , Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private LensData lensData;

//    private final String [] camID= {"0","1","20","21","22","2","6","3"}; //0,1,2,3,4,5,6,7 in realme and stock android
                                  // 0   1   2    3    4    5   6   7

    private CameraManager camManager = null;
    private CameraDevice camDevice = null;
    private CameraCaptureSession camSession = null;
    private CameraCharacteristics characteristics = null;
    private CaptureRequest.Builder captureRequest = null;
    private CaptureRequest.Builder previewCaptureRequest = null;
    private CameraConstrainedHighSpeedCaptureSession highSpeedCaptureSession;
    private MediaRecorder mMediaRecorder;
    private SurfaceTexture stPreview;

    private static String cameraId = "0";
    private String mVideoFileName;
    private int vFPS = 30;
    private int sFPS = 120;
    private String chip_Text;
    private boolean resumed = false, surface = false, ready = false;
    public enum CamState{
        CAMERA,VIDEO,VIDEO_PROGRESSED,HSVIDEO_PROGRESSED,PORTRAIT,PRO,NIGHT,SLOMO,TIMEWARP,HIRES
    }
    public enum VFStates{
        IDLE,FOCUS,DOUBLE_TAP,SLIDE_ZOOM,SLIDE_EXPOSURE;
    }

    private CamState state = CamState.CAMERA;
    private VFStates vfStates = VFStates.IDLE;

    private Vector<Surface> surfaceList = new Vector<>();
    private Vector<Surface> hfrSurfaceList = new Vector<>();
    private Handler mHandler = new Handler();
    private RelativeLayout appbar;
    private AutoFitPreviewView tvPreview;
    private CaptureButton shutter;
    private MaterialTextView wide_lens,tv;
    private AppCompatImageButton front_switch;
    private LinearLayout auxDock;
    private TextView zoomText;
    private ShapeableImageView thumbPreview;
    private ImageButton button1, button2, button3, button4, button5;
    private ImageButton button21, button22, button23, button24, button25;
    private RelativeLayout dock;
    private ImageReader imgReader;
    private HorizontalPicker mModePicker;
    private Slider zSlider;
    private Grids grids;
    private FocusCircle focusCircle;
    private GestureBar gestureBar;
    private LinearLayout btn_grid1,btn_grid2;
    private UncannyChronometer chronometer;
    private Chip vi_info;
    private ResolutionSelector resolutionSelector;
    private RelativeLayout tvPreviewParent;
    private RelativeLayout parent;
    private VerticalSlider exposureControl;
    private VideoMode videoModePicker;

    private int resultCode = 1;
    private long lastClickTime = 0;
    private Size previewSize;
    private Size imageSize;
    private Size mVideoSize;

    private ImageReader snapshotImageReader;
    private float mZoom = 1.0f;
    public float ZOOM_LEVEL = 0;
    public float finger_spacing = 0;
    private int cachedHeight;
    private Rect zoom = new Rect();
    private Rect zoomRect = new Rect();

    private Pair<Size,Range<Integer>> sloMoPair;
    private List<Integer> cameraList;
    private List<Integer> auxCameraList;

    private boolean isVRecording = false;
    private boolean isSLRecording = false;
    private boolean isVideoPaused = false;
    private boolean viewfinderGesture = false;
    private float vfPointerX,vfPointerY;
    private boolean mflash = false;
    private boolean ASPECT_RATIO_43 = true;
    private static boolean ASPECT_RATIO_169 = false;
    private int timer_cc = 1;
    private int gridClick = 0;
    private String zText = "";
    private int tvHeight, tvWidth;

    private CamState getState() {
        return state;
    }

    private void setState(CamState state) {
        this.state = state;
    }

    private VFStates getVfStates(){
        return vfStates;
    }

    private void setVfStates(VFStates vfStates){
        this.vfStates = vfStates;
    }

    private FocusControls focus;

    private Handler mBackgroundHandler;
    private Handler focusHandler;
    private HandlerThread focusHandlerThread;
    private HandlerThread mBackgroundThread;
    private Handler vfHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    private Executor executor = new SerialExecutor(Executors.newCachedThreadPool());

//    private Executor executor1 = new SerialExecutor(Executors.newFixedThreadPool(2));

    private Runnable hideFocusCircle = new Runnable() {
        @Override
        public void run() {
            if(focusCircle.getVisibility()== View.VISIBLE){
                focusCircle.setVisibility(View.GONE);
            }
        }
    };

    private Runnable hideZoomSlider = new Runnable() {
        @Override
        public void run() {
            if(zSlider.getVisibility()==View.VISIBLE){
                zSlider.setVisibility(View.GONE);
            }
        }
    };

    private Runnable hideZoomText = new Runnable() {
        @Override
        public void run() {
            if(zoomText.getVisibility()==View.VISIBLE){
                zoomText.setVisibility(View.GONE);
            }
        }
    };

    private Runnable hideExposureControl = new Runnable() {
        @Override
        public void run() {
            if(exposureControl.getVisibility() == View.VISIBLE) {
                exposureControl.setVisibility(View.GONE);
            }
        }
    };

    private Runnable hideAuxDock = new Runnable() {
        @Override
        public void run() {
            if(characteristics.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_BACK){
                if(getState() == CamState.HIRES || getState() == CamState.VIDEO_PROGRESSED
                        || getState() == CamState.HSVIDEO_PROGRESSED
                        || getState() == CamState.PORTRAIT){
                    auxDock.setVisibility(View.GONE);
                }
                else if(getState() == CamState.SLOMO){
                    auxDock.setVisibility(View.INVISIBLE);
                }
                else if(auxCameraList.size()>0){
                    auxDock.setVisibility(View.VISIBLE);
                }
            }
            else{                                       //FIXME: HANDLE FOR MULTIPLE FRONT CAMERA
                auxDock.setVisibility(View.GONE);
            }
        }
    };

    private Map<Integer,Integer> modeMap = new HashMap<>(); //cameraId,modeIndex
    private String[][] CachedCameraModes = new String[10][];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_camX);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        parent = findViewById(R.id.root);
        parent.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ) {
            Toast.makeText(this, "Grant Camera Permissions to Continue", Toast.LENGTH_SHORT).show();
            requestPermissions(PERMISSION_STRING,REQUEST_PERMISSIONS);
        }

        appbar = findViewById(R.id.appbar);
        thumbPreview = findViewById(R.id.img_gal);
        tvPreview = findViewById(R.id.preview);
        shutter = findViewById(R.id.capture);
        wide_lens = findViewById(R.id.main_wide);
        front_switch = findViewById(R.id.front_back_switch);
        auxDock = findViewById(R.id.aux_cam_switch);
        tvPreviewParent = findViewById(R.id.previewParent);
        dock = findViewById(R.id.relative_layout_button_dock);
        mModePicker = findViewById(R.id.mode_picker_view);
        setAestheticLayout();

        lensData = new LensData(getApplicationContext());
        cameraList =  lensData.getPhysicalCameras();
        auxCameraList = lensData.getAuxiliaryCameras();

        modeMap.put(0,0);
        modeMap.put(1,1);

        tvPreviewParent.setClipToOutline(true);

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

//        tvPreview.setClipToOutline(true); //TODO: test in API 24

        addAuxButtons();
        button1 = findViewById(R.id.btn_1);
        button2 = findViewById(R.id.btn_2);
        button3 = findViewById(R.id.btn_3);
        button4 = findViewById(R.id.btn_4);
        button5 = findViewById(R.id.btn_5);
        button21 = findViewById(R.id.btn_21);
        button22 = findViewById(R.id.btn_22);
        button23 = findViewById(R.id.btn_23);
        button24 = findViewById(R.id.btn_24);
        button25 = findViewById(R.id.btn_25);
        grids = findViewById(R.id.grid);
        zoomText = findViewById(R.id.zoom_text);
        zSlider = findViewById(R.id.zoom_slider);
        focusCircle = findViewById(R.id.focus_circle);
        gestureBar = findViewById(R.id.gesture_bar);
        btn_grid1 = findViewById(R.id.top_bar_0);
        btn_grid2 = findViewById(R.id.top_bar_1);
        vi_info = findViewById(R.id.vi_indicator);
        resolutionSelector = findViewById(R.id.resolution_selector);
        exposureControl = findViewById(R.id.exposureControl);
        videoModePicker = findViewById(R.id.video_mode_picker);

        btn_grid1.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        btn_grid2.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        addCapableVideoResolutions();

        mModePicker.setValues(new String[] {"Night", "Portrait", "Camera", "Video", "Pro"});
        mModePicker.setSelectedItem(2);
        mModePicker.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mModePicker.setOnItemSelectedListener(index -> {
            mModePicker.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            exposureControl.post(hideExposureControl);
            tvPreview.setOnTouchListener(null);
            exposureControl.post(this::setExposureRange);
            switch (index){
                case 0:{
                    setState(CamState.NIGHT);
                    modeNight();
                    Log.e(TAG, "onItemSelected: "+mModePicker.getValues()[3]);
                    break;
                }
                case 1:{
                    setState( CamState.PORTRAIT);
                    modePortrait();
                    Log.e(TAG, "onItemSelected: "+mModePicker.getValues()[2]);
                    break;
                }
                case 2:{
                    setState(CamState.CAMERA);
                    modeCamera();
                    Log.e(TAG, "onItemSelected: CAMERA MODE");
                    break;
                }
                case 3:{
                    setState(CamState.VIDEO);
                    modeVideo();
                    Log.e(TAG, "onItemSelected: VIDEO MODE");
                    break;
                }
                case 4:
                    setState(CamState.PRO);
                    modePro();
                    Log.e(TAG, "onItemSelected: "+mModePicker.getValues()[4]);
                    break;
            }

            auxDock.post(hideAuxDock);
            zoomText.post(hideZoomText);
            if(getState() == CamState.VIDEO && lensData.hasSloMoCapabilities(getCameraId()))
                videoModePicker.setVisibility(View.VISIBLE);
            else videoModePicker.setVisibility(View.GONE);
//            videoModePicker.setVisibility(getState() == CamState.VIDEO ? View.VISIBLE : View.GONE);
            vi_info.setVisibility(getState() == CamState.VIDEO || getState() == CamState.SLOMO || getState() == CamState.TIMEWARP ?
                    View.VISIBLE : View.INVISIBLE);
        });

        //Timer button
        button1.setOnClickListener(v -> {
            timer_cc+=1;
            if(timer_cc == 1){
                button1.setImageResource(R.drawable.ic_timer_btn);
            }
            else if(timer_cc == 2){
                button1.setImageResource(R.drawable.ic_timer_3_btn);
            }
            else if(timer_cc == 3){
                button1.setImageResource(R.drawable.ic_timer_5_btn);
            }
            else if(timer_cc == 4){
                button1.setImageResource(R.drawable.ic_timer_10_btn);
                timer_cc=0;
            }
        });

        //Grid button
        button2.setOnClickListener(v -> {
            gridClick+=1;

            if(grids.getVisibility()==View.VISIBLE && gridClick>2){
                button2.setColorFilter(ContextCompat.getColor(getApplicationContext(),R.color.white)
                        , PorterDuff.Mode.MULTIPLY);
                grids.setVisibility(View.INVISIBLE);
            }
            else{
                button2.setColorFilter(ContextCompat.getColor(getApplicationContext(),R.color.purple_200)
                        , PorterDuff.Mode.MULTIPLY);
                grids.setLines((gridClick == 1 ? 3 : 4));
                grids.setVisibility(View.VISIBLE);
                grids.postInvalidate();
                grids.setViewBounds(tvPreview.getHeight(),tvPreview.getWidth());

            }
            if(gridClick==3) gridClick = 0;

        });

        //Flash button
        button4.setOnClickListener(v -> {
            if(mflash){
                mflash = false;
                button4.setImageResource(R.drawable.ic_flash_off);
                previewCaptureRequest.set(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_OFF);
                try {
                    camSession.setRepeatingRequest(previewCaptureRequest.build(), null, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            else {
                mflash = true;
                button4.setImageResource(R.drawable.ic_flash_on);
                previewCaptureRequest.set(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_TORCH);
                try {
                    camSession.setRepeatingRequest(previewCaptureRequest.build(), null, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            Log.e(TAG, "onClick: flash : "+mflash);
        });

        //Aspect Ratio button
        button23.setOnClickListener(v -> {
            button1.setColorFilter(Color.WHITE);
            if (ASPECT_RATIO_43) {
                ASPECT_RATIO_169 = true;
                ASPECT_RATIO_43 = false;

                closeCamera();
                openCamera();

                button23.setColorFilter(ContextCompat.getColor(getApplicationContext(),R.color.purple_200)
                        , PorterDuff.Mode.MULTIPLY);
            } else if (ASPECT_RATIO_169) {
                ASPECT_RATIO_43 = true;
                ASPECT_RATIO_169 = false;

                closeCamera();
                openCamera();
                tvPreview.measure(previewSize.getHeight(), previewSize.getWidth());


                button23.setColorFilter(ContextCompat.getColor(getApplicationContext(),R.color.white)
                        , PorterDuff.Mode.MULTIPLY);
            }
        });

        //Settings button
        button25.setOnClickListener(v -> {
            Intent settingsIntent = new Intent(CameraActivity.this,SettingsActivity.class);
            settingsIntent.putExtra("c2api",lensData.getCamera2level());
            startActivity(settingsIntent);
        });

        resolutionSelector.setOnClickListener(v -> {
            if(getState() == CamState.VIDEO) {
                int width = translateResolution(resolutionSelector.getHeaderText());
                resolutionSelector
                        .postDelayed(() -> createVideoPreview(tvPreview.getHeight(), width),300);
            }
            else if(getState() == CamState.SLOMO){
                int width = translateResolution(resolutionSelector.getHeaderAndFooterText().split("P")[0]);
                sFPS = Integer.parseInt(resolutionSelector.getHeaderAndFooterText()
                        .split("_")[1].split("FPS")[0]);
                createSloMoPreview(correspondingHeight(width),width);
            }
        });

        tvPreview.setSurfaceTextureListener(surfaceTextureListener);

        shutter.setOnClickListener(v -> {
            chronometer = findViewById(R.id.chronometer);
            switch (getState()) {
                case CAMERA:
                case HIRES:
                case PORTRAIT:
                    captureImage();
                    shutter.animateInnerCircle(getState());
                    MediaActionSound sound = new MediaActionSound();
                    sound.play(MediaActionSound.SHUTTER_CLICK);
                    //TODO : ADD SEMAPHORE
                    break;
                case VIDEO:
                    if(!isVRecording) {
                        startRecording();
                        chronometer.start();
                        setState(CamState.VIDEO_PROGRESSED);
                        thumbPreview.setImageDrawable(ResourcesCompat.getDrawable(getResources()
                                ,R.drawable.ic_video_snapshot,null));
                        thumbPreview.setOnClickListener(captureSnapshot);
                        front_switch.setImageDrawable(ResourcesCompat.getDrawable(getResources()
                                ,R.drawable.ic_video_pause,null));
                        front_switch.setOnClickListener(play_pauseVideo);
                        shutter.animateInnerCircle(getState());

                        auxDock.post(hideAuxDock);
                        mModePicker.setVisibility(View.INVISIBLE);
                        chronometer.setBase(SystemClock.elapsedRealtime());
                        chronometer.setVisibility(View.VISIBLE);
                        isVRecording = true;
                    }
                    break;
                case VIDEO_PROGRESSED:
                    if(isVRecording){
                        isVRecording = false;
                        chronometer.stop();
                        chronometer.setVisibility(View.INVISIBLE);
                        auxDock.post(hideAuxDock);
                        mModePicker.setVisibility(View.VISIBLE);
                        mMediaRecorder.stop(); //TODO: handle stop before preview is generated
                        mMediaRecorder.reset();
                        setState(CamState.VIDEO);
                        createVideoPreview(tvPreview.getHeight(),(lensData.is1080pCapable(getCameraId())
                                ? 1080 : 720));
//                            Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

                        //restore UI state
                        shutter.animateInnerCircle(getState());
                        thumbPreview.setImageDrawable(null);
                        thumbPreview.setOnClickListener(openGallery);
                        displayMediaThumbnailFromGallery();
                        front_switch.setImageDrawable(ResourcesCompat.getDrawable(getResources()
                                ,R.drawable.ic_front_switch,null));
                        front_switch.setOnClickListener(switchFrontCamera);

                    }
                    break;
                case SLOMO:
                    if(!isSLRecording){
                        setState(CamState.HSVIDEO_PROGRESSED);
                        startSloMoRecording();
                        mModePicker.setVisibility(View.INVISIBLE);
                        chronometer.setBase(SystemClock.elapsedRealtime());
                        chronometer.start();
                        chronometer.setVisibility(View.VISIBLE);
                        shutter.animateInnerCircle(getState());
                        isSLRecording = true;

                        thumbPreview.setVisibility(View.INVISIBLE);
                        front_switch.setVisibility(View.INVISIBLE);
                    }
                    break;
                case HSVIDEO_PROGRESSED:
                    if (isSLRecording) {
                        isSLRecording = false;
                        setState(CamState.SLOMO);
                        try {
                            highSpeedCaptureSession.stopRepeating();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                        shutter.animateInnerCircle(getState());
                        chronometer.stop();
                        chronometer.setVisibility(View.INVISIBLE);
                        mModePicker.setVisibility(View.VISIBLE);
                        mMediaRecorder.stop(); //FIXME: handle stop before preview is generated | stop failed.
                        mMediaRecorder.reset();

                        thumbPreview.setVisibility(View.VISIBLE);
                        front_switch.setVisibility(View.VISIBLE);
                        createSloMoPreview(sloMoPair.first.getWidth(), sloMoPair.first.getHeight());
                    }
                    break;
                default:
                    break;
            }
            videoModePicker.setVisibility(getState() == CamState.VIDEO
                    || getState() == CamState.SLOMO
                    || getState() == CamState.TIMEWARP? View.VISIBLE : View.GONE);
            resolutionSelector.setState(getState());
        });

        thumbPreview.setOnClickListener(openGallery);

        wide_lens.setOnClickListener(v -> {
            if (!getCameraId().equals(BACK_CAMERA_ID)) {
                closeCamera();
                tvPreview.setOnTouchListener(null);
                zoomText.post(hideZoomText);
                setCameraId(BACK_CAMERA_ID);
                openCamera();
                if(getState()==CamState.VIDEO) addCapableVideoResolutions();
                else if(getState()==CamState.SLOMO) addCapableSloMoResolutions();
                exposureControl.post(this::setExposureRange);
                for(int id : auxCameraList){
                    tv = auxDock.findViewById(id);
                    tv.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview_small));
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,11);
                }
                wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview));
                wide_lens.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
            }
        });

        front_switch.setOnClickListener(switchFrontCamera);

        tvPreview.setOnTouchListener(touchListener);

        button5.setOnClickListener(v -> inflateButtonMenu());

        gestureBar.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_UP){
                inflateButtonMenu();
            }
            return false;
        });

        try {
            mZoom = ZoomControls.getMaxZoom(getCameraCharacteristics());

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        zSlider.addOnChangeListener((slider, value, fromUser) -> {
            try {
                ZOOM_LEVEL = value * 4.5f;
                setZoom(mZoom,  (int) ZOOM_LEVEL);
            } catch (CameraAccessException | IllegalStateException e) {
                e.printStackTrace();
            }
        });
        zSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                zoomText.removeCallbacks(hideZoomText);
                zSlider.removeCallbacks(hideZoomSlider);
            }
            @SuppressLint("RestrictedApi")
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                setVfStates(VFStates.IDLE);
                auxDock.postDelayed(hideAuxDock,2000);
                zoomText.postDelayed(hideZoomText,2000);
                zSlider.postDelayed(hideZoomSlider,2000);
            }
        });

        exposureControl.setOnSliderChangeListener(new VerticalSlider.OnSliderChangeListener() {
            @Override
            public void onProgressChanged(VerticalSlider seekBar, float progress) {
                adjustExposure(progress);
            }

            @Override
            public void onStartTrackingTouch(VerticalSlider seekBar,float progress) {
                exposureControl.removeCallbacks(hideExposureControl);
            }

            @Override
            public void onStopTrackingTouch(VerticalSlider seekBar) {
                if(getState() == CamState.CAMERA || getState() == CamState.PORTRAIT
                        || getState() == CamState.NIGHT || getState() == CamState.HIRES)
                exposureControl.postDelayed(hideExposureControl,3000);
                exposureControl.postDelayed(() -> {
                    resetAE();
                    resetFocus();
                },3000);

//                else exposureControl.postDelayed(hideExposureControl,3000);
            }
        });

        exposureControl.post(() -> {
            exposureControl.disableTapToMove(true);
            setExposureRange();
        });

        videoModePicker.setIndex(1);
        videoModePicker.setOnClickListener((view, Position) -> {
            auxDock.post(hideAuxDock);
            switch (Position){
                case 0:{
                    if(getState() != CamState.SLOMO) {
                        closeCamera();
                        setState(CamState.SLOMO);
                        modeSloMo();
                    }
                    break;
                }
                case 1:{
                    if(getState() != CamState.VIDEO){
                        setState(CamState.VIDEO);
                        openCamera();
                        modeVideo();
                    }
                    break;
                }
                case 2:{
                    if(getState() != CamState.TIMEWARP) {
                        setState(CamState.TIMEWARP);
                        modeTimeWarp();
                    }
                    break;
                }
            }
        });

        /*
        Caching Camera Modes for every camera id
         */
        if(lensData.isAuxCameraAvailable()){
            for(int i=0;i<cameraList.size();i++){
                CachedCameraModes[i] = lensData.getAvailableModes(cameraList.get(i)+"");
            }
        }
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            displayMediaThumbnailFromGallery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e(TAG, "onRequestPermissionsResult: "+ Arrays.toString(permissions));
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                displayMediaThumbnailFromGallery();
            } else {
                Toast.makeText(this, "Allow permission to continue", Toast.LENGTH_SHORT).show();
                requestRuntimePermission();
            }
        }
    }

    private boolean flashSupported() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * MODES
     */
    private void modeCamera(){
        shutter.animateInnerCircle(getState());
        closeCamera();
        openCamera();
//        new Handler(Looper.myLooper()).post(getSensorSize);
        modifyMenuForPhoto();
        if(front_switch.getVisibility()==View.INVISIBLE) front_switch.setVisibility(View.VISIBLE);
    }

    private void modeVideo(){
        lensData.getFpsResolutionPair_video(getCameraId());
        shutter.animateInnerCircle(getState());
        requestVideoPermissions();
        createVideoPreview(tvPreview.getHeight(),(lensData.is1080pCapable(getCameraId()) ? 1080 : 720));
        modifyMenuForVideo();
        if(front_switch.getVisibility()==View.INVISIBLE) front_switch.setVisibility(View.VISIBLE);
    }

    private void modeSloMo(){
        int tFPS = 120;
//        Log.e(TAG, "modeSloMo: "+lensData.getFpsResolutionPair(getCameraId()));
//                        Log.e(TAG, "modeSloMo: "+lensData.getFpsResolutionPair(getCameraId()).size()); //14 instead 7
        for(Pair<Size,Range<Integer>> pair : lensData.getFpsResolutionPair(getCameraId())){
            if(pair.second.getLower()+pair.second.getUpper() > tFPS) {
                sloMoPair = pair;
            }
            tFPS = pair.second.getLower()+pair.second.getUpper();
//            Log.e(TAG, "modeSloMo: "+pair.first+" , "+pair.second.getLower());
        }
        sFPS = sloMoPair.second.getUpper();
        requestVideoPermissions();
        createSloMoPreview(sloMoPair.first.getWidth(), sloMoPair.first.getHeight());
        shutter.animateInnerCircle(getState());
        modifyMenuForVideo();
        front_switch.setVisibility(lensData.hasSloMoCapabilities("1") ? View.VISIBLE : View.INVISIBLE);
    }

    private void modeTimeWarp(){
        modifyMenuForVideo();
        if(front_switch.getVisibility()==View.INVISIBLE) front_switch.setVisibility(View.VISIBLE);
        shutter.animateInnerCircle(getState());
    }

    private void modeHiRes(){
        shutter.animateInnerCircle(getState());
        ASPECT_RATIO_43 = true;
        closeCamera();
        openCamera();
        modifyMenuForPhoto();
        if(!lensData.isBayerAvailable("1")){
            front_switch.setVisibility(View.INVISIBLE);
            imageSize = lensData.getBayerLensSize();
        }

    }

    private void modePortrait(){
        Log.e(TAG, "modePortrait: exec check !! ");
        modifyMenuForPhoto();
        closeCamera();
        openCamera();
        if(front_switch.getVisibility()==View.INVISIBLE) front_switch.setVisibility(View.VISIBLE);
    }

    private void modeNight(){
        if(front_switch.getVisibility()==View.INVISIBLE) front_switch.setVisibility(View.VISIBLE);
    }

    private void modePro(){
        front_switch.setVisibility(lensData.hasCamera2api() ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * UI CHANGES
     */

    private void addAuxButtons() {
        final float param= getResources().getDimension(R.dimen.aux_param);
        if(auxCameraList.size()>0){
            auxDock.setVisibility(View.VISIBLE);
            for (int i=0 ; i< auxCameraList.size() ; i++) {
                modeMap.put(auxCameraList.get(i),2+i);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) param
                        ,LinearLayout.LayoutParams.MATCH_PARENT);
                layoutParams.weight = 1.0f;
                MaterialTextView aux_btn = new MaterialTextView(this);
                aux_btn.setLayoutParams(layoutParams);
                aux_btn.setId(auxCameraList.get(i));
                aux_btn.setGravity(Gravity.CENTER);
                aux_btn.setTextSize(TypedValue.COMPLEX_UNIT_SP,11);
                aux_btn.setText(auxCameraList.get(i).toString());
                aux_btn.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview_small));
                auxDock.addView(aux_btn);

                aux_btn.setOnClickListener(v -> {
                    if(!(aux_btn.getId()+"").equals(getCameraId())) {
                        tvPreview.setOnTouchListener(null);
                        setCameraId(aux_btn.getId() + "");
                        closeCamera();
                        setCameraId(aux_btn.getId() + "");
                        openCamera();
                        zoomText.post(hideZoomText);
                        if (getState() == CamState.VIDEO) addCapableVideoResolutions();
                        else if (getState() == CamState.SLOMO) addCapableSloMoResolutions();
                        exposureControl.post(this::setExposureRange);
                        wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview_small));
                        wide_lens.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
//                        wide_lens.setTextColor(Color.WHITE);
                        for (int id : auxCameraList) {
                            if ((id + "").equals(getCameraId())) {
                                continue;
                            }
                            tv = auxDock.findViewById(id);
                            tv.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview_small));
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                            tv.setTextColor(Color.WHITE);
                        }
//                        aux_btn.setTextColor(ContextCompat.getColor(getApplicationContext(),R.color.purple_200));
                        aux_btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        aux_btn.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview));
                    }
                });
            }
        }
        else if(cameraList.isEmpty()){
            auxDock.setVisibility(View.GONE);
        }
    }

    private void setAestheticLayout() {
        new AestheticLayout(this);

//        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
//            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
//                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//        }
    }

    private void inflateButtonMenu() {
        Log.e(TAG, "inflateButtonMenu: ch : "+cachedHeight+" rh : "+appbar.getHeight());
        if (cachedHeight == appbar.getHeight()) {
            tvPreview.setOnTouchListener(null);
            tvPreview.setClickable(false);
            tvPreview.setOnClickListener(v -> {
                tvPreview.setOnTouchListener(touchListener);
                deflateButtonMenu();
            });
            button5.animate().rotation(-90f).setInterpolator(new DecelerateInterpolator());

            btn_grid1.animate().translationY(28f)
                    .setInterpolator(new DecelerateInterpolator());
            btn_grid2.animate().translationY(22f)
                    .setInterpolator(new DecelerateInterpolator()).setStartDelay(100);

            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    appbar.getWidth(), cachedHeight * 3);
            appbar.setLayoutParams(layoutParams);

        }
        else {
            tvPreview.setOnTouchListener(touchListener);
            deflateButtonMenu();
        }
    }

    private void deflateButtonMenu() {
        button5.animate().rotation(0f);

        btn_grid1.animate().translationY(0f)
                .setInterpolator(new AccelerateInterpolator(.1f));
        btn_grid2.animate().translationY(0f);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                appbar.getWidth(), cachedHeight);
        appbar.setLayoutParams(layoutParams);
    }

    private void setDockHeight(int width) {
        int tvHeight = (width == 720 ? 1280 : 1440); //FIXME : HANDLE FOR HIRES DISPLAY (USING DP)
        int height = parent.getHeight()-tvHeight-appbar.getHeight()-mModePicker.getHeight();
        int modeMargin;
        if(dock.getMinimumHeight()<height) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width
                    , height);
            layoutParams.addRule(RelativeLayout.ABOVE, R.id.mode_picker_view);
            dock.setLayoutParams(layoutParams);
            modeMargin = parent.getHeight()-appbar.getHeight()-tvHeight-height;
        }
        else{
            modeMargin = parent.getHeight()-appbar.getHeight()-tvHeight-dock.getMinimumHeight();
        }
//        Log.e(TAG, "setDockHeight: MPicker Margin : "+modeMargin);
//        Log.e(TAG, "setDockHeight: PARENT HEIGHT : "+parent.getHeight());

        //set modePicker margin
        if(modeMargin/4 > 0){
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) mModePicker.getLayoutParams();
            mlp.setMargins(0,0,0,modeMargin/4);
        }


    }

    private void modifyMenuForVideo(){
        btn_grid1.findViewById(R.id.btn_3).setVisibility(View.GONE);
        btn_grid2.findViewById(R.id.btn_21).setVisibility(View.GONE);
        btn_grid2.findViewById(R.id.btn_22).setVisibility(View.GONE);
        btn_grid2.findViewById(R.id.btn_23).setVisibility(View.GONE);
        btn_grid2.findViewById(R.id.btn_24).setVisibility(View.GONE);
        btn_grid2.findViewById(R.id.resolution_selector).setVisibility(View.VISIBLE);
        if(getState() == CamState.VIDEO){
            addCapableVideoResolutions();
        }
        else if(getState() == CamState.SLOMO){
            addCapableSloMoResolutions();
        }
    }

    private void modifyMenuForPhoto(){
        btn_grid1.findViewById(R.id.btn_3).setVisibility(View.VISIBLE);
        btn_grid2.findViewById(R.id.btn_21).setVisibility(View.VISIBLE);
        btn_grid2.findViewById(R.id.btn_22).setVisibility(View.VISIBLE);
        btn_grid2.findViewById(R.id.btn_23).setVisibility(View.VISIBLE);
        btn_grid2.findViewById(R.id.btn_24).setVisibility(View.VISIBLE);
        btn_grid2.findViewById(R.id.resolution_selector).setVisibility(View.GONE);
    }

    private void update_chip_text(String size,String fps){
        chip_Text = size+"p | "+fps+"fps";
        vi_info.setText(chip_Text);
        Log.e(TAG, "update_chip_text: CHIP UPDATED | "+chip_Text);
    }

    /**
     * LISTENERS
     */

    private View.OnClickListener switchFrontCamera = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            tvPreview.setOnTouchListener(null);
            zoomText.post(hideZoomText);
            exposureControl.post(() -> setExposureRange());
            closeCamera();
            auxDock.post(hideAuxDock);
            if (characteristics.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_BACK) {
                setCameraId(FRONT_CAMERA_ID);
                if(getState() != CamState.PORTRAIT) openCamera();
                front_switch.animate().rotation(180f).setDuration(300);
            } else {
                setCameraId(BACK_CAMERA_ID);
                if(getState() != CamState.PORTRAIT) openCamera();
                wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview));
                front_switch.animate().rotation(-180f).setDuration(300);
            }
            applyModeChange(getState());
        }
    };

    private View.OnClickListener openGallery = v -> {
        Intent intent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, resultCode);
    };

    private View.OnClickListener captureSnapshot = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                captureRequest = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
                captureRequest.addTarget(snapshotImageReader.getSurface());
                if(ZOOM_LEVEL !=1) {
                    captureRequest.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                }
                camSession.capture(captureRequest.build(), null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            thumbPreview.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP
                    ,HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    };

    private View.OnClickListener play_pauseVideo = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(isVideoPaused){
                isVideoPaused = false;
                mMediaRecorder.resume();
                chronometer.resume();
                front_switch.setImageDrawable(ResourcesCompat.getDrawable(getResources()
                        ,R.drawable.ic_video_pause,null));
            }
            else{
                isVideoPaused = true;
                chronometer.pause();
                mMediaRecorder.pause();
                front_switch.setImageDrawable(ResourcesCompat.getDrawable(getResources()
                        ,R.drawable.ic_video_resume,null));
            }
        }
    };

    private View.OnTouchListener touchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getActionMasked();

            switch (action){
                case MotionEvent.ACTION_DOWN:{
                    viewfinderGesture = false;
                    vfPointerX = event.getX();
                    vfPointerY = event.getY();
                    return true;
                }
                case MotionEvent.ACTION_MOVE:{
                    /*
                     * PINCH TO ZOOM
                     */
                    if (event.getPointerCount() > 1) {
                        pinchToZoom(event);
                        break;
                    }
                    return true;
                }
                case MotionEvent.ACTION_POINTER_UP:{
                    zSlider.postDelayed(hideZoomSlider,2000);
                    zoomText.postDelayed(hideZoomText, 2000);
                    vfHandler.postDelayed(() -> setVfStates(VFStates.IDLE), 700);
                    break;
                }
                case MotionEvent.ACTION_UP:{
                    long clickTime = System.currentTimeMillis();
                    if(vfPointerX-event.getX() > 15 || vfPointerY-event.getY() > 15) viewfinderGesture = true;

                    if ((clickTime - lastClickTime) < 500) {
                        /*
                         * DOUBLE TAP TO ZOOM
                         */
                        setVfStates(VFStates.DOUBLE_TAP);
                        Log.e("**DOUBLE TAP**", " second tap ");
                        try {
                            doubleTapZoom();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                        lastClickTime = 0;
                    } else if (getVfStates() != VFStates.DOUBLE_TAP && getVfStates() != VFStates.SLIDE_ZOOM && !viewfinderGesture) {
                        /*
                         * TOUCH TO FOCUS
                         */
                        Log.e(TAG, "onClick: viewfinderGesture = "+viewfinderGesture);
                        setVfStates(VFStates.FOCUS);
                        exposureControl.removeCallbacks(hideExposureControl);
                        exposureControl.setVisibility(View.VISIBLE);
                        exposureControl.postDelayed(hideExposureControl,3000);
//                        focus.setFocus(v,event);
                        focus(v.getHeight(),v.getWidth(),event);
                        lastClickTime = System.currentTimeMillis();
                        Log.e("** SINGLE  TAP **", " First Tap time  " + lastClickTime);
                        return false;
                    }
                    lastClickTime = clickTime;
                    v.performClick();
                }
            }

            return true;
        }
    };

    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            surface = true;
            stPreview = surfaceTexture;
            cachedHeight = appbar.getHeight();
            setDockHeight(i);
//            Log.e(TAG, "onSurfaceTextureAvailable: w : "+i+" h : "+tvPreview.getHeight());
            tvHeight = i1;tvWidth = i;
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
            if(grids.getVisibility()==View.VISIBLE && gridClick == 1){
                grids.postInvalidate();
                grids.setViewBounds(i1,i);
            }
            else if(grids.getVisibility()==View.VISIBLE && gridClick == 2){
                grids.postInvalidate();
                grids.setViewBounds(i1,i);
            }
            tvHeight = i1; tvWidth = i;
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            surface = false;
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    /**
     *  Z O O O M AND F[ O ]CUS STUFF
     */

    private void doubleTapZoom() throws CameraAccessException {
        zoomText.removeCallbacks(hideZoomText);
        zSlider.removeCallbacks(hideZoomSlider);
        float maxZoom = 1;
        try {
            maxZoom = ZoomControls.getMaxZoom(getCameraCharacteristics());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        float DT_value = maxZoom/2;

        Log.e(TAG, "doubleTaptoZoom: ZoomControls Level "+ ZOOM_LEVEL);
        if(ZOOM_LEVEL <DT_value){
            setZoom(maxZoom, (int) DT_value);
            ZOOM_LEVEL = DT_value;
            zSlider.setValue(5.0f);
        }
        else if(ZOOM_LEVEL >DT_value){
            setZoom(maxZoom, (int) DT_value);
            ZOOM_LEVEL = DT_value;
            zSlider.setValue(5.0f);
        }
        else if(ZOOM_LEVEL ==DT_value){
            setZoom(maxZoom,1);
            ZOOM_LEVEL = 1f;
            zSlider.setValue(0f);
        }
        zSlider.setVisibility(View.VISIBLE);

        zSlider.postDelayed(hideZoomSlider,2000);
        zoomText.postDelayed(hideZoomText,2000);
        setVfStates(VFStates.IDLE);
        Log.e(TAG, "doubleTaptoZoom: D O U B L E - T A P P E D");
    }

    private void pinchToZoom(MotionEvent event){
        setVfStates(VFStates.SLIDE_ZOOM);

        try {
            float maxzoom = ZoomControls.getMaxZoom(getCameraCharacteristics());
            float current_finger_spacing;
            // Multi touch logic
            current_finger_spacing = getFingerSpacing(event);
            if(finger_spacing != 0){
                if(zSlider.getVisibility() != View.VISIBLE) zSlider.setVisibility(View.VISIBLE);

                if(current_finger_spacing > finger_spacing &&  ZOOM_LEVEL < maxzoom){
                    ZOOM_LEVEL +=0.5f;
                    zSlider.setValue((float) getZoomValueSingleDecimal((ZOOM_LEVEL /4.5f)));
                } else if (current_finger_spacing < finger_spacing && ZOOM_LEVEL > 1){
                    ZOOM_LEVEL -=0.5f;
                    zSlider.setValue((float) getZoomValueSingleDecimal((ZOOM_LEVEL /4.5f)));
                }
                setZoom(maxzoom, (int) ZOOM_LEVEL);
            }
            finger_spacing = current_finger_spacing;
        } catch (CameraAccessException e) {
            throw new RuntimeException("can not access camera.", e);
        }

    }

    private void setZoom(float maxZoom, int zoom_level) throws CameraAccessException {
        int minW = (int) (zoomRect.width()  / maxZoom);
        int minH = (int) (zoomRect.height() / maxZoom);
        int cropW = (zoomRect.width() - minW)  / 100 * zoom_level;
        int cropH = (zoomRect.height() - minH) / 100 * zoom_level;
        zoom = new Rect(cropW, cropH, zoomRect.width() - cropW, zoomRect.height() - cropH);

        zText = getZoomValueSingleDecimal(zoom_level/4.5f)+"";
        zoomText.setVisibility(View.VISIBLE);
        zoomText.setText(zText);

        previewCaptureRequest.set(CaptureRequest.SCALER_CROP_REGION, zoom);
        try {
            if(getState() == CamState.HSVIDEO_PROGRESSED)
                highSpeedCaptureSession.createHighSpeedRequestList(previewCaptureRequest.build());
            else
                camSession
                    .setRepeatingRequest(previewCaptureRequest.build(), null, mBackgroundHandler);
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
        }
    }

     private void focus(int height, int width, @Nullable MotionEvent event) {
         focusCircle.removeCallbacks(hideFocusCircle);
         float h;
         float w;
         if(Objects.equals(event,null)){
             h = width/2f;
             w = height/2f;
         }
         else {
             h = event.getX();
             w = event.getY();
         }

         focusCircle.setVisibility(View.VISIBLE);
         focusCircle.setPosition((int)h,(int)w);
         focusCircle.postDelayed(hideFocusCircle,1200);

         Log.e(TAG, "touchToFocus: F O C U S I N G");
         final Rect sensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

         final int y = (int)((h / (float)width)  * (float)sensorArraySize.height());
         final int x = (int)((w / (float)height) * (float)sensorArraySize.width());

         final int halfTouchWidth  = 150;
         final int halfTouchHeight = 150;

         MeteringRectangle focusAreaTouch = new MeteringRectangle(Math.max(x - halfTouchWidth,  0),
                 Math.max(y - halfTouchHeight, 0),
                 halfTouchWidth  * 2,
                 halfTouchHeight * 2,
                 MeteringRectangle.METERING_WEIGHT_MAX - 1);


         resetAE();
         resetFocus();

         previewCaptureRequest.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusAreaTouch});
         previewCaptureRequest.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{focusAreaTouch});
         previewCaptureRequest.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
         previewCaptureRequest.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
         previewCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

         //set focus area repeating,else cam forget after one frame where it should focus
         //trigger af start only once. cam starts focusing till its focused or failed
         previewCaptureRequest.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
         previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
         buildPreview();

         previewCaptureRequest.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
         previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
         buildPreview();

     }

     private void resetFocus(){
         previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
         buildPreview();
     }

     private void lockFocus(){
         previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
         buildPreview();
     }

     private void unlockFocus(){
         previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
         buildPreview();
     }

     private void resetAE(){
//         previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
         adjustExposure(0);
         exposureControl.setPosition(0);

         previewCaptureRequest.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
         previewCaptureRequest.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
         previewCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
         previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
         previewCaptureRequest.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);

         try {
             camSession.setRepeatingRequest(previewCaptureRequest.build(),null,focusHandler);
         } catch (CameraAccessException e) {
             e.printStackTrace();
         }
     }

     private void buildPreview(){
         try {
             camSession.capture(previewCaptureRequest.build(), captureCallbackHandler, focusHandler);
         } catch (CameraAccessException e) {
             e.printStackTrace();
         }
     }

     private CameraCaptureSession.CaptureCallback captureCallbackHandler = new CameraCaptureSession.CaptureCallback() {
         @Override
         public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
             super.onCaptureCompleted(session, request, result);
         }
         @Override
         public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
             super.onCaptureFailed(session, request, failure);
             Log.e(TAG, "Manual AF failure: " + failure);
         }
     };

     private double getZoomValueSingleDecimal(float zoom_level) {
        BigDecimal bd = new BigDecimal(Double.toString(zoom_level));
        bd = bd.setScale(1, RoundingMode.HALF_DOWN);
        return bd.doubleValue();
    }

    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void setExposureRange(){
        Range<Integer> r = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        Log.e(TAG, "EXPOSURE RANGE : "+r);
//        Log.e(TAG, "EXPOSURE RANGE : "+characteristics.get(CameraCharacteristics));
        exposureControl.setMaxValue(r.getLower());
        exposureControl.setMinValue(r.getUpper());

        exposureControl.setPosition(0);
    }

    private void adjustExposure(float progress){
        previewCaptureRequest.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, (int) progress);
        try {
            if(getState() == CamState.HSVIDEO_PROGRESSED)
                highSpeedCaptureSession.createHighSpeedRequestList(previewCaptureRequest.build());
            else
                camSession.setRepeatingRequest(previewCaptureRequest.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        Log.e(TAG, "onCaptureCompleted: AE LOCK "+result.get(CaptureResult.CONTROL_AF_MODE)); //1 AFMODEAUTO
                        Log.e(TAG, "onCaptureCompleted: AE LOCK "+result.get(CaptureResult.CONTROL_AE_MODE)); //1 AEMODEON
                        Log.e(TAG, "onCaptureCompleted: AE LOCK "+result.get(CaptureResult.CONTROL_AE_STATE)); //2 AESTATEPRECONVERGED
                        Log.e(TAG, "onCaptureCompleted: AE LOCK "+result.get(CaptureResult.CONTROL_AE_ANTIBANDING_MODE)); //3 ANITBANDINGMODEAUTO
                        Log.e(TAG, "onCaptureCompleted: AE LOCK "+result.get(CaptureResult.CONTROL_AF_TRIGGER)); //0 AFTRIGGERIDLE
                        Log.e(TAG, "onCaptureCompleted: AE LOCK _________________________________________________________");
                    }
                    @Override
                    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                        super.onCaptureFailed(session, request, failure);
                        Log.e(TAG, "Manual AF failure: " + failure);
                    }
                }, mBackgroundHandler);
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * CAMERA M E T H O D S
     */

    private void openCamera() {
        if (!resumed || !surface)
            return;

        if(getState() == CamState.CAMERA || getState() == CamState.PORTRAIT){
            imageSize = (ASPECT_RATIO_43 ? lensData.getHighestResolution(getCameraId()) :
                    lensData.getHighestResolution169(getCameraId()));
        }
        else if(getState() == CamState.HIRES){
            imageSize = lensData.getBayerLensSize();
        }

        zoomRect = getCameraCharacteristics().get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        StreamConfigurationMap map = getCameraCharacteristics().get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        previewSize = getPreviewResolution(map.getOutputSizes(ImageFormat.JPEG)
                ,(lensData.is1080pCapable(getCameraId())
                        ? 1080 : 720),ASPECT_RATIO_43);

        tvPreview.measure(previewSize.getHeight(), previewSize.getWidth());
        stPreview.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

        surfaceList.clear();
        surfaceList.add(new Surface(stPreview));

        imgReader = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), ImageFormat.JPEG, 5);
        imgReader.setOnImageAvailableListener(snapshotImageCallback, mBackgroundHandler);
        surfaceList.add(imgReader.getSurface());

        //RESET ZOOM
        zSlider.setValue(0f);
        ZOOM_LEVEL = 0;
        Log.e(TAG, "openCamera: ImageReader preview size " + previewSize.getWidth() + "x" + previewSize.getHeight());
        Log.e(TAG, "openCamera: ImageReader capture size " + imageSize.getWidth() + "x" + imageSize.getHeight());

        //Init focus controller

        try {
            if (ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestRuntimePermission();
                return;
            }
            camManager.openCamera(getCameraId(), imageCaptureCallback , mHandler);
        } catch(Exception e) {
            Log.e(TAG, "openCamera: open failed: " + e.getMessage());
        }
    }

    private void captureImage() {
        if(!ready)
            return;
        try {
            //check for ZSL support then only add ZSL request
            captureRequest = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequest.set(CaptureRequest.JPEG_QUALITY,(byte) 100);
            captureRequest.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation());
//            captureRequest.set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_OFF);

            captureRequest.addTarget(surfaceList.get(1));
            if(mflash) {
                captureRequest.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
            }
            else {
                captureRequest.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            }

            if(ZOOM_LEVEL !=1) {
                captureRequest.set(CaptureRequest.SCALER_CROP_REGION, zoom);
            }
            camSession.capture(captureRequest.build(), snapshotCallback, mHandler);
        } catch(Exception e) {
            Log.e(TAG, "captureImage: "+e);
        }
    }

    private void closeCamera() {
        if (null != camDevice) {
            camDevice.close();
            camDevice = null;
        }
        if (null != imgReader) {
            imgReader.close();
            imgReader = null;
        }
        if(null != snapshotImageReader){
            snapshotImageReader.close();
            snapshotImageReader = null;
        }
    }

    /**
     * VIDEO M E T H O D S
     */

    private void createVideoPreview(int height,int width){
        if (!resumed || !surface)
            return;

        StreamConfigurationMap map = getCameraCharacteristics().get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Log.e(TAG, "createVideoPreview: "+ Arrays.toString(map.getOutputSizes(MediaRecorder.class)));

        mVideoSize = getPreviewResolution(map.getOutputSizes(MediaRecorder.class),width,false);

        snapshotImageReader = ImageReader.newInstance(width,height,ImageFormat.JPEG,10);
        snapshotImageReader.setOnImageAvailableListener(videoSnapshotCallback,mHandler);

        update_chip_text(mVideoSize.getHeight()+"",vFPS+"");

        Log.e(TAG, "createVideoPreview: mVideoSize : "+mVideoSize);
        stPreview.setDefaultBufferSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        tvPreview.setAspectRatio(mVideoSize.getHeight(),mVideoSize.getWidth());
        Surface previewSurface = new Surface(stPreview);
        try {
            previewCaptureRequest = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewCaptureRequest.addTarget(previewSurface);
//            previewCaptureRequest.set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_OFF);
//            camDeviceCaptureRequest.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,new Range<>(24,60));
            previewCaptureRequest.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE
                    ,CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
//            captureRequest.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE
//                    ,CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);

            camDevice.createCaptureSession(Collections.singletonList(previewSurface)
                    , new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            camSession = session;
                            try {
                                camSession.setRepeatingRequest(previewCaptureRequest.build()
                                        , null,mBackgroundHandler);

                                focus = new FocusControls(getCameraCharacteristics(),focusCircle,hideFocusCircle,getState(),session
                                        ,previewCaptureRequest,highSpeedCaptureSession,mBackgroundHandler);
                                tvPreview.setOnTouchListener(touchListener);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

     private void setupMediaRecorder(CamcorderProfile camcorderProfile) {
         try {
             mVideoFileName = "CamX" + System.currentTimeMillis() + "_" + getCameraId() + ".mp4";
             Log.e(TAG, "setupMediaRecorder: CP : h : " + camcorderProfile.videoFrameHeight
                     + " w : " + camcorderProfile.videoFrameWidth);
             Log.e(TAG, "setupMediaRecorder: mVideo : h : " + mVideoSize.getHeight()
                     + " w : " + mVideoSize.getWidth());
             Log.e(TAG, "setupMediaRecorder: vBR : " + camcorderProfile.videoBitRate);

             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) mMediaRecorder = new MediaRecorder(getApplicationContext());
             else mMediaRecorder = new MediaRecorder();
             mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
             mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
             mMediaRecorder.setAudioSamplingRate(96);
             mMediaRecorder.setAudioEncodingBitRate(96000); //FIXME : UNABLE TO SET HIGHER THAN 48kbits/sec
             mMediaRecorder.setAudioEncodingBitRate(camcorderProfile.audioBitRate);
             mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
             mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.HEVC);
             mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
             mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
             mMediaRecorder.setVideoFrameRate(vFPS);
             mMediaRecorder.setOrientationHint(90);      //TODO : CHANGE ACCORDING TO SENSOR ORIENTATION
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 mMediaRecorder.setOutputFile(new File("//storage//emulated//0//DCIM//Camera//" + mVideoFileName));
             } else {
                 mMediaRecorder.setOutputFile("//storage//emulated//0//DCIM//Camera//" + mVideoFileName);
             }
             mMediaRecorder.setVideoEncodingBitRate(16400000);
             mMediaRecorder.prepare();           //FIXME : prepare fails on emulator(sdk24)
         }
         catch (IOException e){
             e.printStackTrace();
         }
     }

     private void startRecording(){
        try {
            //TODO: ADD CHECKS FOR FRONT AND BACK ONLY
            int id = (getCameraId().equals("0") ? 0 : 1);
            CamcorderProfile camcorderProfile = CamcorderProfile.get(id
                    ,CamcorderProfile.QUALITY_HIGH);
            if(!isVRecording){
                setupMediaRecorder(camcorderProfile);
                mMediaRecorder.start();
            }
            SurfaceTexture surfaceTexture = tvPreview.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture); //TODO : free surface with #release
            Surface recordSurface = mMediaRecorder.getSurface();
            previewCaptureRequest = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            previewCaptureRequest.addTarget(previewSurface);
            previewCaptureRequest.addTarget(recordSurface);
//            previewCaptureRequest.set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_OFF);
            camDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface, snapshotImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            camSession = session;
                            try {
                                camSession.setRepeatingRequest(
                                        previewCaptureRequest.build(),null, null
                                );
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.d(TAG, "onConfigureFailed: startRecord");
                        }
                    }, null);
        }
        catch (IllegalStateException | CameraAccessException e){
            e.printStackTrace();
        }
    }

    private Size getPreviewResolution(Size[] outputSizes, int resolution, boolean aspectRatio43) {
        ArrayList<Size> sizeArrayList = new ArrayList<>();
        for(Size size : outputSizes){
            float ar = (float) size.getWidth()/ size.getHeight();
            if(aspectRatio43) {
//                Log.e(TAG, "getPreviewResolution: AR43 resolution : "+resolution);
                if (size.getHeight() == resolution && ar > 1.2f) {
                    sizeArrayList.add(size);
                }
            }
            else {
//                Log.e(TAG, "getPreviewResolution: AR169 resolution : "+resolution);
                if (size.getHeight() == resolution && ar > 1.6f) {
                    sizeArrayList.add(size);
                }
            }
        }
        if(sizeArrayList.size() > 0){
            return Collections.min(sizeArrayList,new CompareSizeByArea());
        }
        else{
//            Log.e(TAG, "getPreviewResolution: OTHER WAY ROUND");
            return outputSizes[0];
        }
    }

    /**
     * Slow Motion Methods
     */

    private void createSloMoPreview(int height, int width){
        if (!resumed || !surface)
            return;
        if(sloMoPair != null){
            mVideoSize = new Size(height,width);
        }
        if(camDevice==null){
            openCamera();
        }
        else {
            hfrSurfaceList.clear();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mMediaRecorder = new MediaRecorder(getApplicationContext());
            }
            else {
                mMediaRecorder = new MediaRecorder();
            }
            update_chip_text(width + "", sFPS + "");
            stPreview.setDefaultBufferSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            tvPreview.setAspectRatio(mVideoSize.getHeight(), mVideoSize.getWidth());
            hfrSurfaceList.add(new Surface(stPreview));
            Surface previewSurface = new Surface(stPreview);
            try {
                previewCaptureRequest = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewCaptureRequest.addTarget(previewSurface);
                previewCaptureRequest.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, sloMoPair.second);

                camDevice.createCaptureSession(Collections.singletonList(previewSurface)
                        , new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                camSession = session;
                                smPreview();
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                            }
                        }, null);
            } catch (CameraAccessException | IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    private void smPreview() {
        try {
            if(isSLRecording) {
                previewCaptureRequest.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(sFPS,sFPS));
                camSession.setRepeatingBurst(
                        highSpeedCaptureSession.createHighSpeedRequestList(previewCaptureRequest.build())
                        , null, mBackgroundHandler);
            }
            else{
                camSession.setRepeatingRequest(previewCaptureRequest.build(),null,mBackgroundHandler);

                focus = new FocusControls(getCameraCharacteristics(),focusCircle,hideFocusCircle,getState(),camSession
                        ,previewCaptureRequest,highSpeedCaptureSession,mBackgroundHandler);
                tvPreview.setOnTouchListener(touchListener);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startSloMoRecording(){
        try {
            isSLRecording = true;
            setupMediaRecorder_SloMoe(sloMoPair);

            SurfaceTexture surfaceTexture = tvPreview.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture); //TODO : free surface with #release
            Surface recordSurface = mMediaRecorder.getSurface();
            previewCaptureRequest = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            previewCaptureRequest.addTarget(previewSurface);
            previewCaptureRequest.addTarget(recordSurface);
            camDevice.createConstrainedHighSpeedCaptureSession(hfrSurfaceList,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            camSession = session;
                            highSpeedCaptureSession =(CameraConstrainedHighSpeedCaptureSession) session;
                            smPreview();
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.d(TAG, "onConfigureFailed: startRecord");
                        }
                    }, mBackgroundHandler);
            mMediaRecorder.start();
        }
        catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void setupMediaRecorder_SloMoe(Pair<Size,Range<Integer>> size) {
        mVideoFileName = "CamX"+System.currentTimeMillis()+"_"+getCameraId()+".mp4";
        mMediaRecorder.reset();
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mMediaRecorder.setOutputFile(new File("//storage//emulated//0//DCIM//Camera//"+mVideoFileName));
        }
        else {
            mMediaRecorder.setOutputFile("//storage//emulated//0//DCIM//Camera//"+mVideoFileName);
        }
        mMediaRecorder.setVideoFrameRate(sFPS);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        Log.e(TAG, "setupMediaRecorder_SloMoe: VideoEncodingBitRate : "+(size.first.getWidth()*size.first.getHeight()*size.second.getLower()) / 15);
        mMediaRecorder.setVideoEncodingBitRate((size.first.getWidth()*size.first.getHeight()*size.second.getLower()) / 15);
        mMediaRecorder.setOrientationHint(getJpegOrientation());
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        hfrSurfaceList.add(mMediaRecorder.getSurface());
    }

    /**
     *  U N C A N N Y  M E T H O D S
     */

    private CameraCharacteristics getCameraCharacteristics() {
        if(camManager == null){
            camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        }
        try {
            characteristics = camManager.getCameraCharacteristics(getCameraId());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return characteristics;
    }

    private int getJpegOrientation() {
        int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();

        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        CameraCharacteristics c = getCameraCharacteristics();
        int sensorOrientation =  c.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int surfaceRotation = ORIENTATIONS.get(deviceOrientation);


//        // Round device orientation to a multiple of 90
//        deviceOrientation = (deviceOrientation + 45) / 90 * 90;
//
//        // Reverse device orientation for front-facing cameras
//        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
//        if (facingFront) deviceOrientation = -deviceOrientation;
//
//        // Calculate desired JPEG orientation relative to camera orientation to make
//        // the image upright relative to the device orientation
//
//        return (sensorOrientation + deviceOrientation + 360) % 360;

        return (surfaceRotation + sensorOrientation + 270) % 360;
    }

    private int getRotationCompensation(boolean isFrontFacing) {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // Get the device's sensor orientation.
        int sensorOrientation = getCameraCharacteristics().get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (isFrontFacing) {
            rotationCompensation = (sensorOrientation + rotationCompensation) % 360;
        } else { // back-facing
            rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360;
        }
        return rotationCompensation;
    }

    private int translateResolution(String selectedItem) {
        switch (selectedItem) {
            case "HD":
            case "720":
                return 720;
            case "FHD":
            case "1080":
                return 1080;
            case "4K":
            case "2160":
                return 2160;
            case "8K":
                return 4320;
        }
        return 0;
    }

    private int correspondingHeight(int width) {
        if(width == 720) return 1280;
        if(width == 1080) return 1920;
        if(width == 3840) return 2160;
        return 0;
    }

    private void addCapableVideoResolutions() {
        resolutionSelector.clearHeaderItems();
        resolutionSelector.clearHeaderAndFooterItems();
        resolutionSelector.setHeader("HD");
        if(lensData.is1080pCapable(getCameraId()))
            resolutionSelector.setHeader("FHD");
        if(lensData.is4kCapable(getCameraId()))
            resolutionSelector.setHeader("4K");
        if(lensData.is8kCapable(getCameraId()))
            resolutionSelector.setHeader("8K");

        resolutionSelector.setHaloByHeaderText(lensData.is1080pCapable(getCameraId()) ? "FHD":"HD");
    }

    private void addCapableSloMoResolutions(){
        resolutionSelector.clearHeaderItems();
        resolutionSelector.clearHeaderAndFooterItems();

        ArrayList<String> pairList = new ArrayList<>();
        for(Pair<Size,Range<Integer>> pair : lensData.getFpsResolutionPair(getCameraId())){
            if((pair.first.getWidth()==3840 && pair.first.getHeight()==2160) ||
              (pair.first.getWidth()==1920 && pair.first.getHeight()==1080) ||
              (pair.first.getWidth()==1280 && pair.first.getHeight()==720)){
                if(!pairList.contains(pair.first.getHeight()+"P"+pair.second.getUpper()+"FPS")) {
                    pairList.add(pair.first.getHeight()+"P_"+pair.second.getUpper()+"FPS");
                }
            }
        }
        resolutionSelector.setHeaderAndFooterItems(pairList);
        resolutionSelector.setHaloByHeaderAndFooterText(sloMoPair.first.getHeight()+"P_"+ sloMoPair.second.getUpper()+"FPS");
    }

    private String getCameraId() {
        return cameraId;
    }

    private void setCameraId(String cameraId) {
        CameraActivity.cameraId = cameraId;
    }

    private void applyModeChange(CamState state) {
        switch (state) {
            case CAMERA:
                modifyMenuForPhoto();
                break;
            case VIDEO:
                addCapableVideoResolutions();
                break;
            case SLOMO:
                modeSloMo();
                break;
            case TIMEWARP:
                modeTimeWarp();
                break;
            case HIRES:
                modeHiRes();
                break;
            case PORTRAIT:
                modePortrait();
                break;
            case NIGHT:
                modeNight();
                break;
            case PRO:
                modePro();
                break;
        }
    }

    private int getScreenWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int getScreenHeight() {
        Log.e(TAG, "getScreenHeight: "+getResources().getDisplayMetrics().heightPixels);
        return getResources().getDisplayMetrics().heightPixels;
    }

    private void displayMediaThumbnailFromGallery() {
        ImageDecoderThread idt;
        Completable.fromRunnable(idt = new ImageDecoderThread())
                .subscribeOn(Schedulers.from(executor))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> thumbPreview.setImageBitmap(idt.getBitmap()));
    }

    private void requestVideoPermissions() {
        if (ActivityCompat.checkSelfPermission(CameraActivity.this
                , Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CameraActivity.this
                    , new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
                            , Manifest.permission.RECORD_AUDIO}
                    , REQUEST_PERMISSIONS);
        }
    }

    private void requestRuntimePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(CameraActivity.this
                    , PERMISSION_STRING
                    , REQUEST_PERMISSIONS);
        }
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        focusHandlerThread =new HandlerThread("Focus Thread");
        focusHandlerThread.start();
        focusHandler = new Handler(focusHandlerThread.getLooper());
    }

    protected void stopBackgroundThread() {
        if(mBackgroundThread!=null){
            mBackgroundThread.quitSafely();
            focusHandlerThread.quitSafely();
        }
        else{
            finishAffinity();
            return;
        }
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;

            focusHandlerThread.join();
            focusHandlerThread = null;
            focusHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * C A L L B A C K S
     */

    ImageReader.OnImageAvailableListener snapshotImageCallback = imageReader -> {
        Log.e(TAG, "onImageAvailable: received snapshot image data");
        Completable.fromRunnable(new ImageSaverThread(this,
                imageReader.acquireLatestImage()
                , cameraId
                , getContentResolver()
                , (getState() == CamState.PORTRAIT)
                , false
                , getRotationCompensation((getCameraId().equals("1"))) ))
                .subscribeOn(Schedulers.io())
        .subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onError(@NonNull Throwable e) {
                Toast.makeText(CameraActivity.this, "Could Not Save Image", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {
                displayMediaThumbnailFromGallery();
            }
        });

    };

    ImageReader.OnImageAvailableListener videoSnapshotCallback = reader -> {
        Log.e(TAG, "onImageAvailable: received video snapshot image data");
        new Handler(Looper.getMainLooper()).post(new ImageSaverThread(reader.acquireLatestImage()
                ,cameraId
                ,getContentResolver()));
    };

    CameraDevice.StateCallback imageCaptureCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            camDevice = cameraDevice;
            try {
                if(getState() == CamState.VIDEO){
                    createVideoPreview(tvPreview.getHeight(),(lensData.is1080pCapable(getCameraId())
                            ? 1080 : 720));
                    return;
                }
                if(getState() == CamState.SLOMO){
                    createSloMoPreview(sloMoPair.first.getWidth(), sloMoPair.first.getHeight());
                    return;
                }
                camDevice.createCaptureSession(surfaceList,stateCallback, mBackgroundHandler);
                tvPreview.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                Log.e(TAG, "onOpened: tvPreview.SetAspectRatio : h : "+ previewSize.getHeight() + " w : "+ previewSize.getWidth());

            } catch (Exception e) {
                Log.e(TAG, "onOpened: session create failed: " + e.getMessage());
                camDevice = null;
                finish();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.e(TAG, "onDisconnected: camera disconnected");
            closeCamera();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Log.e(TAG, "onError: camera failed: " + i);
            camDevice = null;
            finish();
        }
    };

    CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            camSession = cameraCaptureSession;
            try {
                previewCaptureRequest = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewCaptureRequest.addTarget(surfaceList.get(0));

                focus = new FocusControls(getCameraCharacteristics(),focusCircle,hideFocusCircle,getState(),camSession
                        ,previewCaptureRequest,highSpeedCaptureSession,focusHandler);
                tvPreview.setOnTouchListener(touchListener);
//                camDeviceCaptureRequest.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE
//                        ,CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE);
//                previewCaptureRequest.set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_OFF);
                //FRONT CAMERA INVERSION FIX
                if(characteristics.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_FRONT){
                    previewCaptureRequest.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(Surface.ROTATION_180));
                }
                else {
                    previewCaptureRequest.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(0));
                }
                camSession.setRepeatingRequest(previewCaptureRequest.build(), previewCallback, null);
                ready = true;
            } catch (Exception e) {
                Log.e(TAG, "IMAGE CAPTURE CALLBACK: onConfigured: create preview failed: " + e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.e(TAG, "IMAGE CAPTURE CALLBACK: onConfigureFailed: configure failed");
        }
    };

    CameraCaptureSession.CaptureCallback previewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

//            Log.e(TAG, "onCaptureCompleted: PREVIEW "+result.get(CaptureResult.CONTROL_MODE)); //1 CONTROLMODEAUTO
//            Log.e(TAG, "onCaptureCompleted: PREVIEW "+result.get(CaptureResult.CONTROL_AF_MODE)); //4 AFMODECONTINUOUSPICTURE
//            Log.e(TAG, "onCaptureCompleted: PREVIEW "+result.get(CaptureResult.CONTROL_AE_MODE)); //1 AEMODEON
//            Log.e(TAG, "onCaptureCompleted: PREVIEW "+result.get(CaptureResult.CONTROL_AE_PRECAPTURE_TRIGGER)); //0 AE_PRECAPTURE_TRIGGER_IDLE
//            Log.e(TAG, "onCaptureCompleted: PREVIEW "+result.get(CaptureResult.CONTROL_AF_TRIGGER)); //0 CONTROL_AF_TRIGGER_IDLE
//            Log.e(TAG, "onCaptureCompleted: PREVIEW "+result.get(CaptureResult.CONTROL_AE_STATE)); //1 CONTROL_AE_STATE_SEARCHING
//            Log.e(TAG, "onCaptureCompleted: PREVIEW "+result.get(CaptureResult.CONTROL_AF_TRIGGER)); //0 AFTRIGGERIDLE
//            Log.e(TAG, "onCaptureCompleted: PREVIEW "+ Arrays.toString(result.get(CaptureResult.CONTROL_AE_REGIONS))); //[(x:0, y:0, w:0, h:0, wt:0)]
//            Log.e(TAG, "onCaptureCompleted: PREVIEW "+ Arrays.toString(result.get(CaptureResult.CONTROL_AF_REGIONS))); //[(x:1400, y:1050, w:1200, h:916, wt:0)]
//            Log.e(TAG, "onCaptureCompleted: PREVIEW _________________________________________________________");

//            Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
//            Log.e(TAG, "onCaptureCompleted: FACES : "+faces.length);
//            if(faces.length > 0){
//                drawFaceRect(faces);
//            }
        }
        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.e(TAG, "onCaptureFailed: lost preview");
        }
    };

    CameraCaptureSession.CaptureCallback snapshotCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);

        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.d(TAG, "onCaptureProgressed() returned:  capture progressed");
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.e(TAG, "onCaptureFailed: lost snapshot");
        }
    };

    /**
     * LIFECYCLE METHODS
     */

    @Override
    public void onResume() {
        super.onResume();
        setAestheticLayout();
        resumed = true;
        startBackgroundThread();
        openCamera();
        displayMediaThumbnailFromGallery();
        applyModeChange(getState());
        tvPreview.setOnTouchListener(touchListener);
        shutter.animateInnerCircle(getState());
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        ready = false;
        resumed = false;
        tvPreview.setOnTouchListener(null);
        if(getState() == CamState.CAMERA)
            closeCamera();
        else if(getState() == CamState.VIDEO) {
            if(isVRecording) {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBackgroundThread();
//        rs.destroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    View view = findViewById(R.id.capture);
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
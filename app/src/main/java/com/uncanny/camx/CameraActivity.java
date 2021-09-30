 package com.uncanny.camx;

 import android.Manifest;
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
 import android.hardware.camera2.CameraMetadata;
 import android.hardware.camera2.CaptureFailure;
 import android.hardware.camera2.CaptureRequest;
 import android.hardware.camera2.CaptureResult;
 import android.hardware.camera2.TotalCaptureResult;
 import android.hardware.camera2.params.MeteringRectangle;
 import android.hardware.camera2.params.StreamConfigurationMap;
 import android.media.CamcorderProfile;
 import android.media.Image;
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
 import android.renderscript.Allocation;
 import android.renderscript.RenderScript;
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
 import com.uncanny.camx.Data.LensData;
 import com.uncanny.camx.UI.CaptureButton;
 import com.uncanny.camx.UI.GestureBar;
 import com.uncanny.camx.UI.HorizontalPicker;
 import com.uncanny.camx.UI.ResolutionSelector;
 import com.uncanny.camx.UI.UncannyChronometer;
 import com.uncanny.camx.UI.ViewFinder.AutoFitPreviewView;
 import com.uncanny.camx.UI.ViewFinder.FocusCircle;
 import com.uncanny.camx.UI.ViewFinder.Grids;
 import com.uncanny.camx.Utils.CompareSizeByArea;
 import com.uncanny.camx.Utils.ImageDecoderThread;
 import com.uncanny.camx.Utils.ImageSaverThread;

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
 import java.util.Vector;

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

    private int resultCode = 1;
    private long time;
    private Size previewSize;
    private Size imageSize;
    private Size mVideoSize;

    private ImageReader snapshotImageReader;
    private float mZoom = 1.0f;
    public float zoom_level = 1;
    public float finger_spacing = 0;
    private int cachedHeight;
    private Rect zoom = new Rect();

    private Pair<Size,Range<Integer>> sloMoPair;
    private List<Integer> cameraList;
    private List<Integer> auxCameraList;

    private int gridClick = 0;

    private CamState getState() {
        return state;
    }

    private void setState(CamState state) {
        this.state = state;
    }

    private CamState state = CamState.CAMERA;
    private boolean isVRecording = false;
    private boolean isSLRecording = false;
    private boolean isVideoPaused = false;
    private boolean mflash = false;
    private volatile boolean is_sliding = false;
    private boolean firstTouch = false;
    private boolean ASPECT_RATIO_43 = true;
    private static boolean ASPECT_RATIO_169 = false;
    private int timer_cc = 1;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

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
            if(zSlider.getVisibility()==View.VISIBLE && !is_sliding){
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

    private Runnable hideAuxDock = new Runnable() {
        @Override
        public void run() {
            if(characteristics.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_BACK){
                if(getState() == CamState.SLOMO
                        || getState() == CamState.HIRES || getState() == CamState.VIDEO_PROGRESSED){
                    auxDock.setVisibility(View.INVISIBLE);
                }
                else{
                    auxDock.setVisibility(View.VISIBLE);
                }
            }
            else{                                       //FIXME: HANDLE FOR MULTIPLE FRONT CAMERA
                auxDock.setVisibility(View.INVISIBLE);
            }
        }
    };

    private int cachedScreenWidth ;
    private int cachedScreenHeight;
    private Map<Integer,Integer> modeMap = new HashMap<>(); //cameraId,modeIndex
    private String[][] CachedCameraModes = new String[10][];

    private RenderScript rs;
    private Allocation alloc;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_camX);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        cachedScreenWidth  = getScreenWidth();
        cachedScreenHeight = getScreenHeight();

        tvPreviewParent.setClipToOutline(true);

//        rs = RenderScript.create(CameraActivity.this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        tvPreview.setClipToOutline(true);
        Log.e(TAG, "onPostCreate: CAN CLIP : "+tvPreview.getClipToOutline());
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
        resolutionSelector = findViewById(R.id.resolution_selector);
        addCapableVideoResolutions();

        mModePicker.setValues(lensData.getAvailableModes(getCameraId()));
        mModePicker.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mModePicker.setOnItemSelectedListener(index -> {
            mModePicker.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            vi_info = findViewById(R.id.vi_indicator);
            auxDock.post(hideAuxDock);
            switch (index){
                case 0:
                    setState(CamState.CAMERA);
                    modeCamera();
                    Log.e(TAG, "onItemSelected: CAMERA MODE");
                    break;
                case 1:
                    setState(CamState.VIDEO);
                    modeVideo();
                    Log.e(TAG, "onItemSelected: VIDEO MODE");
                    break;
                case 2:
                    setState(mModePicker.getValues()[2]=="Slo Moe" ? CamState.SLOMO
                            :(mModePicker.getValues()[2]=="HighRes") ? CamState.HIRES : CamState.PORTRAIT);
                    if(getState() == CamState.SLOMO){
                        modeSloMo();
                    }
                    else if(getState() == CamState.HIRES){
                        Log.e(TAG, "onPostCreate: "+lensData.getBayerLensSize());
                        modeHiRes();
                    }
                    else{
                        //PORTRAIT
                        modePortrait();
                    }
                    Log.e(TAG, "onItemSelected: "+mModePicker.getValues()[2]);
                    break;
                case 3:
                    setState(mModePicker.getValues()[3]=="TimeWarp" ? CamState.TIMEWARP
                            :(mModePicker.getValues()[3]=="Portrait") ? CamState.PORTRAIT : CamState.NIGHT);
                    if(getState() == CamState.TIMEWARP){
                        modeTimeWarp();
                    }
                    else if(getState() == CamState.PORTRAIT){
                        modePortrait();
                    }
                    else{
                        modeNight();
                    }
                    Log.e(TAG, "onItemSelected: "+mModePicker.getValues()[3]);
                    break;
                case 4:
                    setState(mModePicker.getValues()[4]=="HighRes" ? CamState.HIRES
                            :(mModePicker.getValues()[4]=="Night") ? CamState.NIGHT : CamState.PRO);
                    modifyMenuForPhoto();
                    if(getState() == CamState.HIRES){
                        modeHiRes();
                    }
                    else if(getState() == CamState.NIGHT){
                        modeNight();
                    }
                    else {
                        modePro();
                    }
                    Log.e(TAG, "onItemSelected: "+mModePicker.getValues()[4]);
                    break;
                case 5:
                    setState(mModePicker.getValues()[5]=="Portrait" ? CamState.PORTRAIT:CamState.PRO);
                    if(getState() == CamState.PORTRAIT) {
                        modePortrait();
                    }
                    else{
                        modePro();
                    }
                    Log.e(TAG, "onItemSelected: "+mModePicker.getValues()[5]);
                    break;
                case 6:
                    setState(CamState.NIGHT);
                    modeNight();
                    Log.e(TAG, "onItemSelected: "+mModePicker.getValues()[6]);
                    break;
                case 7:
                    setState(CamState.PRO);
                    modePro();
                    Log.e(TAG, "onItemSelected: "+mModePicker.getValues()[7]);
                    break;
            }

            /*
             * For hiding and displaying fps info chip
             */
            vi_info.setVisibility(getState() == CamState.VIDEO || state == CamState.SLOMO || state == CamState.TIMEWARP ?
                    View.VISIBLE : View.INVISIBLE);
        });

        //Timer button
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        //Grid button
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

            }
        });

        //Flash button
        button4.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
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
            }
        });

        //Aspect Ratio button
        button23.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvPreview.setVisibility(View.INVISIBLE);
                button1.setColorFilter(Color.WHITE);
                if (ASPECT_RATIO_43) {
                    ASPECT_RATIO_169 = true;
                    ASPECT_RATIO_43 = false;

                    closeCamera();
                    openCamera();
                    tvPreview.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tvPreview.setVisibility(View.VISIBLE);
                        }
                    },400);

                    button23.setColorFilter(ContextCompat.getColor(getApplicationContext(),R.color.purple_200)
                            , PorterDuff.Mode.MULTIPLY);
                } else if (ASPECT_RATIO_169) {
                    ASPECT_RATIO_43 = true;
                    ASPECT_RATIO_169 = false;

                    closeCamera();
                    openCamera();
                    tvPreview.measure(previewSize.getHeight(), previewSize.getWidth());
                    tvPreview.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tvPreview.setVisibility(View.VISIBLE);
                        }
                    },400);


                    button23.setColorFilter(ContextCompat.getColor(getApplicationContext(),R.color.white)
                            , PorterDuff.Mode.MULTIPLY);
                }
            }
        });

        //Settings button
        button25.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(CameraActivity.this,SettingsActivity.class);
                settingsIntent.putExtra("c2api",lensData.getCamera2level());
                startActivity(settingsIntent);
            }
        });

        resolutionSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getState() == CamState.VIDEO) {
                    int width = translateResolution(resolutionSelector.getHeaderText());
                    createVideoPreview(tvPreview.getHeight(), width);
                }
                else if(getState() == CamState.SLOMO){
                    int width = translateResolution(resolutionSelector.getHeaderAndFooterText().split("P")[0]);
                    sFPS = Integer.parseInt(resolutionSelector.getHeaderAndFooterText()
                            .split("_")[1].split("FPS")[0]);
                    createSloMoPreview(correspondingHeight(width),width);
                }
            }
        });

        tvPreview.setSurfaceTextureListener(surfaceTextureListener);

        shutter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chronometer = findViewById(R.id.chronometer);
                switch (getState()) {
                    case CAMERA:
                    case HIRES:
                        captureImage();
                        shutter.animateInnerCircle(getState());
                        MediaActionSound sound = new MediaActionSound();
                        sound.play(MediaActionSound.SHUTTER_CLICK);
                        //TODO : ADD SEMAPHORE
                        new Handler(Looper.getMainLooper()).postDelayed(() -> displayMediaThumbnailFromGallery(), 1800);
                        break;
                    case VIDEO:
                        if(!isVRecording) {
                            startRecording();
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
                            mMediaRecorder.start();
                            chronometer.start();
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
                resolutionSelector.setState(getState());
            }
        });

        thumbPreview.setOnClickListener(openGallery);

        wide_lens.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mModePicker.setValues(CachedCameraModes[0]);
                if (!getCameraId().equals(BACK_CAMERA_ID)) {
                    closeCamera();
                    setCameraId(BACK_CAMERA_ID);
                    openCamera();
                    if(getState()==CamState.VIDEO) addCapableVideoResolutions();
                    else if(getState()==CamState.SLOMO) addCapableSloMoResolutions();

                    for(int id : auxCameraList){
                        tv = auxDock.findViewById(id);
                        tv.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview_small));
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,11);
                    }
                    wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview));
                    wide_lens.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
                }
            }
        });

        front_switch.setOnClickListener(switchFrontCamera);

        tvPreview.setOnTouchListener(touchListener);

        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inflateButtonMenu();
            }
        });

        gestureBar.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    inflateButtonMenu();
                }
                return false;
            }
        });

        try {
            mZoom = getMaxZoom();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        zSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                try {
                    setZoom(mZoom, value * 4.5f);
                } catch (CameraAccessException | IllegalStateException e) {
                    e.printStackTrace();
                }

            }
        });
        zSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                is_sliding = true;
                zSlider.removeCallbacks(hideZoomSlider);
                auxDock.removeCallbacks(hideAuxDock);
            }
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                auxDock.removeCallbacks(hideAuxDock);
                zSlider.removeCallbacks(hideZoomSlider);
                zoomText.removeCallbacks(hideZoomText);
                is_sliding = false;
                auxDock.postDelayed(hideAuxDock,2000);
                zSlider.postDelayed(hideZoomSlider,2000);
                zoomText.postDelayed(hideZoomText,2000);
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
        modifyMenuForPhoto();
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
            for (int i=0 ; i< auxCameraList.size() ; i++) {
                modeMap.put(auxCameraList.get(i),2+i);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) param
                        ,LinearLayout.LayoutParams.MATCH_PARENT);
                layoutParams.weight = 1.0f;
                MaterialTextView aux_btn = new MaterialTextView(this);
                aux_btn.setLayoutParams(layoutParams);
                aux_btn.setId(auxCameraList.get(i));
//                Log.e(TAG, "addAuxButtons: get aux id : "+aux_btn.getId());
                aux_btn.setGravity(Gravity.CENTER);
                aux_btn.setTextSize(TypedValue.COMPLEX_UNIT_SP,11);
                aux_btn.setText(auxCameraList.get(i).toString());
                aux_btn.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview_small));
                auxDock.addView(aux_btn);
                aux_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mModePicker.setValues(CachedCameraModes[modeMap.get(aux_btn.getId())]);
                        setCameraId(aux_btn.getId()+"");
                        closeCamera();
                        setCameraId(aux_btn.getId()+"");
                        openCamera();
                        if(getState() == CamState.VIDEO) addCapableVideoResolutions();
                        else if(getState() == CamState.SLOMO) addCapableSloMoResolutions();

                        wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview_small));
                        wide_lens.setTextSize(TypedValue.COMPLEX_UNIT_SP,11);
                        for(int id : auxCameraList){
                            if((id + "").equals(getCameraId())){
                                continue;
                            }
                            tv = auxDock.findViewById(id);
                            tv.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview_small));
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,11);
                        }

                        aux_btn.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
                        aux_btn.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview));
                    }
                });
            }
        }
        else{
            auxDock.setVisibility(View.GONE);
        }
    }

    private void setAestheticLayout() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                , WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_FULLSCREEN);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    private void inflateButtonMenu() {
        Log.e(TAG, "inflateButtonMenu: ch : "+cachedHeight+" rh : "+appbar.getHeight());
        if (cachedHeight == appbar.getHeight()) {
            tvPreview.setOnTouchListener(null);
            tvPreview.setClickable(false);
            tvPreview.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    tvPreview.setOnTouchListener(touchListener);
                    deflateButtonMenu();
                }
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

    private void setDockHeight() {
        RelativeLayout parent = findViewById(R.id.parent);
        int tvHeight = 1440;
        int height = parent.getHeight()-tvHeight-appbar.getHeight()-mModePicker.getHeight();
        if(dock.getMinimumHeight()<height) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(cachedScreenWidth
                    , height);
            layoutParams.addRule(RelativeLayout.ABOVE, R.id.mode_picker_view);
            dock.setLayoutParams(layoutParams);
        }
//        Log.e(TAG, "setDockHeight: parent height : "+parent.getHeight());
//        Log.e(TAG, "SETDOCKHEIGHT : pHeight : "+cachedScreenHeight+" tvHeight : "+tvHeight
//                +" apppbar : "+appbar.getHeight()+" hor : "+mModePicker.getHeight()
//                +" calculatedH : "+height+"minHeight : "+dock.getMinimumHeight());
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
        chip_Text = size+"p@"+fps+"fps";
        vi_info.setText(chip_Text);
        Log.e(TAG, "update_chip_text: CHIP UPDATED | "+chip_Text);
    }

    /**
     * LISTENERS
     */

    private View.OnClickListener switchFrontCamera = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            closeCamera();
            if (characteristics.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_BACK) {
                setCameraId(FRONT_CAMERA_ID);

                switch (getState()){
                    case PORTRAIT:
                        mModePicker.setSelectedItem(CachedCameraModes[1].length-3);
                        break;
                    case NIGHT:
                        mModePicker.setSelectedItem(CachedCameraModes[1].length-2);
                        break;
                    case PRO:
                        mModePicker.setSelectedItem(CachedCameraModes[1].length-1);
                        break;
                }

                openCamera();
                mModePicker.setValues(CachedCameraModes[1]);
                auxDock.post(hideAuxDock);
                front_switch.animate().rotation(180f).setDuration(300);
            } else {
                setCameraId(BACK_CAMERA_ID);

                switch (getState()){
                    case PORTRAIT:
                        mModePicker.setSelectedItem(CachedCameraModes[0].length-3);
                        break;
                    case NIGHT:
                        mModePicker.setSelectedItem(CachedCameraModes[0].length-2);
                        break;
                    case PRO:
                        mModePicker.setSelectedItem(CachedCameraModes[0].length-1);
                        break;
                }

                openCamera();
                mModePicker.setValues(CachedCameraModes[0]);
                wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circular_textview));
                auxDock.post(hideAuxDock);
                front_switch.animate().rotation(-180f).setDuration(300);
            }
            Log.e(TAG, "SWITCH CAMERA: getState : " + getState());
            applyModeChange(getState());
        }
    };

    private View.OnClickListener openGallery = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityForResult(intent, resultCode);
        }
    };

    private View.OnClickListener captureSnapshot = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                captureRequest = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
                captureRequest.addTarget(snapshotImageReader.getSurface());
                if(zoom_level!=1) {
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
            if(v.isInTouchMode()){
                v.performClick();
            }
            time = System.currentTimeMillis();
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if(firstTouch && (System.currentTimeMillis() - time) <= 50) {
                    /*
                     * DOUBLE TAP TO ZOOM
                     */
                    Log.e("** DOUBLE TAP**"," second tap ");
                    try {
                        doubleTapZoom();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    firstTouch = false;
                } else if(zSlider.getVisibility()!=View.VISIBLE){
                    /*
                     * TOUCH TO FOCUS
                     */
                    firstTouch = true;
                    Handler handler = HandlerCompat.createAsync(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            firstTouch = false;
                        }
                    }, 700);
                    touchToFocus(v,event);
                    time = System.currentTimeMillis();
                    Log.e("** SINGLE  TAP **"," First Tap time  "+time);
                    return false;
                }
            }
            /*
             * PINCH TO ZOOM
             */
            if (event.getPointerCount() > 1) {
                pinchToZoom(event);
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
            setDockHeight();
            Log.e(TAG, "onSurfaceTextureAvailable: FROM ST : "+cachedHeight);
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
        auxDock.removeCallbacks(hideAuxDock);
        zSlider.removeCallbacks(hideZoomSlider);
        float maxZoom = 1;
        try {
            maxZoom = getMaxZoom();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        float DT_value = maxZoom/2;

        Log.e(TAG, "doubleTaptoZoom: Zoom Level "+zoom_level);
        if(zoom_level<DT_value){
            setZoom(maxZoom,DT_value);
            zoom_level = DT_value;
            zSlider.setValue(5.0f);
        }
        else if(zoom_level>DT_value){
            setZoom(maxZoom,DT_value);
            zoom_level = DT_value;
            zSlider.setValue(5.0f);
        }
        else if(zoom_level==DT_value){
            setZoom(maxZoom,1f);
            zoom_level = 1f;
            zSlider.setValue(0f);
        }
        auxDock.post(hideAuxDock);
        zSlider.setVisibility(View.VISIBLE);

        if(!is_sliding) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                //TODO : ADD SEMAPHORE
                auxDock.post(hideAuxDock);
                zSlider.setVisibility(View.INVISIBLE);
            }, 1500);
        }
        Log.e(TAG, "doubleTaptoZoom: D O U B L E - T A P P E D");
    }

    private void touchToFocus(View v,MotionEvent event) {

        //TODO : create separate class & method for this
        focusCircle.removeCallbacks(hideFocusCircle);
        float h = event.getX();
        float w = event.getY();

//        focusCircle.animateInnerCircle();
        focusCircle.setVisibility(View.VISIBLE);
        focusCircle.setPosition((int)h,(int)w,getScreenWidth());
//        focusCircle.setPivotX((int)h);
//        focusCircle.setPivotY((int)w);
//        focusCircle.animate().scaleX(1.2f).scaleY(1.2f).setDuration(2000).setInterpolator(new CycleInterpolator(1));

        focusCircle.postDelayed(hideFocusCircle,2000);


        Log.e(TAG, "touchToFocus: F O C U S I N G");
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            characteristics = manager.getCameraCharacteristics(getCameraId());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        final Rect sensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        /*
         * here I just flip x,y, but this needs to correspond with the sensor orientation (via SENSOR_ORIENTATION)
         */
        final int y = (int)((event.getX() / (float)v.getWidth())  * (float)sensorArraySize.height());
        final int x = (int)((event.getY() / (float)v.getHeight()) * (float)sensorArraySize.width());

        /*
         * this doesn't represent actual touch size in pixel. Values range in [3, 10]...
         */
        final int halfTouchWidth  = 150; //(int)motionEvent.getTouchMajor();
        final int halfTouchHeight = 150; //(int)motionEvent.getTouchMinor();
        MeteringRectangle focusAreaTouch = new MeteringRectangle(Math.max(x - halfTouchWidth,  0),
                Math.max(y - halfTouchHeight, 0),
                halfTouchWidth  * 2,
                halfTouchHeight * 2,
                MeteringRectangle.METERING_WEIGHT_MAX - 1);

        CameraCaptureSession.CaptureCallback captureCallbackHandler = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
//                mManualFocusEngaged = false;

                if (request.getTag() == "FOCUS_TAG") {
                    //the focus trigger is complete -
                    //resume repeating (preview surface will get frames), clear AF trigger
                    previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, null);
                    try {
                        if(getState()==CamState.HSVIDEO_PROGRESSED)
                            highSpeedCaptureSession.createHighSpeedRequestList(previewCaptureRequest.build());
                        else
                            camSession.setRepeatingRequest(previewCaptureRequest.build(), null, null);

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
                Log.e(TAG, "Manual AF failure: " + failure);
//                mManualFocusEngaged = false;
            }
        };

        //first stop the existing repeating request
//        try {
//            cameraCaptureSessions.stopRepeating();
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }

        //cancel any existing AF trigger (repeated touches, etc.)
        previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        previewCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        try {
            if(getState()==CamState.HSVIDEO_PROGRESSED)
                highSpeedCaptureSession.createHighSpeedRequestList(previewCaptureRequest.build());
            else
                camSession.capture(previewCaptureRequest.build(), captureCallbackHandler, mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        //Now add a new AF trigger with focus region
        if (isMeteringAreaAFSupported()) {
            previewCaptureRequest.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusAreaTouch});
        }

        //TODO:FIX METERING AE
//        if(isMeteringAreaAESupported()){
//            camDeviceCaptureRequest.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{focusAreaTouch});
//        }
        previewCaptureRequest.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        previewCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        previewCaptureRequest.setTag("FOCUS_TAG"); //we'll capture this later for resuming the preview

        //then we ask for a single request (not repeating!)
        try {
            if(getState()==CamState.HSVIDEO_PROGRESSED)
                highSpeedCaptureSession.createHighSpeedRequestList(previewCaptureRequest.build());
            else
                camSession.capture(previewCaptureRequest.build(), captureCallbackHandler, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
//        mManualFocusEngaged = true;
    }

    private void pinchToZoom(MotionEvent event){
        auxDock.removeCallbacks(hideAuxDock);
        zSlider.removeCallbacks(hideZoomSlider);
        try {
            float maxzoom = getMaxZoom();
            float current_finger_spacing;
            // Multi touch logic
            current_finger_spacing = getFingerSpacing(event);
            if(finger_spacing != 0){
//                pinched = true;
                if(current_finger_spacing > finger_spacing && maxzoom > zoom_level){
                    zoom_level+=0.5f;
//                    Log.e(TAG, "pinchtoZoom: Zoom In "+zoom_level);

                    auxDock.post(hideAuxDock);
                    zSlider.setVisibility(View.VISIBLE);
                    zSlider.setValue((float) getZoomValueSingleDecimal((zoom_level/4.5f)));
                } else if (current_finger_spacing < finger_spacing && zoom_level > 1){
                    zoom_level-=0.5f;
//                    Log.e(TAG, "pinchtoZoom: Zoom Out "+zoom_level);

                    auxDock.post(hideAuxDock);
                    zSlider.setVisibility(View.VISIBLE);
                    zSlider.setValue((float) getZoomValueSingleDecimal((zoom_level/4.5f)));
                }
                setZoom(maxzoom,zoom_level);

                if(!is_sliding) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        //TODO : ADD SEMAPHORE
                        auxDock.post(hideAuxDock);
                        zSlider.setVisibility(View.INVISIBLE);
                    }, 1500);
                }

            }
            finger_spacing = current_finger_spacing;
        } catch (CameraAccessException e) {
            throw new RuntimeException("can not access camera.", e);
        }

    }

    private void setZoom(float maxzoom, float zoom_level) throws CameraAccessException {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(getCameraId());
        Rect m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        zoomText.removeCallbacks(hideZoomText);

        int minW = (int) (m.width() / maxzoom);
        int minH = (int) (m.height() / maxzoom);
        int difW = m.width() - minW;
        int difH = m.height() - minH;
        int cropW = difW /100 *(int)zoom_level;
        int cropH = difH /100 *(int)zoom_level;
        cropW -= cropW & 3;
        cropH -= cropH & 3;
        zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);

        String zText = getZoomValueSingleDecimal(zoom_level/4.5f)+"x";
        zoomText.setVisibility(View.VISIBLE);
        zoomText.setText(zText);

        if(!is_sliding){
            //TODO : ADD SEMAPHORE
            zoomText.postDelayed(()-> zoomText.setVisibility(View.INVISIBLE),1800);
        }

        previewCaptureRequest.set(CaptureRequest.SCALER_CROP_REGION, zoom);
        try {
            if(getState() == CamState.HSVIDEO_PROGRESSED)
                highSpeedCaptureSession.createHighSpeedRequestList(previewCaptureRequest.build());
            else
                camSession
                    .setRepeatingRequest(previewCaptureRequest.build(), null, null);
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
        }
    }

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

    public float getMaxZoom() throws CameraAccessException {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(getCameraId());
        return (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))*4.5f;
    }

    /**
     * CAMERA M E T H O D S
     */

    private void openCamera() {
        if (!resumed || !surface)
            return;

        if(getState() == CamState.CAMERA){
            imageSize = (ASPECT_RATIO_43 ? lensData.getHighestResolution(getCameraId()) :
                    lensData.getHighestResolution169(getCameraId()));
        }
        else if(getState() == CamState.HIRES){
            imageSize = lensData.getBayerLensSize();
        }

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
        Log.e(TAG, "openCamera: ImageReader preview size " + previewSize.getWidth() + "x" + previewSize.getHeight());
        Log.e(TAG, "openCamera: ImageReader capture size " + imageSize.getWidth() + "x" + imageSize.getHeight());

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

            if(zoom_level!=1) {
                captureRequest.set(CaptureRequest.SCALER_CROP_REGION, zoom);
            }
            camSession.capture(captureRequest.build(), snapshotCallback, mHandler);
        } catch(Exception e) {
            Log.e(TAG, "captureImage: "+e.toString());
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

    private boolean isMeteringAreaAESupported() {
        Integer aeState = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        return aeState!=null && aeState >=1;
    }

    private boolean isMeteringAreaAFSupported() {
        return characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) >= 1;
    }

    /**
     * VIDEO M E T H O D S
     */

    private void createVideoPreview(int height,int width){
        if (!resumed || !surface)
            return;
        mMediaRecorder = new MediaRecorder();
        StreamConfigurationMap map = getCameraCharacteristics().get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Log.e(TAG, "createVideoPreview: "+ Arrays.toString(map.getOutputSizes(MediaRecorder.class)));

        mVideoSize = getPreviewResolution(map.getOutputSizes(MediaRecorder.class),width,false);

        snapshotImageReader = ImageReader.newInstance(width,height,ImageFormat.JPEG,10);
        snapshotImageReader.setOnImageAvailableListener(videoSnapshotCallback,mBackgroundHandler);

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
            camDevice.createCaptureSession(Arrays.asList(previewSurface)
                    , new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            camSession = session;
                            try {
                                camSession.setRepeatingRequest(previewCaptureRequest.build(), null,mBackgroundHandler);
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

    private void startRecording(){
        try {
            //TODO: ADD CHECKS FOR FRONT AND BACK ONLY
            int id = (getCameraId().equals("0") ? 0 : 1);
            CamcorderProfile camcorderProfile = CamcorderProfile.get(id
                    ,CamcorderProfile.QUALITY_HIGH);
            if(!isVRecording){
                setupMediaRecorder(camcorderProfile);
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
        catch (IOException |CameraAccessException e){
            e.printStackTrace();
        }
    }

    private Size getPreviewResolution(Size[] outputSizes, int resolution, boolean aspectRatio43) {
        ArrayList<Size> sizeArrayList = new ArrayList<>();
        for(Size size : outputSizes){
            float ar = (float) size.getWidth()/ size.getHeight();
            if(aspectRatio43) {
                Log.e(TAG, "getPreviewResolution: AR43 resolution : "+resolution);
                if (size.getHeight() == resolution && ar > 1.2f) {
                    sizeArrayList.add(size);
                }
            }
            else {
                Log.e(TAG, "getPreviewResolution: AR169 resolution : "+resolution);
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

    private void setupMediaRecorder(CamcorderProfile camcorderProfile) throws IOException {
        mVideoFileName = "CamX"+System.currentTimeMillis()+"_"+getCameraId()+".mp4";
        Log.e(TAG, "setupMediaRecorder: CP : h : "+camcorderProfile.videoFrameHeight
                +" w : "+camcorderProfile.videoFrameWidth);
        Log.e(TAG, "setupMediaRecorder: vBR : "+camcorderProfile.videoBitRate );
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setAudioSamplingRate(96);
        mMediaRecorder.setAudioEncodingBitRate(96000); //FIXME : UNABLE TO SET HIGHER THAN 48kbits/sec
        mMediaRecorder.setAudioEncodingBitRate(camcorderProfile.audioBitRate);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setVideoFrameRate(vFPS);
        mMediaRecorder.setVideoEncodingBitRate(16400000);
        mMediaRecorder.setVideoSize(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.HEVC);

        mMediaRecorder.setOrientationHint(90);      //TODO : CHANGE ACCORDING TO SENSOR ORIENTATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mMediaRecorder.setOutputFile(new File("//storage//emulated//0//DCIM//Camera//"+mVideoFileName));
        }
        else {
            mMediaRecorder.setOutputFile("//storage//emulated//0//DCIM//Camera//"+mVideoFileName);
        }
        mMediaRecorder.prepare();           //FIXME : prepare fails on emulator(sdk24)
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
            mMediaRecorder = new MediaRecorder();
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
        camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
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
        Log.e(TAG, "getScreenWidth: "+getResources().getDisplayMetrics().widthPixels);
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int getScreenHeight() {
        Log.e(TAG, "getScreenHeight: "+getResources().getDisplayMetrics().heightPixels);
        return getResources().getDisplayMetrics().heightPixels;
    }

    private void displayMediaThumbnailFromGallery() { //FIXME : PROCESSING CAUSES LAG IN VIEWFINDER
        new Handler(Looper.getMainLooper()).post(new ImageDecoderThread(thumbPreview));
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
    }

    protected void stopBackgroundThread() {
        if(mBackgroundThread!=null){
            mBackgroundThread.quitSafely();
        }
        else{
            finishAffinity();
            return;
        }
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * C A L L B A C K S
     */

    ImageReader.OnImageAvailableListener snapshotImageCallback = imageReader -> {
        Log.e(TAG, "onImageAvailable: received snapshot image data");
        Image img = imageReader.acquireLatestImage();
        new Handler(Looper.getMainLooper()).post(new ImageSaverThread(this,img,cameraId,getContentResolver()));
    };

    ImageReader.OnImageAvailableListener videoSnapshotCallback = reader -> {
        Log.e(TAG, "onImageAvailable: received video snapshot image data");
        Image img = reader.acquireLatestImage();
        new Handler(Looper.getMainLooper()).post(new ImageSaverThread(img,cameraId,getContentResolver()));
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
//            ImageWriter iwReprocess = ImageWriter.newInstance(cameraCaptureSession.getInputSurface(), 2);
//            iwReprocess.dequeueInputImage();

            try {
                previewCaptureRequest = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewCaptureRequest.addTarget(surfaceList.get(0));
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
        shutter.animateInnerCircle(getState());
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        ready = false;
        resumed = false;

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
package com.example.camx;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.HandlerCompat;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class AndroidCameraApi extends AppCompatActivity{
    private static final String TAG = "AndroidCameraApi";
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final List<String> ACCEPTED_FILES_EXTENSIONS = Arrays.asList("JPG", "JPEG", "DNG");
    private static final FilenameFilter FILENAME_FILTER = (dir, name) -> {
       int index = name.lastIndexOf(46);
       return ACCEPTED_FILES_EXTENSIONS.contains(-1 == index ? "" : name.substring(index + 1).toUpperCase()) && new File(dir, name).length() > 0;
    };
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
       ORIENTATIONS.append(Surface.ROTATION_0, 90);
       ORIENTATIONS.append(Surface.ROTATION_90, 0);
       ORIENTATIONS.append(Surface.ROTATION_180, 270);
       ORIENTATIONS.append(Surface.ROTATION_270, 180);
   }

    private final String [] camID= {"0","1","20","21","22","2","6","3"}; //0,1,2,3,4,5,6,7 in realme and stock android
                                  // 0   1   2    3    4    5   6   7
    private final int resultCode = 1;

//    private Range<Integer> FpsRangeHigh = Range.create(31,60); // Force High FPS preview
//    private Range<Integer>[] ranges;
    public float finger_spacing = 0;
    public float zoom_level = 1;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
//    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private AppCompatImageButton takePictureButton;
    private TextureView textureView;
    private MaterialTextView ultra_wide_lens,wide_lens,macro_tele_lens;
    private AppCompatImageButton front_switch;
    private MaterialCardView cardView;
    private TextView logtext;
    private ImageView gallery;
    private ImageButton aspectRatio;
    private ImageButton flash;
//    private HorizontalScrollView horizontalScrollView;
//    private RecyclerView rv;
//    private PickerAdapter adapter;
    private String cameraId;
    private Size imageDimension;
    private ImageReader imageReader;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Rect zoom = new Rect();
    private boolean firstTouch = false;
    private long time;
    private double ratioCoeff = 1;
    private CameraCharacteristics characteristics;
    private Uri uri;
    //FLAGS
    private boolean mManualFocusEngaged = false;
    private boolean ASPECT_RATIO_43 = true;
    private boolean ASPECT_RATIO_169 = false;
    private Map<Integer,Size> hRes = new HashMap<>();
    private Map<Integer,Size> map169 = new HashMap<>();
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            if(cameraDevice!=null) {
                cameraDevice.close();
            }
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            createCameraPreview();
        }
    };

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
            configureTextureView(width,height);
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height

        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private void configureTextureView(int width, int height) {
        Log.e(TAG, "configureTextureView: width : "+width+" height : "+height);
        Log.e(TAG, "configureTextureView: hxw "+width*height);
        int device_width = textureView.getWidth();
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(device_width, (int) (device_width * (ASPECT_RATIO_43 ? 1.333f : 1.777f)));
        textureView.setLayoutParams(layoutParams);
    }

    static void closeOutput(OutputStream outputStream) {
        if (null != outputStream) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static int gcd(int a, int b) {
        if (a == 0)
            return b;
        if (b == 0)
            return a;
        if (a == b)
            return a;
        if (a > b)
            return gcd(a-b, b);
        return gcd(a, b-a);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        textureView = findViewById(R.id.preview);
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = findViewById(R.id.capture);
        ultra_wide_lens = findViewById(R.id.ultra_wide);
        wide_lens = findViewById(R.id.main_wide);
        macro_tele_lens = findViewById(R.id.macro_tele);
        front_switch = findViewById(R.id.front_back_switch);
        cardView = findViewById(R.id.aux_cam_switch);
        gallery = findViewById(R.id.image_gallery);
        aspectRatio = findViewById(R.id.aspectRatio);
        flash = findViewById(R.id.flash);
//        horizontalScrollView = findViewById(R.id.horizontal_scrollView);
//        rv = findViewById(R.id.rv);
        logtext = findViewById(R.id.logtxt);

        zoom = new Rect(0,0,textureView.getWidth(),textureView.getHeight());

        requestRuntimePermission();
        check_aux();
        display_latest_image_from_gallery();

        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler(Looper.getMainLooper()).post(() -> takePicture());
                new Handler(Looper.getMainLooper()).postDelayed(() -> display_latest_image_from_gallery(),1800);
            }
        });

        ultra_wide_lens.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
               //makes changes in takePicture() and  opencamera()
                if(check_null_camiID(camID[3])){
                    if(!getCameraId().equals(camID[3])) {
                        closeCamera();
                        setCameraId(camID[3]);
                        hRes = getHighestResolution(getcameraCharacteristics());
                        openCamera();
                        ultra_wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.colored_textview));
                        wide_lens.setBackground(null);
                        macro_tele_lens.setBackground(null);
                   }
               }
                else if (check_null_camiID(camID[5])){ //green lens on samsung
                    if(!getCameraId().equals(camID[5])) {
                        closeCamera();
                        setCameraId(camID[5]);
                        hRes = getHighestResolution(getcameraCharacteristics());
                        openCamera();
                        ultra_wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.colored_textview));
                        wide_lens.setBackground(null);
                        macro_tele_lens.setBackground(null);
                    }
               }
                else if (check_null_camiID(camID[2])){ //green lens on samsung
                    if(!getCameraId().equals(camID[2])) {
                        closeCamera();
                        setCameraId(camID[2]);
                        hRes = getHighestResolution(getcameraCharacteristics());
                        openCamera();
                        ultra_wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.colored_textview));
                        wide_lens.setBackground(null);
                        macro_tele_lens.setBackground(null);
                    }
               }
           }
       });

        wide_lens.setOnClickListener(new View.OnClickListener(){
           @Override
           public void onClick(View v) {
               if(!getCameraId().equals(camID[0])) {
                   zoom_level = 1;
                   closeCamera();
                   setCameraId(camID[0]);
                   hRes = getHighestResolution(getcameraCharacteristics());
                   openCamera();
                   wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.colored_textview));
                   ultra_wide_lens.setBackground(null);
                   macro_tele_lens.setBackground(null);
               }
           }
       });

        macro_tele_lens.setOnClickListener(new View.OnClickListener(){
           @Override
           public void onClick(View v) {
//               if(check_null_camiID(camID[4])){
                       closeCamera();
                       setCameraId((check_null_camiID(camID[6])) ? ((!getCameraId().equals(camID[6]))?camID[6] : getCameraId())
                               : ((!getCameraId().equals(camID[4]))?camID[4] : getCameraId()));
                       hRes = getHighestResolution(getcameraCharacteristics());
                       openCamera();
                       macro_tele_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.colored_textview));
                       ultra_wide_lens.setBackground(null);
                       wide_lens.setBackground(null);
//               }
//               else if (check_null_camiID(camID[6])){
//                   if(!getCameraId().equals(camID[6])) {
//                       closeCamera();
//                       setCameraId(camID[6]);
//                       getHighestResolution(getcameraCharacteristics());
//                       openCamera();
//                       macro_tele_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.colored_textview));
//                       ultra_wide_lens.setBackground(null);
//                       wide_lens.setBackground(null);
//                   }
//               }
           }
       });

        front_switch.setOnClickListener(new View.OnClickListener(){
           @Override
           public void onClick(View v) {
               zoom_level = 1;
               if(cardView.getVisibility()==View.VISIBLE){
                   closeCamera();
                   setCameraId(camID[1]);
                   hRes = getHighestResolution(getcameraCharacteristics());
                   openCamera();
                   cardView.setVisibility(View.INVISIBLE);
                   front_switch.animate().rotation(180f);
               }
               else if(cardView.getVisibility()==View.INVISIBLE){
                   closeCamera();
                   setCameraId(camID[0]);
                   wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.colored_textview));
                   ultra_wide_lens.setBackground(null);
                   macro_tele_lens.setBackground(null);
                   hRes = getHighestResolution(getcameraCharacteristics());
                   openCamera();
                   cardView.setVisibility(View.VISIBLE);
                   front_switch.animate().rotation(-180f);
               }
           }
       });

        gallery.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               //METHOD 1
               Intent intent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                intent.setType("image/*");
               intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);

               //METHOD 2
//                Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.apps.photos");
//                startActivity(intent);

               //METHOD 3
//                Intent intent = new Intent(Intent.ACTION_MAIN);
////                intent.setComponent(new ComponentName("com.google.android.apps.photos", "com.google.android.apps.photos.pager.HostPhotoPagerActivity"));
//                intent.setComponent(new ComponentName("com.miui.gallery","com.miui.gallery.activity.ExternalPhotoPageActivity"));
//                if (intent.resolveActivity(getPackageManager()) != null)
//                {
//                    startActivity(intent);
//                }
               startActivityForResult(intent, resultCode);
           }
       });

        textureView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                time = System.currentTimeMillis();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if(firstTouch && (System.currentTimeMillis() - time) <= 300) {
                        /**
                         * DOUBLE TAP TO ZOOM
                         */
                        Log.e("** DOUBLE TAP**"," second tap ");
                        try {
                            doubleTaptoZoom();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                        firstTouch = false;
                    } else {
                        /**
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
                        Log.e("** SINGLE  TAP**"," First Tap time  "+time);
                        return false;
                    }
                }
                /**
                 * PINCH TO ZOOM
                 */
                if (event.getPointerCount() > 1) {
                    pinchtoZoom(event);
                }
                return true;
           }

       });

        aspectRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ASPECT_RATIO_43){
                    ASPECT_RATIO_43 = false;
                    ASPECT_RATIO_169 = true;
                    configureTextureView(textureView.getWidth(),textureView.getHeight());
                    openCamera();
                    aspectRatio.setImageResource(R.drawable.ic_sixteennine);
                }
                else if(ASPECT_RATIO_169){
                    ASPECT_RATIO_169 = false;
                    ASPECT_RATIO_43 = true;
                    configureTextureView(textureView.getWidth(),textureView.getHeight());
                    openCamera();
                    aspectRatio.setImageResource(R.drawable.ic_fourthree);
                }
            }
        });

//       PickerLayoutManager pickerLayoutManager = new PickerLayoutManager(this, PickerLayoutManager.HORIZONTAL, false);
//       pickerLayoutManager.setChangeAlpha(true);
//       pickerLayoutManager.setScaleDownBy(1.0f);
//       pickerLayoutManager.setScaleDownDistance(2f);
//
//       List<String> adapter_data = new ArrayList<>();
//       adapter_data.add("Night");
//       adapter_data.add("Photo");
//       adapter_data.add("Video");
//       adapter_data.add("Slo Motion");
//       adapter_data.add("Timelapse");
//       adapter_data.add("Reverse");
//       adapter_data.add("Pro");
//        adapter = new PickerAdapter(this, adapter_data, rv);
//        SnapHelper snapHelper = new LinearSnapHelper();
//        snapHelper.attachToRecyclerView(rv);
//        rv.setLayoutManager(pickerLayoutManager);
//        rv.setAdapter(adapter);

//        pickerLayoutManager.scrollToPosition(1);
//        pickerLayoutManager.setOnScrollStopListener(new PickerLayoutManager.onScrollStopListener() {
//            @Override
//            public void selectedView(View view) {
//                view.setHapticFeedbackEnabled(true);
//                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP
//                        ,HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
//                Toast.makeText(AndroidCameraApi.this, ("Selected value : "+((TextView) view).getText().toString()), Toast.LENGTH_SHORT).show();
//            }
//        });
   }

    private CameraCharacteristics getcameraCharacteristics() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            characteristics = manager.getCameraCharacteristics(getCameraId());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return characteristics;
    }

    private void doubleTaptoZoom() throws CameraAccessException {
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
        }
        else if(zoom_level>DT_value){
            setZoom(maxZoom,DT_value);
            zoom_level = DT_value;
        }
        else if(zoom_level==DT_value){
            setZoom(maxZoom,1f);
            zoom_level = 1f;
        }
        Log.e(TAG, "doubleTaptoZoom: D O U B L E - T A P P E D");
    }

    private void touchToFocus(View v,MotionEvent event) {
        Log.e(TAG, "touchToFocus: F O C U S I N G");
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            characteristics = manager.getCameraCharacteristics(getCameraId());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        final Rect sensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        /** here I just flip x,y, but this needs to correspond with the sensor orientation (via SENSOR_ORIENTATION)
        */
        final int y = (int)((event.getX() / (float)v.getWidth())  * (float)sensorArraySize.height());
        final int x = (int)((event.getY() / (float)v.getHeight()) * (float)sensorArraySize.width());
        /**
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
                mManualFocusEngaged = false;

                if (request.getTag() == "FOCUS_TAG") {
                    //the focus trigger is complete -
                    //resume repeating (preview surface will get frames), clear AF trigger
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null);
                    try {
                        cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
                Log.e(TAG, "Manual AF failure: " + failure);
                mManualFocusEngaged = false;
            }
        };

        //first stop the existing repeating request
//        try {
//            cameraCaptureSessions.stopRepeating();
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }

        //cancel any existing AF trigger (repeated touches, etc.)
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        try {
            cameraCaptureSessions.capture(captureRequestBuilder.build(), captureCallbackHandler, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        //Now add a new AF trigger with focus region
        if (isMeteringAreaAFSupported()) {
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusAreaTouch});
        }
        if(isMeteringAreaAESupported()){
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{focusAreaTouch});
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        captureRequestBuilder.setTag("FOCUS_TAG"); //we'll capture this later for resuming the preview

        //then we ask for a single request (not repeating!)
        try {
            cameraCaptureSessions.capture(captureRequestBuilder.build(), captureCallbackHandler, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
//        mManualFocusEngaged = true;
    }

    private void pinchtoZoom(MotionEvent event){
        try {

            float maxzoom = getMaxZoom();
            float current_finger_spacing;

//            Log.e(TAG, "pinchtoZoom: Pointer Count : "+event.getPointerCount());

                // Multi touch logic
                current_finger_spacing = getFingerSpacing(event);
//                Log.e(TAG, "pinchtoZoom: getFingerSpacing : "+current_finger_spacing);
                if(finger_spacing != 0){
                    if(current_finger_spacing > finger_spacing && maxzoom > zoom_level){
                        zoom_level+=0.5f;
                        Log.e(TAG, "pinchtoZoom: Zoom In "+zoom_level);
                    } else if (current_finger_spacing < finger_spacing && zoom_level > 1){
                        zoom_level-=0.5f;
                        Log.e(TAG, "pinchtoZoom: Zoom Out "+zoom_level);
                    }
                    setZoom(maxzoom,zoom_level);
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

        int minW = (int) (m.width() / maxzoom);
        int minH = (int) (m.height() / maxzoom);
        int difW = m.width() - minW;
        int difH = m.height() - minH;
        int cropW = difW /100 *(int)zoom_level;
        int cropH = difH /100 *(int)zoom_level;
        cropW -= cropW & 3;
        cropH -= cropH & 3;
        zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
//                    Log.e(TAG, "pinchtoZoom: ZOOM VALUE : "+zoom);
        captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
        try {
            cameraCaptureSessions
                    .setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    protected void startBackgroundThread() {
       mBackgroundThread = new HandlerThread("Camera Background");
       mBackgroundThread.start();
       mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
   }

    protected void stopBackgroundThread() {
       mBackgroundThread.quitSafely();
       try {
           mBackgroundThread.join();
           mBackgroundThread = null;
           mBackgroundHandler = null;
       } catch (InterruptedException e) {
           e.printStackTrace();
       }
   }


    protected void takePicture() {
       if(null == cameraDevice) {
           Log.e(TAG, "cameraDevice is null");
           return;
       }
       final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
       try {
           characteristics = manager.getCameraCharacteristics(getCameraId());
//            Log.e(TAG, "takePicture: getcamera Characteristics => "+ characteristics.getAvailableCaptureRequestKeys());
           Size[] jpegSizes = null;
           Map <Integer,Size> res = (ASPECT_RATIO_43 ? hRes:map169);
           /**
            * 0x100 gives output size [6000*8000] in redmi k20 a10
            */
           int imageFormat= 0x20;
           for (Integer item : res.keySet()) {
               imageFormat = Integer.parseUnsignedInt(Integer.toHexString(item), 16);
               Log.e(TAG, "takePicture: ImageFormat in Hex : 0x" + String.format("%x", imageFormat));
           }

           if (characteristics != null) {
               jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(imageFormat);
//               Log.e(TAG, "takePicture: Camera Characteristics : "+characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP));
           }

           int width = 640;
           int height = 480;
           if (jpegSizes != null && 0 < jpegSizes.length) {
               for(Size size : res.values()){
                   /**
                    * 48mp(8000*6000) 64mp(6944*9280)
                    */
                   width = size.getWidth();
                   height = size.getHeight();
               }
           }
           Log.e(TAG, "takePicture: jpeg sizes before taking pic : width : "+width+"height : "+height);

           ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
           List<Surface> outputSurfaces = new ArrayList<>(2);
           outputSurfaces.add(reader.getSurface());
           outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));

           final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

           captureBuilder.addTarget(reader.getSurface());
           captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
           // Orientation

           int rotation = 0;
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
               rotation = getDisplay().getRotation();
           }
           assert characteristics != null;

           /**
            * FRONT CAMERA INVERSION FIX
            */
           if(characteristics.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_FRONT){
               captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(Surface.ROTATION_180));
           }
           else {
               captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
           }
           Log.e(TAG, "takePicture: Zoom Value "+zoom+" Zoom Level : "+zoom_level);
           if(zoom_level!=1) {
               captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
           }

           ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
               @Override
               public void onImageAvailable(ImageReader reader) {
//                   new Handler(Looper.getMainLooper()).post(new ImageSaver(reader.acquireNextImage(),getCameraId(),getContentResolver(),uri));
                   mBackgroundHandler.post(new ImageSaverThread(reader.acquireNextImage(),getCameraId(),getContentResolver(),uri));
               }
           };
           reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
           CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
               @Override
               public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                   super.onCaptureCompleted(session, request, result);
                   Toast.makeText(AndroidCameraApi.this, "Saved:" , Toast.LENGTH_SHORT).show();
                   createCameraPreview();
               }
           };
           cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
               @Override
               public void onConfigured(CameraCaptureSession session) {
                   try {
                       session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                   } catch (CameraAccessException e) {
                       e.printStackTrace();
                   }
               }
               @Override
               public void onConfigureFailed(CameraCaptureSession session) {
               }
           }, mBackgroundHandler);
       } catch (CameraAccessException e) {
           e.printStackTrace();
       }

   }

    protected void createCameraPreview() {
       try {
           SurfaceTexture texture = textureView.getSurfaceTexture();
           assert texture != null;
           texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
           Log.e(TAG, "createCameraPreview: Dimension : "+imageDimension);
           Surface surface = new Surface(texture);
           captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
           captureRequestBuilder.addTarget(surface);
           if(zoom_level != 1) {
               captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
           }
           cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback(){
               @Override
               public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                   //The camera is already closed
                   if (null == cameraDevice) {
                       return;
                   }
                   // When the session is ready, we start displaying the preview.
                   cameraCaptureSessions = cameraCaptureSession;
                   updatePreview();
               }
               @Override
               public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                   Toast.makeText(AndroidCameraApi.this, "Configuration change", Toast.LENGTH_SHORT).show();
               }
           }, null);
       } catch (CameraAccessException e) {
           e.printStackTrace();
       }
   }

    private void openCamera() {
       CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
       Log.e(TAG, "is camera open");
       try {
           CameraCharacteristics characteristics = manager.getCameraCharacteristics(getCameraId());
//            ranges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
//            for (Range<Integer> value : ranges){
//                Log.d(TAG, "openCamera: FPS RANGES : "+value);
//            }
           StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
           assert map != null;
//           Log.e(TAG, "openCamera: Stream Config Map" + map.toString());
//            Size opres = map.getOutputSizes(SurfaceTexture.class)[1];
//            Log.e(TAG, "openCamera: Stream Config Map ; OutputSize "+opres);
           Log.e(TAG, "openCamera: SENSOR PRE "+characteristics.get(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE));

           imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
           Map<Integer,Size> hMap = (ASPECT_RATIO_43 ? getHighestResolution(characteristics):map169);
           for(Size item : hMap.values()){
               imageDimension = item;
               Log.e(TAG, "openCamera: imageDimension : "+item);
           }

           requestRuntimePermission();
           if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
               ActivityCompat.requestPermissions(AndroidCameraApi.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
           }
           else {
               Log.e(TAG, "openCamera: cameraID :> " + cameraId);
               manager.openCamera(getCameraId(), stateCallback, null);
           }
       } catch (CameraAccessException | IllegalArgumentException e) {
           e.printStackTrace();
       }
   }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(AndroidCameraApi.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void requestRuntimePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(AndroidCameraApi.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
        }
    }

    protected void updatePreview() {
       if(null == cameraDevice) {
           Log.e(TAG, "updatePreview error, return");
       }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA);
//        }
//        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, FpsRangeHigh); Force High FPS preview
//       captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
       try {
           cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
       } catch (CameraAccessException e) {
           e.printStackTrace();
       }
   }

    private void closeCamera() {
       if (null != cameraDevice) {
           cameraDevice.close();
           cameraDevice = null;
       }
       if (null != imageReader) {
           imageReader.close();
           imageReader = null;
       }
   }

    private boolean check_null_camiID(String id){
       try {
           CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
           CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
           return !characteristics.getAvailableCaptureRequestKeys().isEmpty();
       }
       catch (IllegalArgumentException | CameraAccessException ignored){ }
       return false;
   }

    private void check_aux() {
        StringBuilder msg = new StringBuilder("CAMID : ");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i<=144 ; i++){
                    try {
                        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                        CameraCharacteristics characteristics = manager.getCameraCharacteristics(String.valueOf(i));
                        if (!characteristics.getAvailableCaptureRequestKeys().isEmpty() ) {
//                            Log.e(TAG, "check_aux: getcamera Characteristics => "+ characteristics.getAvailableCaptureRequestKeys());
                            msg.append(i).append(",");
                            Log.e(TAG, "check_aux: value of array at " + i + " : " + i);
                            Set<String> ids;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                ids = characteristics.getPhysicalCameraIds();
                                for (String id: ids) {
                                    Log.e(TAG, "openCamera: getPhysicalCameraIds "+id);
                                    msg.append(" pid_").append(id).append("_at_").append(i).append(",");
                            }
                            }
                        }
                    }
                    catch (IllegalArgumentException | CameraAccessException ignored){ }
                }
                logtext.setText(msg);
            }
        });
        Toast.makeText(AndroidCameraApi.this, "COMPLETE EXECUTION cam_aux()", Toast.LENGTH_SHORT).show();
    }

    private Map<Integer, Size> getHighestResolution(CameraCharacteristics characteristics) {
        Size [] resolutions;
        int highest = 0,iF = 0;
        Size hSize = null;
        Map<Integer,Size> imageFormat_resolution_map = new HashMap<>();
        Map<Integer,Size> imageFormat_resolution_map_169 = new HashMap<>();
        Map<Integer,Size> mreturn = new HashMap<>();
        ArrayList<Integer> image_formats = new ArrayList<>();
        image_formats.add(ImageFormat.RAW_PRIVATE);
        image_formats.add(ImageFormat.RAW_SENSOR);
        image_formats.add(ImageFormat.JPEG);
        image_formats.add(ImageFormat.YUV_420_888);
//        image_formats.add(ImageFormat.RAW10);
//        image_formats.add(ImageFormat.RAW12);

        if(characteristics!=null){
            for(Integer i:image_formats){
                int previous=0,previous169 = 0;
                resolutions = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(i);
                int imageFormat = Integer.parseUnsignedInt(Integer.toHexString(i),16);
                Log.e(TAG, "getHighestResolution: imageFormat : 0x"+String.format("%x",imageFormat)+"  resolutions :  "
                        + Arrays.toString(resolutions));
                if (resolutions!=null){
                    for (Size resolution : resolutions) {
                        float resolution_coeff = (float)resolution.getWidth() / resolution.getHeight();
                        Log.e(TAG, "getHighestResolution: "+resolution.getWidth()+"x"+resolution.getHeight()+" coeff : "+resolution_coeff);
                        if( resolution_coeff> 1.4f && resolution_coeff< 1.9f) {
                            if (previous169<resolution.getHeight()*resolution.getWidth()) {
                                imageFormat_resolution_map_169.put(i, resolution);
                                previous169 = resolution.getHeight()*resolution.getWidth();
                            }
                        }
                        if(previous<(resolution.getHeight()*resolution.getWidth())){
                            imageFormat_resolution_map.put(i,resolution);
                            Log.e(TAG, "getHighestResolution: resolution coeff float : "+((float) resolution.getWidth()/resolution.getHeight())%.2f);
                            previous = resolution.getHeight()*resolution.getWidth();
                        }
                    }
                }
            }
             Log.e(TAG, "getHighestResolution: imageformatresolutionmap "+imageFormat_resolution_map );
             Log.e(TAG, "getHighestResolution: imageformatresolutionmap 16:9 "+imageFormat_resolution_map_169 );

            Set<Map.Entry<Integer, Size>> set = imageFormat_resolution_map.entrySet();
            Set<Map.Entry<Integer, Size>> set169 = imageFormat_resolution_map_169.entrySet();
            for (Map.Entry<Integer, Size> entry : set) {
                Log.e(TAG, "getHighestResolution: resolution :  "+entry.getValue()+"  Imageformat : "+entry.getKey());
                int c = entry.getValue().getHeight()*entry.getValue().getWidth();
                if(highest<=c){
                    highest=c;
                    iF=entry.getKey();
                    hSize=entry.getValue();
                }
            }
            mreturn.put(iF,hSize);
            highest = 0;iF=0;hSize=null;
            //highest 16 : 9 resolution
            for (Map.Entry<Integer, Size> entry : set169) {
                Log.e(TAG, "getHighestResolution: resolution 16:9 :  "+entry.getValue()+"  Imageformat : "+entry.getKey());
                int c = entry.getValue().getHeight()*entry.getValue().getWidth();
                if(highest<=c){
                    highest=c;
                    iF=entry.getKey();
                    hSize=entry.getValue();
                }
            }
            map169.put(iF,hSize);

            for(Size size : mreturn.values()){
                ratioCoeff =(double) size.getWidth()/size.getHeight();
//                 Log.e(TAG,"getHighestResolution: aspect Ratio "+aspectRatio+" height: "+size.getHeight()+"width: "+size.getWidth());
            }
        }
        Log.e(TAG, "getHighestResolution: mReturn(highest res) : "+mreturn.entrySet() );
        Log.e(TAG, "getHighestResolution: map169(highest res 16:9) : "+map169.entrySet() );
        return  mreturn;
    }

    /**
     *  For checking if Legacy Camera Support is available to the cameraID
     */
    private boolean isLegacyLocked() throws CameraAccessException {
       CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
       CameraCharacteristics characteristics = manager.getCameraCharacteristics(getCameraId());
        return characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ==
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public float getMaxZoom() throws CameraAccessException {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(getCameraId());
        return (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))*4.5f;
   }

    private void display_latest_image_from_gallery() {
        File f = new File("//storage//emulated//0//DCIM//Camera//");
        File[] dcimFiles = f.listFiles(FILENAME_FILTER);

        List<File> filesList = new ArrayList<>(Arrays.asList(dcimFiles != null ? dcimFiles : new File[0]));
        if (!filesList.isEmpty()) {
            filesList.sort((file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()));
        } else {
            Log.e(TAG, "getAllImageFiles(): Could not find any Image Files");
        }

        File lastImage = filesList.get(0);
        Uri liu = Uri.fromFile(lastImage);
        Glide.with(this).load(liu).into(gallery);

    }

    private boolean isMeteringAreaAESupported() {
       Integer aeState = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
       return aeState!=null && aeState >=1;
   }

    private boolean isMeteringAreaAFSupported() {
       return characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) >= 1;
   }

    private double getRatioCoeff(){
       return ratioCoeff;
   }

    @Override
    protected void onResume() {
       super.onResume();
       startBackgroundThread();
       if(Objects.equals(getCameraId(),null)) {
           setCameraId(camID[0]);
           wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.colored_textview));
           ultra_wide_lens.setBackground(null);
           macro_tele_lens.setBackground(null);
       }
       if (textureView.isAvailable()) {
           openCamera();
       } else {
           textureView.setSurfaceTextureListener(textureListener);
       }
   }

    @Override
    protected void onPause() {
       Log.e(TAG, "onPause");
       closeCamera();
       stopBackgroundThread();
       super.onPause();
   }

}
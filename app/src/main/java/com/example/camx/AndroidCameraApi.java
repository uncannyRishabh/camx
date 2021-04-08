 package com.example.camx;

 import android.Manifest;
 import android.content.Context;
 import android.content.Intent;
 import android.content.pm.PackageManager;
 import android.graphics.ImageFormat;
 import android.graphics.SurfaceTexture;
 import android.hardware.camera2.CameraAccessException;
 import android.hardware.camera2.CameraCaptureSession;
 import android.hardware.camera2.CameraCharacteristics;
 import android.hardware.camera2.CameraDevice;
 import android.hardware.camera2.CameraManager;
 import android.hardware.camera2.CameraMetadata;
 import android.hardware.camera2.CaptureRequest;
 import android.hardware.camera2.TotalCaptureResult;
 import android.hardware.camera2.params.StreamConfigurationMap;
 import android.media.Image;
 import android.media.ImageReader;
 import android.net.Uri;
 import android.os.Build;
 import android.os.Bundle;
 import android.os.Environment;
 import android.os.Handler;
 import android.os.HandlerThread;
 import android.util.Log;
 import android.util.Range;
 import android.util.Size;
 import android.util.SparseIntArray;
 import android.view.Surface;
 import android.view.TextureView;
 import android.view.View;
 import android.widget.ImageView;
 import android.widget.TextView;
 import android.widget.Toast;

 import androidx.annotation.NonNull;
 import androidx.appcompat.app.AppCompatActivity;
 import androidx.appcompat.widget.AppCompatImageButton;
 import androidx.core.app.ActivityCompat;
 import androidx.core.content.ContextCompat;

 import com.bumptech.glide.Glide;
 import com.google.android.material.button.MaterialButton;
 import com.google.android.material.card.MaterialCardView;
 import com.google.android.material.textview.MaterialTextView;

 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.FilenameFilter;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.nio.ByteBuffer;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 import java.util.Objects;
 import java.util.Set;

public class AndroidCameraApi extends AppCompatActivity{
    private static final String TAG = "AndroidCameraApi";
    private MaterialButton takePictureButton;
    private TextureView textureView;
    private MaterialTextView ultra_wide_lens,wide_lens,macro_tele_lens;
    private AppCompatImageButton front_switch;
    private MaterialCardView cardView;
    private TextView logtext;
    private ImageView gallery;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private final String [] camID= {"0","1","20","21","22","12","13","14"}; //0,1,2,3,4,5,6,7 in realme and stock android
                                  // 0   1   2    3    4    5   6   7

//    private Range<Integer> FpsRangeHigh = Range.create(31,60); // Force High FPS preview
    private Range<Integer>[] ranges;
    private final int resultCode = 1;
    private static final List<String> ACCEPTED_FILES_EXTENSIONS = Arrays.asList("JPG", "JPEG", "DNG");
    private static final FilenameFilter FILENAME_FILTER = (dir, name) -> {
        int index = name.lastIndexOf(46);
        return ACCEPTED_FILES_EXTENSIONS.contains(-1 == index ? "" : name.substring(index + 1).toUpperCase()) && new File(dir, name).length() > 0;
    };

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                |View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        textureView = findViewById(R.id.preview);
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = findViewById(R.id.capture);
        ultra_wide_lens = findViewById(R.id.ultra_wide);
        wide_lens = findViewById(R.id.main_wide);
        macro_tele_lens = findViewById(R.id.macro_tele);
        front_switch = findViewById(R.id.front_back_switch);
        cardView = findViewById(R.id.aux_cam_switch);
        gallery = findViewById(R.id.image_gallery);

        logtext = findViewById(R.id.logtxt);
        //TEST CODE #0
        check_aux();
        display_latest_image_from_gallery();

        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        display_latest_image_from_gallery();
                    }
                },2500);

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
                    closeCamera();
                    setCameraId(camID[0]);
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
                if(check_null_camiID(camID[4])){
                    if(!getCameraId().equals(camID[4])) {
                        closeCamera();
                        setCameraId(camID[4]);
                        openCamera();
                        macro_tele_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.colored_textview));
                        ultra_wide_lens.setBackground(null);
                        wide_lens.setBackground(null);
                    }
                }
                else if (check_null_camiID(camID[6])){
                    if(!getCameraId().equals(camID[6])) {
                        closeCamera();
                        setCameraId(camID[6]);
                        openCamera();
                        macro_tele_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.colored_textview));
                        ultra_wide_lens.setBackground(null);
                        wide_lens.setBackground(null);
                    }
                }
            }
        });

        front_switch.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(cardView.getVisibility()==View.VISIBLE){
                    closeCamera();
                    setCameraId(camID[1]);
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
                Intent intent = new Intent(Intent.ACTION_VIEW,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
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
    }

    private void display_latest_image_from_gallery() {
        File external_dir = Environment.getExternalStorageDirectory();
        File f = new File(external_dir + "//DCIM//Camera//");
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

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
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
            cameraDevice.close();
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
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(getCameraId());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                characteristics.getKeysNeedingPermission();
            }
//            Log.e(TAG, "takePicture: getcamera Characteristics => "+ characteristics.getAvailableCaptureRequestKeys());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.YUV_420_888);
                Log.e(TAG, "takePicture: Camera Characteristics : "+characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP));
                Log.e(TAG, "takePicture: IMAGE FORMAT : "+
                        Arrays.toString(characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                                .getHighSpeedVideoFpsRanges()));
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.set(CaptureRequest.CONTROL_ENABLE_ZSL,true);
            
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));  //replace with SURFACE_ROTATION_0

            File imgLocation = new File(Environment.getExternalStorageDirectory() + "//DCIM//Camera//" );
            File file =new File(imgLocation.getAbsolutePath(),"camX_"+ System.currentTimeMillis() +"_"+getCameraId()+".jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(AndroidCameraApi.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
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
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
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
            Log.e(TAG, "openCamera: Stream Config Map" + map.toString());
            Size opres = map.getOutputSizes(SurfaceTexture.class)[1];
            Log.e(TAG, "openCamera: Stream Config Map ; OutputSize "+opres);
            Log.e(TAG, "openCamera: SENSOR PRE"+characteristics.get(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE));

            if(characteristics.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_FRONT){
                imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            }
            else{
                imageDimension = map.getOutputSizes(SurfaceTexture.class)[1];
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(AndroidCameraApi.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            Log.e(TAG, "openCamera: cameraID :> "+cameraId);
            manager.openCamera(getCameraId(), stateCallback, null);
        } catch (CameraAccessException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA);
        }
//        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, FpsRangeHigh); Force High FPS preview
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
            if (!characteristics.getAvailableCaptureRequestKeys().isEmpty()) {
                return true;
            }
            else{
                return false;
            }
        }
        catch (IllegalArgumentException | CameraAccessException ignored){ }
        return false;
    }

    private void check_aux() {
        StringBuilder msg = new StringBuilder("CAMID : ");
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i<=32 ; i++){
                    try {
                        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                        CameraCharacteristics characteristics = manager.getCameraCharacteristics(String.valueOf(i));
                        if (!characteristics.getAvailableCaptureRequestKeys().isEmpty() ) {
//                            Log.e(TAG, "check_aux: getcamera Characteristics => "+ characteristics.getAvailableCaptureRequestKeys());
                            msg.append(i).append(",");
                            Log.e(TAG, "check_aux: value of array at " + i + " : " + i);
                            Set<String> ids = null;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(AndroidCameraApi.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
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

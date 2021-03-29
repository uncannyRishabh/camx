package com.example.camx;

import android.Manifest;
import android.content.Context;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    private final String [] camID= {"0","1","21","22","20"};

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

        logtext = findViewById(R.id.logtxt);
        //TEST CODE #0
        check_aux();


        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        ultra_wide_lens.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //makes changes in takePicture() and  opencamera()
                if(!getCameraId().equals(camID[2])) {
                    closeCamera();
                    setCameraId(camID[2]);
                    openCamera();
                    ultra_wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.colored_textview));
                    wide_lens.setBackground(null);
                    macro_tele_lens.setBackground(null);
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
                if(!getCameraId().equals(camID[4])) {
                    closeCamera();
                    setCameraId(camID[4]);
                    openCamera();
                    macro_tele_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.colored_textview));
                    wide_lens.setBackground(null);
                    ultra_wide_lens.setBackground(null);
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
                }
                else if(cardView.getVisibility()==View.INVISIBLE){
                    closeCamera();
                    setCameraId(camID[0]);
                    wide_lens.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.colored_textview));
                    ultra_wide_lens.setBackground(null);
                    macro_tele_lens.setBackground(null);
                    openCamera();
                    cardView.setVisibility(View.VISIBLE);
                }
            }
        });

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

    @RequiresApi(api = Build.VERSION_CODES.Q)
    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(getCameraId());
            characteristics.getKeysNeedingPermission();
//            Log.e(TAG, "takePicture: getcamera Characteristics => "+ characteristics.getPhysicalCameraIds());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.RAW_SENSOR);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
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

            File file = new File( getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)+"/"+System.currentTimeMillis() +"_camX.jpg");
//            final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+"/pic.jpg");
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
                @RequiresApi(api = Build.VERSION_CODES.R)
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
//            cameraId = manager.getCameraIdList()[0]; //0 for back 1 for front camera
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            //TEST CODE #1
            Set<String> ids = characteristics.getPhysicalCameraIds();
            for (String id: ids
                 ) {
                Log.e(TAG, "openCamera: getPhysicalCameraIds"+id);
            }

            //TEST CODE #2
            for (String cameraId : manager.getCameraIdList()){
                characteristics = manager.getCameraCharacteristics(cameraId);
                Log.e("Testing", "Camera #" + cameraId + " ===> " + getCameraId());
            }
            
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(AndroidCameraApi.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            Log.e(TAG, "openCamera: cameraID :> "+cameraId);
            manager.openCamera(getCameraId(), stateCallback, null);
        } catch (CameraAccessException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA);
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

    private void check_aux() {
        String []array = new String[111];
        StringBuilder msg = new StringBuilder("CAMID : ");
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i<111 ; i++){
                    try {
                        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                        CameraCharacteristics characteristics = manager.getCameraCharacteristics(String.valueOf(i));
                        if (!characteristics.getAvailableCaptureRequestKeys().isEmpty()) {
                            array[i] = String.valueOf(i);
                            msg.append(array[i]).append(",");
                            Log.d(TAG, "check_aux: value of array at " + i + " : " + array[i]);
//                    Toast.makeText(AndroidCameraApi.this, "EXECUTING"+i, Toast.LENGTH_SHORT).show();
                            Thread.sleep(100);
                        }
                    }
                    catch (IllegalArgumentException | CameraAccessException | InterruptedException ignored){

                    }
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
                // close the app
                Toast.makeText(AndroidCameraApi.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
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

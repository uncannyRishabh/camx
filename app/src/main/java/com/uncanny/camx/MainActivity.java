package com.uncanny.camx;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button takePictureButton;
    private TextureView textureView;
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

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        Objects.requireNonNull(getSupportActionBar()).hide();
//
//        textureView = findViewById(R.id.preview);
//        takePictureButton = findViewById(R.id.capture);
//
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                |View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//
//        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
//            @Override
//            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
//                openCamera();
//            }
//
//            @Override
//            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
//
//            }
//
//            @Override
//            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
//                return false;
//            }
//
//            @Override
//            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
//
//            }
//        });
//
//
//
//
//
//
//    }

    private void openCamera() {

    }


}
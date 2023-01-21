package com.uncanny.camx.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.uncanny.camx.CameraManager.CameraControls;
import com.uncanny.camx.Data.LensData;
import com.uncanny.camx.R;
import com.uncanny.camx.UI.Views.CaptureButton;
import com.uncanny.camx.UI.Views.ViewFinder.AutoFitPreviewView;
import com.uncanny.camx.Utils.AsyncThreads.ImageSaverThread;

@SuppressLint("ClickableViewAccessibility")
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class CameraActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final String[] PERMISSION_STRING = { Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE
            , Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private AutoFitPreviewView previewView;
    private CaptureButton shutter;


    private LensData lensData;
    private CameraControls cameraControls;

    private boolean isLongPressed;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        previewView = findViewById(R.id.preview);
        shutter = findViewById(R.id.shutter);

        if (ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            previewView.setSurfaceTextureListener(surfaceTextureListener);
        else requestPermissions();

        lensData = new LensData(this);
        cameraControls = new CameraControls(this);

        shutter.setOnClickListener(this);

        if(lensData.supportBurstCapture("0")){
            shutter.setOnLongClickListener(v -> {
                //Start Repeating Burst
                isLongPressed = true;
                cameraControls.captureBurstImage();
                Log.e(TAG, "onCreate: Start Repeating Burst");
                return false;
            });

            shutter.setOnTouchListener((v, event) -> {
                if(event.getActionMasked() == MotionEvent.ACTION_UP && isLongPressed){
                    //Stop Repeating Burst
                    isLongPressed = false;
                    cameraControls.createPreview();
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
            cameraControls.captureImage();
        }
        else if (id == R.id.thumbPreview) {
            if(ImageSaverThread.staticUri == null){
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/jpeg");
                startActivity(i);
            }
            else{
                Log.e(TAG, "onClick: uri : "+ImageSaverThread.staticUri);
                final String GALLERY_REVIEW = "com.android.camera.action.REVIEW";
                Intent i = new Intent(GALLERY_REVIEW);
                i.setData(ImageSaverThread.staticUri);
                startActivity(i);
            }
        }

    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            cameraControls.setSurfaceTexture(surface);
            cameraControls.openCamera("0");

            runOnUiThread(() -> {
                previewView.measure(1080, 1440);
                previewView.setAspectRatio(1080,1440);
            });
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        cameraControls.setResumed(true);
        cameraControls.startBackgroundThread();
        cameraControls.openCamera("0");

        //Permission Check
        //displayLatestImage
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraControls.setResumed(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraControls.closeCamera();
        cameraControls.stopBackgroundThread();
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
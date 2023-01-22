package com.uncanny.camx.CameraManager;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.app.ActivityCompat;

import com.uncanny.camx.Data.CamState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class CameraControls {
    private String TAG = "CameraControls";
    private Activity activity;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private CameraDevice cameraDevice;
    private CameraManager cameraManager;
    private CameraCaptureSession cameraCaptureSession;
    private CameraCharacteristics cameraCharacteristics;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest.Builder previewRequestBuilder;
    private ImageReader imageReader;
    private MediaRecorder mMediaRecorder;
    private CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
    private MediaActionSound sound = new MediaActionSound();

    private OutputConfiguration previewConfiguration;
    private OutputConfiguration recordConfiguration;
    private OutputConfiguration snapshotConfiguration;

    private Handler cameraHandler;
    private Handler bHandler;
    private HandlerThread mBackgroundThread;
    private HandlerThread bBackgroundThread;
    private Executor bgExecutor = Executors.newSingleThreadExecutor();

    private Surface recordSurface,previewSurface;
    private SurfaceTexture stPreview;
    private List<Surface> surfaceList;

    private int CODE_CAMERA_PERMISSION = 101;
    private boolean resumed;
    private boolean shouldDeleteEmptyFile;
    private File videoFile;

    private CamState state;

    public CameraControls(Activity activity) {
        this.activity = activity;
        init();
    }

    private void init(){
        state.setState(CamState.CAMERA);
        surfaceList = new ArrayList<>();
    }

    public void setResumed(boolean resumed){
        this.resumed = resumed;
    }

    public boolean isShouldDeleteEmptyFile() {
        return shouldDeleteEmptyFile;
    }

    public void setShouldDeleteEmptyFile(boolean shouldDeleteEmptyFile) {
        this.shouldDeleteEmptyFile = shouldDeleteEmptyFile;
    }

    public boolean deleteFile(){
        return videoFile.delete();
    }

    public void setSurfaceTexture(SurfaceTexture texture){
        this.stPreview = texture;
    }

    public void openCamera(String cameraId) {
        if(!resumed || stPreview == null) return;
        try {
            cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

//        Don't need cause hardcoded the values
//        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            //set preview resolution
//            previewView.measure(1080, 1440);
            stPreview.setDefaultBufferSize(1440, 1080);

            //set capture resolution
            imageReader = ImageReader.newInstance(4000, 3000, ImageFormat.JPEG, 3);
            imageReader.setOnImageAvailableListener(new OnJpegImageAvailableListener(), cameraHandler);

            surfaceList.clear();
            surfaceList.add(new Surface(stPreview));
            surfaceList.add(imageReader.getSurface());

            try {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(new String[]{ Manifest.permission.CAMERA}, CODE_CAMERA_PERMISSION);
                }
                cameraManager.openCamera(cameraId, cameraDeviceStateCallback, bHandler);
            } catch(CameraAccessException e) {
                Log.e(TAG, "openCamera: open failed: " + e.getMessage());
            }
        }
        catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * CAMERA
     */
    public void createPreview(){
        try {
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            previewRequestBuilder.addTarget(surfaceList.get(0));
            captureRequestBuilder.addTarget(surfaceList.get(0));

            cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, bHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void captureImage() {
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY,(byte) 100);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation());
            captureRequestBuilder.addTarget(surfaceList.get(1));
            captureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
            cameraCaptureSession.capture(captureRequestBuilder.build(), null, bHandler);

            sound.play(MediaActionSound.SHUTTER_CLICK);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void captureBurstImage(){
        try {
            captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY,(byte) 100);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation());

            captureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);

            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, true);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, true);

            captureRequestBuilder.addTarget(surfaceList.get(1));

            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build()
                    , new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                            cameraHandler.post(() -> sound.play(MediaActionSound.SHUTTER_CLICK));
                        }

                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);
                        }
                    }, bHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    /**
     * VIDEO
     */
    private void prepareMediaRecorder(){
        String mVideoLocation = "//storage//emulated//0//DCIM//Camera//";
        String mVideoSuffix = "Camera2_Video_" + System.currentTimeMillis() + ".mp4";

        if(resumed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) mMediaRecorder = new MediaRecorder(activity);
            else mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setOrientationHint(getJpegOrientation());
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setAudioSamplingRate(camcorderProfile.audioSampleRate);
        mMediaRecorder.setAudioEncodingBitRate(camcorderProfile.audioBitRate);
        mMediaRecorder.setAudioChannels(camcorderProfile.audioChannels);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mMediaRecorder.setInputSurface(persistentSurface);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.HEVC);
        mMediaRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
        mMediaRecorder.setVideoSize(1920,1080);

        shouldDeleteEmptyFile = true;
        videoFile = new File(mVideoLocation+mVideoSuffix);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mMediaRecorder.setOutputFile(videoFile);
        } else {
            mMediaRecorder.setOutputFile(mVideoLocation+mVideoSuffix);
        }

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createVideoPreview()  {
        if(!resumed || stPreview == null) return;

        try {
            prepareMediaRecorder();
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
//            previewView.getSurfaceTexture().setDefaultBufferSize(1920, 1080);
//            previewSurface = new Surface(surfaceList.get(0));

//            recordSurface = persistentSurface; // TODO: PersistentSurface not recording video in some devices
            recordSurface = mMediaRecorder.getSurface();

            previewRequestBuilder.addTarget(recordSurface);

            previewRequestBuilder.addTarget(surfaceList.get(0));

            previewRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE
                    ,CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                OutputConfiguration previewConfiguration  = new OutputConfiguration(previewSurface);
                OutputConfiguration recordConfiguration   = new OutputConfiguration(recordSurface);
                OutputConfiguration snapshotConfiguration = new OutputConfiguration(imageReader.getSurface());

//                previewConfiguration.enableSurfaceSharing();

                SessionConfiguration sessionConfiguration = new SessionConfiguration(SessionConfiguration.SESSION_REGULAR
                        , Arrays.asList(previewConfiguration,recordConfiguration,snapshotConfiguration)
                        , bgExecutor
                        , streamlineCaptureSessionCallback);

                cameraDevice.createCaptureSession(sessionConfiguration);
            }
            else{
                cameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface, imageReader.getSurface())
                        ,streamlineCaptureSessionCallback,null);
            }

        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private CameraCaptureSession.StateCallback streamlineCaptureSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            cameraCaptureSession = session;
            try {
                cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null,bHandler);
//                if(isVRecording){
//                    Log.e(TAG, "onConfigured: Preparing media Recorder");
//                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "onConfigureFailed: createVideoPreview()");
        }
    };

    private void startRecording(){
        sound.play(MediaActionSound.START_VIDEO_RECORDING);
        shouldDeleteEmptyFile = false;
        mMediaRecorder.start();
    }

    private void stopRecording(){
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
//        performMediaScan(videoFile.getAbsolutePath(),"video"); //TODO : Handle Efficiently
        createVideoPreview(); //without persistentSurface
        sound.play(MediaActionSound.STOP_VIDEO_RECORDING);
    }

    private void captureVideoSnapshot() {
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
            captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY,(byte) 100);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation());
            captureRequestBuilder.addTarget(imageReader.getSurface());

            cameraCaptureSession.capture(captureRequestBuilder.build(), null, bHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private class OnJpegImageAvailableListener implements ImageReader.OnImageAvailableListener {
        private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());
        private String cameraDir = Environment.getExternalStorageDirectory()+"//DCIM//Camera//";

        @WorkerThread
        @Override
        public void onImageAvailable(ImageReader imageReader) {
//            Log.e(TAG, "onImageAvailable: "+isLongPressed);
            try(Image image = imageReader.acquireNextImage()) {
                if (image != null ) {
                    ByteBuffer jpegByteBuffer = image.getPlanes()[0].getBuffer();
                    byte[] jpegByteArray = new byte[jpegByteBuffer.remaining()];
                    jpegByteBuffer.get(jpegByteArray);
                    bgExecutor.execute(() -> {
                        long date = System.currentTimeMillis();
                        String title = "Camx_" + dateFormat.format(date);
                        String displayName = title + ".jpeg";
                        String path = cameraDir + "/" + displayName;

                        File file = new File(path);

                        ContentValues values = new ContentValues();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Camera/");
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
                        values.put(MediaStore.Images.ImageColumns.TITLE, title);
                        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName);
                        values.put(MediaStore.Images.ImageColumns.DATA, path);
                        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date);
                        Uri u = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        saveByteBuffer(jpegByteArray, file, u);
                    });
                }
            }
            catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void saveByteBuffer(byte[] bytes, File file, Uri uri) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try(OutputStream outputStream = activity.getContentResolver().openOutputStream(uri)) {
                outputStream.write(bytes);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try(FileOutputStream fos = new FileOutputStream(file)){
                fos.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int getJpegOrientation() {
        int deviceOrientation = activity.getWindowManager().getDefaultDisplay().getRotation();

        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        int sensorOrientation =  cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int surfaceRotation = ORIENTATIONS.get(deviceOrientation);

        return (surfaceRotation + sensorOrientation + 270) % 360;
    }

    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Main");
        bBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        bBackgroundThread.start();
        cameraHandler = new Handler(mBackgroundThread.getLooper());
        bHandler = new Handler(bBackgroundThread.getLooper());
    }

    public void stopBackgroundThread() {
        if(mBackgroundThread!=null) mBackgroundThread.quitSafely();
        if(bBackgroundThread!=null) bBackgroundThread.quitSafely();
        else{
            activity.finishAffinity();
            return;
        }
        try {
            mBackgroundThread.join();
            bBackgroundThread.join();
            cameraHandler = null;
            bHandler = null;
            mBackgroundThread = null;
            bBackgroundThread = null;
            cameraManager = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;

            if(state.getState() == CamState.CAMERA){
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    List<OutputConfiguration> outputs = new ArrayList<>();
                    outputs.add(new OutputConfiguration(surfaceList.get(0)));
                    outputs.add(new OutputConfiguration(surfaceList.get(1)));
                    SessionConfiguration sessionConfiguration = new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                            outputs,
                            bgExecutor,
                            stateCallback);

                    try {
                        cameraDevice.createCaptureSession(sessionConfiguration);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    try {
                        cameraDevice.createCaptureSession(surfaceList,stateCallback, bHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if(state.getState() == CamState.VIDEO){
                if(resumed) {
                    Log.e(TAG, "onOpened: SURFACE ABANDONED");
//                persistentSurface = MediaCodec.createPersistentInputSurface();
                    mMediaRecorder = null;
                }
                createVideoPreview();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice = null;
            Log.e(TAG, "onError: error int : "+error);
        }
    };

    private CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            cameraCaptureSession = session;
            createPreview();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "IMAGE CAPTURE CALLBACK: onConfigureFailed: configure failed");
        }
    };

}

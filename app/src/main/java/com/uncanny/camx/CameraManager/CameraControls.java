package com.uncanny.camx.CameraManager;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
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
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.app.ActivityCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.material.imageview.ShapeableImageView;
import com.uncanny.camx.Data.CamState;
import com.uncanny.camx.R;
import com.uncanny.camx.UI.Views.UncannyChronometer;

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
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class CameraControls {
    private String TAG = "CameraControls";
    private Activity activity;
    private final int CODE_CAMERA_PERMISSION = 101;
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
    private CameraConstrainedHighSpeedCaptureSession highSpeedCaptureSession;
    private CameraCharacteristics cameraCharacteristics;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest.Builder previewRequestBuilder;
    private ImageReader imageReader;
    private MediaRecorder mMediaRecorder;
    private CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH_SPEED_1080P); //TODO: DYNAMIC
    private MediaActionSound sound = new MediaActionSound();

    private Handler cameraHandler;
    private Handler bHandler;
    private HandlerThread mBackgroundThread;
    private HandlerThread bBackgroundThread;
    private Executor bgExecutor = Executors.newSingleThreadExecutor();

    private Surface recordSurface,previewSurface;
    private SurfaceTexture previewSurfaceTexture;
    private List<Surface> surfaceList;


    private int counter =0;
    private long pauseDuration;
    private boolean activityResumed;
    private boolean cameraDeviceClosed;
    private boolean videoPaused = false;
    private boolean shouldDeleteEmptyFile;
    private Uri uri;
    private File scan,videoFile;
    private List<CaptureRequest> captureRequest = new ArrayList<>();

    private ShapeableImageView thumbPreview;
    private UncannyChronometer chronometer;

//    private CamState state = CamState.CAMERA;

    public CameraControls(Activity activity) {
        this.activity = activity;
        init();
    }

    private void init(){
        CamState.getInstance().setState(CamState.CAMERA);
        surfaceList = new ArrayList<>();
    }

    public void setActivityResumed(boolean activityResumed){
        this.activityResumed = activityResumed;
    }

    public boolean isVideoPaused(){
        return videoPaused;
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

    public void setThumbView(ShapeableImageView thumbPreview) {
        this.thumbPreview = thumbPreview;
    }

    public void setSurfaceTexture(SurfaceTexture texture){
        this.previewSurfaceTexture = texture;
    }


    public Optional<Uri> getUri(){
        return Optional.ofNullable(uri);
    }

    private void setUri(Uri uri){
        this.uri = uri;
    }

    public int getLensFacing(){
        return cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
    }

    private void getCameraCharacteristics(String id){
        cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(id);
        } catch (CameraAccessException e) {
            Log.e(TAG, "getCameraCharacteristics: "+e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * CAMERA
     */

    public void openCamera(String cameraId) {
        if(!activityResumed || previewSurfaceTexture == null) return;
        if(sound == null) sound = new MediaActionSound();
        getCameraCharacteristics(cameraId);

        surfaceList.clear();
        setPreviewSize();
        setImageSize();

        try {
            if(cameraDevice!=null && cameraCaptureSession!=null && !cameraDeviceClosed) {
                //causes IllegalState Exception: CameraDevice was already closed, onResume
                cameraCaptureSession.stopRepeating();
                Log.e(TAG, "openCamera: STOPPING REPEATING");
            }
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{ Manifest.permission.CAMERA}, CODE_CAMERA_PERMISSION);
            }
            cameraManager.openCamera(cameraId, openCameraCallback, bHandler);
        } catch(CameraAccessException e) {
            Log.e(TAG, "openCamera: open failed: " + e.getMessage());
        }
    }

    public void setPreviewSize(){
//        if(previewSurface!=null) previewSurface.release();
        Log.e(TAG, "setPreviewSize: "+CamState.getInstance().getState());
        if(CamState.getInstance().getState() == CamState.VIDEO ||
           CamState.getInstance().getState() == CamState.TIMELAPSE)
            previewSurfaceTexture.setDefaultBufferSize(1920, 1080);
        else
            previewSurfaceTexture.setDefaultBufferSize(1440, 1080);
        previewSurface = new Surface(previewSurfaceTexture);
        surfaceList.add(previewSurface);
    }

    public void setImageSize(){
        if(CamState.getInstance().getState() == CamState.VIDEO ||
                CamState.getInstance().getState() == CamState.TIMELAPSE)
            imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 3);
        else
            imageReader = ImageReader.newInstance(4000, 3000, ImageFormat.JPEG, 3); // BURST LAG IS OK

        imageReader.setOnImageAvailableListener(new OnImageAvailableListener(), cameraHandler);
        surfaceList.add(imageReader.getSurface());
    }

    private void createSession() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            List<OutputConfiguration> outputs = new ArrayList<>();
            outputs.add(new OutputConfiguration(surfaceList.get(0)));
            outputs.add(new OutputConfiguration(surfaceList.get(1)));
            SessionConfiguration sessionConfiguration = new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                    outputs,
                    bgExecutor,
                    cameraCaptureSessionCallback);

            try {
                previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

                cameraDevice.createCaptureSession(sessionConfiguration);
            } catch (CameraAccessException e) {
                Log.e(TAG, "createSession: "+e.getMessage());
            }
        }
        else{
            try {
                previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

                cameraDevice.createCaptureSession(surfaceList, cameraCaptureSessionCallback, bHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, "createSession: "+e.getMessage());
            }
        }
    }

    public void createPreview(){
        try {
            previewRequestBuilder.addTarget(surfaceList.get(0));
            captureRequestBuilder.addTarget(surfaceList.get(0));

            cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, bHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "createPreview: "+e.getMessage());
        }
    }

    public void captureImage() {
        try {
            captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY,(byte) 100);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation());
            captureRequestBuilder.addTarget(surfaceList.get(1));
            captureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
            cameraCaptureSession.capture(captureRequestBuilder.build(), null, bHandler);

            sound.play(MediaActionSound.SHUTTER_CLICK);
        } catch (CameraAccessException e) {
            Log.e(TAG, "captureImage: "+e.getMessage());
        }
    }

    /**
     * Tips to minimize burst lag :<br>
     * - Use lower resolution for image reader.<br>
     * - Use Limited Burst Capture only.
     */
    public void captureLimitedBurst(int limit){
        try {
            captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY,(byte) 100);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation());

            captureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);

            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, true);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, true);

            captureRequestBuilder.addTarget(surfaceList.get(1));

            for(int i=0; i<20; i++){
                captureRequest.add(captureRequestBuilder.build());
            }

            cameraCaptureSession.captureBurst(captureRequest
                    , new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                        }

                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);
                            Log.e(TAG, "onCaptureCompleted: counter : "+ ++counter);
                        }

                        @Override
                        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                            counter = 0;

                        }

                        @Override
                        public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
                            super.onCaptureSequenceAborted(session, sequenceId);
                            counter = 0;
                        }

                        @Override
                        public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
                            super.onCaptureBufferLost(session, request, target, frameNumber);
                        }
                    }
                    ,cameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "captureLimitedBurst: "+e.getMessage());
        }
    }

    public void captureBurstImage(){
        try {
            captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY,(byte) 100);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation());

            captureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

//            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, true);
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, true);

            captureRequestBuilder.addTarget(surfaceList.get(1));

            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build()
                    , new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                            super.onCaptureStarted(session, request, timestamp, frameNumber);
//                            cameraHandler.post(() -> sound.play(MediaActionSound.SHUTTER_CLICK));
                        }

                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);
                        }
                    }, cameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "captureBurstImage: "+e.getMessage());
        }
    }

    public void stopBurstCapture(){
        createPreview();
    }

    public void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if(previewSurface != null) previewSurface.release();
    }

    private CameraDevice.StateCallback openCameraCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;

            Log.e(TAG, "onOpened: "+CamState.getInstance().getState());
            if(CamState.getInstance().getState() == CamState.CAMERA){
                createSession();
            }
            else if(CamState.getInstance().getState() == CamState.VIDEO ||
                    CamState.getInstance().getState() == CamState.TIMELAPSE ||
                    CamState.getInstance().getState() == CamState.SLOMO){
                if(activityResumed) {
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

    private CameraCaptureSession.StateCallback cameraCaptureSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            cameraDeviceClosed = false;
            cameraCaptureSession = session;
            createPreview();
        }

        @Override
        public void onReady(@NonNull CameraCaptureSession session) {
            super.onReady(session);
        }

        @Override
        public void onCaptureQueueEmpty(@NonNull CameraCaptureSession session) {
            super.onCaptureQueueEmpty(session);
            // TODO: Minimize latency of burst requests here
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            cameraDeviceClosed = true;
            super.onClosed(session);
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "IMAGE CAPTURE CALLBACK: onConfigureFailed: configure failed");
        }
    };

    /**
     * VIDEO
     */

    @WorkerThread
    private void prepareMediaRecorder(){
        if(activityResumed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) mMediaRecorder = new MediaRecorder(activity);
            else mMediaRecorder = new MediaRecorder();

        String mVideoLocation= "", mVideoSuffix="";
//        if(recordSurface != null) recordSurface.release();

        if(CamState.getInstance().getState() == CamState.VIDEO){
            mVideoLocation = "//storage//emulated//0//DCIM//Camera//";
            mVideoSuffix = "CamX_" + System.currentTimeMillis() + ".mp4";
            camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
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

        }
        else if(CamState.getInstance().getState() == CamState.TIMELAPSE){
            mVideoLocation = "//storage//emulated//0//DCIM//Camera//";
            mVideoSuffix = "CamX_" + System.currentTimeMillis() + "_TIMELAPSE.mp4";
            camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_1080P);
            mMediaRecorder.setOrientationHint(getJpegOrientation());
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setProfile(camcorderProfile);
            mMediaRecorder.setCaptureRate(10f);  //12 10 2

        }
        else if(CamState.getInstance().getState() == CamState.SLOMO){
            mVideoLocation = "//storage//emulated//0//DCIM//Camera//";
            mVideoSuffix = "Camera2_Video_" + System.currentTimeMillis()+ "_HSR_120" + ".mp4";

            mMediaRecorder.setOrientationHint(getJpegOrientation());
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setAudioSamplingRate(48000);
            mMediaRecorder.setAudioEncodingBitRate(96000);
            mMediaRecorder.setAudioChannels(1);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setVideoFrameRate(120); // for maximizing support (will add checks later)
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.HEVC);
            mMediaRecorder.setVideoEncodingBitRate(7372800); // calculation -> 720*1280*120/15
            mMediaRecorder.setVideoSize(1280,720); // for maximizing support (will add checks later)
        }

        shouldDeleteEmptyFile = true;
        videoFile = new File(mVideoLocation+mVideoSuffix);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mMediaRecorder.setOutputFile(videoFile);
        } else {
            mMediaRecorder.setOutputFile(mVideoLocation+mVideoSuffix);
        }

        try {
            mMediaRecorder.prepare();
            Log.e(TAG, "prepareMediaRecorder: P R E P A R E D");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createVideoPreview() {
        if(!activityResumed || previewSurfaceTexture == null) return;
        if (CamState.getInstance().getState() == CamState.SLOMO) {
            createSlowMotionPreview();
            return;
        }

        try {
            prepareMediaRecorder();
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            previewSurfaceTexture.setDefaultBufferSize(1920, 1080); //TODO: Redundant ?? see setPreviewSize()

//            recordSurface = persistentSurface; // FIXME: PersistentSurface not recording video in some devices
            previewSurface = surfaceList.get(0);
            recordSurface = mMediaRecorder.getSurface(); //TODO: Address resource close

            previewRequestBuilder.addTarget(recordSurface);

            previewRequestBuilder.addTarget(previewSurface);

            previewRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE
                    ,CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                List<OutputConfiguration> outputs = new ArrayList<>();
                outputs.add(new OutputConfiguration(previewSurface));
                outputs.add(new OutputConfiguration(recordSurface));
                outputs.add(new OutputConfiguration(imageReader.getSurface()));

//                previewConfiguration.enableSurfaceSharing();

                SessionConfiguration sessionConfiguration = new SessionConfiguration(SessionConfiguration.SESSION_REGULAR
                        , outputs
                        , bgExecutor
                        , videoCaptureSessionCallback);

                cameraDevice.createCaptureSession(sessionConfiguration);
            }
            else{
                cameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface, imageReader.getSurface())
                        , videoCaptureSessionCallback,bHandler);
            }

        }
        catch (CameraAccessException e) {
            Log.e(TAG, "createVideoPreview: "+e.getMessage());
        }

    }

    private CameraCaptureSession.StateCallback videoCaptureSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            cameraDeviceClosed = false;
            if(CamState.getInstance().getState() == CamState.SLOMO){
                highSpeedCaptureSession = (CameraConstrainedHighSpeedCaptureSession) session;
                try {
                    highSpeedCaptureSession.setRepeatingBurst(
                            highSpeedCaptureSession.createHighSpeedRequestList(previewRequestBuilder.build()),null,bHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            else {
                cameraCaptureSession = session;
                try {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) cameraCaptureSession.finalizeOutputConfigurations(Arrays.asList(previewConfiguration, recordConfiguration, snapshotConfiguration));
                    cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null,bHandler);
//                if(isVRecording){
//                    Log.e(TAG, "onConfigured: Preparing media Recorder");
//                }
                } catch (CameraAccessException e) {
                    Log.e(TAG, "onConfigured: "+e.getMessage());
                }
            }
        }

        @Override
        public void onCaptureQueueEmpty(@NonNull CameraCaptureSession session) {
            super.onCaptureQueueEmpty(session);
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            super.onClosed(session);
            Log.e(TAG, "onClosed: Camera Device Closed");
            cameraDeviceClosed = true;
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            cameraDeviceClosed = true;
            Log.e(TAG, "onConfigureFailed: ");
        }
    };

    public void startRecording(){
        sound.play(MediaActionSound.START_VIDEO_RECORDING);
        shouldDeleteEmptyFile = false;
//        activity.runOnUiThread(() -> {
            if(chronometer==null) chronometer = activity.findViewById(R.id.chronometer);
            chronometer.setVisibility(View.VISIBLE);
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
//        });
        mMediaRecorder.start();
    }

    public void pauseResume(){
        if(videoPaused) {
            mMediaRecorder.resume();
            chronometer.resume();
        }
        else {
            mMediaRecorder.pause();
            chronometer.pause();
        }
        videoPaused = !videoPaused;
    }

    public void stopRecording(){
        scan = videoFile;
        videoPaused = false;
        chronometer.stop();
        chronometer.setVisibility(View.GONE);
        mMediaRecorder.stop(); //FIXME: HANDLE IMMEDIATE STOP AFTER START
//        mMediaRecorder.reset();
//        mMediaRecorder.release();
//        if(recordSurface.isValid()) recordSurface.release();
        if (recordSurface.isValid()) {
            recordSurface.release();
            mMediaRecorder.release();
            Log.e(TAG, "stopRecording: R E L E A S I N G  RECORDER SURFACE");
        }

        bHandler.post(() -> mediaScan(scan,"video"));
        cameraHandler.post(this::createVideoPreview); //without persistentSurface
        sound.play(MediaActionSound.STOP_VIDEO_RECORDING);
    }

    public void captureVideoSnapshot() {
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
            captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY,(byte) 100);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation());
            captureRequestBuilder.addTarget(imageReader.getSurface());

            cameraCaptureSession.capture(captureRequestBuilder.build(), null, bHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "captureVideoSnapshot: "+e.getMessage());
        }
    }

    /**
     * Slow Motion
     */

    private void createSlowMotionPreview() {
        if(!activityResumed || previewSurfaceTexture == null) return;

        try {
            prepareMediaRecorder();
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            previewSurfaceTexture.setDefaultBufferSize(1280, 720);

//            recordSurface = persistentSurface; // TODO: PersistentSurface not recording video in some devices
            previewSurface = surfaceList.get(0);
            recordSurface = mMediaRecorder.getSurface();

            previewRequestBuilder.addTarget(recordSurface);

            previewRequestBuilder.addTarget(previewSurface);

            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(120,120));

            previewRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE
                    ,CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                List<OutputConfiguration> outputs = new ArrayList<>();
                outputs.add(new OutputConfiguration(previewSurface));
                outputs.add(new OutputConfiguration(recordSurface));
//                previewConfiguration.enableSurfaceSharing();

                SessionConfiguration sessionConfiguration = new SessionConfiguration(SessionConfiguration.SESSION_HIGH_SPEED
                        , outputs
                        , bgExecutor
                        , videoCaptureSessionCallback);

                cameraDevice.createCaptureSession(sessionConfiguration);
            }
            else{
                cameraDevice.createConstrainedHighSpeedCaptureSession(Arrays.asList(previewSurface, recordSurface)
                        ,videoCaptureSessionCallback,null);
            }

        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    @WorkerThread
    private void mediaScan(File file,String type){
        String mimeType = null;
        if(type.equals("image")) mimeType = "image/jpeg";
        else if(type.equals("video")) mimeType = "video/mp4";
        MediaScannerConnection.scanFile(activity
                ,new String[] {file.getAbsolutePath() }
                ,new String[] { mimeType }
                ,(path, uri) -> {
                    Log.e("TAG", "Scanned " + path + ":");
                    Log.e("TAG", "-> uri=" + uri);
                    setUri(uri);
//                    try(MediaMetadataRetriever retriever = new MediaMetadataRetriever()){
//                        retriever.setDataSource(path);
//                        Bitmap bitmap = retriever.getFrameAtTime();
//                        activity.runOnUiThread(() -> thumbPreview.setImageBitmap(bitmap));
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }

                    try {
                        Bitmap thumbnail;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            thumbnail = activity.getContentResolver().loadThumbnail(uri,new Size(96, 96), new CancellationSignal());
                            activity.runOnUiThread(() -> thumbPreview.setImageBitmap(thumbnail));
                        }
                        else {
                            thumbnail = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND);
                            activity.runOnUiThread(() -> thumbPreview.setImageBitmap(thumbnail));
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "mediaScan: "+e.getMessage());
                    }
                });
    }

    @WorkerThread
    private class OnImageAvailableListener implements ImageReader.OnImageAvailableListener {
        private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());
        private String cameraDir = Environment.getExternalStorageDirectory()+"//DCIM//Camera//";

        @Override
        public void onImageAvailable(ImageReader imageReader) {
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

                        // MediaStore.Images.ImageColumns.F_NUMBER

                        ContentValues values = new ContentValues();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Camera/");
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
                        values.put(MediaStore.Images.ImageColumns.TITLE, title);
                        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName);
                        values.put(MediaStore.Images.ImageColumns.DATA, path);
                        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date);
                        Uri u = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        setUri(u);
                        saveByteBuffer(jpegByteArray, file, u);

                        if(CamState.getInstance().getState() != CamState.VIDEO_PROGRESSED &&
                           CamState.getInstance().getState() != CamState.TIMELAPSE_PROGRESSED)
                            activity.runOnUiThread(() -> thumbPreview.setImageBitmap(getThumbnail(path)));
                    });
                }
            }
            catch (Exception e) { e.printStackTrace(); }
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

        private Bitmap getThumbnail(String jpegPath) {
            ExifInterface exifInterface;
            try {
                exifInterface = new ExifInterface(jpegPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            float orientation;
            switch (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_NORMAL:
                    orientation = 0.0F;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    orientation = 90.0F;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    orientation = 180.0F;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    orientation = 270.0F;
                    break;
                default:
                    orientation = 0.0F;
            }

            Bitmap thumbnail;
            if (exifInterface.hasThumbnail()) {
                thumbnail = exifInterface.getThumbnailBitmap();
            } else {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 16;
                thumbnail = BitmapFactory.decodeFile(jpegPath, options);
            }

            if (orientation != 0.0F && thumbnail != null) {
                Matrix matrix = new Matrix();
                matrix.setRotate(orientation);
                thumbnail = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), matrix, true);
            }

            return thumbnail;
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
//        bHandler = new Handler(activity.getMainLooper());
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

    public void shutDown(){
        if(sound != null){
            sound.release();
        }
        previewSurface.release();
        recordSurface.release();
        mMediaRecorder.release();
        previewSurfaceTexture.release();
    }
}

package com.uncanny.camx.Control;

import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.uncanny.camx.Activity.CameraActivity;
import com.uncanny.camx.UI.Views.ViewFinder.FocusCircle;

import java.util.Objects;

@SuppressWarnings({"FieldCanBeLocal","FieldMayBeFinal"})
public class FocusControls {
    private final String TAG = "FocusControls";

    private final CameraCharacteristics characteristics;
    private CameraCaptureSession camSession;
    private CaptureRequest.Builder previewCaptureRequest;
    private CameraConstrainedHighSpeedCaptureSession highSpeedCaptureSession;

    private FocusCircle focusCircle;
    private Runnable hideFocusCircle;
    private Handler handler;
    private CameraActivity.CamState state;

    public FocusControls(CameraCharacteristics characteristics, FocusCircle focusCircle, Runnable hideCallback
    ,CameraActivity.CamState state, CameraCaptureSession camSession, CaptureRequest.Builder previewCaptureRequest
    ,CameraConstrainedHighSpeedCaptureSession highSpeedCaptureSession, Handler handler){
        this.characteristics = characteristics;
        this.focusCircle = focusCircle;
        this.hideFocusCircle = hideCallback;
        this.state = state;
        this.camSession = camSession;
        this.previewCaptureRequest = previewCaptureRequest;
        this.highSpeedCaptureSession = highSpeedCaptureSession;
        this.handler = handler;
    }

    public void setFocus(int height, int width){
        focus( height, width,null);
    }

    public void setFocus(View v, MotionEvent e){
        focus(v.getHeight(), v.getWidth(),e);
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

        //first stop the existing repeating request
//        try {
//            cameraCaptureSessions.stopRepeating();
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }

        //cancel any existing AF trigger (repeated touches, etc.)
        previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
//        previewCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        buildPreview();
        previewCaptureRequest.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusAreaTouch});
        previewCaptureRequest.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{focusAreaTouch});
        previewCaptureRequest.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        previewCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        previewCaptureRequest.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);

        //set focus area repeating,else cam forget after one frame where it should focus
        //trigger af start only once. cam starts focusing till its focused or failed
        previewCaptureRequest.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        buildPreview();

        previewCaptureRequest.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
        previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);

        previewCaptureRequest.setTag("FOCUS_TAG"); //we'll capture this later for resuming the preview

        buildPreview();
//
    }

    private boolean isMeteringAreaAESupported() {
        Integer aeState = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        return aeState!=null && aeState >=1;
    }

    private boolean isMeteringAreaAFSupported() {
        return characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) >= 1;
    }

    private void buildPreview(){
        try {
            camSession.capture(previewCaptureRequest.build(), captureCallbackHandler, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void buildRepeatingPreview(){
        try {
            if(state== CameraActivity.CamState.HSVIDEO_PROGRESSED)
                highSpeedCaptureSession.createHighSpeedRequestList(previewCaptureRequest.build());
            else
                camSession.setRepeatingRequest(previewCaptureRequest.build(), captureCallbackHandler, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback captureCallbackHandler = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            if (request.getTag() == "FOCUS_TAG") {
                //the focus trigger is complete -
                //resume repeating (preview surface will get frames), clear AF trigger
//                mBackgroundHandler.postDelayed(() ->
//                        previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE)
//                        ,3000);

                Log.e(TAG, "onCaptureCompleted: FOCUS COMPLETED ");
//                previewCaptureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
//                previewCaptureRequest.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
            }
        }
        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.e(TAG, "Manual AF failure: " + failure);
        }
    };

}

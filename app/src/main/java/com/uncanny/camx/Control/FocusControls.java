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

import com.uncanny.camx.App.CameraActivity;
import com.uncanny.camx.UI.ViewFinder.FocusCircle;

@SuppressWarnings({"FieldCanBeLocal","FieldMayBeFinal"})
public class FocusControls {
    private final String TAG = "FocusControls";

    private final CameraCharacteristics characteristics;
    private CameraCaptureSession camSession;
    private CaptureRequest.Builder previewCaptureRequest;
    private CameraConstrainedHighSpeedCaptureSession highSpeedCaptureSession;

    private FocusCircle focusCircle;
    private Runnable hideFocusCircle;
    private Handler mBackgroundHandler;
    private CameraActivity.CamState state;

    public FocusControls(CameraCharacteristics characteristics, FocusCircle focusCircle, Runnable hideCallback
    ,CameraActivity.CamState state, CameraCaptureSession camSession, CaptureRequest.Builder previewCaptureRequest
    ,CameraConstrainedHighSpeedCaptureSession highSpeedCaptureSession, Handler mBackgroundHandler){
        this.characteristics = characteristics;
        this.focusCircle = focusCircle;
        this.hideFocusCircle = hideCallback;
        this.state = state;
        this.camSession = camSession;
        this.previewCaptureRequest = previewCaptureRequest;
        this.highSpeedCaptureSession = highSpeedCaptureSession;
        this.mBackgroundHandler = mBackgroundHandler;

        setFocus();
    }

    public static void setFocus(){

    }

    public void touchToFocus(View v, MotionEvent event) {
        //TODO : create separate class & method for this
        focusCircle.removeCallbacks(hideFocusCircle);
        float h = event.getX();
        float w = event.getY();

        focusCircle.setVisibility(View.VISIBLE);
        focusCircle.setPosition((int)h,(int)w);
        focusCircle.postDelayed(hideFocusCircle,1200);

        Log.e(TAG, "touchToFocus: F O C U S I N G");

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
                        if(state== CameraActivity.CamState.HSVIDEO_PROGRESSED)
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
            if(state== CameraActivity.CamState.HSVIDEO_PROGRESSED)
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
            if(state== CameraActivity.CamState.HSVIDEO_PROGRESSED)
                highSpeedCaptureSession.createHighSpeedRequestList(previewCaptureRequest.build());
            else
                camSession.capture(previewCaptureRequest.build(), captureCallbackHandler, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
//        mManualFocusEngaged = true;
    }


    private boolean isMeteringAreaAESupported() {
        Integer aeState = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        return aeState!=null && aeState >=1;
    }

    private boolean isMeteringAreaAFSupported() {
        return characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) >= 1;
    }

}

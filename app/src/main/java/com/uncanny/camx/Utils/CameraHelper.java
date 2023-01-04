package com.uncanny.camx.Utils;

import static android.content.Context.CAMERA_SERVICE;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.util.SizeF;

import java.util.Arrays;
import java.util.Locale;

public class CameraHelper {
    private String TAG = "CameraHelper";

    public float getCameraFov(Context context, String cameraId) throws CameraAccessException {
        // Get the CameraManager instance
        CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);

        // Get the CameraCharacteristics for the camera
        CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

        // Get the width of the camera's image sensor in millimeters
        SizeF sensorWidth = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);

        Log.e(TAG, "getCameraFov: Sensor size : "+cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE));
        // Get the array of available apertures for the camera
        float[] availableApertures = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);

        Log.e(TAG, "getCameraFov: available Apertures : "+ Arrays.toString(availableApertures));
        // Calculate the focal length in millimeters
        float focalLength = availableApertures[0] / 2.0f;

        float horizontalFOV = 2 * (float) Math.atan(sensorWidth.getWidth() / (2 * focalLength));
        float verticalFOV = 2 * (float) Math.atan(sensorWidth.getHeight() / (2 * focalLength));;

        Log.e(TAG, "getCameraFov: width : "+sensorWidth+" HfovDegrees : "+Math.toDegrees(horizontalFOV)+" VfovDegrees : "+Math.toDegrees(verticalFOV));
        // Return the FOV in degrees
        return horizontalFOV;
    }

    public void getZoomFactor(Context context, String cameraID){
        try{
            CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics("0");

            float _35mmfocalLength_main = (36.0f / characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getWidth()
                    * characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0]);

            characteristics = cameraManager.getCameraCharacteristics(cameraID);
            float _35mmfocalLength = (36.0f / characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getWidth()
                    * characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0]);

            float zoomFactor = _35mmfocalLength / _35mmfocalLength_main;

            Log.e(TAG, "getZoomFactor: ZoomFactor : "+cameraID+" : "+getAuxButtonName(zoomFactor));
        }
        catch (CameraAccessException ignore){

        }

    }

    private static String getAuxButtonName(float zoomFactor) {
        return String.format(Locale.US, "%.1fx", (zoomFactor - 0.049)).replace(".0", "");
    }
}

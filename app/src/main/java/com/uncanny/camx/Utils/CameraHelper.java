package com.uncanny.camx.Utils;

import static android.content.Context.CAMERA_SERVICE;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;

import java.util.Arrays;
import java.util.Locale;

public class CameraHelper {
    private final String TAG = "CameraHelper";

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
        float verticalFOV = 2 * (float) Math.atan(sensorWidth.getHeight() / (2 * focalLength));

        Log.e(TAG, "getCameraFov     : HfovDegrees : "+String.format(Locale.US, "%.1f", Math.toDegrees(horizontalFOV))+" VfovDegrees : "+String.format(Locale.US, "%.1f", Math.toDegrees(horizontalFOV)));
        // Return the FOV in degrees
        return horizontalFOV;
    }

    public int computeViewAngles(Context context, String id) throws CameraAccessException {
        CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);

        Rect active_size = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        SizeF physical_size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        Size pixel_size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
        float [] focal_lengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        if( active_size == null || physical_size == null || pixel_size == null || focal_lengths == null || focal_lengths.length == 0 ) {
//            return new SizeF(55.0f, 43.0f);
            return 55;
        }

        float frac_x = ((float)active_size.width())/(float)pixel_size.getWidth();
//        float frac_y = ((float)active_size.height())/(float)pixel_size.getHeight();
        float view_angle_x = (float)Math.toDegrees(2.0 * Math.atan2(physical_size.getWidth() * frac_x, (2.0 * focal_lengths[0])));
//        float view_angle_y = (float)Math.toDegrees(2.0 * Math.atan2(physical_size.getHeight() * frac_y, (2.0 * focal_lengths[0])));

//        Log.e(TAG, "computeViewAngles: HfovDegrees : "+String.format(Locale.US, "%.1f", (view_angle_x))+" id : "+id);
//        return new SizeF(view_angle_x, view_angle_y);
        return (int) view_angle_x;
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
        return String.format(Locale.US, "%.1f", (zoomFactor)).replace(".0", "");
    }
}

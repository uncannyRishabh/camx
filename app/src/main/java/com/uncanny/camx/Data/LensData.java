package com.uncanny.camx.Data;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import com.uncanny.camx.Utility.CompareSizeByArea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LensData {
    private static final String TAG = "LensData";
    Activity activity;
    CameraCharacteristics characteristics;
    CameraManager cameraManager;
    Set<Integer> cameraId = new HashSet<>();
    Set<String> physicalCameraId = new HashSet<>();
    LensResolutionData lensResolutionData = new LensResolutionData();

    public LensData(Activity activity){
        this.activity = activity;
        getAuxCameras();
    }

    /**
     * First query aux cam availability then only call
     * {@link LensData#getCameraId()}
     */
    public boolean isAuxCameraAvailable(){
        return (cameraId.size() != 0);
    }

    /**
     * returns the number of Auxiliary cameras(Integer).
     */
    public Set<Integer> getCameraId(){
        return cameraId;
    }

    public int numberOfAux(){
        return cameraId.size();
    }

    private void getAuxCameras(){
        for(int i = 0; i<=33 ; i++){
            try {
                cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(String.valueOf(i));
                if (!characteristics.getAvailableCaptureRequestKeys().isEmpty() ) {
                    cameraId.add(i);
                    Log.e(TAG, "check_aux: value of array at " + i + " : " + i);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        physicalCameraId = characteristics.getPhysicalCameraIds();
                    }
                }
            }
            catch (IllegalArgumentException | CameraAccessException ignored){ }
        }
        Toast.makeText(activity, "Execution Completed cam_aux()", Toast.LENGTH_SHORT).show();
    }

    public void getCameraLensCharacteristics(String id){
        StreamConfigurationMap map = getCameraCharacteristics(id)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Log.e(TAG, "getCameraLensCharacteristics: SLO MO : "+Arrays.toString(map.getHighSpeedVideoSizes()));

    }

    private void BayerCheck(int id) {
        StreamConfigurationMap map = getCameraCharacteristics(id+"")
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map.getHighResolutionOutputSizes(ImageFormat.JPEG) != null) {
            Size[] size = (map.getHighResolutionOutputSizes(ImageFormat.JPEG).length > 0 ?
                    map.getHighResolutionOutputSizes(ImageFormat.JPEG) :
                    map.getHighResolutionOutputSizes(ImageFormat.RAW_SENSOR));
            if (size.length > 0) {
                lensResolutionData.setBayer(true);
                ArrayList<Size> sizeArrayList = new ArrayList<>(Arrays.asList(size));
                Size mSize = Collections.max(sizeArrayList, new CompareSizeByArea());
                lensResolutionData.setBayerPhotoSize(mSize);
//                Log.e(TAG, "BayerCheck: BAYER SENSOR SIZE : " + Arrays.toString(size) + " mSize : " + mSize);
            } else {
                lensResolutionData.setBayer(false);
                Log.e(TAG, "BayerCheck: NOT BAYER : ID : " + id);
            }
        }
    }

    private CameraCharacteristics getCameraCharacteristics(String camId) {
        cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            characteristics = cameraManager.getCameraCharacteristics(camId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return characteristics;
    }
}

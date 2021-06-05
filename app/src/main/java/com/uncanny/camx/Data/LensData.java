package com.uncanny.camx.Data;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import com.uncanny.camx.Utility.CompareSizeByArea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LensData {
    private static final String TAG = "LensData";
    private static final String CAMERA_MAIN_BACK = "0";
    private static final String CAMERA_MAIN_FRONT = "1";
    private int LOGICAL_ID;
    Context activity;
    CameraCharacteristics characteristics;
    CameraManager cameraManager;
    List<Integer> physicalCameras = new ArrayList<>();
    List<Integer> logicalCameras  = new ArrayList<>();
    List<Integer> auxiliaryCameras  = new ArrayList<>();
    LensResolutionData lensResolutionData = new LensResolutionData();
    int[] camcorderQualities = {
            CamcorderProfile.QUALITY_LOW, CamcorderProfile.QUALITY_HIGH, CamcorderProfile.QUALITY_QCIF
            , CamcorderProfile.QUALITY_CIF, CamcorderProfile.QUALITY_480P, CamcorderProfile.QUALITY_720P
            , CamcorderProfile.QUALITY_1080P, CamcorderProfile.QUALITY_2160P, CamcorderProfile.QUALITY_TIME_LAPSE_LOW
            , CamcorderProfile.QUALITY_TIME_LAPSE_HIGH, CamcorderProfile.QUALITY_TIME_LAPSE_QCIF, CamcorderProfile.QUALITY_TIME_LAPSE_CIF
            , CamcorderProfile.QUALITY_TIME_LAPSE_480P, CamcorderProfile.QUALITY_TIME_LAPSE_720P, CamcorderProfile.QUALITY_TIME_LAPSE_1080P
            , CamcorderProfile.QUALITY_TIME_LAPSE_2160P, CamcorderProfile.QUALITY_HIGH_SPEED_LOW, CamcorderProfile.QUALITY_HIGH_SPEED_HIGH
            , CamcorderProfile.QUALITY_HIGH_SPEED_480P, CamcorderProfile.QUALITY_HIGH_SPEED_720P, CamcorderProfile.QUALITY_HIGH_SPEED_1080P
            , CamcorderProfile.QUALITY_HIGH_SPEED_2160P
    };

    /**
     * Constructor for this class.
     */
    public LensData(Context activity){
        this.activity = activity;
        getAuxCameras();
    }

    /**
     * Returns boolean for Auxiliary Camera availability.
     * First query aux cam availability then only call
     * {@link LensData#getPhysicalCameras()}.
     */
    public boolean isAuxCameraAvailable(){
        return (auxiliaryCameras.size() != 0);
    }

    /**
     * Returns number of Auxiliary Camera Sensors other than cameraId (0,1).
     */
    public int totalAuxCameras(){
        return auxiliaryCameras.size();
    }

    /**
     * Returns a list of camera Ids for {@link LensData#totalAuxCameras()}.
     */
    public List<Integer> getAuxiliaryCameras(){
        auxiliaryCameras = new ArrayList<>(physicalCameras);
        auxiliaryCameras.remove((Object)0);
        auxiliaryCameras.remove((Object)1);
        Log.e(TAG, "getAuxiliaryCameras: "+auxiliaryCameras);
        return auxiliaryCameras;
    }

    /**
     * Returns total number of Camera Sensors including cameraId (0,1).
     */
    public int totalPhysicalCameras(){
        return physicalCameras.size();
    }

    /**
     * Returns a list of camera Ids for {@link LensData#totalPhysicalCameras()}.
     */
    public List<Integer> getPhysicalCameras(){
        return physicalCameras;
    }

    /**
     * Returns the number of logical cameras (if present)
     * check {@link CameraCharacteristics#getPhysicalCameraIds()}.
     */
    public int totalLogicalCameras(){
        return logicalCameras.size();
    }

    /**
     * Returns a list of camera Ids for {@link LensData#totalLogicalCameras()} ()}.
     */
    public List<Integer> getLogicalCameras(){
        return logicalCameras;
    }


    /**
     * Returns boolean for Camera2api availability.
     */
    public boolean hasCamera2api(){
        Integer c2api = getCameraCharacteristics("0").get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if(c2api!=null && c2api == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED){
            Log.e(TAG, "hasCamera2api: HARDWARE_LEVEL_LIMITED");
            return true;
        }
        else if(c2api!=null && c2api == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL){
            Log.e(TAG, "hasCamera2api: HARDWARE_LEVEL_FULL");
            return true;
        }
        else if(c2api!=null && c2api == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY){
            Log.e(TAG, "hasCamera2api: HARDWARE_LEVEL_LEGACY");
            return true;
        }
        else if(c2api!=null && c2api == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3){
            Log.e(TAG, "hasCamera2api: HARDWARE_LEVEL_3");
            return true;
        }
        else if(c2api!=null && c2api == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL){
            Log.e(TAG, "hasCamera2api: HARDWARE_LEVEL_EXTERNAL");
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * Use preference key to save modes.
     */
    public String[] getAvailableModes(String id){
        List<String> cameraModes = new ArrayList<>();
        cameraModes.add("Camera");
        cameraModes.add("Video");
        StreamConfigurationMap map = getStreamConfigMap(id);
        Log.e(TAG, "getAvailableModes: SLO MOE CHECK CID : "+id+" HSV ranges : "+ Arrays.toString(map.getHighSpeedVideoFpsRanges()));
        if(Arrays.asList(map.getHighSpeedVideoFpsRanges()).size()!=0){
            cameraModes.add("Slo Moe");
            //didn't add the unnecessary checks
            cameraModes.add("TimeWarp");
        }
        cameraModes.add("Portrait");
        cameraModes.add("Night");
        if(hasCamera2api()){
            cameraModes.add("Pro");
        }
        //ADD CHECK FOR MULTI MODE (if there's one)
        return cameraModes.toArray(new String[0]);
    }

    /**
     * DEBUGGING purposes
     */
    public void getCameraLensCharacteristics(String id){
        StreamConfigurationMap map = getStreamConfigMap(id);
        CameraCharacteristics cc = getCameraCharacteristics(id);
        cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Log.e(TAG, "getCameraLensCharacteristics: "+cc.getPhysicalCameraIds());
        }
        Log.e(TAG, "getCameraLensCharacteristics: SLO MO : FPS RANGE : "+ Arrays.toString(map.getHighSpeedVideoFpsRanges()));
//        Log.e(TAG, "getCameraLensCharacteristics: SLO MO : VDO SIZES : "+ Arrays.toString(map.getHighSpeedVideoSizes()));

    }

    /**
     * returns a {@link CamcorderProfile} for camera ids [0,1] only..not recommended.
     * @param id the id for the camera.
     */
    public CamcorderProfile getCamcorderProfile(int id){
        for(int qualities : camcorderQualities) {
            if (CamcorderProfile.hasProfile(id,qualities)) {
                Log.e(TAG, "getCamcorderProfile: Qualities : cid : "+id+" w :"
                        +CamcorderProfile.get(id,qualities).videoFrameWidth
                        +" h : "+CamcorderProfile.get(id,qualities).videoFrameHeight);
//                return CamcorderProfile.get(id,qualities);
            }
        }
        return CamcorderProfile.get(id,CamcorderProfile.QUALITY_HIGH);
    }

    private void getAuxCameras(){
        for(int i = 0; i<=102 ; i++){
            try {
                cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(String.valueOf(i));
                if (characteristics!=null ) {
                    Log.e(TAG, "check_aux: value of array at " + i + " : " + i);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if(characteristics.getPhysicalCameraIds().size() > 0){
                            LOGICAL_ID = i;
                            Toast.makeText(activity, "Execution Completed cam_aux() Logical_ID "
                                            +i, Toast.LENGTH_LONG).show();
                        }
                    }
                    if(LOGICAL_ID != 0 && i >= LOGICAL_ID){
                        logicalCameras.add(i);
                    }
                    else  {
                        physicalCameras.add(i);
                    }
                }
            }
            catch (IllegalArgumentException | CameraAccessException ignored){ }
        }
        Toast.makeText(activity, "Execution Completed cam_aux() Physical ids "+physicalCameras, Toast.LENGTH_SHORT).show();
        Toast.makeText(activity, "Execution Completed cam_aux() Logical  ids "+logicalCameras , Toast.LENGTH_SHORT).show();
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

    private StreamConfigurationMap getStreamConfigMap(String id){
        return getCameraCharacteristics(id)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
    }

}
package com.uncanny.camx.Data;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.util.Size;

import androidx.annotation.WorkerThread;

import com.uncanny.camx.Utils.CameraHelper;
import com.uncanny.camx.Utils.CompareSizeByArea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

//@WorkerThread
public class LensData {
    private static final String TAG = "LensData";
    private static final String CAMERA_MAIN_BACK = "0";
    private static final String CAMERA_MAIN_FRONT = "1";
    private int LOGICAL_ID;
    private int FOCAL_LENGTH_FRONT, FOCAL_LENGTH_BACK;

    Context context;
    CameraCharacteristics characteristics;

    int[] capabilities;
    Size imageSize,imageSize169;
    CameraManager cameraManager;
    List<Integer> physicalCameras = new ArrayList<>();
    List<Integer> logicalCameras  = new ArrayList<>();
    List<Integer> auxiliaryCameras  = new ArrayList<>();
    ArrayList<Pair<Size, Range<Integer>>> fpsResolutionPair = new ArrayList<>();
    ArrayList<Pair<Size, Range<Integer>>> fpsResolutionPair_video = new ArrayList<>();
    String camera2level;

    private boolean isBayer;
    private Size bayerPhotoSize;

    /**
     * Constructor for this class.
     */
    public LensData(Context context){
        this.context = context;
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

        return auxiliaryCameras;
    }

    public ArrayList<ArrayList<String>> getCameraAliasBack(){
        Log.e(TAG, "getCameraAliasBack: physicalCamera : "+physicalCameras);
        ArrayList<String> camIdList = new ArrayList<>();
        ArrayList<String> aliasList = new ArrayList<>();
        ArrayList<ArrayList<String>> camAliasList = new ArrayList<>();

        List<Integer> tList = new ArrayList<>(physicalCameras);
        tList.remove((Object)0);
        tList.remove((Object)1);

        int uw = ultraWideCheck();
        int tele = telephotoCheck();

        if(getZoomFactor(uw+"") < 0.7) {
            tList.remove((Object)uw);
            camIdList.add(uw+"");
            aliasList.add(0, getAuxButtonName(uw + ""));
        }

        if(getZoomFactor(tele+"") > 1.3) {
            tList.remove((Object)tele);
            camIdList.add(tele+"");
            aliasList.add(getAuxButtonName(tele + ""));
        }
        for(int id : tList){
            camIdList.add(id+"");
            aliasList.add(getAuxButtonName(id + ""));
        }

        if(!camIdList.isEmpty()){
            camIdList.add(1,CAMERA_MAIN_BACK);
            aliasList.add(1,"1×");
        }
        else {
            camIdList.add(CAMERA_MAIN_BACK);
            aliasList.add("1×");
        }

        camAliasList.add(0,camIdList);
        camAliasList.add(1,aliasList);

        return camAliasList;
    }

    CameraHelper ch = new CameraHelper();

    public int ultraWideCheck(){
        List<Integer> tList = new ArrayList<>(physicalCameras);
        tList.remove((Object)0);
        tList.remove((Object)1);
        for (int i : tList){
            float zf = getFocalLength(i+"")/getMainBackFocalLength();
            if(zf < 0.7){
                return i;
            }
            Log.e(TAG, "ultraWideCheck: "+i+" : "+zf);
        }
        return 0;
    }

    public int telephotoCheck(){
        List<Integer> tList = new ArrayList<>(physicalCameras);
        tList.remove((Object)0);
        tList.remove((Object)1);
        for (int i : tList){
            float zf = getFocalLength(i+"")/getMainBackFocalLength();
            if(zf > 1.3){ //TODO: CHECK WITH K20 PRO 2x TELEPHOTO
                return i;
            }
            Log.e(TAG, "telephotoCheck: "+i+" : "+zf);
        }
        return 0;
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
            camera2level = "LIMITED";
            return true;
        }
        else if(c2api!=null && c2api == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL){
            Log.e(TAG, "hasCamera2api: HARDWARE_LEVEL_FULL");
            camera2level = "FULL";
            return true;
        }
        else if(c2api!=null && c2api == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY){
            Log.e(TAG, "hasCamera2api: HARDWARE_LEVEL_LEGACY");
            camera2level = "LEGACY";
            return true;
        }
        else if(c2api!=null && c2api == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3){
            Log.e(TAG, "hasCamera2api: HARDWARE_LEVEL_3");
            camera2level = "LEVEL 3";
            return true;
        }
        else if(c2api!=null && c2api == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL){
            Log.e(TAG, "hasCamera2api: HARDWARE_LEVEL_EXTERNAL");
            camera2level = "EXTERNAL";
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * @return camera2api level of support.
     */
    public String getCamera2level() {
        return camera2level;
    }

    /**
     * Checks if BURST_CAPTURE is supported
     */
    public boolean supportBurstCapture(String id){
        StreamConfigurationMap map = getStreamConfigMap(id);
        Size [] sizes = map.getHighResolutionOutputSizes(ImageFormat.JPEG);
        return sizes.length > 0;
    }

    /**
     * Returns true if given camera id has HSV capabilities
     */
    public boolean hasSloMoCapabilities(String id){
        capabilities = getCameraCharacteristics(id).get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        return findHSVCapability(capabilities);
    }

    /**
     * Use preference key to save modes.
     */
    public String[] getAvailableModes(String id){
        List<String> cameraModes = new ArrayList<>();
        capabilities = getCameraCharacteristics(id).get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        cameraModes.add("Camera");
        cameraModes.add("Video");

        if(capabilities!=null && findHSVCapability(capabilities)){
            cameraModes.add("Slo Moe");
            cameraModes.add("TimeWarp");
        }

        if(isBayerAvailable(id)){
            cameraModes.add("HighRes");
        }

        if(id.equals("0") || id.equals("1"))
            cameraModes.add("Portrait");

        cameraModes.add("Night");
        if(hasCamera2api()){
            cameraModes.add("Pro");
        }
        //ADD CHECK FOR MULTI MODE (if there's one)
        return cameraModes.toArray(new String[0]);
    }

    /**
     * Returns Pair of Size and FPS ranges for Slow Motion recording.
     */
    public ArrayList<Pair<Size, Range<Integer>>> getFpsResolutionPair(String id){
        StreamConfigurationMap map = getStreamConfigMap(id);
        fpsResolutionPair.clear();
        for(Range<Integer> range : map.getHighSpeedVideoFpsRanges()){
            if(range.getLower().equals(range.getUpper())) {
                for (Size size : map.getHighSpeedVideoSizesFor(range)) {
                    fpsResolutionPair.add(new Pair<>(size, range));
                }
            }
        }
        return fpsResolutionPair;
    }

    /**
     * Returns Pair of Size and FPS ranges for Media recording.
     */
    public ArrayList<Pair<Size, Range<Integer>>> getFpsResolutionPair_video(String id){
        StreamConfigurationMap map = getStreamConfigMap(id);
        Log.e(TAG, "getFpsResolutionPair_video: id : " + id + Arrays.toString(map.getOutputSizes(MediaRecorder.class)));
//        map.getOutputSizes(MediaRecorder.class);

        // now get fps ranges

        return fpsResolutionPair_video;
    }

    /**
     * DEBUGGING purposes
     */
    public void getCameraLensCharacteristics(String id){
        StreamConfigurationMap map = getStreamConfigMap(id);
        CameraCharacteristics cc = getCameraCharacteristics(id);
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
//        Log.e(TAG, "getCameraLensCharacteristics: SLO MO : FPS RANGE : "+ Arrays.toString(map.getHighSpeedVideoFpsRanges()));
//        Log.e(TAG, "getCameraLensCharacteristics: SLO MO : FPS RANGE : "+ Arrays.toString(map.getHighSpeedVideoSizes()));
//        Log.e(TAG, "getCameraLensCharacteristics: CAMID : "+id+" resolutions : "+ Arrays.toString(map.getOutputSizes(MediaRecorder.class)));

    }

    /**
     * returns a {@link CamcorderProfile} for camera ids [0,1] only..not recommended.
     * @param id the id for the camera.
     */
    public CamcorderProfile getCamcorderSMProfile(int id, Size size){
        Log.e(TAG, "getCamcorderSMProfile: "+size);
        if(size.getWidth() == 480){
            Log.e(TAG, "getCamcorderSMProfile: 1 : "+CamcorderProfile.get(0,CamcorderProfile.QUALITY_HIGH_SPEED_480P));
            return CamcorderProfile.get(0,CamcorderProfile.QUALITY_HIGH_SPEED_480P);
        }
        if(size.getWidth() == 720){
            Log.e(TAG, "getCamcorderSMProfile: 1 : "+CamcorderProfile.get(0,CamcorderProfile.QUALITY_HIGH_SPEED_720P));
            return CamcorderProfile.get(0,CamcorderProfile.QUALITY_HIGH_SPEED_720P);
        }
        if(size.getWidth() == 1080){
            Log.e(TAG, "getCamcorderSMProfile: 1 : "+CamcorderProfile.get(0,CamcorderProfile.QUALITY_HIGH_SPEED_1080P));
            return CamcorderProfile.get(0,CamcorderProfile.QUALITY_HIGH_SPEED_1080P);
        }
        if(CamcorderProfile.hasProfile(0,CamcorderProfile.QUALITY_HIGH_SPEED_2160P)){
            Log.e(TAG, "getCamcorderSMProfile: 1 : "+CamcorderProfile.get(0,CamcorderProfile.QUALITY_HIGH_SPEED_2160P));
            return CamcorderProfile.get(0,CamcorderProfile.QUALITY_HIGH_SPEED_2160P);
        }
        if(CamcorderProfile.hasProfile(0,CamcorderProfile.QUALITY_HIGH_SPEED_HIGH)){
            Log.e(TAG, "getCamcorderSMProfile: 1 : "+CamcorderProfile.get(0,CamcorderProfile.QUALITY_HIGH_SPEED_HIGH));
            return CamcorderProfile.get(0,CamcorderProfile.QUALITY_HIGH_SPEED_HIGH);
        }
        if(CamcorderProfile.hasProfile(0,CamcorderProfile.QUALITY_HIGH_SPEED_LOW)){
            Log.e(TAG, "getCamcorderSMProfile: 1 : "+CamcorderProfile.get(0,CamcorderProfile.QUALITY_HIGH_SPEED_LOW));
            return CamcorderProfile.get(0,CamcorderProfile.QUALITY_HIGH_SPEED_LOW);
        }
        return CamcorderProfile.get(id,CamcorderProfile.QUALITY_HIGH);
    }

    /**
     * checks if 1080p recording is possible
     * @param id Camera id
     * @return boolean
     */
    public boolean is1080pCapable(String id){
        Size [] sizes = getStreamConfigMap(id).getOutputSizes(MediaRecorder.class);
        for(Size s : sizes){
            if(s.getWidth()==1920 && s.getHeight()==1080){
                return true;
            }
        }
        return false;
    }

    /**
     * checks if 4k recording is possible
     * @param id Camera id
     * @return boolean
     */
    public boolean is4kCapable(String id){
        Size [] sizes = getStreamConfigMap(id).getOutputSizes(MediaRecorder.class);
        for(Size s : sizes){
            if(s.getWidth()==3840 && s.getHeight()==2160){
                return true;
            }
        }
        return false;
    }

    /**
     * checks if 8k recording is possible
     * @param id Camera id
     * @return boolean
     */
    public boolean is8kCapable(String id){
        Size [] sizes = getStreamConfigMap(id).getOutputSizes(MediaRecorder.class);
        for(Size s : sizes){
            if(s.getWidth()==7680 && s.getHeight()==4320){
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if lens supports capturing higher resolution images.
     */
    public boolean isBayerAvailable(String id){
        performBayerCheck(id);
        return isBayer;
    }

    /**
     * Returns the highest resolution a camera lens can capture image at.
     * [NOTE : first check if Bayer is available by calling {@link LensData#isBayerAvailable(String)}]
     */
    public Size getBayerLensSize(){
        return bayerPhotoSize;
    }

    public Size getHighestResolution(String id){
        ArrayList<Size> sizeArrayList = new ArrayList<>();
        ArrayList<Integer> image_formats = new ArrayList<>();
        image_formats.add(ImageFormat.JPEG);
        image_formats.add(ImageFormat.RAW_SENSOR);
        for(Integer i:image_formats){
            if(!Objects.equals(getStreamConfigMap(id).getOutputSizes(i),null)){
                sizeArrayList.addAll(Arrays.asList(getStreamConfigMap(id).getOutputSizes(i)));
            }
            if(!Objects.equals(getStreamConfigMap(id).getHighResolutionOutputSizes(i),null)){
                sizeArrayList.addAll(Arrays.asList(getStreamConfigMap(id).getHighResolutionOutputSizes(i)));
            }
        }
        if(isBayerAvailable(id)) sizeArrayList.remove(bayerPhotoSize);

        imageSize = Collections.max(sizeArrayList, new CompareSizeByArea());
        return imageSize;
    }

    public Size getHighestResolution169(String id){
        ArrayList<Size> sizeArrayList = new ArrayList<>();
        ArrayList<Integer> image_formats = new ArrayList<>();
        image_formats.add(ImageFormat.JPEG);
        image_formats.add(ImageFormat.RAW_SENSOR);
        for(Integer i:image_formats){
            for(Size size : getStreamConfigMap(id).getOutputSizes(i)){
                float ar = (float) size.getWidth()/ size.getHeight();
                if(ar > 1.6f && ar < 1.8f){
                    sizeArrayList.add(size);
                }
            }
        }
        imageSize169 = Collections.max(sizeArrayList, new CompareSizeByArea());
        return imageSize169;
    }

    public float getMainBackFocalLength(){
        if(FOCAL_LENGTH_BACK == 0){
            try{
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(CAMERA_MAIN_BACK);
                return (36.0f / characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getWidth()
                        * characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0]);
            }
        catch (CameraAccessException e){
                e.printStackTrace();
            }
        }
        return 0;
    }

    public float getMainFrontFocalLength(){
        if(FOCAL_LENGTH_FRONT == 0){
            try{
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(CAMERA_MAIN_FRONT);
                return (36.0f / characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getWidth()
                        * characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0]);
            }
            catch (CameraAccessException e){
                e.printStackTrace();
            }
        }
        return 0;
    }

    public float getFocalLength(String cameraId){
        try{
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            return (36.0f / characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getWidth()
                    * characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0]);
        }
        catch (CameraAccessException e){
            e.printStackTrace();
        }
        return 0;
    }

    private float getZoomFactor(String id){
        return getFocalLength(id)/getMainBackFocalLength();
    }

    private String getAuxButtonName(String id) {
        return String.format(Locale.US, "%.1f", getZoomFactor(id)).replace(".0", "");
    }

    /**
     * Init
     */
    private void getAuxCameras(){
//        CameraHelper ch = new CameraHelper();
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        for(int i = 0; i<=31 ; i++){       // FIXME: 8/11/2021 fix extra aux lens problem @_@
            try {
//                characteristics = cameraManager.getCameraCharacteristics(String.valueOf(i));
                characteristics = getCameraCharacteristics(String.valueOf(i));
                if (characteristics!=null ) {
                    Log.e(TAG, "check_aux: value of array at " + i + " : " + i);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if(characteristics.getPhysicalCameraIds().size() > 0){
                            LOGICAL_ID = i;
                        }
                    }
                    if(LOGICAL_ID != 0 && i >= LOGICAL_ID){
                        logicalCameras.add(i);
                    }
                    else  {
                        physicalCameras.add(i);
//                        ch.getCameraFov(context,i+"");
//                        ch.computeViewAngles(context,i+"");
                    }
                }
            }
            catch (IllegalArgumentException ignored){
            }
        }

        Log.e(TAG, "getAuxCameras: "+logicalCameras);
        Log.e(TAG, "getAuxCameras: "+physicalCameras);

    }

    private void performBayerCheck(String id) {
        //TODO : improve logic
        StreamConfigurationMap map = getCameraCharacteristics(id)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] sizes;
        if (map.getHighResolutionOutputSizes(ImageFormat.JPEG)!=null &&
                map.getHighResolutionOutputSizes(ImageFormat.JPEG).length !=0 ) {
            sizes = (map.getHighResolutionOutputSizes(ImageFormat.JPEG).length > 0 ?
                    map.getHighResolutionOutputSizes(ImageFormat.JPEG) :
                    map.getHighResolutionOutputSizes(ImageFormat.RAW_SENSOR));
            if (sizes.length > 0) {
                for(Size size1 : sizes) {
                    if (size1.getWidth() * size1.getHeight() > 21000000){
                        isBayer = true;
                        ArrayList<Size> sizeArrayList = new ArrayList<>(Arrays.asList(sizes));
                        bayerPhotoSize = Collections.max(sizeArrayList, new CompareSizeByArea());
                    }
                    else{
                        isBayer = false;
//                        Log.e(TAG, "BayerCheck: NOT BAYER : ID : " + id);
                    }
                }
//                Log.e(TAG, "BayerCheck: BAYER SENSOR SIZE : "+id+" size : "+ bayerPhotoSize);
            }
            else {
                isBayer = false;
//                Log.e(TAG, "BayerCheck: NOT BAYER : ID : " + id);
            }
        }
        else{
            /*
             * FOR OEMS WHICH DO NOT MENTION BAYER SENSOR SIZE IN getHighResolutionOutputSizes(ImageFormat.JPEG)
             */
            sizes = map.getOutputSizes(ImageFormat.JPEG);
            for(Size size1 : sizes){
                if(size1.getWidth()*size1.getHeight() > 17000000){
                    isBayer = true;
                    bayerPhotoSize = size1;
//                    Log.e(TAG, "performBayerCheck: ID : "+id+" size : "+bayerPhotoSize);
                }
                else{
                    isBayer = false;
//                    Log.e(TAG, "BayerCheck: NOT BAYER(2) : ID : " + id);
                }

            }
        }
    }

    private static boolean findHSVCapability(int[] capabilities) {
        for(int caps : capabilities){
            if(caps == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO) return true;
        }
        return false;
    }

    private CameraCharacteristics getCameraCharacteristics(String camId) {
        if(cameraManager==null)
            cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            characteristics = cameraManager.getCameraCharacteristics(camId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return characteristics;
    }

    private StreamConfigurationMap getStreamConfigMap(String id){
        return getCameraCharacteristics(id).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
    }

}
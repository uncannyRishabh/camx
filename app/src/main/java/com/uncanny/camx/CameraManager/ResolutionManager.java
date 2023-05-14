package com.uncanny.camx.CameraManager;

import android.util.Size;

import androidx.annotation.Nullable;

import com.uncanny.camx.Data.LensData;
import com.uncanny.camx.Utils.CameraConstants;
import com.uncanny.camx.Utils.CompareSizeByArea;

import java.util.ArrayList;
import java.util.Collections;

public class ResolutionManager {
    private static final String TAG = "ResolutionManager";

    private final String DEFAULT_ASPECT_RATIO;
    private LensData lensData;

    public ResolutionManager(LensData lensData, String aspectRatio){
        DEFAULT_ASPECT_RATIO = aspectRatio;
        this.lensData = lensData;
    }

    public Size getPreviewResolution(@Nullable String aspectRatio){
        if(null != aspectRatio) {
            switch (aspectRatio) {
                case CameraConstants.DisplayConstants.ASPECT_RATIO_FULL:
                    return DEFAULT_ASPECT_RATIO.equals(CameraConstants.DisplayConstants.DISPLAY_RES_720) ?
                            CameraConstants.ResolutionConstants.RES_HD_FULL : CameraConstants.ResolutionConstants.RES_FHD_FULL;
                case CameraConstants.DisplayConstants.ASPECT_RATIO_16_9:
                    return DEFAULT_ASPECT_RATIO.equals(CameraConstants.DisplayConstants.DISPLAY_RES_720) ?
                            CameraConstants.ResolutionConstants.IPR_RES_169_HD : CameraConstants.ResolutionConstants.IPR_RES_169_FHD;
                default:
                    return DEFAULT_ASPECT_RATIO.equals(CameraConstants.DisplayConstants.DISPLAY_RES_720) ?
                            CameraConstants.ResolutionConstants.IPR_RES_43_HD : CameraConstants.ResolutionConstants.IPR_RES_43_FHD;
            }
        }

        if(DEFAULT_ASPECT_RATIO.equals(CameraConstants.DisplayConstants.DISPLAY_RES_720))
            return CameraConstants.ResolutionConstants.IPR_RES_43_HD;
        else
            return CameraConstants.ResolutionConstants.IPR_RES_43_FHD;
    }

    public Size getCaptureResolution(String id, String aspectRatio){
        switch (aspectRatio) {
            case CameraConstants.DisplayConstants.ASPECT_RATIO_16_9:
                return lensData.getCaptureResolution16by9(id);
            case CameraConstants.DisplayConstants.ASPECT_RATIO_FULL:
                return lensData.getCaptureResolutionFull(id);
            default:
                return lensData.getCaptureResolution4by3(id);
        }
    }

    public Size getVideoPreviewResolution(String id, String aspectRatio){
        if(lensData.is1080pCapable(id)){
            if(aspectRatio.equals(CameraConstants.DisplayConstants.ASPECT_RATIO_FULL))
                return CameraConstants.ResolutionConstants.RES_FHD_FULL;
            else
                return CameraConstants.ResolutionConstants.RES_1080p;
        }
        else{
            if(aspectRatio.equals(CameraConstants.DisplayConstants.ASPECT_RATIO_FULL))
                return CameraConstants.ResolutionConstants.RES_HD_FULL;
            else
                return CameraConstants.ResolutionConstants.RES_720p;
        }
    }

    public Size getVideoCaptureResolution(@Nullable String resolution){
        if(null == resolution)
            return CameraConstants.ResolutionConstants.RES_720p;

        switch (resolution) {
            case CameraConstants.ResolutionConstants.RES_STR_1080p:
                return CameraConstants.ResolutionConstants.RES_1080p;
            case CameraConstants.ResolutionConstants.RES_STR_1440p:
                return CameraConstants.ResolutionConstants.RES_1440p;
            case CameraConstants.ResolutionConstants.RES_STR_4k:
                return CameraConstants.ResolutionConstants.RES_4k;
            case CameraConstants.ResolutionConstants.RES_STR_8k:
                return CameraConstants.ResolutionConstants.RES_8k;
            default:
                return CameraConstants.ResolutionConstants.RES_720p;
        }
    }

    private Size getPreviewResolution(Size[] outputSizes, int resolution, String aspectRatio) {
        ArrayList<Size> sizeArrayList = new ArrayList<>();
        for(Size size : outputSizes){
            float ar = (float) size.getWidth()/ size.getHeight();
            if(aspectRatio.equals(CameraConstants.DisplayConstants.ASPECT_RATIO_4_3)) {
//                Log.e(TAG, "getPreviewResolution: AR43 resolution : "+resolution);
                if (size.getHeight() == resolution && ar > 1.2f) {
                    sizeArrayList.add(size);
                }
            }
            else {
//                Log.e(TAG, "getPreviewResolution: AR169 resolution : "+resolution);
                if (size.getHeight() == resolution && ar > 1.6f) {
                    sizeArrayList.add(size);
                }
            }
        }
        if(sizeArrayList.size() > 0){
            return Collections.min(sizeArrayList,new CompareSizeByArea());
        }
        else{
//            Log.e(TAG, "getPreviewResolution: FINAL ELSE");
            return outputSizes[0];
        }
    }


}

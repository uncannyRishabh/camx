package com.uncanny.camx.CameraManager;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;

public class ZoomControls {

    public static float getMaxZoom(CameraCharacteristics characteristics) throws CameraAccessException {
        return (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))*4.5f;
    }
}

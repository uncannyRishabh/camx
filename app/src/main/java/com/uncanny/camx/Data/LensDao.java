package com.uncanny.camx.Data;

import com.uncanny.camx.Utils.CameraConstants;

/**
 * Store Each Lens Data ;e.g. Supported resolution for video, slow motion, fps etc
 * Fetch Data from either Camera API or from storage.
 */
public class LensDao {
    private String id = CameraConstants.LensConstants.CAMERA_ID_BACK;
    private String aspectRatio = CameraConstants.DisplayConstants.ASPECT_RATIO_4_3;
    private boolean lensFacing;
    private boolean isAuxiliary;
//    private boolean isOIS

    private boolean is1080VideoSupported;
    private boolean is4KVideoSupported;
    private boolean is8KVideoSupported;

    private boolean isSlowMotionSupported;
    private boolean is1080SlowMotionSupported;
    private boolean is4kSlowMotionSupported;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public boolean isLensFacing() {
        return lensFacing;
    }

    public void setLensFacing(boolean lensFacing) {
        this.lensFacing = lensFacing;
    }

    public boolean isAuxiliary() {
        return isAuxiliary;
    }

    public void setAuxiliary(boolean auxiliary) {
        isAuxiliary = auxiliary;
    }

    public boolean isIs1080VideoSupported() {
        return is1080VideoSupported;
    }

    public void setIs1080VideoSupported(boolean is1080VideoSupported) {
        this.is1080VideoSupported = is1080VideoSupported;
    }

    public boolean isIs4KVideoSupported() {
        return is4KVideoSupported;
    }

    public void setIs4KVideoSupported(boolean is4KVideoSupported) {
        this.is4KVideoSupported = is4KVideoSupported;
    }

    public boolean isIs8KVideoSupported() {
        return is8KVideoSupported;
    }

    public void setIs8KVideoSupported(boolean is8KVideoSupported) {
        this.is8KVideoSupported = is8KVideoSupported;
    }

    public boolean isSlowMotionSupported() {
        return isSlowMotionSupported;
    }

    public void setSlowMotionSupported(boolean slowMotionSupported) {
        isSlowMotionSupported = slowMotionSupported;
    }

    public boolean isIs1080SlowMotionSupported() {
        return is1080SlowMotionSupported;
    }

    public void setIs1080SlowMotionSupported(boolean is1080SlowMotionSupported) {
        this.is1080SlowMotionSupported = is1080SlowMotionSupported;
    }

    public boolean isIs4kSlowMotionSupported() {
        return is4kSlowMotionSupported;
    }

    public void setIs4kSlowMotionSupported(boolean is4kSlowMotionSupported) {
        this.is4kSlowMotionSupported = is4kSlowMotionSupported;
    }

}

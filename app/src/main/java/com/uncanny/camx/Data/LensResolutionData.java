package com.uncanny.camx.Data;

import android.util.Size;

/**
 * Helper class for providing Lens Data
 */

public class LensResolutionData {
    private boolean isBayer;
    private boolean is1080at60supported,is4k30Supported,is4k60Supported,is8kSupported;
    private boolean isSloMoAt120supported,isSloMoAt240supported,isSloMoAt480supported,isSloMoAt960supported;

    private Size bayerPhotoSize;
    private Size highestPhotoSize;
//    private Size video720at30,video720at60,video1080at30,video1080at60,video4kat30,video4kat60;
    private Size videoSloMo120,videoSloMo240,videoSloMo480,videoSloMo960;

    public LensResolutionData(){ }

    public boolean isBayer() {
        return isBayer;
    }

    public void setBayer(boolean bayer) {
        isBayer = bayer;
    }

    public boolean isIs1080at60supported() {
        return is1080at60supported;
    }

    public void setIs1080at60supported(boolean is1080at60supported) {
        this.is1080at60supported = is1080at60supported;
    }

    public boolean isIs4k30Supported() {
        return is4k30Supported;
    }

    public void setIs4k30Supported(boolean is4k30Supported) {
        this.is4k30Supported = is4k30Supported;
    }

    public boolean isIs4k60Supported() {
        return is4k60Supported;
    }

    public void setIs4k60Supported(boolean is4k60Supported) {
        this.is4k60Supported = is4k60Supported;
    }

    public boolean isIs8kSupported() {
        return is8kSupported;
    }

    public void setIs8kSupported(boolean is8kSupported) {
        this.is8kSupported = is8kSupported;
    }

    public boolean isSloMoAt120supported() {
        return isSloMoAt120supported;
    }

    public void setSloMoAt120supported(boolean sloMoAt120supported) {
        isSloMoAt120supported = sloMoAt120supported;
    }

    public boolean isSloMoAt240supported() {
        return isSloMoAt240supported;
    }

    public void setSloMoAt240supported(boolean sloMoAt240supported) {
        isSloMoAt240supported = sloMoAt240supported;
    }

    public boolean isSloMoAt480supported() {
        return isSloMoAt480supported;
    }

    public void setSloMoAt480supported(boolean sloMoAt480supported) {
        isSloMoAt480supported = sloMoAt480supported;
    }

    public boolean isSloMoAt960supported() {
        return isSloMoAt960supported;
    }

    public void setSloMoAt960supported(boolean sloMoAt960supported) {
        isSloMoAt960supported = sloMoAt960supported;
    }

    public Size getBayerPhotoSize() {
        return bayerPhotoSize;
    }

    public void setBayerPhotoSize(Size bayerPhotoSize) {
        this.bayerPhotoSize = bayerPhotoSize;
    }

    public Size getHighestPhotoSize() {
        return highestPhotoSize;
    }

    public void setHighestPhotoSize(Size highestPhotoSize) {
        this.highestPhotoSize = highestPhotoSize;
    }

    public Size getVideoSloMo120() {
        return videoSloMo120;
    }

    public void setVideoSloMo120(Size videoSloMo120) {
        this.videoSloMo120 = videoSloMo120;
    }

    public Size getVideoSloMo240() {
        return videoSloMo240;
    }

    public void setVideoSloMo240(Size videoSloMo240) {
        this.videoSloMo240 = videoSloMo240;
    }

    public Size getVideoSloMo480() {
        return videoSloMo480;
    }

    public void setVideoSloMo480(Size videoSloMo480) {
        this.videoSloMo480 = videoSloMo480;
    }

    public Size getVideoSloMo960() {
        return videoSloMo960;
    }

    public void setVideoSloMo960(Size videoSloMo960) {
        this.videoSloMo960 = videoSloMo960;
    }

}

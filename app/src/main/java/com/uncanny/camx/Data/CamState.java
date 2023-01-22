package com.uncanny.camx.Data;

import androidx.annotation.NonNull;

public enum CamState {
    CAMERA,VIDEO,VIDEO_PROGRESSED,HSVIDEO_PROGRESSED
    ,TIMELAPSE_PROGRESSED,PORTRAIT,PRO,NIGHT,SLOMO,TIMELAPSE,HIRES;

    private static CamState state;

    public void setState(CamState camState){
        state = camState;
    }

    public CamState getState(){
        return state;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString();
    }
}

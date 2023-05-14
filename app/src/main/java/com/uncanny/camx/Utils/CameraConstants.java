package com.uncanny.camx.Utils;

import android.util.Size;

public interface CameraConstants {

    public interface LensConstants {

        String CAMERA_ID_BACK = "0";
        String CAMERA_ID_FRONT = "1";

//        String


    }

    public interface ResolutionConstants {

        Size IPR_RES_43_HD    = new Size(720, 960);
        Size IPR_RES_43_FHD   = new Size(1080,1440);
        Size IPR_RES_169_HD   = new Size(720,1280);
        Size IPR_RES_169_FHD  = new Size(1080,1920);

        //Calculated during Runtime
        Size RES_HD_FULL  = new Size(720,1280);
        Size RES_FHD_FULL = new Size(1080,1920);

        String RES_STR_720p  = "HD";
        String RES_STR_1080p = "FHD";
        String RES_STR_1440p = "QHD";
        String RES_STR_4k    = "4K";
        String RES_STR_8k    = "8K";

        Size RES_720p  = new Size(720,1280);
        Size RES_1080p = new Size(1080,1920);
        Size RES_1440p = new Size(1440, 2560);
        Size RES_4k    = new Size(2160, 3840);
        Size RES_8k    = new Size(4320, 7680);

    }

    public interface DisplayConstants {

        String DISPLAY_RES_720 = "720p";
        String DISPLAY_RES_1080 = "1080p";

        String ASPECT_RATIO_4_3 = "4:3";
        String ASPECT_RATIO_16_9 = "16:9";
        String ASPECT_RATIO_FULL = "FULL_SCREEN";

    }

}

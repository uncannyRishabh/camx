package com.uncanny.camx.Utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;

public class FileHandler {
    private Uri fileUri = null;
    private final Context context;

    public FileHandler(Context context){
        this.context = context;
    }

    public Uri getFileUri() {
        return fileUri;
    }

    public void setFileUri(Uri fileUri) {
        this.fileUri = fileUri;
    }

    public void performMediaScan(String filename, String type){
        String mimeType = null;
        if(type.equals("image")) mimeType = "image/jpeg";
        else if(type.equals("video")) mimeType = "video/mp4";
        MediaScannerConnection.scanFile(context
                ,new String[] { filename }
                ,new String[] { mimeType }
                ,(path, uri) -> {
                    Log.i("TAG", "Scanned " + path + ":");
                    Log.i("TAG", "-> uri=" + uri);
                    setFileUri(uri);
                });
    }
}

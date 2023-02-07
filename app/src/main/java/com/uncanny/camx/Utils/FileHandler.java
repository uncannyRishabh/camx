package com.uncanny.camx.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;

public class FileHandler {
    private Uri fileUri = null;
    private Context context;
    private Bitmap bitmap;

    public FileHandler(){

    }

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

    public Bitmap getExifThumbnail(String filePath) {
        ExifInterface exif;
//        Bitmap bitmap = BitmapFactory.decodeFile(filePath);

        //Causes Memory Leak
        bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(filePath), 100, 100);

        try {
            exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotate = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
            if (rotate != 0) {
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}

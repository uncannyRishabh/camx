package com.uncanny.camx.Utils.AsyncThreads;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@WorkerThread
public class LatestThumbnailGenerator implements Runnable{
    private static final String TAG = "LatestThumbnailGenerator";
    private static final List<String> IMAGE_FILES_EXTENSIONS = Arrays.asList("JPG", "JPEG", "DNG");
    private final Context context;
    private Bitmap bitmap;

    public Bitmap getBitmap(){
        if(bitmap==null)
            return Bitmap.createBitmap(96,96, Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    public static Uri latestUri;

    public Uri getUri(){
        return latestUri;
    }

    public LatestThumbnailGenerator(Context context){
        this.context = context;
    }

    @Override
    public void run() {
        String[] projection = new String[] {MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.MEDIA_TYPE};
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        final Cursor cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external")
                , projection
                , selection, null
                , MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

        if (cursor.moveToFirst()) {
            do{
                if(cursor.getString(0).contains("DCIM/Camera")){
                    String imageLocation = cursor.getString(0);
                    File latestMedia = new File(imageLocation);
                    latestUri = Uri.fromFile(latestMedia);

                    Log.e(TAG, "run: URI : "+latestUri);
                    if (latestMedia.exists()) {
                        if(fileIsImage(String.valueOf(latestMedia))){
//                            bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(String.valueOf(latestMedia)),100,100);
                            bitmap = applyExifRotation(latestMedia.getAbsolutePath());
                        }
                        else {
                            bitmap = ThumbnailUtils.createVideoThumbnail(String.valueOf(latestMedia), MediaStore.Images.Thumbnails.MINI_KIND);
                        }

                        Log.e(TAG, "Latest media: "+latestMedia);
                    }
                    break;
                }
            }while (cursor.moveToNext());
        }
        cursor.close();
    }

    public static Bitmap applyExifRotation(String filePath) {
        ExifInterface exif;
//        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        Bitmap bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(filePath), 100, 100);
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

    private boolean fileIsImage(String file) {
        int index = file.lastIndexOf(46);
        return IMAGE_FILES_EXTENSIONS.contains(-1 == index ? "" : file.substring(index + 1).toUpperCase());
    }

}
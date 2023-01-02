package com.uncanny.camx.Utils.AsyncThreads;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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
            String imageLocation = cursor.getString(0);
            File latestMedia = new File(imageLocation);
            if (latestMedia.exists()) {
                if(fileIsImage(String.valueOf(latestMedia))){
                    bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(String.valueOf(latestMedia)),100,100);
                }
                else {
                    bitmap = ThumbnailUtils.createVideoThumbnail(String.valueOf(latestMedia), MediaStore.Images.Thumbnails.MINI_KIND);
                }
                Log.e(TAG, "Latest media: "+latestMedia);
            }
        }

        cursor.close();
    }

    private boolean fileIsImage(String file) {
        int index = file.lastIndexOf(46);
        return IMAGE_FILES_EXTENSIONS.contains(-1 == index ? ""
                : file.substring(index + 1).toUpperCase());
    }

}
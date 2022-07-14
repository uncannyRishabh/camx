package com.uncanny.camx.Utils.AsyncThreads;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageDecoderThread implements Runnable{
    private static final String TAG = "ImageDecoderThread";
    private static final List<String> ACCEPTED_FILES_EXTENSIONS = Arrays.asList("JPG", "JPEG", "DNG","MP4");
    private static final List<String> IMAGE_FILES_EXTENSIONS = Arrays.asList("JPG", "JPEG", "DNG");
    private static final FilenameFilter FILENAME_FILTER = (dir, name) -> {
        int index = name.lastIndexOf(46);
        return ACCEPTED_FILES_EXTENSIONS.contains(-1 == index ? "" : name.substring(index + 1).toUpperCase())
                && new File(dir, name).length() > 0;
    };
    private Bitmap bitmap;

    public Bitmap getBitmap(){
        if(bitmap==null)
            return Bitmap.createBitmap(96,96, Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    public ImageDecoderThread(){
    }

    @Override
    public void run() {
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "DCIM/Camera" + "/";
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){
            File f = new File(dirPath);
            File[] dcimFiles = f.listFiles(FILENAME_FILTER);
            List<File> filesList = new ArrayList<>(Arrays.asList(dcimFiles != null ? dcimFiles : new File[0]));
            if (!filesList.isEmpty()) {
                filesList.sort((file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()));
                File lastImage = filesList.get(0);
                if(fileIsImage(String.valueOf(lastImage))){
                    Bitmap bmp = BitmapFactory.decodeFile(String.valueOf(lastImage));
                    bitmap = ThumbnailUtils.extractThumbnail(bmp,100,100);
                }
                else {
                    bitmap = ThumbnailUtils.createVideoThumbnail(String.valueOf(lastImage)
                            , MediaStore.Images.Thumbnails.MINI_KIND);
                }

            } else {
                Log.e(TAG, "display_latest_image_from_gallery(): Could not find any Image Files [1]");
            }
        }
        else {
            File f = new File("//storage//emulated//0//DCIM//Camera//");
            File[] dcimFiles = f.listFiles(FILENAME_FILTER);
            List<File> filesList = new ArrayList<>(Arrays.asList(dcimFiles != null ? dcimFiles : new File[0]));
            if (!filesList.isEmpty()) {
                filesList.sort((file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()));
                File lastImage = filesList.get(0);
                Log.e(TAG, "display_latest_image_from_gallery: latest : "+lastImage);
                if(fileIsImage(String.valueOf(lastImage))){
                    bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(String.valueOf(lastImage))
                            ,100,100);
                }
                else {
                    try {
                        bitmap = ThumbnailUtils.createVideoThumbnail(lastImage
                                , new Size(96, 96), new CancellationSignal());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } else {
                Log.e(TAG, "getAllImageFiles(): Could not find any Image Files");
            }
        }
    }

    private boolean fileIsImage(String file) {
        int index = file.lastIndexOf(46);
        return IMAGE_FILES_EXTENSIONS.contains(-1 == index ? ""
                : file.substring(index + 1).toUpperCase());
    }

}
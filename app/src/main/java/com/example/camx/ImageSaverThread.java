package com.example.camx;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageSaverThread implements Runnable {
    private final Image mImage;
    private final String cameraId;
    private final ContentResolver contentResolver;
    private Uri uri;
    Path jpgPath;

    ImageSaverThread(Image image, String cameraId, ContentResolver contentResolver, Uri uri) {
        mImage = image;
        this.cameraId = cameraId;
        this.contentResolver = contentResolver;
        this.uri = uri;
    }

    @Override
    public void run() {
        ContentValues values = new ContentValues();
        File EXTERNAL_DIR = new File("//storage//emulated//0");
        jpgPath = Paths.get(EXTERNAL_DIR.getAbsolutePath(),"//DCIM//Camera//"+"camX_"+ System.currentTimeMillis() +"_"+cameraId + '.' + ".jpg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Camera/");
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "CamX_"+System.currentTimeMillis()+"_"+cameraId+".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            values.put( MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis() );
            values.put(MediaStore.Images.Media.TITLE, "Image.jpg");
            values.put( MediaStore.MediaColumns.IS_PENDING, true );

            Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            uri = contentResolver.insert(external, values);
        }

        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                OutputStream outputStream = contentResolver.openOutputStream(uri);
                outputStream.write(bytes);
                outputStream.close();
            }
            else {
                Files.write(jpgPath, bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mImage.close();
        }
    }

}


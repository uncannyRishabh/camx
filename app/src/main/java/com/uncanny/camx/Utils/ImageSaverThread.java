package com.uncanny.camx.Utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageSaverThread implements Runnable {
    private final Image mImage;
    private final String cameraId;
    private final ContentResolver contentResolver;
    private Uri uri;
    Path jpgPath;

//    private RenderScript rs;
//    private Allocation allocIn,allocOut;
//    private Bitmap op;
//    private ScriptC_filter filter;

    public ImageSaverThread(Image image, String cameraId, ContentResolver contentResolver) {
        this.mImage = image;
        this.cameraId = cameraId;
        this.contentResolver = contentResolver;
    }

    public ImageSaverThread(Context context, Image image, String cameraId, ContentResolver contentResolver) {
        this.mImage = image;
        this.cameraId = cameraId;
        this.contentResolver = contentResolver;

//        rs = RenderScript.create(context);
//        filter = new ScriptC_filter(rs);
    }

    @Override
    public void run() {
        ContentValues values = new ContentValues();
        File EXTERNAL_DIR = new File("//storage//emulated//0");
        File DIR_DCIM = new File(Environment.getExternalStorageDirectory()+"//DCIM//Camera//");
        if(!DIR_DCIM.exists()){
            DIR_DCIM.mkdirs();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String currentDateandTime = sdf.format(new Date());

        File img = new File(Environment.getExternalStorageDirectory()+"//DCIM//Camera//"
                ,"CamX_"+currentDateandTime+cameraId+".jpg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            jpgPath = Paths.get(EXTERNAL_DIR.getAbsolutePath(),"//DCIM//Camera//");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Camera/");
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "CamX_"+currentDateandTime+cameraId+".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            values.put( MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis() );
            values.put(MediaStore.Images.Media.TITLE, "Image.jpg");

            Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            uri = contentResolver.insert(external, values);
        }

        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                OutputStream outputStream = contentResolver.openOutputStream(uri);
//                applyfilter(bytes).compress(Bitmap.CompressFormat.JPEG,100,outputStream);
//                BitmapFactory.decodeByteArray(bytes,0,bytes.length)
//                        .compress(Bitmap.CompressFormat.JPEG,100,outputStream);
                outputStream.write(bytes);
                outputStream.close();
            }
            else {
                try{
                    FileOutputStream fos = new FileOutputStream(img);
                    fos.write(bytes);
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            mImage.close();
        }
    }

//    private Bitmap applyfilter(byte[] bytes) {
//        //add allocation hacks
//        Bitmap in = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//
//        allocIn = Allocation.createFromBitmap(rs,in);
//        allocOut = Allocation.createFromBitmap(rs,in);
//        op = Bitmap.createBitmap(in.getWidth(),in.getHeight(),in.getConfig());
//        allocIn.copyFrom(in);
//        filter.forEach_root(allocIn,allocOut);
//        allocOut.copyTo(op);
//        return op;
//    }

}
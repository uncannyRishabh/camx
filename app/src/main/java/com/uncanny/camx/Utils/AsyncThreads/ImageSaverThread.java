package com.uncanny.camx.Utils.AsyncThreads;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.uncanny.camx.Modes.Portrait;
import com.uncanny.camx.Utils.BitmapUtils;
import com.uncanny.camx.Utils.Blur;
import com.uncanny.camx.Utils.FileHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ImageSaverThread implements Runnable {
    private final Image mImage;
    private final String cameraId;
    private final ContentResolver contentResolver;
    private Uri uri;
    public static Uri staticUri;
    private Context context;
    private FileHandler fileHandler;
    private boolean isPortrait;
    private boolean isFlipped;
    private int rotation;
    Bitmap blurBmp, temp, op;
    Executor executor = new SerialExecutor(Executors.newFixedThreadPool(2));

    public ImageSaverThread(Image image, String cameraId, ContentResolver contentResolver) {
        this.mImage = image;
        this.cameraId = cameraId;
        this.contentResolver = contentResolver;
        fileHandler = new FileHandler(context);
    }

    public ImageSaverThread(Context context, Image image, String cameraId
            , ContentResolver contentResolver,boolean isPortrait,boolean isFlipped,int rotation) {
        this.context = context;
        this.mImage = image;
        this.cameraId = cameraId;
        this.contentResolver = contentResolver;
        this.isPortrait = isPortrait;
        this.rotation = rotation;
        this.isFlipped = isFlipped;
        fileHandler = new FileHandler(context);
    }

    @Override
    public void run() {
        ContentValues values = new ContentValues();
        File DIR_DCIM = new File(Environment.getExternalStorageDirectory()+"//DCIM//Camera//");
        if(!DIR_DCIM.exists()){
            DIR_DCIM.mkdirs();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String currentDateandTime = sdf.format(new Date());

        File file = new File(Environment.getExternalStorageDirectory()+"//DCIM//Camera//"
                ,"CamX_"+currentDateandTime+cameraId+".jpg");


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Camera/");
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "CamX_"+currentDateandTime+cameraId+".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            values.put( MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis() );
            values.put(MediaStore.Images.Media.TITLE, "Image.jpg");

            Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            uri = contentResolver.insert(external, values);
            staticUri = uri;
        }

        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        if(isPortrait){
            Completable c1 = Completable.fromRunnable(() -> blurBmp = new Blur(context)
                    .blur(BitmapFactory.decodeByteArray(bytes, 0, bytes.length),8));
            c1.andThen(Completable.fromRunnable(() -> temp = BitmapFactory.decodeByteArray(bytes , 0, bytes.length)))
                    .observeOn(Schedulers.computation())
                    .subscribe(() -> new Portrait(temp,blurBmp,rotation,contentResolver));
        }

        if(isFlipped){
            executor.execute(() -> temp = BitmapFactory.decodeByteArray(bytes , 0, bytes .length));
            executor.execute(() -> op = BitmapUtils.flip_bitmap(temp,true,false));
            executor.execute(()->saveBitmap(op,90));
        }
        else{
            saveByteBuffer(bytes,file);
        }

    }

    private void saveBitmap(Bitmap bitmap,int bitmapRotation) {
        if(bitmapRotation != 0){
            bitmap = BitmapUtils.RotateBitmap(bitmap,bitmapRotation);
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                OutputStream outputStream = contentResolver.openOutputStream(uri);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();
//                bitmap.recycle();
                outputStream.write(byteArray);
                outputStream.close();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveByteBuffer(byte[] bytes, File file) {
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                OutputStream outputStream = contentResolver.openOutputStream(uri);
                outputStream.write(bytes);
                outputStream.close();
            }
            else {
                try{
                    FileOutputStream fos = new FileOutputStream(file);
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

}
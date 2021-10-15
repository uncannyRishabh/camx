package com.uncanny.camx.Modes;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.SegmentationMask;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Portrait {
    private static final String TAG = "PORTRAIT";
    private ByteBuffer buffer;
    private int width,height,rotation,format;
    private int maskWidth,maskHeight;
    private Bitmap bitmap,cacheBitmap;
    private ContentResolver contentResolver;
    private byte[] bytes;

    public Portrait(Bitmap data, int width, int height, int rotation, int format, ContentResolver contentResolver){
//        this.buffer = data;
//        this.bytes = data;
        this.bitmap = data;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.format = format;
        this.contentResolver = contentResolver;

//        bitmap = RotateBitmap(bitmap,180);
        performSegmentation();
    }

    private void performSegmentation() {
        InputImage image = InputImage
//            .fromByteArray(bytes,width,height,rotation,InputImage.IMAGE_FORMAT_BITMAP);
//                .fromByteBuffer(buffer, width, height, rotation, InputImage.IMAGE_FORMAT_NV21);
        .fromBitmap(bitmap,rotation);
        cacheBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);

        SelfieSegmenterOptions options =
                new SelfieSegmenterOptions.Builder()
                        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                        .build();

        Segmenter segmenter = Segmentation.getClient(options);
        segmenter.process(image).addOnSuccessListener(new OnSuccessListener<SegmentationMask>() {
            @Override
            public void onSuccess(@NonNull SegmentationMask mask) {
                // Task completed successfully
                maskHeight = mask.getHeight();
                maskWidth = mask.getWidth();
                bitmap = Bitmap
//                        .createBitmap(maskColorsFromByteBuffer(mask.getBuffer()), maskWidth, maskHeight, Bitmap.Config.ARGB_8888);
                        .createBitmap(coloredBG(mask.getBuffer()));
//                saveBitmap(bitmap);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Task failed with an exception
                Log.e(TAG, "onFailure: "+e);
            }
        });
    }

    private Bitmap coloredBG(ByteBuffer byteBuffer){
        int alpha;
        float confidence;
        for(int row=maskHeight-1; row>=0; row--){
            for(int col=0; col<maskWidth; col++){
                confidence = 1 - byteBuffer.getFloat();
                if(confidence > 0.9){
                    cacheBitmap.setPixel(row,col, Color.argb(255, 255, 0, 255));
                }
                else if (confidence > 0.3) {
                    alpha = (int) (182.9 * confidence - 36.6 + 0.5);
                    cacheBitmap.setPixel(row,col,Color.argb(alpha, 255, 0, 255));
                }
            }
        }
        saveBitmap(cacheBitmap);
        return cacheBitmap;
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @ColorInt
    private int[] maskColorsFromByteBuffer(ByteBuffer byteBuffer) {
        @ColorInt int[] colors = new int[maskWidth * maskHeight];
        for (int i = 0; i < maskWidth * maskHeight; i++) {
            float backgroundLikelihood = 1 - byteBuffer.getFloat();
            if (backgroundLikelihood > 0.9) {
                colors[i] = Color.argb(128, 255, 0, 255);
            } else if (backgroundLikelihood > 0.2) {
                // Linear interpolation to make sure when backgroundLikelihood is 0.2, the alpha is 0 and
                // when backgroundLikelihood is 0.9, the alpha is 128.
                // +0.5 to round the float value to the nearest int.
                int alpha = (int) (182.9 * backgroundLikelihood - 36.6 + 0.5);
                colors[i] = Color.argb(alpha, 255, 0, 255);
            }
        }
        return colors;
    }

    private void saveBitmap(Bitmap bitmap) {
        Uri uri = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String currentDateandTime = sdf.format(new Date());
        ContentValues values = new ContentValues();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Camera/");
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "CamX_"+currentDateandTime+".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            values.put( MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis() );
            values.put(MediaStore.Images.Media.TITLE, "Image.jpg");

            Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            uri = contentResolver.insert(external, values);
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

}

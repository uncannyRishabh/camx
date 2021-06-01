package com.uncanny.camx;

import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThumbPreview {
    private static final String TAG = "ThumbPreview";
    private static final List<String> ACCEPTED_FILES_EXTENSIONS = Arrays.asList("JPG", "JPEG", "DNG");
    private static final FilenameFilter FILENAME_FILTER = (dir, name) -> {
        int index = name.lastIndexOf(46);
        return ACCEPTED_FILES_EXTENSIONS.contains(-1 == index ? "" : name.substring(index + 1).toUpperCase()) && new File(dir, name).length() > 0;
    };
    public static File sEXTERNAL_DIR = Environment.getExternalStorageDirectory();
    public static File DCIM =new File(sEXTERNAL_DIR+"//DCIM//Camera//");

    @NonNull
    public static List<File> getAllImageFiles() {
        File[] dcimFiles = ThumbPreview.DCIM.listFiles(FILENAME_FILTER);
        List<File> filesList = new ArrayList<>(Arrays.asList(dcimFiles != null ? dcimFiles : new File[0]));
        if (!filesList.isEmpty()) {
            filesList.sort((file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()));
        } else {
            Log.e(TAG, "getAllImageFiles(): Could not find any Image Files");
        }
        Log.e(TAG, "getAllImageFiles: "+filesList);
        return filesList;
    }
}

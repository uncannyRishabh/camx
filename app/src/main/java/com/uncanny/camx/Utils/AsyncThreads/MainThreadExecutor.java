package com.uncanny.camx.Utils.AsyncThreads;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

public class MainThreadExecutor implements Executor {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(Runnable command) {
       mainHandler.post(command);
    }
}

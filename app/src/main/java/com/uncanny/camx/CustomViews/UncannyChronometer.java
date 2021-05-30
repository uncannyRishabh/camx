package com.uncanny.camx.CustomViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * @author uncannyRishabh (30/05/2021)
 */

public class UncannyChronometer extends View {
    private RectF rectF;
    private Paint paint,tPaint;
    private String drawTime="00:00:00";
    private long inputTimeInMillis;
    private long drawTimeInMillis;
    private boolean isRunning = false;
    private int cx,cy;
    private int seconds,minutes,hours;
    private String mSeconds,mMinutes,mHours;

    private Runnable startTick = new Runnable() {
        @Override
        public void run() {
            if(isRunning){
                updateDrawText();
                postDelayed(startTick,1000);
            }
        }
    };

    public UncannyChronometer(Context context) {
        super(context);
        init();
    }

    public UncannyChronometer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UncannyChronometer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        rectF = new RectF();
        paint = new Paint();
        tPaint = new Paint();

        paint.setColor(Color.RED);
        paint.setAntiAlias(true);

        tPaint.setColor(Color.WHITE);
        tPaint.setTextAlign(Paint.Align.CENTER);
        tPaint.setAntiAlias(true);
        tPaint.setTextSize(34f);
    }

    private void updateDrawText(){
        long currentTime = SystemClock.elapsedRealtime();
        long timeDiff =  currentTime -  inputTimeInMillis;
        seconds = (int) TimeUnit.MILLISECONDS.toSeconds(timeDiff);
        minutes = (int) TimeUnit.MILLISECONDS.toMinutes(timeDiff);
        hours   = (int) TimeUnit.MILLISECONDS.toHours(timeDiff);

        mSeconds = (seconds<10 ? "0"+seconds : seconds+"");
        mMinutes = (minutes<10 ? "0"+minutes : minutes+"");
        mHours = (hours<10 ? "0"+hours : hours+"");

        drawTime = mHours+":"+mMinutes+":"+mSeconds;
//        Log.e("TAG", "setBase: "+currentTime);
        this.invalidate();
    }

    public void setBase(long millis){
        inputTimeInMillis = millis;
    }

    public void start(){
        isRunning = true;
        postDelayed(startTick,1000);
    }

    public void stop(){
        isRunning = false;
        removeCallbacks(startTick);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        cx=getHeight()/2;
        cy=getWidth()/2;

        rectF.set(0,0,getWidth(),getHeight());
        canvas.drawRoundRect(rectF,48f,48f,paint);
        canvas.drawText(drawTime,cy,cx+10,tPaint);

    }

}
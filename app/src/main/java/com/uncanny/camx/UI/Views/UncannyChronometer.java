package com.uncanny.camx.UI.Views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * @author uncannyRishabh (30/05/2021)
 */

@SuppressWarnings({"FieldCanBeLocal","FieldMayBeFinal"})
public class UncannyChronometer extends View {
    private RectF rectF;
    private Paint paint,tPaint;
    private String drawTime="00:00";
    private long inputTimeInMillis;
    private long pauseDuration;
    private boolean isRunning = false;
    private int cx,cy;
    private int seconds,minutes,hours;
    private String mSeconds,mMinutes,mHours;
    private Typeface Poppins;

    private Runnable startTick = new Runnable() {
        @Override
        public void run() {
            if(isRunning){
                updateDrawText();
                postDelayed(startTick,1000);
            }
        }
    };

    private Runnable countPauseDuration = new Runnable() {
        @Override
        public void run() {
            if(isRunning){
                pauseDuration += 1000;
                postDelayed(countPauseDuration,1000);
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

        Poppins = Typeface.create("Poppins", Typeface.NORMAL);

        paint.setColor(0xFFF75C5C); //MUTE RED
        paint.setAntiAlias(true);

        tPaint.setColor(Color.BLACK);
        tPaint.setFakeBoldText(true);
        tPaint.setTextAlign(Paint.Align.CENTER);
        tPaint.setAntiAlias(true);
        tPaint.setTextSize(40f);
        tPaint.setTypeface(Poppins);
    }

    private void updateDrawText(){
        long currentTime = SystemClock.elapsedRealtime();
        long timeDiff =  currentTime -  inputTimeInMillis - pauseDuration;
        seconds = (int) TimeUnit.MILLISECONDS.toSeconds(timeDiff);
        minutes = (int) TimeUnit.MILLISECONDS.toMinutes(timeDiff);
        hours   = (int) TimeUnit.MILLISECONDS.toHours(timeDiff);

        mSeconds = (seconds>60 ? (seconds%60<10 ? "0"+seconds%60:seconds%60+"" ) : (seconds<10 ? "0"+seconds: seconds+""));
        mMinutes = (minutes>60 ? (minutes%60<10 ? "0"+minutes%60:minutes%60+"" ) : (minutes<10 ? "0"+minutes: minutes+""));
        mHours   = (hours>60   ? (hours%60<10 ? "0"+hours%60:hours%60+"" )       : (hours<10   ? "0"+hours: hours+""));

        drawTime = (hours>0 ? mHours+":" : "" )+mMinutes+":"+mSeconds;
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
        drawTime = "00:00";
    }

    public void pause(){
        postDelayed(countPauseDuration,1000);
    }

    public void resume(){
        removeCallbacks(countPauseDuration);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        cx=getHeight()/2;
        cy=getWidth()/2;

        rectF.set(0,0,getWidth(),getHeight());
        canvas.drawRoundRect(rectF,48f,48f,paint);
        canvas.drawText(drawTime
                ,cy
                ,(getHeight()+ tPaint.getTextSize())/2f-5
                ,tPaint);

    }
}
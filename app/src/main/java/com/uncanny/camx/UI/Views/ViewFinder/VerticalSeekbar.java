package com.uncanny.camx.UI.Views.ViewFinder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class VerticalSeekbar extends Slider {

    public VerticalSeekbar(Context context) {
        super(context);
    }

    public VerticalSeekbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalSeekbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public VerticalSeekbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

}

abstract class Slider extends View{
    private Paint thumbPaint;
    private Paint trackPaint;
    private Paint activeTrackPaint;

    private float height;
    private float position;
    private float halfWidth;
    private float thumbRadius;
    private float trackThickness;
    private float THUMB_PADDING;

    private final float density = getResources().getDisplayMetrics().density;

    public Slider(Context context) {
        super(context);
        init();
    }

    public Slider(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Slider(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public Slider(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init(){
        trackPaint = new Paint();
        thumbPaint = new Paint();
        activeTrackPaint = new Paint();

        height = getHeight();
        position = 0f;

        THUMB_PADDING = 8 * density;
        trackThickness = 4.5f * density;
        thumbRadius = 8.5f * density;

        trackPaint.setAntiAlias(true);
        trackPaint.setColor(Color.WHITE);
        trackPaint.setStrokeWidth(trackThickness);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        activeTrackPaint.setAntiAlias(true);
        activeTrackPaint.setColor(Color.DKGRAY);
        activeTrackPaint.setStrokeWidth(trackThickness);
        activeTrackPaint.setStyle(Paint.Style.STROKE);
        activeTrackPaint.setStrokeCap(Paint.Cap.ROUND);

        thumbPaint.setColor(Color.GRAY);
        thumbPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        height = getHeight();
        setMinimumWidth((int) (getWidth() + 2*THUMB_PADDING));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action){
            case MotionEvent.ACTION_DOWN:{
                Log.e("TAG", "onTouchEvent: POINTER DOWN"+height);
                return true;
            }
            case MotionEvent.ACTION_MOVE:{
                Log.e("TAG", "onTouchEvent: X : "+ event.getX() +" Y : "+ event.getY());
                position = Math.max(0, Math.min(height-2*thumbRadius-THUMB_PADDING, event.getY()));
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:{
                position = Math.max(0, Math.min(height-2*thumbRadius-THUMB_PADDING, event.getY()));
                invalidate();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        halfWidth = getWidth()/2f;

        canvas.drawLine(halfWidth,
                thumbRadius+THUMB_PADDING,
                halfWidth,
                getHeight()-thumbRadius-THUMB_PADDING,
                trackPaint);

        canvas.drawLine(halfWidth,
                thumbRadius+THUMB_PADDING,
                halfWidth,
                thumbRadius+position+THUMB_PADDING,
                activeTrackPaint);

        canvas.drawCircle(halfWidth,
                thumbRadius+position+THUMB_PADDING,
                thumbRadius,
                thumbPaint);

    }

}

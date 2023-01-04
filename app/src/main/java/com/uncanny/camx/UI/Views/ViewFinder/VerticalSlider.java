package com.uncanny.camx.UI.Views.ViewFinder;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.uncanny.camx.R;

public class VerticalSlider extends Slider {

    public interface OnSliderChangeListener {

        void onProgressChanged(VerticalSlider seekBar, float progress);

        void onStartTrackingTouch(VerticalSlider seekBar, float progress);

        void onStopTrackingTouch(VerticalSlider seekBar);
    }

    private OnSliderChangeListener mOnSliderChangeListener;

    public VerticalSlider(Context context) {
        super(context);
    }

    public VerticalSlider(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalSlider(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnSliderChangeListener(OnSliderChangeListener l) {
        mOnSliderChangeListener = l;
    }

    @Override
    float onTrackingGesture() {
        if(mOnSliderChangeListener !=null){
            mOnSliderChangeListener.onProgressChanged(this, super.onTrackingGesture());
        }
        return super.onTrackingGesture();
    }

    @Override
    void onStartTrackingTouch() {
        super.onStartTrackingTouch();
        if(mOnSliderChangeListener !=null){
            mOnSliderChangeListener.onStartTrackingTouch(this, super.onTrackingGesture());
        }
    }

    @Override
    void onStopTrackingTouch() {
        super.onStopTrackingTouch();
        if (mOnSliderChangeListener != null) {
            mOnSliderChangeListener.onStopTrackingTouch(this);
        }
    }
}

abstract class Slider extends View{
    private Paint thumbPaint;
    private Paint activeTrackPaint;
    private Paint inactiveTrackPaint;

    private int minValue;
    private int maxValue;

    private float height;
    private float position;
    private float halfWidth;
    private float thumbRadius;
    private float thumbRadiusCache;
    private float trackThickness;
    private float THUMB_PADDING;

    private float setPosition;
    private boolean requestSetPosition = false;
    private boolean disableTapToMove = false;
    private boolean isDragging;

    private final float density = getResources().getDisplayMetrics().density;
    private final Runnable expandThumb = () -> animateThumb(thumbRadius,thumbRadius*1.3f);
    private final Runnable shrinkThumb = () -> animateThumb(thumbRadius*1.3f,thumbRadius);

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

    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }

    public void  setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setPosition(int position){
        requestSetPosition = true;
        setPosition = position;
        invalidate();
    }

    public void disableTapToMove(boolean b){
        disableTapToMove = b;
    }

    private float getPosition() {
        return normalize(position, getMinValue(), getMaxValue(), THUMB_PADDING+thumbRadius, height-thumbRadius-THUMB_PADDING);
    }

    private float normalize(float x, float newMin, float newMax, float oldMin, float oldMax) {
        return (((x - oldMin)/(oldMax - oldMin))*(newMax - newMin)) + newMin;
    }

    private void init(){
        activeTrackPaint = new Paint();
        thumbPaint = new Paint();
        inactiveTrackPaint = new Paint();

        minValue = 0;
        maxValue = 100;

        THUMB_PADDING = 8 * density;
        trackThickness = 4.5f * density;
        thumbRadius = 10f * density;
        thumbRadiusCache = thumbRadius;

        position = THUMB_PADDING+thumbRadius;

        activeTrackPaint.setAntiAlias(true);
        activeTrackPaint.setColor(Color.WHITE);       //ACCENT
        activeTrackPaint.setStrokeWidth(trackThickness-2);
        activeTrackPaint.setStyle(Paint.Style.STROKE);
        activeTrackPaint.setStrokeCap(Paint.Cap.ROUND);

        inactiveTrackPaint.setAntiAlias(true);
        inactiveTrackPaint.setColor(ContextCompat.getColor(getContext(), R.color.md3_neutral2_800));
        inactiveTrackPaint.setStrokeWidth(trackThickness);
        inactiveTrackPaint.setStyle(Paint.Style.STROKE);
        inactiveTrackPaint.setStrokeCap(Paint.Cap.ROUND);

        thumbPaint.setAntiAlias(true);
        thumbPaint.setColor(ContextCompat.getColor(getContext(), R.color.md3_accent2_100));    //ACCENT
        thumbPaint.setStyle(Paint.Style.FILL);
    }

    private void animateThumb(float vInitial, float vFinal){
        PropertyValuesHolder oPropertyValuesHolder = PropertyValuesHolder.ofFloat("radius",vInitial,vFinal);
        ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(oPropertyValuesHolder);
        valueAnimator.setInterpolator(new AnticipateOvershootInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            thumbRadiusCache = (float) animation.getAnimatedValue();
            invalidate();
        });
        valueAnimator.setDuration(500);
        valueAnimator.start();
    }

    private float clamp(float v, float min, float max) {
        return Math.min(Math.max(v, min), max);
    }

    private void setPosition(float position){
        this.position = clamp(position,0,getHeight()-thumbRadius-THUMB_PADDING);
        this.position = clamp(this.position,thumbRadius+THUMB_PADDING,getHeight()-thumbRadius-THUMB_PADDING);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.removeCallbacks(expandThumb);
        int action = event.getActionMasked();
        switch (action){
            case MotionEvent.ACTION_DOWN:{
                this.post(expandThumb);
                requestSetPosition = false;
                onStartTrackingTouch();
                return true;
            }
            case MotionEvent.ACTION_MOVE:{
                setPosition(event.getY());
                onTrackingGesture();
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:{
                if(!disableTapToMove) setPosition(event.getY());
                if (isDragging) onStopTrackingTouch();
                this.post(shrinkThumb);
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
        height = getHeight();

        if(requestSetPosition){
            setPosition( normalize(setPosition
                    , THUMB_PADDING+thumbRadius, height-thumbRadius-THUMB_PADDING
                    , getMinValue(), getMaxValue()) );
        }

        //INACTIVE TRACK
        canvas.drawLine(halfWidth,
                thumbRadius+THUMB_PADDING,
                halfWidth,
                height-thumbRadius-THUMB_PADDING,
                inactiveTrackPaint);

        //ACTIVE TRACK
        canvas.drawLine(halfWidth,
                position,
                halfWidth,
                height-thumbRadius-THUMB_PADDING,
                activeTrackPaint);

        //THUMB
        canvas.drawCircle(halfWidth,
                position,
                thumbRadiusCache,
                thumbPaint);
    }

    float onTrackingGesture(){
        return getPosition();
    }

    void onStartTrackingTouch() {
        isDragging = true;
    }

    void onStopTrackingTouch() {
        isDragging = false;
    }
}
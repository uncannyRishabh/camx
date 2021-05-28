package com.uncanny.camx.CustomViews;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.CycleInterpolator;

import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import com.uncanny.camx.CamxFragment.CamState;


/**
 * @author uncannyRishabh (27/05/2021)
 */

public class CaptureButton extends View {
    private Paint paint, nPaint;
    private int cx,cy;
    private float icRadius;
    private CamState mState;

    public CaptureButton(Context context) {
        super(context);
        init();
    }

    public CaptureButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CaptureButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        nPaint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(10f);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        nPaint.setColor(Color.WHITE);
        nPaint.setAntiAlias(true);
        nPaint.setStrokeWidth(10f);
        nPaint.setStyle(Paint.Style.STROKE);

        icRadius = 52f;
        mState = CamState.CAMERA;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        cx = getWidth()/2;
        cy = getHeight()/2;
    }

    public void setState(CamState state){
        mState = state;
        switch (state){
            case CAMERA:
                icRadius = 52f*2;
                break;
            case VIDEO:
                break;
            case VIDEO_PROGRESSED:
                break;
            case SLOMO:
                break;
            default:
                break;
        }
    }

    public void colorInnerCircle(CamState state){
        setState(state);
        sizeInnerCircle(.5f);
        if(mState == CamState.VIDEO){
            paint.setColor(Color.RED);
        }
        else if(mState == CamState.CAMERA){
            icRadius = 52f;
            paint.setColor(Color.WHITE);
        }
        invalidate();
    }

    public void animateShutterButton(float multiplier) {
        PropertyValuesHolder rPropertyValuesHolder = PropertyValuesHolder.ofFloat("radius"
                                                        ,icRadius,icRadius*multiplier);
        ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(rPropertyValuesHolder);
        valueAnimator.setInterpolator(new CycleInterpolator(1));
        valueAnimator.addUpdateListener(animation -> {
            invalidate();
            icRadius = (float) animation.getAnimatedValue();
        });
        valueAnimator.setDuration(520);
        valueAnimator.start();
        invalidate();
    }

    public void sizeInnerCircle(float multiplier) {
        PropertyValuesHolder rPropertyValuesHolder = PropertyValuesHolder.ofFloat("radius"
                ,icRadius,icRadius*multiplier);
        ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(rPropertyValuesHolder);
        valueAnimator.setInterpolator(new LinearOutSlowInInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            invalidate();
            icRadius = (float) animation.getAnimatedValue();
        });
        valueAnimator.setDuration(520);
        valueAnimator.start();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        cx = getWidth()/2;
        cy = getHeight()/2;

        canvas.drawCircle(cx,cy,icRadius,paint);
        canvas.drawCircle(cx,cy,Math.min(cx,cy)-10, nPaint);
    }

}
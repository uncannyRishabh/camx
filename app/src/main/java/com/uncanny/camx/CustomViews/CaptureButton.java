package com.uncanny.camx.CustomViews;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import com.uncanny.camx.CameraActivity.CamState;


/**
 * @author uncannyRishabh (27/05/2021)
 */

public class CaptureButton extends View {
    private RectF rectF;
    private Paint paint, nPaint, mPaint;
    private int cx,cy,screenWidth;
    private final int RECT_PADDING = 72;
    private float icRadius;
    private CamState mState;
    private boolean drawSquare=false;
    private boolean drawSlomo=false;

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
        rectF = new RectF();
        paint = new Paint();
        nPaint = new Paint();
        mPaint = new Paint();

        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(10f);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        nPaint.setColor(Color.WHITE);
        nPaint.setAntiAlias(true);
        nPaint.setStrokeWidth(10f);
        nPaint.setStyle(Paint.Style.STROKE);

        mPaint.setColor(Color.RED);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(10f);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        screenWidth = getScreenWidth();
        icRadius = screenWidth/12f;
        mState = CamState.CAMERA;
    }

    private void setState(CamState state){
        mState = state;
        switch (state){
            case CAMERA:
                icRadius = screenWidth/12f;
                nPaint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.WHITE);
                break;
            case VIDEO:
                icRadius = screenWidth/16f;
                nPaint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.RED);
                break;
            case VIDEO_PROGRESSED:
                nPaint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.RED);
                break;
            case SLOMO:
                icRadius = screenWidth/16f;
                nPaint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.parseColor("#FDDDDC"));
                break;
            default:
                break;
        }
    }

    /**
     * Call this method to animate {@link CaptureButton#icRadius}.
     * @param state current camera state {@link CamState}
     */
    public void colorInnerCircle(CamState state){
        setState(state);
        sizeInnerCircle(.5f);
        invalidate();
    }

    private void sizeInnerCircle(float multiplier) {
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

    private static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    private static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        cx = getWidth()/2;
        cy = getHeight()/2;
        rectF.set(RECT_PADDING, RECT_PADDING,getWidth()- RECT_PADDING,getHeight()- RECT_PADDING);

        canvas.drawCircle(cx,cy,cy-10, nPaint); //OUTER CIRCLE
        switch(mState){
            case CAMERA:
                canvas.drawCircle(cx, cy, icRadius, paint); //INNER CIRCLE
                break;
            case VIDEO:
                canvas.drawCircle(cx,cy,icRadius,mPaint);
                break;
            case VIDEO_PROGRESSED:
                canvas.drawRoundRect(rectF,12,12,mPaint);
                break;
            case SLOMO:
                canvas.drawCircle(cx-icRadius/2,cy,icRadius,paint);
                canvas.drawCircle(cx+icRadius/2,cy,icRadius,mPaint);
                break;
        }
    }
}
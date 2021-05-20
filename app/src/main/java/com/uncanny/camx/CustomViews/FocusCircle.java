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

/**
 * @author uncannyRishabh (16/05/2021)
 */

public class FocusCircle extends View {
    Paint paint = new Paint();
    int height,width,screenWidth;
    int radius;
    float circleRadius = 12f;

    public FocusCircle(Context context) {
        super(context);
        init();
    }

    public FocusCircle(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStrokeWidth(2.0f);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
    }

    public void setPosition(int height,int width,int screenWidth){
        invalidate();
        this.height = height;
        this.width = width;
        this.screenWidth = screenWidth;
        radius = screenWidth/10;
        animateInnerCircle();
    }

    public void animateInnerCircle(){
        PropertyValuesHolder rPropertyValuesHolder = PropertyValuesHolder.ofFloat("radius",10f,2f);
        PropertyValuesHolder aPropertyValuesHolder = PropertyValuesHolder.ofFloat("alpha",1f,.1f);
        ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(rPropertyValuesHolder,aPropertyValuesHolder);
        valueAnimator.setInterpolator(new CycleInterpolator(1));
        valueAnimator.addUpdateListener(animation -> {
            invalidate();
            circleRadius = (float) animation.getAnimatedValue();
        });
        valueAnimator.setDuration(1000);
        valueAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        invalidate();
        if(screenWidth>0){
            canvas.drawCircle(height,width, circleRadius,paint);
            canvas.drawCircle(height,width,radius,paint);
        }
        else{
            canvas.drawCircle(height,width, circleRadius,paint);
            canvas.drawCircle(height,width,90f,paint);
        }
    }

}
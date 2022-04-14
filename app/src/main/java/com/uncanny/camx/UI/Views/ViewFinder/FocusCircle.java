package com.uncanny.camx.UI.Views.ViewFinder;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;

import androidx.annotation.Nullable;

/**
 * @author uncannyRishabh (16/05/2021)
 */

public class FocusCircle extends View {
    Paint oPaint = new Paint();
    Paint iPaint = new Paint();
    int height,width,screenWidth;
    int radius;
    float iRadius = 0f;
    float oRadius = 72f;

    public FocusCircle(Context context) {
        super(context);
        init();
    }

    public FocusCircle(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        oPaint.setStrokeWidth(4.0f);
        oPaint.setAntiAlias(true);
        oPaint.setStyle(Paint.Style.STROKE);
        oPaint.setColor(Color.WHITE);

        iPaint.setStyle(Paint.Style.FILL);
        iPaint.setColor(0x80FFFFFF);
    }

    public void setPosition(int height,int width){
        invalidate();
        this.height = height;
        this.width = width;
        this.screenWidth = getScreenWidth();
        radius = screenWidth/10;
        animateInnerCircle();
    }

    public void animateInnerCircle(){
        PropertyValuesHolder oPropertyValuesHolder = PropertyValuesHolder.ofFloat("radius",radius/3f,radius);
        ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(oPropertyValuesHolder);
        valueAnimator.setInterpolator(new AnticipateOvershootInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            invalidate();
            iRadius = (float) animation.getAnimatedValue();
        });
        valueAnimator.setDuration(650);
        valueAnimator.start();

        PropertyValuesHolder iPropertyValuesHolder = PropertyValuesHolder.ofFloat("radius",radius/2f,radius);
        valueAnimator = ValueAnimator.ofPropertyValuesHolder(iPropertyValuesHolder);
        valueAnimator.setInterpolator(new AnticipateOvershootInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            invalidate();
            oRadius = (float) animation.getAnimatedValue();
        });
        valueAnimator.setDuration(450);
        valueAnimator.start();
    }

    private static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        invalidate();
        if(screenWidth>0){
            canvas.drawCircle(height,width,iRadius,iPaint); // inner circle
            canvas.drawCircle(height,width,oRadius,oPaint); // outer circle
        }
        else{
            canvas.drawCircle(height,width, iRadius,iPaint); // inner circle
            canvas.drawCircle(height,width,90f,oPaint); // outer circle
        }
    }

}
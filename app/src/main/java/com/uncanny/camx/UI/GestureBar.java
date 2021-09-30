package com.uncanny.camx.UI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.CycleInterpolator;

import androidx.annotation.Nullable;

/**
 * @author uncannyRishabh (17/5/2021)
 */

@SuppressWarnings({"FieldMayBeFinal","FieldCanBeLocal"})
public class GestureBar extends View {
    private int sWidth,startX,stopX;
    private Paint paint = new Paint();

    public GestureBar(Context context) {
        super(context);
        init();
    }

    public GestureBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(8.0f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        sWidth = getWidth();
        startX = (sWidth/2)-45;
        stopX  = (sWidth/2)+45;
        canvas.drawLine(startX,getHeight()/2f,stopX,getHeight()/2f,paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            this.animate().scaleX(1.3f).setInterpolator(new CycleInterpolator(1));
        }
        return super.onTouchEvent(event);
    }

}

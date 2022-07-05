package com.uncanny.camx.UI.Views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.uncanny.camx.R;

/**
 * @author uncannyRishabh (17/5/2021)
 */

@SuppressWarnings({"FieldMayBeFinal","FieldCanBeLocal"})
public class GestureBar extends View {
    private int sWidth,startX,stopX;
    private Paint paint = new Paint();
    private final float density = getResources().getDisplayMetrics().density;
    private float thickness = 8.0f;
    private float delta = 0f;

    public GestureBar(Context context) {
        super(context);
        init();
    }

    public GestureBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
//        thickness = 500f * (density / 160); //8.___ f
        thickness = 3f * density + .5f;
        delta = 18f * density + .5f;
        Log.e("TAG", "init: thickness : "+thickness);
//        paint.setColor(Color.WHITE);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.md3_accent2_100));
        paint.setStrokeWidth(thickness);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        sWidth = getWidth();
        startX = (sWidth/2)+(int) delta;
        stopX  = (sWidth/2)-(int) delta;
        canvas.drawLine(startX
                ,(getHeight()+getPaddingTop()-getPaddingBottom())/2f
                ,stopX
                ,(getHeight()+getPaddingTop()-getPaddingBottom())/2f
                ,paint);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action){
            case MotionEvent.ACTION_DOWN:{
                performClick();
                this.animate().scaleX(1.4f).setInterpolator(new AnticipateOvershootInterpolator(2));
                return true;
            }
            case MotionEvent.ACTION_UP:{
                this.animate().scaleX(1f).setInterpolator(new AnticipateOvershootInterpolator(2));
            }
        }
        return super.onTouchEvent(event);
    }

}

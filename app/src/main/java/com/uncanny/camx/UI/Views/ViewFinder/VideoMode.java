package com.uncanny.camx.UI.Views.ViewFinder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VideoMode extends ModePicker{

    public interface OnCLickListener {
        /**
         * @param view ModePicker View
         * @param Position Position of the element, starting from 0.
         */
        void onClick(VideoMode view, int Position);
    }

    private OnCLickListener onCLickListener;

    public VideoMode(Context context) {
        super(context);
    }

    public VideoMode(Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoMode(Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public VideoMode(Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setOnClickListener(OnCLickListener onCLickListener) {
        this.onCLickListener = onCLickListener;
    }

    @Override
    public int invokeOnTouch() {
        if(onCLickListener !=null){
            onCLickListener.onClick(this,super.invokeOnTouch());
        }
        return super.invokeOnTouch();
    }
}

class ModePicker extends View {
    private Paint bgPaint;
    private Paint sPaint;
    private Paint tPaint;
    private RectF sRect;
    private RectF bgRect;
    private Typeface Poppins;

    private int index;
    private int cacheIndex;
    private float tSize;
    private float halfHeight;
    private int divisionSize;
    private String []modes = {"Slow Motion","Normal","Time Warp"};
    private final float density = getResources().getDisplayMetrics().density;

    public void setModes(String[] modes) {
        this.modes = modes;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
        this.cacheIndex = index;
    }

    private void setIndexVal(int index) {
        this.index = index;
    }

    public ModePicker(Context context) {
        super(context);
        init();
    }

    public ModePicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ModePicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ModePicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init(){
        bgPaint = new Paint();
        sPaint = new Paint();
        tPaint = new Paint();
        sRect = new RectF();
        bgRect = new RectF();
        Poppins = Typeface.create("Poppins", Typeface.NORMAL);

        tSize = (float) (13.5 * density);

        bgPaint.setColor(0x80000000);
        bgPaint.setAntiAlias(true);
        bgPaint.setStyle(Paint.Style.FILL);

        sPaint.setColor(Color.DKGRAY);
        sPaint.setAntiAlias(true);
        sPaint.setStyle(Paint.Style.FILL);

        tPaint.setAntiAlias(true);
        tPaint.setColor(Color.WHITE);
        tPaint.setTextSize(tSize);
        tPaint.setStyle(Paint.Style.FILL);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action){
            case MotionEvent.ACTION_DOWN:{
            }
            case MotionEvent.ACTION_UP:{
                for(int i=0; i<modes.length; i++){
                    if(event.getX()<(divisionSize*(i+1)) && event.getX()>(divisionSize*i)){
                        setIndexVal(i);
                    }
                }
                invalidate();
                invokeOnTouch();
            }
        }
        return true;
    }

    public int invokeOnTouch(){
            return index;
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if(visibility == GONE) {
            index = cacheIndex;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        halfHeight = getHeight()/2f;
        divisionSize = (getWidth()/ modes.length);

        bgRect.set(0,0,getWidth(),getHeight());
        sRect.set(divisionSize*index + getPaddingStart(),
                getPaddingTop()
                ,divisionSize*(index+1) - getPaddingEnd()
                ,getHeight()-getPaddingBottom());

        canvas.drawRoundRect(bgRect,50,50,bgPaint);

        if(modes!=null){
            canvas.drawRoundRect(sRect,50,50,sPaint);
            for(int i=0; i<modes.length; i++){
                canvas.drawText(modes[i]
                        ,((float)divisionSize*i)+((float)divisionSize-tPaint.measureText(modes[i]))/2
                        ,(getHeight()+ tSize)/2f-5
                        ,tPaint);
            }

        }

    }
}

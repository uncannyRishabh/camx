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
import androidx.core.content.ContextCompat;

import com.uncanny.camx.R;

import java.util.ArrayList;

public class VideoModePicker extends ModePicker{

    public interface OnCLickListener {
        /**
         * @param view ModePicker View
         * @param modeName name of the mode.
         */
        void onClick(VideoModePicker view, String modeName);
    }

    private OnCLickListener onCLickListener;

    public VideoModePicker(Context context) {
        super(context);
    }

    public VideoModePicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoModePicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public VideoModePicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setOnClickListener(OnCLickListener onCLickListener) {
        this.onCLickListener = onCLickListener;
    }

    @Override
    public String invokeOnTouch() {
        if(onCLickListener !=null){
            onCLickListener.onClick(this,super.invokeOnTouch());
        }
        return super.invokeOnTouch();
    }
}

abstract class ModePicker extends View {
    private Paint bgPaint;
    private Paint sPaint;
    private Paint tPaint;
    private RectF sRect;
    private RectF bgRect;
    private Typeface Poppins;

    private int index;
    private int cacheIndex;
    private float tSize;
    private int divisionSize;
    private ArrayList<String> modes = new ArrayList<>();
    private final float density = getResources().getDisplayMetrics().density;

    public static String MODE_VIDEO = "Video";
    public static String MODE_SLOW_MOTION = "Slow Motion";
    public static String MODE_TIME_LAPSE = "Time Lapse";

    public void setModes(ArrayList<String> modes) {
        this.modes = modes;
        postInvalidate();
    }

    public void addMode(int index, String modeName){
        if(!modes.contains(modeName)){
            modes.add(index,modeName);
            setIndex(MODE_VIDEO);
        }
        divisionSize = (getWidth()/ modes.size());
        postInvalidate();
    }

    public void removeMode(String modeName){
        boolean ds = modes.remove(modeName);
        if(ds) setIndex(MODE_VIDEO);
        divisionSize = (getWidth()/ modes.size());
        postInvalidate();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(String modeName) {
        this.index = modes.indexOf(modeName);
        this.cacheIndex = modes.indexOf(modeName);
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

        tSize = (float) (12.5 * density);

//        bgPaint.setColor(0x40000000);
        int color = ContextCompat.getColor(getContext(), R.color.md3_neutral2_800);
        int aColor = Color.argb(200, Color.red(color), Color.green(color), Color.blue(color));
        bgPaint.setColor(aColor);
        bgPaint.setAntiAlias(true);
        bgPaint.setStyle(Paint.Style.FILL);

//        sPaint.setColor(0x99000000);
        sPaint.setColor(ContextCompat.getColor(getContext(),R.color.md3_accent2_100));
        sPaint.setAntiAlias(true);
        sPaint.setStyle(Paint.Style.FILL);

        tPaint.setAntiAlias(true);
        tPaint.setColor(Color.WHITE);
        tPaint.setTextSize(tSize);

        modes.add(MODE_SLOW_MOTION);
        modes.add(MODE_VIDEO);
        modes.add(MODE_TIME_LAPSE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action){
            case MotionEvent.ACTION_DOWN:{
                break;
            }
            case MotionEvent.ACTION_UP:{
                for(int i=0; i<modes.size(); i++){
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

    public String invokeOnTouch(){
            return modes.get(index);
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

        divisionSize = (getWidth()/ modes.size());

        bgRect.set(0,0,getWidth(),getHeight());
        sRect.set(divisionSize*index + getPaddingStart(),
                getPaddingTop()
                ,divisionSize*(index+1) - getPaddingEnd()
                ,getHeight()-getPaddingBottom());

        canvas.drawRoundRect(bgRect,60,60,bgPaint);

        if(modes!=null){
            canvas.drawRoundRect(sRect,50,50,sPaint);
            for(int i=0; i<modes.size(); i++){
                if(i==getIndex()) tPaint.setColor(ContextCompat.getColor(getContext(),R.color.md3_neutral1_900));
                else tPaint.setColor(0xFFFFFFFF);
                canvas.drawText(modes.get(i)
                        ,((float)divisionSize*i)+((float)divisionSize-tPaint.measureText(modes.get(i)))/2
                        ,(getHeight()+ tSize)/2f-5
                        ,tPaint);
            }

        }

    }
}

package com.uncanny.camx.UI.Views.ViewFinder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.uncanny.camx.R;

import java.util.ArrayList;

public class AuxiliaryCameraPicker extends AuxDock {

    public interface OnClickListener{
        void onClick(AuxiliaryCameraPicker view, String id);
    }

    private OnClickListener onClickListener;

    public AuxiliaryCameraPicker(Context context) {
        super(context);
    }

    public AuxiliaryCameraPicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AuxiliaryCameraPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    @Override
    public String invokeOnTouch() {
        if(onClickListener !=null){
            onClickListener.onClick(this,super.invokeOnTouch());
        }
        return super.invokeOnTouch();
    }
}

abstract class AuxDock extends View {
    private Paint bgPaint;
    private Paint textPaint;
    private Paint activeLensPaint;
    private RectF bgRect;
    private Rect textBounds = new Rect();

    private int index=1;
    private int division;
    private float textSize;
    private float height,width;
    private float textHeight, textWidth;
    private final float density = getResources().getDisplayMetrics().density;

    // 0: ultra-wide ; 1: wide ; 2: macro/telephoto ; 3: other ...
    private ArrayList<String> camList = new ArrayList<>();
    // 0: alias ; 1: cmaIDs
    private ArrayList<ArrayList<String>> camAliasList = new ArrayList<>();

    public ArrayList<ArrayList<String>> getCamAliasList() {
        return camAliasList;
    }

    public void setCamAliasList(ArrayList<ArrayList<String>> camAliasList) {
        this.camAliasList = camAliasList;
        camList = camAliasList.get(0);
        invalidate();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
        invalidate();
    }

    public AuxDock(Context context) {
        super(context);
        init();
    }

    public AuxDock(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuxDock(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint = new Paint();
        textPaint = new Paint();
        activeLensPaint = new Paint();
        bgRect = new RectF();

        textSize = (float) (13.5 * density);
        height = 40 * density;
        width = 38 * density; //36

        int color = ContextCompat.getColor(getContext(), R.color.md3_neutral2_800);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(Color.argb(200,Color.red(color),Color.green(color),Color.blue(color)));

        activeLensPaint.setStyle(Paint.Style.FILL);
        activeLensPaint.setColor(ContextCompat.getColor(getContext(),R.color.md3_accent2_100));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(textSize);

//        if(!camAliasList.isEmpty()){
//            camList = camAliasList.get(1);
//        }
//        else{
////            Test data
//        camList.add("21");
//        camList.add("1");
//        camList.add("22");
//        camList.add("23");
//        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int desiredWidth = (int) (camList.size()*width);//+getPaddingStart()+getPaddingEnd();
        int desiredHeight = (int) height;// +getPaddingTop()+getPaddingBottom();

        int width = measureDimension(widthMeasureSpec, desiredWidth);
        int height = measureDimension(heightMeasureSpec, desiredHeight);

        setMeasuredDimension(width, height);
    }

    private int measureDimension(int measureSpec, int desiredSize) {
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);

        if (mode == MeasureSpec.EXACTLY) {
            return size;
        } else if (mode == MeasureSpec.AT_MOST) {
            return Math.min(desiredSize, size);
        } else {
            return desiredSize;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action){
            case MotionEvent.ACTION_DOWN:{
                break;
            }
            case MotionEvent.ACTION_UP:{
                for(int i=0; i<camList.size(); i++){
                    if(event.getX()<(division*(i+1)) && event.getX()>(division*i)){
                        setIndex(i);
                        Log.i("TAG", "onTouchEvent: "+i);
                    }
                }
                invalidate();
                invokeOnTouch();
            }
        }
        return true;
    }

    public String invokeOnTouch(){
        return camAliasList.get(1).get(index);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(!camList.isEmpty()){
            division = getWidth()/camList.size();
            bgRect.set(0,0,(camList.size()*width)//+getPaddingStart()+getPaddingEnd()
                    ,height);//+getPaddingTop()+getPaddingBottom());
            canvas.drawRoundRect(bgRect,50,50,bgPaint);

            canvas.drawCircle(getIndex()*((float)division)+(width/2f)//+getPaddingStart()
                    ,getHeight()/2f
                    ,((height)/2)-getPaddingStart()
                    ,activeLensPaint);

            for(int i=0; i<camList.size();i++){
                if(i==getIndex()) {
                    textPaint.setTextSize((float) (16 * density));
                    textPaint.setColor(ContextCompat.getColor(getContext(), R.color.md3_neutral1_900));
                }
                else {
                    textPaint.setTextSize(textSize);
                    textPaint.setColor(0xFFFFFFFF);
                }

                textPaint.getTextBounds(camList.get(i), 0, camList.get(i).length(), textBounds);
                textWidth = textPaint.measureText(camList.get(i));
                textHeight = textBounds.height();

                canvas.drawText(camList.get(i)
                        ,((float)division*i)+((float)division-textWidth)/2f
                        ,(getHeight()/2f)+textHeight/2f
                        ,textPaint);
            }
        }

    }

}

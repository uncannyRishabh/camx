package com.uncanny.camx.CustomViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * @author uncannyRishabh (11/07/2021)
 */
public class ResolutionSelector extends View {
    private RectF rectF,rectIn;
    private Paint paint,tPaint,sPaint;
    private String selectedItem;
    private float headingTextSize = 32f;
    private float subtitleTextSize = 26f;
    private float index;
    private float divisions;
    private ArrayList<String> items = new ArrayList<>();

    public ResolutionSelector(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ResolutionSelector(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        rectF = new RectF();
        rectIn = new RectF();
        paint = new Paint();
        tPaint = new Paint();
        sPaint = new Paint();

        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#1E1E1E"));

        tPaint.setColor(Color.WHITE);
        tPaint.setTextAlign(Paint.Align.CENTER);
        tPaint.setAntiAlias(true);
        tPaint.setTextSize(headingTextSize);

        sPaint.setColor(Color.parseColor("#FFBA83FD"));
        sPaint.setStyle(Paint.Style.FILL);
        sPaint.setAntiAlias(true);

        items.add("720p");
        items.add("1080p");
        items.add("4k");

        selectedItem = items.get(0);
    }

    public void addItem(String item){
        items.add(item);
    }

    public String getSelectedItem(){
        return selectedItem;
    }

    public void setSelectedItem(String item){
        selectedItem = item;
    }

    private int getDivisions(){
        return items.size();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        for(int i=1; i<=getDivisions();i++){
            if(event.getX() < (getWidth()/(float) getDivisions())*i){
                setSelectedItem(items.get(i-1));
                break;
            }
        }
        index = items.indexOf(getSelectedItem())+1;
        invalidate();
        Log.e("TAG", "onTouchEvent: "+items.indexOf(getSelectedItem()));
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //OUTER RECT
        rectF.set(getPaddingLeft()
                ,getPaddingTop()
                ,getWidth()-getPaddingEnd()
                ,getHeight()-getPaddingBottom());

        canvas.drawRoundRect(rectF,48f,48f,paint);

        divisions = getDivisions();

        //INNER RECT
        rectIn.set((getWidth()/divisions)*index - (getWidth()/divisions) + 16,
                getPaddingTop()+6,
                (getWidth()/divisions)*index -16,
                getHeight()-getPaddingBottom()-6);

        canvas.drawRoundRect(rectIn,48f,48f,sPaint);

        //RECT TEXT
        for(int i=1; i<=items.size();i++){
            canvas.drawText(items.get(i-1)
                    ,(((getWidth()/divisions)*i - (getWidth()/divisions) + getPaddingStart() +4)
                            +((getWidth()/divisions)*i -getPaddingEnd() -4))/2
                    ,(getHeight()+ headingTextSize)/2f-5
                    ,tPaint);
        }
    }

}
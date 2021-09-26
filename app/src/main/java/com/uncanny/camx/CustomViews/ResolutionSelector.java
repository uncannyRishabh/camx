package com.uncanny.camx.CustomViews;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import com.uncanny.camx.CameraActivity.CamState;

import java.util.ArrayList;

/**
 * @author uncannyRishabh (11/07/2021)
 */
@SuppressWarnings({"FieldMayBeFinal","FieldCanBeLocal"})
public class ResolutionSelector extends View {
    private RectF rectF,rectIn;
    private Paint paint, headerPaint,footerPaint,sPaint;
    private String headerText;
    private String headerAndFooterText;
    private float headingTextSize = 30f;
    private float footerTextSize = 24f;
    private float index,prev;
    private float divisions;
    private int i = 0;
    private ArrayList<String> headerItems = new ArrayList<>(8);
    private ArrayList<String> headerAndFooterItems = new ArrayList<>(8);

    private PropertyValuesHolder valuesHolder;
    private ValueAnimator valueAnimator;

    private CamState state;
    private HandlerThread handlerThread;

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
        headerPaint = new Paint();
        footerPaint = new Paint();
        sPaint = new Paint();

        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#252525"));

        headerPaint.setColor(Color.WHITE);
        headerPaint.setTextAlign(Paint.Align.CENTER);
        headerPaint.setAntiAlias(true);
        headerPaint.setTextSize(headingTextSize);

        footerPaint.setColor(Color.WHITE);
        footerPaint.setTextAlign(Paint.Align.CENTER);
        footerPaint.setAntiAlias(true);
        footerPaint.setTextSize(footerTextSize);

        sPaint.setColor(Color.parseColor("#9883F0"));
        sPaint.setStyle(Paint.Style.FILL);
        sPaint.setAntiAlias(true);

        handlerThread = new HandlerThread("bgExec");
        handlerThread.start();

        state = CamState.CAMERA;
    }

    public void setHaloByHeaderText(String item){
        if(state != CamState.VIDEO_PROGRESSED){
            prev = index;
            headerText = item;
            index = headerItems.indexOf(item)+1;

            if(prev != 0f){
                valuesHolder = PropertyValuesHolder.ofFloat("i",prev,index);
                valueAnimator = new ValueAnimator();
                valueAnimator.setValues(valuesHolder);
                valueAnimator.setDuration(600);
                valueAnimator.setInterpolator(new LinearOutSlowInInterpolator());
                valueAnimator.addUpdateListener(animation -> {
                    index = (float) animation.getAnimatedValue("i");
                    invalidate();
                });
                valueAnimator.start();
            }
        }
    }

    public void setHaloByHeaderAndFooterText(String item){
        if(state != CamState.HSVIDEO_PROGRESSED) {
            prev = index;
            headerAndFooterText = item;
            index = headerAndFooterItems.indexOf(item)+1;

            if(prev != 0f){
                valuesHolder = PropertyValuesHolder.ofFloat("i",prev,index);
                valueAnimator = new ValueAnimator();
                valueAnimator.setValues(valuesHolder);
                valueAnimator.setDuration(600);
                valueAnimator.setInterpolator(new LinearOutSlowInInterpolator());
                valueAnimator.addUpdateListener(animation -> {
                    index = (float) animation.getAnimatedValue("i");
                    invalidate();
                });
                valueAnimator.start();
            }
        }
    }

    public void clearHeaderItems(){
        headerItems.clear();
    }

    public void clearHeaderAndFooterItems(){
        headerAndFooterItems.clear();
    }

    /**
     * Getter & Setter for HeaderText
     */
    public String getHeaderText(){
        return headerText;
    }

    public void setHeader(String item){
        headerItems.add(item);
        headerPaint.setTextSize(headerItems.size()>4 ? footerTextSize : headingTextSize);
    }

    public void setHeader(ArrayList<String> i){
        headerItems = i;
        headerPaint.setTextSize(headerItems.size()>4 ? footerTextSize : headingTextSize);
    }


    /**
     * Getter & Setter for HeaderAndFooterText
     */
    public String getHeaderAndFooterText(){
        return headerAndFooterText;
    }

    public void setHeaderAndFooterItems(ArrayList<String> i){
        headerAndFooterItems = i;
    }

    private int getDivisions(){
        return (headerItems.isEmpty() ? headerAndFooterItems.size() : headerItems.size());
    }

    public void setState(CamState state){
        this.state = state;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        for(i=1; i<=getDivisions();i++){
            if(event.getX() < (getWidth()/(float) getDivisions())*i){
                if(headerAndFooterItems.isEmpty())
                    new Handler(Looper.getMainLooper())
                            .postAtFrontOfQueue(() -> {
                        setHaloByHeaderText(headerItems.get(i-1));
                    });
                else
                    new Handler(Looper.getMainLooper())
                            .postAtFrontOfQueue(() -> {
                                setHaloByHeaderAndFooterText(headerAndFooterItems.get(i-1));
                            });
                break;
            }
        }
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

        //INNER RECT (HALO)
        rectIn.set((getWidth()/divisions)*index - (getWidth()/divisions) + 16,
                getPaddingTop()+8,
                (getWidth()/divisions)*index -16,
                getHeight()-getPaddingBottom()-8);

        canvas.drawRoundRect(rectIn,48f,48f,sPaint);


        if(headerAndFooterItems.isEmpty()){
            //HEADER TEXT
            headerPaint.setFakeBoldText(false);
            for(int i = 1; i<= headerItems.size(); i++){
                canvas.drawText(headerItems.get(i-1)
                        ,((getWidth()/divisions)*(2*i - 1) + getPaddingStart() -getPaddingEnd())/2
                        ,(getHeight()+ headingTextSize)/2f-5
                        , headerPaint);
            }
        }
        else{
            //HEADER TEXT
            headerPaint.setFakeBoldText(true);
            for(int i = 1; i<= headerAndFooterItems.size(); i++){
                canvas.drawText(getHeader(headerAndFooterItems.get(i-1))
                        ,((getWidth()/divisions)*(2*i - 1) + getPaddingStart() -getPaddingEnd())/2
                        ,getHeight()/2f
                        , headerPaint);
            }

            //FOOTER TEXT
            for(int i = 1; i<= headerAndFooterItems.size(); i++){
                canvas.drawText(getFooter(headerAndFooterItems.get(i-1))
                        ,((getWidth()/divisions)*(2*i - 1) + getPaddingStart() -getPaddingEnd())/2
                        ,(getHeight()/2f) + headingTextSize
                        , footerPaint);
            }
        }

    }

    private String getHeader(String s) {
        return s.split("_")[0];
    }

    private String getFooter(String s) {
        return s.split("_")[1];
    }
}
package com.uncanny.camx.CustomViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * @author uncannyRishabh (11/07/2021)
 */
public class ResolutionSelector extends View {
    private RectF rectF;
    private Paint paint,tPaint;
    private String tempText = "TEMP TEXT";

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
        paint = new Paint();
        tPaint = new Paint();

        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);

        tPaint.setColor(Color.BLACK);
        tPaint.setTextAlign(Paint.Align.CENTER);
        tPaint.setAntiAlias(true);
        tPaint.setTextSize(36f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        rectF.set(getPaddingLeft(),getPaddingTop()
                ,getWidth()-getPaddingEnd()
                ,getHeight()-getPaddingBottom());
        canvas.drawRoundRect(rectF,48f,48f,paint);
        canvas.drawText(tempText,getWidth()/2f,getHeight()/2f,tPaint);
    }
}
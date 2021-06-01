package com.uncanny.camx.CustomViews.ViewFinder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * @author uncannyRishabh (16/05/2021)
 */

public class FaceMeteringRect extends View {
    RectF rectF = new RectF(200,200,400,400);
    Paint paint = new Paint();

    public FaceMeteringRect(Context context) {
        super(context);
        init();
    }

    public FaceMeteringRect(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
    }

    public void setMeteringRect(RectF rect){
        rectF.set(rect);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(rectF,paint);
        invalidate();
    }
}

package com.uncanny.camx.CustomViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * @author uncannyRishabh (12/05/2021)
 */

@SuppressWarnings({"FieldMayBeFinal"})
public class Grids extends View {
    private static final String TAG = "Grids";
    private RectF mDrawBounds ; //=  new RectF(0,0,1080,1440);
    private int lines = 3;
    Paint paint = new Paint();

    public Grids(Context context) {
        super(context);
        init();
    }

    public Grids(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init(){
        paint.setStrokeWidth(2.0f);
        paint.setColor(Color.WHITE);
    }

    public void setLines(int lines){
        this.lines = lines;
    }

    public void setViewBounds(int height,int width){
        mDrawBounds = new RectF(0,0,width,height);
        Log.e(TAG, "setViewBounds: height "+height+" width "+width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float thirdWidth = mDrawBounds.width() / lines;
        float thirdHeight = mDrawBounds.height() / lines;
        for (int i = 1; i < lines; i++) {
            // Draw the vertical lines.
            final float x = thirdWidth * i;
            canvas.drawLine(mDrawBounds.left + x, mDrawBounds.top,
                    mDrawBounds.left + x, mDrawBounds.bottom, paint);
            // Draw the horizontal lines.
            final float y = thirdHeight * i;
            canvas.drawLine(mDrawBounds.left, mDrawBounds.top + y,
                    mDrawBounds.right, mDrawBounds.top + y, paint);
        }
    }

}
package com.uncanny.camx.CustomViews;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * @author uncannyRishabh(13/05/2021)
 */

public class BlurPreview extends View {
    private RectF rectF = new RectF(0,0,1080,1440);
    private Paint paint = new Paint();
    private BlurMaskFilter blurFilter = new BlurMaskFilter(14, BlurMaskFilter.Blur.INNER);
    private int height,width;

    public BlurPreview(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//        init();
    }

    public BlurPreview(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
//        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        init();
    }

    private void init(){
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.parseColor("#0CFFFFFF"));
        paint.setMaskFilter(blurFilter);
    }

    public void setPreviewSize(int h,int w){
        height = h;
        width = w;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(rectF,paint);
//        canvas.drawRoundRect(rectF,1080,1440,paint);

    }
}

 package com.uncanny.camx.UI;

 import android.content.Context;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.graphics.Canvas;
 import android.graphics.Color;
 import android.graphics.Paint;
 import android.graphics.Path;
 import android.graphics.PorterDuff;
 import android.graphics.PorterDuffXfermode;
 import android.graphics.Rect;
 import android.graphics.Region;
 import android.os.Build;
 import android.util.AttributeSet;
 import android.util.Log;
 import android.view.View;

 import androidx.annotation.NonNull;
 import androidx.annotation.Nullable;

 import com.uncanny.camx.R;

/**
 * @author uncannyRishabh (22/5/2021)
 */

@SuppressWarnings({"FieldMayBeFinal"})
public class CircularImageView extends View {
    private static final String TAG = "CircularImageView";
    private Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.rami);
    private Path mPath   = new Path();
    private Paint paint  = new Paint();
    private Rect srcRect,dstRect;
    private int center;

    public CircularImageView(@NonNull Context context) {
        super(context);
        init();
    }

    public CircularImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(3f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));

    }

    public void setBitmap(Bitmap bitmap) {
        img = bitmap;
        invalidate();
    }

    private void setRects() {
        srcRect = new Rect(0,0,img.getWidth(),img.getHeight());
        dstRect = new Rect(0,0,getWidth(),getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        center = getHeight()/2;
        setRects();

        mPath.setFillType(Path.FillType.WINDING);
        mPath.addCircle(center,center,center, Path.Direction.CCW);
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            canvas.clipPath(mPath, Region.Op.REVERSE_DIFFERENCE);
        } else {
            canvas.clipPath(mPath);
        }

        canvas.drawBitmap(img,srcRect,dstRect,null);

        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(6f);
        paint.setXfermode(null);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        canvas.drawCircle(center,center,center-3,paint);

        Log.e(TAG, "onDraw: height : "+getHeight()+" width : "+getWidth()+" radius : "+ center);
    }
}

package com.uncanny.camx.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class Blur {
    private static RenderScript renderScript;

    public Blur(Context context){
        renderScript = RenderScript.create(context, RenderScript.ContextType.NORMAL);
    }

    public Bitmap blur(Bitmap src, int radius) {
        final Allocation input = Allocation.createFromBitmap(renderScript, src);
        final Allocation output = Allocation.createTyped(renderScript, input.getType());
        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        script.setRadius(radius);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(src);

        renderScript.destroy();
        return src;
    }
}

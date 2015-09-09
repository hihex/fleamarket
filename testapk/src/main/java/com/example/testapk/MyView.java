package com.example.testapk;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class MyView extends View {
    private final String mText;
    private final int mColor;
    private final Paint mPaint;

    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MyView, 0, 0);
        try {
            mText = a.getString(R.styleable.MyView_text);
            mColor = a.getInt(R.styleable.MyView_color, Color.BLACK);

            final Paint paint = new Paint();
            paint.setColor(mColor);
            paint.setTextSize(24);
            paint.setTextAlign(Paint.Align.LEFT);
            mPaint = paint;
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawText(mText, 0, getHeight(), mPaint);
    }
}

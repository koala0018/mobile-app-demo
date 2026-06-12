package com.example.mobileproject01;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.LinearInterpolator;

final class KineticStripView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] colors = {
            Styles.GREEN,
            Styles.PINK,
            Styles.YELLOW,
            Styles.BLUE,
            0xFFFFFFFF
    };
    private float phase;

    KineticStripView(Context context) {
        super(context);
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            phase = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Styles.PAPER);
        canvas.drawRect(0, 0, width, height, paint);

        float block = width / 5.5f;
        float offset = phase * block;
        for (int i = -1; i < 7; i++) {
            paint.setColor(colors[Math.floorMod(i, colors.length)]);
            float left = i * block - offset;
            canvas.drawRect(left, 0, left + block * 0.78f, height, paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Styles.dp(getContext(), 1));
        paint.setColor(Styles.LINE);
        canvas.drawRect(0, 0, width, height, paint);
    }
}

package com.example.mobileproject01;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
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
    private final RectF rect = new RectF();
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

        drawBlocks(canvas, width, height);
        drawPulseDots(canvas, width, height);
        drawScanLines(canvas, width, height);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Styles.dp(getContext(), 2));
        paint.setColor(Styles.LINE);
        canvas.drawRect(0, 0, width, height, paint);
    }

    private void drawBlocks(Canvas canvas, int width, int height) {
        float block = width / 5.2f;
        float offset = phase * block;
        for (int i = -1; i < 8; i++) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors[Math.floorMod(i, colors.length)]);
            float left = i * block - offset;
            rect.set(left, 0, left + block * 0.74f, height);
            canvas.drawRect(rect, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Styles.dp(getContext(), 1));
            paint.setColor(Styles.LINE);
            canvas.drawRect(rect, paint);
        }
    }

    private void drawPulseDots(Canvas canvas, int width, int height) {
        float cell = width / 7f;
        float radius = Styles.dp(getContext(), 4) + (float) Math.sin(phase * Math.PI * 2) * Styles.dp(getContext(), 2);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 8; i++) {
            paint.setColor(colors[Math.floorMod(i + 2, colors.length)]);
            float cx = i * cell + cell * 0.45f;
            float cy = height * (i % 2 == 0 ? 0.28f : 0.72f);
            canvas.drawCircle(cx, cy, Math.max(Styles.dp(getContext(), 3), radius), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Styles.dp(getContext(), 1));
            paint.setColor(Styles.LINE);
            canvas.drawCircle(cx, cy, Math.max(Styles.dp(getContext(), 3), radius), paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawScanLines(Canvas canvas, int width, int height) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Styles.dp(getContext(), 2));
        paint.setColor(Styles.LINE);
        float gap = Styles.dp(getContext(), 18);
        float offset = phase * gap * 2f;
        for (float x = -width; x < width * 2f; x += gap) {
            canvas.drawLine(x + offset, height, x + offset + height, 0, paint);
        }
    }
}

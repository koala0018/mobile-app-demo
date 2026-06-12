package com.example.mobileproject01;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

public class ParticleFieldView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long startedAt;

    public ParticleFieldView(Context context) {
        super(context);
        init();
    }

    public ParticleFieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ParticleFieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        startedAt = SystemClock.uptimeMillis();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        canvas.drawColor(Styles.PAPER);

        paint.setStrokeWidth(1f);
        paint.setColor(0x14111111);
        int grid = Styles.dp(getContext(), 32);
        for (int x = -grid; x < width + grid; x += grid) {
            canvas.drawLine(x, 0, x, height, paint);
        }
        for (int y = -grid; y < height + grid; y += grid) {
            canvas.drawLine(0, y, width, y, paint);
        }

        float t = (SystemClock.uptimeMillis() - startedAt) / 1000f;
        drawBand(canvas, Styles.PINK, -width * 0.25f + wave(t, width * 0.2f), height * 0.08f, width * 0.85f, 20f);
        drawBand(canvas, Styles.GREEN, width * 0.55f + wave(t * 0.7f, width * 0.12f), height * 0.18f, width * 0.62f, -16f);
        drawBand(canvas, Styles.BLUE, -width * 0.16f + wave(t * 0.55f, width * 0.1f), height * 0.78f, width * 0.72f, -12f);
        drawBand(canvas, Styles.YELLOW, width * 0.62f + wave(t * 0.9f, width * 0.08f), height * 0.88f, width * 0.5f, 14f);
        postInvalidateOnAnimation();
    }

    private void drawBand(Canvas canvas, int color, float x, float y, float length, float angle) {
        canvas.save();
        canvas.rotate(angle, x, y);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRect(x, y, x + length, y + Styles.dp(getContext(), 18), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Styles.dp(getContext(), 1));
        paint.setColor(Styles.LINE);
        canvas.drawRect(x, y, x + length, y + Styles.dp(getContext(), 18), paint);
        canvas.restore();
    }

    private float wave(float t, float size) {
        return (float) Math.sin(t) * size;
    }
}

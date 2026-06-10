package com.example.mobileproject01;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleFieldView extends View {
    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backgroundPaint = new Paint();
    private long lastFrameNanos;
    private int width;
    private int height;

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
        linePaint.setStrokeWidth(1.5f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        particles.clear();
        int count = Math.max(18, (w * h) / 90000);
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                    randomRange(w),
                    randomRange(h),
                    randomDirection() * randomRange(14f, 38f),
                    randomDirection() * randomRange(10f, 30f),
                    randomRange(2.5f, 5.5f),
                    randomRange(0.25f, 0.75f)));
        }
        backgroundPaint.setShader(new LinearGradient(
                0, 0, 0, h,
                0xFF08111E,
                0xFF0C1730,
                Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPaint(backgroundPaint);
        drawGlow(canvas, width * 0.2f, height * 0.18f, width * 0.85f, 0x226EE7FF);
        drawGlow(canvas, width * 0.82f, height * 0.22f, width * 0.7f, 0x1EF59E0B);
        drawGlow(canvas, width * 0.72f, height * 0.82f, width * 0.8f, 0x1EA78BFA);

        long now = SystemClock.uptimeMillis();
        float dt = lastFrameNanos == 0 ? 0.016f : (now - lastFrameNanos) / 1000f;
        lastFrameNanos = now;
        updateParticles(dt);
        drawConnections(canvas);
        drawParticles(canvas);
        postInvalidateOnAnimation();
    }

    private void drawGlow(Canvas canvas, float cx, float cy, float radius, int color) {
        glowPaint.setShader(new RadialGradient(
                cx, cy, radius,
                color,
                0x00000000,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, glowPaint);
    }

    private void updateParticles(float dt) {
        if (dt <= 0) {
            return;
        }
        for (Particle particle : particles) {
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            if (particle.x < -20 || particle.x > width + 20) {
                particle.x = randomRange(width);
            }
            if (particle.y < -20 || particle.y > height + 20) {
                particle.y = randomRange(height);
            }
        }
    }

    private void drawConnections(Canvas canvas) {
        for (int i = 0; i < particles.size(); i++) {
            Particle a = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle b = particles.get(j);
                float dx = a.x - b.x;
                float dy = a.y - b.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (distance < 180f) {
                    int alpha = (int) ((1f - distance / 180f) * 95);
                    linePaint.setColor((alpha << 24) | 0x6EE7FF);
                    canvas.drawLine(a.x, a.y, b.x, b.y, linePaint);
                }
            }
        }
    }

    private void drawParticles(Canvas canvas) {
        for (Particle particle : particles) {
            int alpha = (int) (particle.alpha * 255f);
            dotPaint.setColor((alpha << 24) | 0xFFFFFF);
            canvas.drawCircle(particle.x, particle.y, particle.radius, dotPaint);
        }
    }

    private float randomRange(float max) {
        return random.nextFloat() * max;
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private float randomDirection() {
        return random.nextBoolean() ? 1f : -1f;
    }

    private static final class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float radius;
        float alpha;

        Particle(float x, float y, float vx, float vy, float radius, float alpha) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
            this.alpha = alpha;
        }
    }
}

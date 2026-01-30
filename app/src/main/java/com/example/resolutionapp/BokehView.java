package com.example.resolutionapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BokehView extends View {

    private class Particle {
        float x, y;
        float radius;
        int alpha;
        float speedX, speedY;
        int color;

        public Particle(float x, float y, float radius, int alpha, int color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.alpha = alpha;
            this.color = color;
            // Improved movement
            this.speedX = (float) (Math.random() * 6 - 3); // -3 to 3
            this.speedY = (float) (Math.random() * 6 - 3); // -3 to 3
        }
    }

    private List<Particle> particles = new ArrayList<>();
    private Paint paint;
    private boolean isAnimating = false;
    private Random random = new Random();
    private int[] colors = {
            Color.parseColor("#FFD700"), // Gold
            Color.parseColor("#FFFFFF"), // White
            Color.parseColor("#FF69B4"), // HotPink
            Color.parseColor("#00BFFF") // DeepSkyBlue
    };

    public BokehView(Context context) {
        super(context);
        init();
    }

    public BokehView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
    }

    public void startAnimation() {
        setVisibility(VISIBLE);

        if (getWidth() == 0 || getHeight() == 0) {
            // View not laid out yet, retry after a short delay
            postDelayed(this::startAnimation, 50);
            return;
        }

        particles.clear();
        int particleCount = 80;
        for (int i = 0; i < particleCount; i++) {
            particles.add(createParticle());
        }
        isAnimating = true;
        invalidate();
    }

    public void stopAnimation() {
        isAnimating = false;
        particles.clear();
        setVisibility(GONE);
        invalidate();
    }

    private Particle createParticle() {
        float x = random.nextFloat() * getWidth();
        float y = random.nextFloat() * getHeight();
        float radius = 20 + random.nextFloat() * 60; // 20 to 80 radius
        int alpha = 100 + random.nextInt(155); // 100 to 255
        int color = colors[random.nextInt(colors.length)];
        return new Particle(x, y, radius, alpha, color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isAnimating)
            return;

        for (Particle p : particles) {
            paint.setColor(p.color);
            paint.setAlpha(p.alpha);
            canvas.drawCircle(p.x, p.y, p.radius, paint);

            // Update position
            p.x += p.speedX;
            p.y += p.speedY;

            // Fade out or blink
            p.alpha -= 1;
            if (p.alpha <= 0) {
                // Reset particle
                Particle newP = createParticle();
                p.x = newP.x;
                p.y = newP.y;
                p.alpha = 255;
                p.radius = newP.radius;
                p.color = newP.color;
            }
        }

        invalidate(); // Loop
    }
}

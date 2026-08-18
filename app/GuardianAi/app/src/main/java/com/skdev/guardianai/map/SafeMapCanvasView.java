package com.skdev.guardianai.map;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import com.skdev.guardianai.data.RouteOption;
import com.skdev.guardianai.data.SafetyLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Cyber-Dark Vector Map Canvas Engine.
 * Supports smooth multi-touch pan & pinch-zoom, crime heatmap overlays,
 * route polylines with neon glow, pulsing GPS location indicator, and police checkpoints.
 */
public class SafeMapCanvasView extends View {

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint roadGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mainRoadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint parkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routeSafePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routeModPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routeRiskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routeGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<RouteOption> routes = new ArrayList<>();
    private int selectedRouteIndex = 0;
    private SafetyLocation originLocation;
    private SafetyLocation destinationLocation;
    private List<SafetyLocation> nearbyLocalities = new ArrayList<>();

    // Pan & Zoom
    private float scaleFactor = 1.0f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float lastTouchX;
    private float lastTouchY;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private ScaleGestureDetector scaleDetector;

    // Pulse animation
    private float pulseRadius = 0f;
    private ValueAnimator pulseAnimator;

    public SafeMapCanvasView(Context context) {
        super(context);
        init();
    }

    public SafeMapCanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SafeMapCanvasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint.setColor(Color.parseColor("#0B111E"));

        roadGridPaint.setColor(Color.parseColor("#152033"));
        roadGridPaint.setStrokeWidth(2f);
        roadGridPaint.setStyle(Paint.Style.STROKE);

        mainRoadPaint.setColor(Color.parseColor("#1C2C45"));
        mainRoadPaint.setStrokeWidth(8f);
        mainRoadPaint.setStyle(Paint.Style.STROKE);

        waterPaint.setColor(Color.parseColor("#0C2340"));
        waterPaint.setStyle(Paint.Style.FILL);

        parkPaint.setColor(Color.parseColor("#0B2820"));
        parkPaint.setStyle(Paint.Style.FILL);

        // Safe Route (Green)
        routeSafePaint.setColor(Color.parseColor("#10B981"));
        routeSafePaint.setStrokeWidth(12f);
        routeSafePaint.setStyle(Paint.Style.STROKE);
        routeSafePaint.setStrokeCap(Paint.Cap.ROUND);
        routeSafePaint.setStrokeJoin(Paint.Join.ROUND);

        // Moderate Route (Amber)
        routeModPaint.setColor(Color.parseColor("#F59E0B"));
        routeModPaint.setStrokeWidth(9f);
        routeModPaint.setStyle(Paint.Style.STROKE);
        routeModPaint.setStrokeCap(Paint.Cap.ROUND);
        routeModPaint.setStrokeJoin(Paint.Join.ROUND);

        // High Risk Route (Red)
        routeRiskPaint.setColor(Color.parseColor("#EF4444"));
        routeRiskPaint.setStrokeWidth(8f);
        routeRiskPaint.setStyle(Paint.Style.STROKE);
        routeRiskPaint.setStrokeCap(Paint.Cap.ROUND);
        routeRiskPaint.setStrokeJoin(Paint.Join.ROUND);

        routeGlowPaint.setStyle(Paint.Style.STROKE);
        routeGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        routeGlowPaint.setStrokeJoin(Paint.Join.ROUND);

        textPaint.setColor(Color.parseColor("#F8FAFC"));
        textPaint.setTextSize(30f);
        textPaint.setFakeBoldText(true);

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.7f, Math.min(scaleFactor, 3.5f));
                invalidate();
                return true;
            }
        });

        // Start GPS pulse animation
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1600);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.addUpdateListener(animation -> {
            pulseRadius = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    public void setMapData(SafetyLocation origin, SafetyLocation destination, List<RouteOption> routeOptions, List<SafetyLocation> nearby) {
        this.originLocation = origin;
        this.destinationLocation = destination;
        this.routes = routeOptions != null ? routeOptions : new ArrayList<>();
        this.nearbyLocalities = nearby != null ? nearby : new ArrayList<>();
        this.selectedRouteIndex = 0;
        this.offsetX = 0;
        this.offsetY = 0;
        this.scaleFactor = 1.0f;
        invalidate();
    }

    public void setSelectedRoute(int index) {
        if (index >= 0 && index < routes.size()) {
            this.selectedRouteIndex = index;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        canvas.save();
        canvas.translate(w / 2f + offsetX, h / 2f + offsetY);
        canvas.scale(scaleFactor, scaleFactor);
        canvas.translate(-w / 2f, -h / 2f);

        // 1. Draw Map Base Background
        canvas.drawRect(-w, -h, w * 2, h * 2, bgPaint);

        // 2. Draw Parks & Water Features
        drawTerrainFeatures(canvas, w, h);

        // 3. Draw Grid Road Networks
        drawRoadNetwork(canvas, w, h);

        // 4. Draw Crime Heatmap Blooms
        drawCrimeHeatmaps(canvas, w, h);

        // 5. Draw Route Alternatives
        drawRoutes(canvas, w, h);

        // 6. Draw Police Checkpoints
        drawPoliceCheckpoints(canvas, w, h);

        // 7. Draw Origin (GPS Location) & Destination Pin
        drawMarkers(canvas, w, h);

        canvas.restore();
    }

    private void drawTerrainFeatures(Canvas canvas, int w, int h) {
        // Water river
        Path waterPath = new Path();
        waterPath.moveTo(-w * 0.5f, h * 0.15f);
        waterPath.cubicTo(w * 0.3f, h * 0.25f, w * 0.7f, h * 0.05f, w * 1.5f, h * 0.20f);
        waterPath.lineTo(w * 1.5f, h * 0.28f);
        waterPath.cubicTo(w * 0.7f, h * 0.13f, w * 0.3f, h * 0.33f, -w * 0.5f, h * 0.23f);
        waterPath.close();
        canvas.drawPath(waterPath, waterPaint);

        // Parks / Green Zones
        canvas.drawRoundRect(new RectF(w * 0.08f, h * 0.45f, w * 0.28f, h * 0.65f), 30, 30, parkPaint);
        canvas.drawRoundRect(new RectF(w * 0.72f, h * 0.60f, w * 0.92f, h * 0.82f), 30, 30, parkPaint);
    }

    private void drawRoadNetwork(Canvas canvas, int w, int h) {
        // Minor grid
        for (int x = -w; x < w * 2; x += 90) {
            canvas.drawLine(x, -h, x, h * 2, roadGridPaint);
        }
        for (int y = -h; y < h * 2; y += 90) {
            canvas.drawLine(-w, y, w * 2, y, roadGridPaint);
        }

        // Major diagonal avenues
        canvas.drawLine(-w * 0.2f, h * 0.85f, w * 1.2f, h * 0.15f, mainRoadPaint);
        canvas.drawLine(-w * 0.2f, h * 0.35f, w * 1.2f, h * 0.75f, mainRoadPaint);
        canvas.drawLine(w * 0.5f, -h * 0.5f, w * 0.5f, h * 1.5f, mainRoadPaint);
    }

    private void drawCrimeHeatmaps(Canvas canvas, int w, int h) {
        // High risk heatmap bloom (Red)
        RadialGradient redGradient = new RadialGradient(
                w * 0.35f, h * 0.68f, 130f,
                Color.parseColor("#4DEE4444"), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        );
        Paint heatPaintRed = new Paint(Paint.ANTI_ALIAS_FLAG);
        heatPaintRed.setShader(redGradient);
        canvas.drawCircle(w * 0.35f, h * 0.68f, 130f, heatPaintRed);

        // Moderate risk heatmap bloom (Amber)
        RadialGradient amberGradient = new RadialGradient(
                w * 0.75f, h * 0.35f, 110f,
                Color.parseColor("#3BF59E0B"), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        );
        Paint heatPaintAmber = new Paint(Paint.ANTI_ALIAS_FLAG);
        heatPaintAmber.setShader(amberGradient);
        canvas.drawCircle(w * 0.75f, h * 0.35f, 110f, heatPaintAmber);

        // Safe green safety zone
        RadialGradient safeGradient = new RadialGradient(
                w * 0.48f, h * 0.38f, 150f,
                Color.parseColor("#2B10B981"), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        );
        Paint heatPaintSafe = new Paint(Paint.ANTI_ALIAS_FLAG);
        heatPaintSafe.setShader(safeGradient);
        canvas.drawCircle(w * 0.48f, h * 0.38f, 150f, heatPaintSafe);
    }

    private void drawRoutes(Canvas canvas, int w, int h) {
        float startX = w * 0.22f;
        float startY = h * 0.74f;
        float endX = w * 0.78f;
        float endY = h * 0.26f;

        // Route 3: High Risk Shortcut (Red)
        Path riskPath = new Path();
        riskPath.moveTo(startX, startY);
        riskPath.cubicTo(w * 0.32f, h * 0.70f, w * 0.42f, h * 0.52f, w * 0.58f, h * 0.45f);
        riskPath.lineTo(endX, endY);

        // Route 2: Moderate Route (Amber)
        Path modPath = new Path();
        modPath.moveTo(startX, startY);
        modPath.lineTo(w * 0.45f, h * 0.74f);
        modPath.lineTo(w * 0.45f, h * 0.32f);
        modPath.lineTo(endX, endY);

        // Route 1: Safe Route (Green)
        Path safePath = new Path();
        safePath.moveTo(startX, startY);
        safePath.lineTo(w * 0.22f, h * 0.42f);
        safePath.cubicTo(w * 0.25f, h * 0.38f, w * 0.35f, h * 0.26f, w * 0.55f, h * 0.26f);
        safePath.lineTo(endX, endY);

        // Draw unselected paths first with dimming
        if (selectedRouteIndex != 2) {
            routeRiskPaint.setAlpha(100);
            canvas.drawPath(riskPath, routeRiskPaint);
        }
        if (selectedRouteIndex != 1) {
            routeModPaint.setAlpha(100);
            canvas.drawPath(modPath, routeModPaint);
        }
        if (selectedRouteIndex != 0) {
            routeSafePaint.setAlpha(100);
            canvas.drawPath(safePath, routeSafePaint);
        }

        // Draw selected route with bright glow
        if (selectedRouteIndex == 0) {
            routeSafePaint.setAlpha(255);
            routeGlowPaint.setColor(Color.parseColor("#5510B981"));
            routeGlowPaint.setStrokeWidth(26f);
            canvas.drawPath(safePath, routeGlowPaint);
            canvas.drawPath(safePath, routeSafePaint);
        } else if (selectedRouteIndex == 1) {
            routeModPaint.setAlpha(255);
            routeGlowPaint.setColor(Color.parseColor("#55F59E0B"));
            routeGlowPaint.setStrokeWidth(22f);
            canvas.drawPath(modPath, routeGlowPaint);
            canvas.drawPath(modPath, routeModPaint);
        } else if (selectedRouteIndex == 2) {
            routeRiskPaint.setAlpha(255);
            routeGlowPaint.setColor(Color.parseColor("#55EF4444"));
            routeGlowPaint.setStrokeWidth(20f);
            canvas.drawPath(riskPath, routeGlowPaint);
            canvas.drawPath(riskPath, routeRiskPaint);
        }
    }

    private void drawPoliceCheckpoints(Canvas canvas, int w, int h) {
        // Police station 1
        drawPoliceShield(canvas, w * 0.32f, h * 0.36f, "PCR 1");
        // Police station 2
        drawPoliceShield(canvas, w * 0.65f, h * 0.58f, "Police Stn");
    }

    private void drawPoliceShield(Canvas canvas, float cx, float cy, String label) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor("#06B6D4"));
        canvas.drawCircle(cx, cy, 14f, p);

        p.setColor(Color.parseColor("#0A0F1D"));
        canvas.drawCircle(cx, cy, 10f, p);

        p.setColor(Color.parseColor("#06B6D4"));
        p.setTextSize(18f);
        p.setFakeBoldText(true);
        canvas.drawText("P", cx - 5f, cy + 6f, p);

        Paint lblPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lblPaint.setColor(Color.parseColor("#67E8F9"));
        lblPaint.setTextSize(20f);
        canvas.drawText(label, cx - 25f, cy + 30f, lblPaint);
    }

    private void drawMarkers(Canvas canvas, int w, int h) {
        float startX = w * 0.22f;
        float startY = h * 0.74f;
        float endX = w * 0.78f;
        float endY = h * 0.26f;

        // GPS Pulsing rings
        pulsePaint.setColor(Color.parseColor("#10B981"));
        pulsePaint.setStyle(Paint.Style.FILL);
        pulsePaint.setAlpha((int) ((1f - pulseRadius) * 140));
        canvas.drawCircle(startX, startY, 20f + pulseRadius * 40f, pulsePaint);

        // Origin marker (Current GPS location)
        markerPaint.setColor(Color.parseColor("#10B981"));
        markerPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(startX, startY, 18f, markerPaint);

        markerPaint.setColor(Color.WHITE);
        canvas.drawCircle(startX, startY, 7f, markerPaint);

        // Start Label
        textPaint.setColor(Color.parseColor("#34D399"));
        String startText = originLocation != null ? originLocation.getArea() + " (You)" : "Current Location";
        canvas.drawText(startText, startX - 40f, startY + 40f, textPaint);

        // Destination Marker (Red Pin with Flag)
        markerPaint.setColor(Color.parseColor("#EF4444"));
        canvas.drawCircle(endX, endY, 18f, markerPaint);

        markerPaint.setColor(Color.WHITE);
        canvas.drawCircle(endX, endY, 7f, markerPaint);

        // Destination Label
        textPaint.setColor(Color.parseColor("#FCA5A5"));
        String destText = destinationLocation != null ? destinationLocation.getArea() : "Destination";
        canvas.drawText(destText, endX - 30f, endY - 26f, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int pointerIndex = event.getActionIndex();
                final float x = event.getX(pointerIndex);
                final float y = event.getY(pointerIndex);
                lastTouchX = x;
                lastTouchY = y;
                activePointerId = event.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex != -1) {
                    final float x = event.getX(pointerIndex);
                    final float y = event.getY(pointerIndex);

                    if (!scaleDetector.isInProgress()) {
                        final float dx = x - lastTouchX;
                        final float dy = y - lastTouchY;
                        offsetX += dx;
                        offsetY += dy;
                        invalidate();
                    }

                    lastTouchX = x;
                    lastTouchY = y;
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = event.getActionIndex();
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == activePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    lastTouchX = event.getX(newPointerIndex);
                    lastTouchY = event.getY(newPointerIndex);
                    activePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
    }
}

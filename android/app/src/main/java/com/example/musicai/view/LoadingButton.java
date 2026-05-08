package com.example.musicai.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.musicai.R;

public class LoadingButton extends View {
    
    private Paint backgroundPaint;
    private Paint textPaint;
    private Paint loadingPaint;
    private RectF backgroundRect;
    
    private boolean isLoading = false;
    private boolean enabled = true;
    private float loadingRotation = 0;
    private ValueAnimator loadingAnimator;
    
    private String normalText = "生成";
    private OnClickListener clickListener;
    
    public LoadingButton(Context context) {
        super(context);
        init();
    }
    
    public LoadingButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public LoadingButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(ContextCompat.getColor(getContext(), R.color.apple_accent));
        backgroundPaint.setStyle(Paint.Style.FILL);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(spToPx(15));
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        loadingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        loadingPaint.setColor(0xFFFFFFFF);
        loadingPaint.setStrokeWidth(3);
        loadingPaint.setStyle(Paint.Style.STROKE);
        
        backgroundRect = new RectF();
        
        setClickable(true);
        setMinimumHeight((int) dpToPx(44));
        setWillNotDraw(false);
        
        loadingAnimator = ValueAnimator.ofFloat(0, 360);
        loadingAnimator.setDuration(1000);
        loadingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        loadingAnimator.addUpdateListener(animation -> {
            loadingRotation = (float) animation.getAnimatedValue();
            invalidate();
        });
    }
    
    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }
    
    private float spToPx(float sp) {
        return sp * getContext().getResources().getDisplayMetrics().scaledDensity;
    }
    
    public void setText(String text) {
        this.normalText = text;
        invalidate();
    }
    
    public String getText() {
        return isLoading ? "正在生成中..." : normalText;
    }
    
    public void setLoading(boolean loading) {
        this.isLoading = loading;
        if (loading) {
            loadingAnimator.start();
            setEnabled(false);
        } else {
            loadingAnimator.cancel();
            loadingRotation = 0;
            setEnabled(enabled);
        }
        invalidate();
    }
    
    public boolean isLoadingState() {
        return isLoading;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        super.setEnabled(enabled);
        if (!isLoading) {
            backgroundPaint.setAlpha(enabled ? 255 : 128);
        }
        invalidate();
    }
    
    @Override
    public void setOnClickListener(OnClickListener listener) {
        this.clickListener = listener;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (int) dpToPx(160);
        int desiredHeight = (int) dpToPx(44);
        
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        
        int width = widthMode == MeasureSpec.EXACTLY ? widthSize : desiredWidth;
        int height = heightMode == MeasureSpec.EXACTLY ? heightSize : desiredHeight;
        
        setMeasuredDimension(width, height);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float cornerRadius = dpToPx(12);
        backgroundRect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint);
        
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        
        if (isLoading) {
            float radius = dpToPx(10);
            
            RectF arcRect = new RectF(centerX - dpToPx(50) - radius, centerY - radius, 
                    centerX - dpToPx(50) + radius, centerY + radius);
            canvas.save();
            canvas.rotate(loadingRotation, centerX - dpToPx(50), centerY);
            canvas.drawArc(arcRect, 0, 270, false, loadingPaint);
            canvas.restore();
            
            textPaint.setTextSize(spToPx(14));
            canvas.drawText("正在生成中...", centerX - dpToPx(10), centerY + dpToPx(5), textPaint);
        } else {
            textPaint.setTextSize(spToPx(15));
            canvas.drawText(normalText, centerX, centerY + dpToPx(5), textPaint);
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!enabled || isLoading) {
            return false;
        }
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                backgroundPaint.setAlpha(200);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                backgroundPaint.setAlpha(255);
                invalidate();
                if (clickListener != null) {
                    clickListener.onClick(this);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                backgroundPaint.setAlpha(255);
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (loadingAnimator != null) {
            loadingAnimator.cancel();
        }
    }
}

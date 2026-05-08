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

public class UnifiedPlaybackButton extends View {
    
    public enum State {
        PLAY,      
        PAUSE,    
        RESUME    
    }
    
    private Paint backgroundPaint;
    private Paint iconPaint;
    private Paint loadingPaint;
    private RectF backgroundRect;
    
    private State currentState = State.PLAY;
    private boolean isLoading = false;
    private float loadingRotation = 0;
    private ValueAnimator loadingAnimator;
    
    private OnPlaybackStateChangeListener listener;
    
    public interface OnPlaybackStateChangeListener {
        void onPlayClicked();
        void onPauseClicked();
        void onResumeClicked();
    }
    
    public UnifiedPlaybackButton(Context context) {
        super(context);
        init();
    }
    
    public UnifiedPlaybackButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public UnifiedPlaybackButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(ContextCompat.getColor(getContext(), R.color.apple_accent));
        backgroundPaint.setStyle(Paint.Style.FILL);
        
        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setColor(0xFFFFFFFF);
        iconPaint.setStrokeWidth(4);
        iconPaint.setStyle(Paint.Style.FILL);
        
        loadingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        loadingPaint.setColor(0xFFFFFFFF);
        loadingPaint.setStrokeWidth(3);
        loadingPaint.setStyle(Paint.Style.STROKE);
        
        backgroundRect = new RectF();
        
        setClickable(true);
        setMinimumHeight((int) dpToPx(44));
        setBackgroundColor(android.graphics.Color.TRANSPARENT);
        
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
    
    public void setOnPlaybackStateChangeListener(OnPlaybackStateChangeListener listener) {
        this.listener = listener;
    }
    
    public void setState(State state) {
        this.currentState = state;
        invalidate();
    }
    
    public State getState() {
        return currentState;
    }
    
    public void setLoading(boolean loading) {
        this.isLoading = loading;
        if (loading) {
            loadingAnimator.start();
        } else {
            loadingAnimator.cancel();
            loadingRotation = 0;
        }
        invalidate();
    }
    
    public boolean isLoading() {
        return isLoading;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (int) dpToPx(200);
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
        
        if (isLoading) {
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = dpToPx(12);
            
            RectF arcRect = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
            canvas.save();
            canvas.rotate(loadingRotation, centerX, centerY);
            canvas.drawArc(arcRect, 0, 270, false, loadingPaint);
            canvas.restore();
            
            String text = "正在生成中...";
            iconPaint.setTextSize(dpToPx(15));
            iconPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(text, dpToPx(36), centerY + dpToPx(5), iconPaint);
        } else {
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            
            iconPaint.setTextSize(dpToPx(20));
            iconPaint.setTextAlign(Paint.Align.CENTER);
            
            switch (currentState) {
                case PLAY:
                    String playIcon = "\u25B6"; 
                    String playText = "播放";
                    canvas.drawText(playIcon + " " + playText, centerX, centerY + dpToPx(5), iconPaint);
                    break;
                case PAUSE:
                    String pauseIcon = "\u23F8";
                    String pauseText = "暂停";
                    canvas.drawText(pauseIcon + " " + pauseText, centerX, centerY + dpToPx(5), iconPaint);
                    break;
                case RESUME:
                    String resumeIcon = "\u25B6";
                    String resumeText = "继续";
                    canvas.drawText(resumeIcon + " " + resumeText, centerX, centerY + dpToPx(5), iconPaint);
                    break;
            }
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isLoading) {
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
                if (listener != null) {
                    switch (currentState) {
                        case PLAY:
                            listener.onPlayClicked();
                            break;
                        case PAUSE:
                            listener.onPauseClicked();
                            break;
                        case RESUME:
                            listener.onResumeClicked();
                            break;
                    }
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

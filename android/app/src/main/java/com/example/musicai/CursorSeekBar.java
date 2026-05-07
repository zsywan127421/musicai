package com.example.musicai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class CursorSeekBar extends SeekBar {
    
    private Paint cursorPaint;
    private Paint cursorGlowPaint;
    private float cursorRadius = 12f;
    private float cursorGlowRadius = 18f;
    private int cursorColor = 0xFF007AFF;
    private boolean showCursor = true;
    
    private OnSeekBarChangeListenerWithCursor customListener;
    
    public CursorSeekBar(Context context) {
        super(context);
        init();
    }
    
    public CursorSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public CursorSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cursorPaint.setColor(cursorColor);
        cursorPaint.setStyle(Paint.Style.FILL);
        
        cursorGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cursorGlowPaint.setColor(cursorColor);
        cursorGlowPaint.setAlpha(80);
        cursorGlowPaint.setStyle(Paint.Style.FILL);
        
        setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (customListener != null) {
                    customListener.onProgressChanged(seekBar, progress, fromUser);
                }
                invalidate();
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (customListener != null) {
                    customListener.onStartTrackingTouch(seekBar);
                }
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (customListener != null) {
                    customListener.onStopTrackingTouch(seekBar);
                }
            }
        });
    }
    
    public void setCursorColor(int color) {
        this.cursorColor = color;
        cursorPaint.setColor(color);
        cursorGlowPaint.setColor(color);
        cursorGlowPaint.setAlpha(80);
        invalidate();
    }
    
    public void setShowCursor(boolean show) {
        this.showCursor = show;
        invalidate();
    }
    
    public void setCustomListener(OnSeekBarChangeListenerWithCursor listener) {
        this.customListener = listener;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (!showCursor) return;
        
        int progress = getProgress();
        int max = getMax();
        if (max == 0) return;
        
        float progressRatio = (float) progress / max;
        float thumbX = progressRatio * getWidth();
        float thumbY = getHeight() / 2f;
        
        cursorGlowPaint.setAlpha(60);
        canvas.drawCircle(thumbX, thumbY, cursorGlowRadius, cursorGlowPaint);
        
        cursorPaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(thumbX, thumbY, cursorRadius * 0.8f, cursorPaint);
        
        cursorPaint.setColor(cursorColor);
        canvas.drawCircle(thumbX, thumbY, cursorRadius * 0.6f, cursorPaint);
    }
    
    public interface OnSeekBarChangeListenerWithCursor {
        void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser);
        void onStartTrackingTouch(SeekBar seekBar);
        void onStopTrackingTouch(SeekBar seekBar);
    }
}

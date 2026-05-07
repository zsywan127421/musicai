package com.example.musicai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class WheelPickerView extends View {
    
    private Paint textPaint;
    private Paint selectedPaint;
    private Paint dividerPaint;
    
    private List<String> items = new ArrayList<>();
    private int selectedIndex = 0;
    
    private float itemHeight = 50f;
    private int visibleItems = 5;
    private float centerY;
    
    private Scroller scroller;
    private int scrollY = 0;
    private int lastY = 0;
    
    private OnSelectedListener listener;
    
    public interface OnSelectedListener {
        void onSelected(int index, String value);
    }
    
    public WheelPickerView(Context context) {
        super(context);
        init();
    }
    
    public WheelPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(dp2px(16));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.apple_text));
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPaint.setTextSize(dp2px(20));
        selectedPaint.setColor(ContextCompat.getColor(getContext(), R.color.apple_accent));
        selectedPaint.setTextAlign(Paint.Align.CENTER);
        selectedPaint.setFakeBoldText(true);
        
        dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(ContextCompat.getColor(getContext(), R.color.apple_separator));
        dividerPaint.setStrokeWidth(dp2px(1));
        
        scroller = new Scroller(getContext());
        
        setItems(new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"});
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        itemHeight = h / (float) visibleItems;
        centerY = h / 2f;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int firstVisible = (int) (scrollY / itemHeight);
        float offset = scrollY % itemHeight;
        
        for (int i = -visibleItems / 2; i <= visibleItems / 2 + 1; i++) {
            int index = firstVisible + i;
            if (index < 0 || index >= items.size()) continue;
            
            float y = centerY - offset + i * itemHeight;
            float distance = Math.abs(y - centerY);
            float scale = 1f - Math.min(distance / centerY, 0.5f);
            
            String text = items.get(index);
            boolean isSelected = (index == selectedIndex);
            
            Paint paint = isSelected ? selectedPaint : textPaint;
            
            float alpha = 1f - Math.min(distance / centerY, 0.7f);
            paint.setAlpha((int) (255 * alpha));
            
            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, text.length(), bounds);
            
            float textX = getWidth() / 2f;
            float textY = y + bounds.height() / 2f;
            
            canvas.drawText(text, textX, textY, paint);
            
            if (isSelected) {
                canvas.drawLine(dp2px(16), centerY - itemHeight / 2, getWidth() - dp2px(16), centerY - itemHeight / 2, dividerPaint);
                canvas.drawLine(dp2px(16), centerY + itemHeight / 2, getWidth() - dp2px(16), centerY + itemHeight / 2, dividerPaint);
            }
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastY = (int) event.getY();
                scroller.forceFinished(true);
                return true;
                
            case MotionEvent.ACTION_MOVE:
                int dy = (int) (lastY - event.getY());
                scrollY += dy;
                lastY = (int) event.getY();
                updateSelection();
                invalidate();
                return true;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                int velocity = 0;
                scroller.fling(0, scrollY, 0, (int) -event.getY() * 2, 0, 0, 0, (int) (items.size() * itemHeight));
                postInvalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
    
    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollY = scroller.getCurrY();
            updateSelection();
            invalidate();
        }
    }
    
    private void updateSelection() {
        int newIndex = Math.round(scrollY / itemHeight);
        newIndex = Math.max(0, Math.min(newIndex, items.size() - 1));
        
        if (newIndex != selectedIndex) {
            selectedIndex = newIndex;
            if (listener != null) {
                listener.onSelected(selectedIndex, items.get(selectedIndex));
            }
        }
    }
    
    public void setItems(String[] data) {
        items.clear();
        for (String item : data) {
            items.add(item);
        }
        selectedIndex = 0;
        scrollY = 0;
        invalidate();
    }
    
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < items.size()) {
            selectedIndex = index;
            scrollY = (int) (index * itemHeight);
            invalidate();
        }
    }
    
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    public String getSelectedValue() {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            return items.get(selectedIndex);
        }
        return "";
    }
    
    public void setOnSelectedListener(OnSelectedListener listener) {
        this.listener = listener;
    }
    
    private float dp2px(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }
}

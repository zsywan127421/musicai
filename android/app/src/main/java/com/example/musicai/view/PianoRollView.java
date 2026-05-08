package com.example.musicai.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.musicai.MusicData;
import com.example.musicai.R;
import com.example.musicai.util.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class PianoRollView extends View {
    
    private static final int NOTE_HEIGHT = 36;
    private static final int BEAT_WIDTH = 60;
    private static final int PIANO_KEY_WIDTH = 50;
    private static final int MIN_NOTE_WIDTH = 20;
    private static final int RESIZE_HANDLE_WIDTH = 12;
    private static final int MIN_OCTAVE = 2;
    private static final int MAX_OCTAVE = 7;
    
    private static final String[] PITCH_LABELS = {"B", "A#", "A", "G#", "G", "F#", "F", "E", "D#", "D", "C#", "C"};
    
    private Paint whiteKeyPaint;
    private Paint blackKeyPaint;
    private Paint gridPaint;
    private Paint notePaint;
    private Paint noteSelectedPaint;
    private Paint textPaint;
    private Paint beatTextPaint;
    private Paint playheadPaint;
    
    private List<MusicData.Note> notes = new ArrayList<>();
    private int minOctave = 3;
    private int maxOctave = 5;
    private int totalBeats = 32;
    private float scrollX = 0;
    private float scrollY = 0;
    private float scaleFactor = 1.0f;
    private int playheadPosition = 0;
    
    private int selectedNoteIndex = -1;
    private boolean isDragging = false;
    private boolean isResizing = false;
    private boolean isDraggingPlayhead = false;
    private boolean isPanning = false;
    private float dragStartX;
    private float dragStartY;
    private int draggedNoteIndex = -1;
    private int originalStartTime;
    private String originalPitch;
    private int originalOctave;
    private int originalDuration;
    private float originalNoteWidth;
    
    private OnNoteChangedListener listener;
    private OnPlayheadChangedListener playheadListener;
    
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    
    public interface OnNoteChangedListener {
        void onNoteChanged(int index, String pitch, int octave, int duration, int startTime);
        void onNoteSelected(int index);
    }
    
    public interface OnPlayheadChangedListener {
        void onPlayheadChanged(int position);
    }
    
    public PianoRollView(Context context) {
        super(context);
        init();
    }
    
    public PianoRollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public PianoRollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        boolean isDark = ThemeManager.isNightMode(getContext());
        
        whiteKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteKeyPaint.setColor(isDark ? 0xFF2C2C2E : 0xFFF5F5F5);
        whiteKeyPaint.setStyle(Paint.Style.FILL);
        
        blackKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackKeyPaint.setColor(isDark ? 0xFF1C1C1E : 0xFF333333);
        blackKeyPaint.setStyle(Paint.Style.FILL);
        
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(isDark ? 0xFF3C3C3E : 0xFFE0E0E0);
        gridPaint.setStrokeWidth(1);
        
        notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        notePaint.setColor(ContextCompat.getColor(getContext(), R.color.apple_accent));
        notePaint.setStyle(Paint.Style.FILL);
        
        noteSelectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        noteSelectedPaint.setColor(isDark ? 0xFF0A84FF : 0xFF0066CC);
        noteSelectedPaint.setStyle(Paint.Style.FILL);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(isDark ? 0xFFEBEBF5 : 0xFF666666);
        textPaint.setTextSize(20);
        
        beatTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        beatTextPaint.setColor(isDark ? 0xFF8E8E93 : 0xFF999999);
        beatTextPaint.setTextSize(24);
        beatTextPaint.setTextAlign(Paint.Align.CENTER);
        
        playheadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        playheadPaint.setColor(0xFFFF3B30);
        playheadPaint.setStrokeWidth(3);
        
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (!isDragging && !isResizing && !isDraggingPlayhead) {
                    scrollX += distanceX;
                    scrollY += distanceY;
                    constrainScroll();
                    invalidate();
                }
                return true;
            }
            
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                scrollToStart();
                return true;
            }
        });
        
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.3f, Math.min(3.0f, scaleFactor));
                invalidate();
                return true;
            }
        });
    }
    
    private void constrainScroll() {
        float maxScrollX = Math.max(0, (int)(totalBeats * BEAT_WIDTH * scaleFactor) - getWidth() + PIANO_KEY_WIDTH);
        float maxScrollY = Math.max(0, ((maxOctave - minOctave + 1) * 12 * NOTE_HEIGHT) - getHeight());
        
        scrollX = Math.max(0, Math.min(maxScrollX, scrollX));
        scrollY = Math.max(0, Math.min(maxScrollY, scrollY));
    }
    
    public void setOnPlayheadChangedListener(OnPlayheadChangedListener listener) {
        this.playheadListener = listener;
    }
    
    public void setPlayheadPosition(int position) {
        this.playheadPosition = position;
        invalidate();
    }
    
    public void setNotes(List<MusicData.Note> notes) {
        this.notes = notes != null ? notes : new ArrayList<>();
        selectedNoteIndex = -1;
        calculateNoteRanges();
        updateTotalBeats();
        autoScrollToNotes();
        invalidate();
    }
    
    private void calculateNoteRanges() {
        if (notes == null || notes.isEmpty()) {
            minOctave = 3;
            maxOctave = 5;
            return;
        }
        
        int minNoteOctave = 8;
        int maxNoteOctave = 0;
        
        for (MusicData.Note note : notes) {
            if (note.octave < minNoteOctave) minNoteOctave = note.octave;
            if (note.octave > maxNoteOctave) maxNoteOctave = note.octave;
        }
        
        minOctave = Math.max(MIN_OCTAVE, minNoteOctave - 1);
        maxOctave = Math.min(MAX_OCTAVE, maxNoteOctave + 1);
        
        if (maxOctave - minOctave < 3) {
            int center = (minOctave + maxOctave) / 2;
            minOctave = Math.max(MIN_OCTAVE, center - 2);
            maxOctave = Math.min(MAX_OCTAVE, center + 2);
        }
    }
    
    private void updateTotalBeats() {
        int maxEnd = 32;
        for (MusicData.Note note : notes) {
            int end = note.startTime + note.duration;
            if (end > maxEnd) maxEnd = end;
        }
        totalBeats = Math.max(32, maxEnd + 16);
    }
    
    private void autoScrollToNotes() {
        if (notes == null || notes.isEmpty()) {
            scrollX = 0;
            scrollY = 0;
            return;
        }
        
        int minStartTime = Integer.MAX_VALUE;
        for (MusicData.Note note : notes) {
            if (note.startTime < minStartTime) minStartTime = note.startTime;
        }
        
        scrollX = Math.max(0, (int)(minStartTime * BEAT_WIDTH * scaleFactor) - PIANO_KEY_WIDTH);
        
        int centerOctave = (minOctave + maxOctave) / 2;
        int centerRow = (MAX_OCTAVE - centerOctave) * 12;
        float centerY = centerRow * NOTE_HEIGHT - getHeight() / 2f;
        scrollY = Math.max(0, (int) centerY);
    }
    
    public void setOnNoteChangedListener(OnNoteChangedListener listener) {
        this.listener = listener;
    }
    
    public void setScaleFactor(float scale) {
        this.scaleFactor = Math.max(0.3f, Math.min(3.0f, scale));
        constrainScroll();
        invalidate();
    }
    
    public void scrollToStart() {
        scrollX = 0;
        autoScrollToNotes();
        invalidate();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int totalHeight = (maxOctave - minOctave + 1) * 12 * NOTE_HEIGHT;
        int height = Math.max(MeasureSpec.getSize(heightMeasureSpec), totalHeight);
        setMeasuredDimension(width, Math.max(height, 400));
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float scaledBeatWidth = BEAT_WIDTH * scaleFactor;
        int totalNotes = (maxOctave - minOctave + 1) * 12;
        int viewHeight = totalNotes * NOTE_HEIGHT;
        
        canvas.drawRect(PIANO_KEY_WIDTH, 0, getWidth(), viewHeight, whiteKeyPaint);
        
        for (int i = 0; i < totalNotes; i++) {
            int y = i * NOTE_HEIGHT - (int) scrollY;
            if (y < -NOTE_HEIGHT || y > getHeight()) continue;
            
            boolean isBlackKey = isBlackKey(i);
            if (isBlackKey) {
                canvas.drawRect(PIANO_KEY_WIDTH, y, getWidth(), y + NOTE_HEIGHT, blackKeyPaint);
            }
        }
        
        canvas.drawLine(PIANO_KEY_WIDTH, 0, PIANO_KEY_WIDTH, viewHeight, gridPaint);
        
        for (int octave = 0; octave <= maxOctave - minOctave; octave++) {
            int y = (maxOctave - minOctave - octave) * 12 * NOTE_HEIGHT - (int) scrollY;
            if (y < -NOTE_HEIGHT || y > getHeight()) continue;
            canvas.drawLine(PIANO_KEY_WIDTH, y, getWidth(), y, gridPaint);
            
            String octaveLabel = "C" + (minOctave + octave);
            canvas.drawText(octaveLabel, PIANO_KEY_WIDTH / 2, y + NOTE_HEIGHT / 2 + 6, textPaint);
        }
        
        for (int beat = 0; beat <= totalBeats; beat++) {
            int x = PIANO_KEY_WIDTH + (int)(beat * scaledBeatWidth) - (int) scrollX;
            if (x < PIANO_KEY_WIDTH || x > getWidth()) continue;
            
            Paint linePaint = new Paint(gridPaint);
            if (beat % 4 == 0) {
                linePaint.setStrokeWidth(2);
            } else {
                linePaint.setStrokeWidth(1);
                linePaint.setAlpha(80);
            }
            canvas.drawLine(x, 0, x, viewHeight, linePaint);
            
            if (beat % 4 == 0) {
                canvas.drawText(String.valueOf(beat + 1), x, beatTextPaint.getTextSize() + 4, beatTextPaint);
            }
        }
        
        int playheadX = PIANO_KEY_WIDTH + (int)(playheadPosition * scaledBeatWidth) - (int) scrollX;
        if (playheadX >= PIANO_KEY_WIDTH && playheadX <= getWidth()) {
            canvas.drawLine(playheadX, 0, playheadX, viewHeight, playheadPaint);
        }
        
        for (int i = 0; i < notes.size(); i++) {
            MusicData.Note note = notes.get(i);
            drawNote(canvas, note, i, i == selectedNoteIndex, scaledBeatWidth);
        }
    }
    
    private void drawNote(Canvas canvas, MusicData.Note note, int index, boolean isSelected, float beatWidth) {
        int pitchIndex = getPitchIndex(note.pitch);
        if (pitchIndex < 0) return;
        
        int noteRow = (note.octave - minOctave) * 12 + (11 - pitchIndex);
        if (noteRow < 0 || noteRow >= (maxOctave - minOctave + 1) * 12) return;
        
        int noteLeft = PIANO_KEY_WIDTH + (int)(note.startTime * beatWidth) - (int) scrollX;
        int noteTop = noteRow * NOTE_HEIGHT - (int) scrollY;
        int noteWidth = Math.max((int)(note.duration * beatWidth), MIN_NOTE_WIDTH);
        
        if (noteLeft + noteWidth < PIANO_KEY_WIDTH || noteLeft > getWidth()) return;
        if (noteTop + NOTE_HEIGHT < 0 || noteTop > getHeight()) return;
        
        Paint paint = isSelected ? noteSelectedPaint : notePaint;
        RectF rect = new RectF(noteLeft, noteTop + 2, noteLeft + noteWidth - 2, noteTop + NOTE_HEIGHT - 2);
        canvas.drawRoundRect(rect, 4, 4, paint);
        
        if (isSelected) {
            Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            handlePaint.setColor(0xFFFFFFFF);
            handlePaint.setAlpha(180);
            int handleWidth = Math.min(RESIZE_HANDLE_WIDTH, noteWidth / 4);
            canvas.drawRoundRect(
                new RectF(noteLeft + noteWidth - handleWidth - 2, noteTop + 4, 
                         noteLeft + noteWidth - 2, noteTop + NOTE_HEIGHT - 4),
                4, 4, handlePaint);
        }
        
        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFFFFFFFF);
        labelPaint.setTextSize(16);
        String label = note.pitch + note.octave;
        if (noteWidth > 40) {
            canvas.drawText(label, noteLeft + 4, noteTop + NOTE_HEIGHT / 2 + 5, labelPaint);
        }
    }
    
    private boolean isBlackKey(int rowInOctave) {
        int pitch = 11 - (rowInOctave % 12);
        return pitch == 1 || pitch == 3 || pitch == 6 || pitch == 8 || pitch == 10;
    }
    
    private int getPitchIndex(String pitch) {
        for (int i = 0; i < PITCH_LABELS.length; i++) {
            if (PITCH_LABELS[i].equals(pitch)) {
                return 11 - i;
            }
        }
        return -1;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        
        float scaledBeatWidth = BEAT_WIDTH * scaleFactor;
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getPointerCount() == 1) {
                    int noteIndex = findNoteAt(event.getX(), event.getY());
                    if (noteIndex >= 0) {
                        MusicData.Note note = notes.get(noteIndex);
                        int noteLeft = PIANO_KEY_WIDTH + (int)(note.startTime * scaledBeatWidth);
                        int noteWidth = Math.max((int)(note.duration * scaledBeatWidth), MIN_NOTE_WIDTH);
                        
                        if (event.getX() > noteLeft + noteWidth - RESIZE_HANDLE_WIDTH * 2) {
                            selectedNoteIndex = noteIndex;
                            isResizing = true;
                            draggedNoteIndex = noteIndex;
                            originalStartTime = note.startTime;
                            originalDuration = note.duration;
                            originalNoteWidth = noteWidth;
                            dragStartX = event.getX();
                        } else {
                            selectedNoteIndex = noteIndex;
                            isDragging = true;
                            draggedNoteIndex = noteIndex;
                            originalStartTime = note.startTime;
                            originalPitch = note.pitch;
                            originalOctave = note.octave;
                            dragStartX = event.getX();
                            dragStartY = event.getY();
                        }
                        
                        if (listener != null) {
                            listener.onNoteSelected(noteIndex);
                        }
                    } else {
                        selectedNoteIndex = -1;
                        isDraggingPlayhead = true;
                        dragStartX = event.getX();
                    }
                    invalidate();
                }
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() > 1) {
                    return true;
                }
                
                if (isResizing && draggedNoteIndex >= 0) {
                    float deltaX = event.getX() - dragStartX;
                    int beatDelta = Math.round(deltaX / scaledBeatWidth);
                    MusicData.Note note = notes.get(draggedNoteIndex);
                    note.duration = Math.max(1, originalDuration + beatDelta);
                    
                    if (listener != null) {
                        listener.onNoteChanged(draggedNoteIndex, note.pitch, note.octave, note.duration, note.startTime);
                    }
                    invalidate();
                } else if (isDragging && draggedNoteIndex >= 0) {
                    float deltaX = event.getX() - dragStartX;
                    float deltaY = event.getY() - dragStartY;
                    
                    MusicData.Note note = notes.get(draggedNoteIndex);
                    
                    int timeDelta = Math.round(deltaX / scaledBeatWidth);
                    note.startTime = Math.max(0, originalStartTime + timeDelta);
                    
                    int rowDelta = Math.round(-deltaY / NOTE_HEIGHT);
                    int noteRow = (originalOctave - minOctave) * 12 + (11 - getPitchIndex(originalPitch));
                    int newRow = Math.max(0, Math.min((maxOctave - minOctave + 1) * 12 - 1, noteRow + rowDelta));
                    
                    int newOctave = minOctave + newRow / 12;
                    int newPitchIndex = 11 - (newRow % 12);
                    String newPitch = PITCH_LABELS[11 - newPitchIndex];
                    
                    note.pitch = newPitch;
                    note.octave = Math.max(MIN_OCTAVE, Math.min(MAX_OCTAVE, newOctave));
                    
                    if (listener != null) {
                        listener.onNoteChanged(draggedNoteIndex, note.pitch, note.octave, note.duration, note.startTime);
                    }
                    
                    invalidate();
                } else if (isDraggingPlayhead) {
                    float x = event.getX();
                    if (x > PIANO_KEY_WIDTH) {
                        int newPosition = Math.round((x - PIANO_KEY_WIDTH + scrollX) / scaledBeatWidth);
                        playheadPosition = Math.max(0, newPosition);
                        if (playheadListener != null) {
                            playheadListener.onPlayheadChanged(playheadPosition);
                        }
                        invalidate();
                    }
                }
                return true;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                isResizing = false;
                isDraggingPlayhead = false;
                isPanning = false;
                draggedNoteIndex = -1;
                return true;
        }
        return super.onTouchEvent(event);
    }
    
    private int findNoteAt(float x, float y) {
        float scaledBeatWidth = BEAT_WIDTH * scaleFactor;
        for (int i = 0; i < notes.size(); i++) {
            MusicData.Note note = notes.get(i);
            int pitchIndex = getPitchIndex(note.pitch);
            if (pitchIndex < 0) continue;
            
            int noteRow = (note.octave - minOctave) * 12 + (11 - pitchIndex);
            if (noteRow < 0 || noteRow >= (maxOctave - minOctave + 1) * 12) continue;
            
            int noteLeft = PIANO_KEY_WIDTH + (int)(note.startTime * scaledBeatWidth);
            int noteWidth = Math.max((int)(note.duration * scaledBeatWidth), MIN_NOTE_WIDTH);
            
            if (x >= noteLeft && x <= noteLeft + noteWidth && 
                y >= noteRow * NOTE_HEIGHT - scrollY && 
                y <= noteRow * NOTE_HEIGHT - scrollY + NOTE_HEIGHT) {
                return i;
            }
        }
        return -1;
    }
    
    public int getSelectedNoteIndex() {
        return selectedNoteIndex;
    }

    public void setStartOctave(int octave) {
        this.minOctave = octave;
        if (octave > maxOctave - 1) maxOctave = octave + 1;
        invalidate();
    }
}

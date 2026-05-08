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

import java.util.ArrayList;
import java.util.List;

public class PianoRollView extends View {
    
    private static final int NOTE_HEIGHT = 30;
    private static final int BEAT_WIDTH = 80;
    private static final int PIANO_KEY_WIDTH = 60;
    private static final int MIN_NOTE_WIDTH = 20;
    
    private static final String[] PITCH_LABELS = {"B", "A#", "A", "G#", "G", "F#", "F", "E", "D#", "D", "C#", "C"};
    
    private Paint whiteKeyPaint;
    private Paint blackKeyPaint;
    private Paint gridPaint;
    private Paint notePaint;
    private Paint noteSelectedPaint;
    private Paint textPaint;
    private Paint beatTextPaint;
    
    private List<MusicData.Note> notes = new ArrayList<>();
    private int startOctave = 3;
    private int numOctaves = 4;
    private float scrollX = 0;
    private float scrollY = 0;
    private float scaleFactor = 1.0f;
    
    private int selectedNoteIndex = -1;
    private boolean isDragging = false;
    private float dragStartX;
    private float dragStartY;
    private int draggedNoteIndex = -1;
    private int originalStartTime;
    private String originalPitch;
    private int originalOctave;
    
    private OnNoteChangedListener listener;
    
    public interface OnNoteChangedListener {
        void onNoteChanged(int index, String pitch, int octave, int duration, int startTime);
        void onNoteSelected(int index);
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
        whiteKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteKeyPaint.setColor(0xFFF5F5F5);
        whiteKeyPaint.setStyle(Paint.Style.FILL);
        
        blackKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackKeyPaint.setColor(0xFF333333);
        blackKeyPaint.setStyle(Paint.Style.FILL);
        
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0xFFE0E0E0);
        gridPaint.setStrokeWidth(1);
        
        notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        notePaint.setColor(ContextCompat.getColor(getContext(), R.color.apple_accent));
        notePaint.setStyle(Paint.Style.FILL);
        
        noteSelectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        noteSelectedPaint.setColor(0xFF0066CC);
        noteSelectedPaint.setStyle(Paint.Style.FILL);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF666666);
        textPaint.setTextSize(24);
        
        beatTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        beatTextPaint.setColor(0xFF999999);
        beatTextPaint.setTextSize(28);
        beatTextPaint.setTextAlign(Paint.Align.CENTER);
    }
    
    public void setNotes(List<MusicData.Note> notes) {
        this.notes = notes != null ? notes : new ArrayList<>();
        selectedNoteIndex = -1;
        invalidate();
    }
    
    public void setOnNoteChangedListener(OnNoteChangedListener listener) {
        this.listener = listener;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int totalHeight = numOctaves * 12 * NOTE_HEIGHT;
        int height = Math.max(MeasureSpec.getSize(heightMeasureSpec), totalHeight);
        setMeasuredDimension(width, height);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int pianoRollWidth = getWidth() - PIANO_KEY_WIDTH;
        int totalNotes = numOctaves * 12;
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
        
        for (int octave = 0; octave <= numOctaves; octave++) {
            int y = (numOctaves - octave) * 12 * NOTE_HEIGHT - (int) scrollY;
            if (y < -NOTE_HEIGHT || y > getHeight()) continue;
            canvas.drawLine(PIANO_KEY_WIDTH, y, getWidth(), y, gridPaint);
            
            String octaveLabel = "C" + (startOctave + octave);
            canvas.drawText(octaveLabel, PIANO_KEY_WIDTH / 2, y + NOTE_HEIGHT / 2 + 8, textPaint);
        }
        
        for (int beat = 0; beat <= 32; beat++) {
            int x = PIANO_KEY_WIDTH + beat * BEAT_WIDTH - (int) scrollX;
            if (x < PIANO_KEY_WIDTH || x > getWidth()) continue;
            
            canvas.drawLine(x, 0, x, viewHeight, beat % 4 == 0 ? gridPaint : gridPaint);
            
            if (beat % 4 == 0) {
                canvas.drawText(String.valueOf(beat + 1), x, beatTextPaint.getTextSize() + 4, beatTextPaint);
            }
        }
        
        for (int i = 0; i < notes.size(); i++) {
            MusicData.Note note = notes.get(i);
            drawNote(canvas, note, i, i == selectedNoteIndex);
        }
    }
    
    private void drawNote(Canvas canvas, MusicData.Note note, int index, boolean isSelected) {
        int pitchIndex = getPitchIndex(note.pitch);
        if (pitchIndex < 0) return;
        
        int noteRow = (note.octave - startOctave) * 12 + (11 - pitchIndex);
        if (noteRow < 0 || noteRow >= numOctaves * 12) return;
        
        int noteLeft = PIANO_KEY_WIDTH + note.startTime * BEAT_WIDTH / 4 - (int) scrollX;
        int noteTop = noteRow * NOTE_HEIGHT - (int) scrollY;
        int noteWidth = Math.max(note.duration * BEAT_WIDTH / 4, MIN_NOTE_WIDTH);
        
        if (noteLeft + noteWidth < PIANO_KEY_WIDTH || noteLeft > getWidth()) return;
        if (noteTop + NOTE_HEIGHT < 0 || noteTop > getHeight()) return;
        
        Paint paint = isSelected ? noteSelectedPaint : notePaint;
        RectF rect = new RectF(noteLeft, noteTop + 2, noteLeft + noteWidth - 2, noteTop + NOTE_HEIGHT - 2);
        canvas.drawRoundRect(rect, 6, 6, paint);
        
        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFFFFFFFF);
        labelPaint.setTextSize(20);
        String label = note.pitch + note.octave;
        if (noteWidth > 50) {
            canvas.drawText(label, noteLeft + 8, noteTop + NOTE_HEIGHT / 2 + 6, labelPaint);
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
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int noteIndex = findNoteAt(event.getX(), event.getY());
                if (noteIndex >= 0) {
                    selectedNoteIndex = noteIndex;
                    isDragging = true;
                    draggedNoteIndex = noteIndex;
                    MusicData.Note note = notes.get(noteIndex);
                    originalStartTime = note.startTime;
                    originalPitch = note.pitch;
                    originalOctave = note.octave;
                    dragStartX = event.getX();
                    dragStartY = event.getY();
                    if (listener != null) {
                        listener.onNoteSelected(noteIndex);
                    }
                } else {
                    selectedNoteIndex = -1;
                }
                invalidate();
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (isDragging && draggedNoteIndex >= 0) {
                    float deltaX = event.getX() - dragStartX;
                    float deltaY = event.getY() - dragStartY;
                    
                    MusicData.Note note = notes.get(draggedNoteIndex);
                    
                    int timeDelta = Math.round(deltaX / (BEAT_WIDTH / 4.0f));
                    note.startTime = Math.max(0, originalStartTime + timeDelta);
                    
                    int rowDelta = Math.round(-deltaY / NOTE_HEIGHT);
                    int noteRow = (originalOctave - startOctave) * 12 + (11 - getPitchIndex(originalPitch));
                    int newRow = Math.max(0, Math.min(numOctaves * 12 - 1, noteRow + rowDelta));
                    
                    int newOctave = startOctave + newRow / 12;
                    int newPitchIndex = 11 - (newRow % 12);
                    String newPitch = PITCH_LABELS[11 - newPitchIndex];
                    
                    note.pitch = newPitch;
                    note.octave = Math.max(1, Math.min(7, newOctave));
                    
                    if (listener != null) {
                        listener.onNoteChanged(draggedNoteIndex, note.pitch, note.octave, note.duration, note.startTime);
                    }
                    
                    invalidate();
                }
                return true;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                draggedNoteIndex = -1;
                return true;
        }
        return super.onTouchEvent(event);
    }
    
    private int findNoteAt(float x, float y) {
        for (int i = 0; i < notes.size(); i++) {
            MusicData.Note note = notes.get(i);
            int pitchIndex = getPitchIndex(note.pitch);
            if (pitchIndex < 0) continue;
            
            int noteRow = (note.octave - startOctave) * 12 + (11 - pitchIndex);
            if (noteRow < 0 || noteRow >= numOctaves * 12) continue;
            
            int noteLeft = PIANO_KEY_WIDTH + note.startTime * BEAT_WIDTH / 4 - (int) scrollX;
            int noteTop = noteRow * NOTE_HEIGHT - (int) scrollY;
            int noteWidth = Math.max(note.duration * BEAT_WIDTH / 4, MIN_NOTE_WIDTH);
            
            if (x >= noteLeft && x <= noteLeft + noteWidth && y >= noteTop && y <= noteTop + NOTE_HEIGHT) {
                return i;
            }
        }
        return -1;
    }
    
    public void scrollToStart() {
        scrollX = 0;
        scrollY = 0;
        invalidate();
    }
    
    public int getSelectedNoteIndex() {
        return selectedNoteIndex;
    }
    
    public void setStartOctave(int octave) {
        this.startOctave = octave;
        invalidate();
    }
}

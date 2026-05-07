package com.example.musicai.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.musicai.R;
import com.example.musicai.WheelPickerView;
import com.example.musicai.MusicRepository;

public class NoteEditBottomSheet {
    
    private static final String[] PITCHES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] OCTAVES = {"2", "3", "4", "5", "6", "7"};
    private static final String[] DURATIONS = {"1", "2", "4", "8", "16"};
    
    public static void show(Context context, int index, MusicRepository.NoteData note, OnNoteUpdateListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet_note_wheel);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setWindowAnimations(android.R.style.Animation_Dialog);
        }
        
        WheelPickerView wheelPitch = dialog.findViewById(R.id.wheel_pitch);
        WheelPickerView wheelOctave = dialog.findViewById(R.id.wheel_octave);
        WheelPickerView wheelDuration = dialog.findViewById(R.id.wheel_duration);
        TextView tvTitle = dialog.findViewById(R.id.tv_title);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        View btnConfirm = dialog.findViewById(R.id.btn_confirm);
        
        tvTitle.setText("编辑音符 #" + (index + 1));
        
        wheelPitch.setItems(PITCHES);
        wheelOctave.setItems(OCTAVES);
        wheelDuration.setItems(DURATIONS);
        
        int pitchIndex = getIndexOf(PITCHES, note.pitch);
        int octaveIndex = getIndexOf(OCTAVES, String.valueOf(note.octave));
        int durationIndex = getIndexOf(DURATIONS, String.valueOf(note.duration));
        
        wheelPitch.setSelectedIndex(pitchIndex >= 0 ? pitchIndex : 0);
        wheelOctave.setSelectedIndex(octaveIndex >= 0 ? octaveIndex : 2);
        wheelDuration.setSelectedIndex(durationIndex >= 0 ? durationIndex : 2);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            MusicRepository.NoteData updatedNote = new MusicRepository.NoteData();
            updatedNote.pitch = wheelPitch.getSelectedValue();
            updatedNote.octave = Integer.parseInt(wheelOctave.getSelectedValue());
            updatedNote.duration = Integer.parseInt(wheelDuration.getSelectedValue());
            updatedNote.startTime = note.startTime;
            
            listener.onUpdated(updatedNote);
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private static int getIndexOf(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }
    
    public interface OnNoteUpdateListener {
        void onUpdated(MusicRepository.NoteData updatedNote);
    }
}

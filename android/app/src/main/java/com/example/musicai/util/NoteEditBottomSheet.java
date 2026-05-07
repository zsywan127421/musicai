package com.example.musicai.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.musicai.MusicRepository;
import com.example.musicai.R;

public class NoteEditBottomSheet {
    
    private static final String[] PITCHES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] OCTAVES = {"2", "3", "4", "5", "6", "7"};
    private static final String[] DURATIONS = {"1", "2", "4", "8", "16"};
    
    public interface OnSaveListener {
        void onSave(MusicRepository.NoteData note);
    }
    
    public static void show(Context context, int index, MusicRepository.NoteData note, OnSaveListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_edit_note, null);
        dialog.setContentView(view);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setWindowAnimations(android.R.style.Animation_Dialog);
        }
        
        TextView tvNoteInfo = view.findViewById(R.id.tv_note_info);
        Spinner spPitch = view.findViewById(R.id.sp_pitch);
        Spinner spOctave = view.findViewById(R.id.sp_octave);
        Spinner spDuration = view.findViewById(R.id.sp_duration);
        Button btnSave = view.findViewById(R.id.btn_save);
        ImageButton btnClose = view.findViewById(R.id.btn_close);
        
        tvNoteInfo.setText(String.format("%d. %s%d | 时值:%d", index + 1, note.pitch, note.octave, note.duration));
        
        ArrayAdapter<String> pitchAdapter = new ArrayAdapter<>(context, R.layout.spinner_item, PITCHES);
        pitchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spPitch.setAdapter(pitchAdapter);
        for (int i = 0; i < PITCHES.length; i++) {
            if (PITCHES[i].equals(note.pitch)) {
                spPitch.setSelection(i);
                break;
            }
        }
        
        ArrayAdapter<String> octaveAdapter = new ArrayAdapter<>(context, R.layout.spinner_item, OCTAVES);
        octaveAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spOctave.setAdapter(octaveAdapter);
        for (int i = 0; i < OCTAVES.length; i++) {
            if (OCTAVES[i].equals(String.valueOf(note.octave))) {
                spOctave.setSelection(i);
                break;
            }
        }
        
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(context, R.layout.spinner_item, DURATIONS);
        durationAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spDuration.setAdapter(durationAdapter);
        for (int i = 0; i < DURATIONS.length; i++) {
            if (DURATIONS[i].equals(String.valueOf(note.duration))) {
                spDuration.setSelection(i);
                break;
            }
        }
        
        btnSave.setOnClickListener(v -> {
            note.pitch = PITCHES[spPitch.getSelectedItemPosition()];
            note.octave = Integer.parseInt(OCTAVES[spOctave.getSelectedItemPosition()]);
            note.duration = Integer.parseInt(DURATIONS[spDuration.getSelectedItemPosition()]);
            listener.onSave(note);
            dialog.dismiss();
        });
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
}

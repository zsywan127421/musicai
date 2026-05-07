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

import com.example.musicai.MusicRepository;
import com.example.musicai.R;

public class ChordEditBottomSheet {
    
    private static final String[] CHORD_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] CHORD_TYPES = {"major", "minor", "seventh", "diminished", "augmented", "sus2", "sus4"};
    private static final String[] DURATIONS = {"1", "2", "4", "8", "16"};
    
    public interface OnSaveListener {
        void onSave(MusicRepository.ChordData chord);
    }
    
    public static void show(Context context, int index, MusicRepository.ChordData chord, OnSaveListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_edit_chord, null);
        dialog.setContentView(view);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setWindowAnimations(android.R.style.Animation_Dialog);
        }
        
        TextView tvChordInfo = view.findViewById(R.id.tv_chord_info);
        Spinner spName = view.findViewById(R.id.sp_chord_name);
        Spinner spType = view.findViewById(R.id.sp_chord_type);
        Spinner spDuration = view.findViewById(R.id.sp_chord_duration);
        Button btnSave = view.findViewById(R.id.btn_save);
        ImageButton btnClose = view.findViewById(R.id.btn_close);
        
        tvChordInfo.setText(String.format("%d. %s %s | 时值:%d", index + 1, chord.name, chord.type, chord.duration));
        
        ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(context, R.layout.spinner_item, CHORD_NAMES);
        nameAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spName.setAdapter(nameAdapter);
        for (int i = 0; i < CHORD_NAMES.length; i++) {
            if (CHORD_NAMES[i].equals(chord.name)) {
                spName.setSelection(i);
                break;
            }
        }
        
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(context, R.layout.spinner_item, CHORD_TYPES);
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spType.setAdapter(typeAdapter);
        for (int i = 0; i < CHORD_TYPES.length; i++) {
            if (CHORD_TYPES[i].equals(chord.type)) {
                spType.setSelection(i);
                break;
            }
        }
        
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(context, R.layout.spinner_item, DURATIONS);
        durationAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spDuration.setAdapter(durationAdapter);
        for (int i = 0; i < DURATIONS.length; i++) {
            if (DURATIONS[i].equals(String.valueOf(chord.duration))) {
                spDuration.setSelection(i);
                break;
            }
        }
        
        btnSave.setOnClickListener(v -> {
            chord.name = CHORD_NAMES[spName.getSelectedItemPosition()];
            chord.type = CHORD_TYPES[spType.getSelectedItemPosition()];
            chord.duration = Integer.parseInt(DURATIONS[spDuration.getSelectedItemPosition()]);
            listener.onSave(chord);
            dialog.dismiss();
        });
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
}

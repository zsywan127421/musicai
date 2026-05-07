package com.example.musicai.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.musicai.R;
import com.example.musicai.WheelPickerView;
import com.example.musicai.MusicRepository;

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
        dialog.setContentView(R.layout.bottom_sheet_chord_wheel);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setWindowAnimations(android.R.style.Animation_Dialog);
        }
        
        TextView tvTitle = dialog.findViewById(R.id.tv_title);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        WheelPickerView wheelName = dialog.findViewById(R.id.wheel_name);
        WheelPickerView wheelType = dialog.findViewById(R.id.wheel_type);
        WheelPickerView wheelDuration = dialog.findViewById(R.id.wheel_duration);
        View btnConfirm = dialog.findViewById(R.id.btn_confirm);
        
        tvTitle.setText("编辑和弦 #" + (index + 1));
        
        wheelName.setItems(CHORD_NAMES);
        wheelType.setItems(CHORD_TYPES);
        wheelDuration.setItems(DURATIONS);
        
        int nameIndex = getIndexOf(CHORD_NAMES, chord.name);
        int typeIndex = getIndexOf(CHORD_TYPES, chord.type);
        int durationIndex = getIndexOf(DURATIONS, String.valueOf(chord.duration));
        
        wheelName.setSelectedIndex(nameIndex >= 0 ? nameIndex : 0);
        wheelType.setSelectedIndex(typeIndex >= 0 ? typeIndex : 0);
        wheelDuration.setSelectedIndex(durationIndex >= 0 ? durationIndex : 2);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            chord.name = wheelName.getSelectedValue();
            chord.type = wheelType.getSelectedValue();
            chord.duration = Integer.parseInt(wheelDuration.getSelectedValue());
            listener.onSave(chord);
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
}

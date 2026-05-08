package com.example.musicai.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.musicai.R;

public class MetronomeBottomSheet {
    
    private Dialog dialog;
    private Metronome metronome;
    private TextView tvBpm;
    private TextView tvBeatIndicators;
    private SeekBar seekBarBpm;
    private Button btnStartStop;
    private LinearLayout beatIndicatorContainer;
    
    private int[] beatViews;
    private int currentBeat = 0;
    
    public interface OnDismissListener {
        void onDismiss();
    }
    
    private OnDismissListener dismissListener;
    
    public MetronomeBottomSheet(Context context) {
        metronome = new Metronome();
        
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet_metronome);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setWindowAnimations(android.R.style.Animation_Dialog);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.4f);
        }
        
        initViews(context);
        setupListeners();
        
        metronome.setListener(new Metronome.MetronomeListener() {
            @Override
            public void onBeat(int beat, boolean isDownbeat) {
                updateBeatIndicator(beat, isDownbeat);
            }
            
            @Override
            public void onTempoChanged(int bpm) {
                tvBpm.setText(bpm + " BPM");
            }
        });
    }
    
    private void initViews(Context context) {
        tvBpm = dialog.findViewById(R.id.tv_bpm);
        tvBeatIndicators = dialog.findViewById(R.id.tv_beat_indicators);
        seekBarBpm = dialog.findViewById(R.id.seekbar_bpm);
        btnStartStop = dialog.findViewById(R.id.btn_start_stop);
        beatIndicatorContainer = dialog.findViewById(R.id.beat_indicator_container);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        Button btnDecrease = dialog.findViewById(R.id.btn_decrease);
        Button btnIncrease = dialog.findViewById(R.id.btn_increase);
        
        btnClose.setOnClickListener(v -> dismiss());
        
        tvBpm.setText(metronome.getBpm() + " BPM");
        seekBarBpm.setMax(280);
        seekBarBpm.setProgress(metronome.getBpm() - 20);
        
        updateBeatIndicators();
    }
    
    private void setupListeners() {
        btnStartStop.setOnClickListener(v -> toggleMetronome());
        
        seekBarBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int bpm = progress + 20;
                metronome.setBpm(bpm);
                tvBpm.setText(bpm + " BPM");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        dialog.findViewById(R.id.btn_decrease).setOnClickListener(v -> {
            int bpm = metronome.getBpm();
            if (bpm > 20) {
                metronome.setBpm(bpm - 1);
                seekBarBpm.setProgress(bpm - 20);
                tvBpm.setText((bpm - 1) + " BPM");
            }
        });
        
        dialog.findViewById(R.id.btn_increase).setOnClickListener(v -> {
            int bpm = metronome.getBpm();
            if (bpm < 300) {
                metronome.setBpm(bpm + 1);
                seekBarBpm.setProgress(bpm + 1 - 20);
                tvBpm.setText((bpm + 1) + " BPM");
            }
        });
    }
    
    private void updateBeatIndicators() {
        beatIndicatorContainer.removeAllViews();
        beatViews = new int[metronome.getBeatsPerMeasure()];
        
        int indicatorSize = (int) (24 * dialog.getContext().getResources().getDisplayMetrics().density);
        int margin = (int) (4 * dialog.getContext().getResources().getDisplayMetrics().density);
        
        for (int i = 0; i < metronome.getBeatsPerMeasure(); i++) {
            android.view.View indicator = new android.view.View(dialog.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(indicatorSize, indicatorSize);
            params.setMargins(margin, 0, margin, 0);
            indicator.setLayoutParams(params);
            indicator.setBackgroundResource(R.drawable.beat_indicator_bg);
            indicator.setEnabled(false);
            beatIndicatorContainer.addView(indicator);
            beatViews[i] = i;
        }
    }
    
    private void updateBeatIndicator(int beat, boolean isDownbeat) {
        if (beatIndicatorContainer == null || beatViews == null) return;
        
        for (int i = 0; i < beatViews.length; i++) {
            android.view.View indicator = beatIndicatorContainer.getChildAt(i);
            if (indicator != null) {
                if (i == beat) {
                    indicator.setBackgroundResource(isDownbeat ? R.drawable.beat_indicator_active : R.drawable.beat_indicator_secondary);
                } else {
                    indicator.setBackgroundResource(R.drawable.beat_indicator_bg);
                }
            }
        }
        currentBeat = beat;
    }
    
    private void toggleMetronome() {
        if (metronome.isRunning()) {
            metronome.stop();
            btnStartStop.setText("开始");
            resetBeatIndicators();
        } else {
            metronome.start();
            btnStartStop.setText("停止");
        }
    }
    
    private void resetBeatIndicators() {
        if (beatIndicatorContainer == null) return;
        for (int i = 0; i < beatIndicatorContainer.getChildCount(); i++) {
            android.view.View indicator = beatIndicatorContainer.getChildAt(i);
            if (indicator != null) {
                indicator.setBackgroundResource(R.drawable.beat_indicator_bg);
            }
        }
    }
    
    public void show() {
        dialog.show();
    }
    
    public void dismiss() {
        if (metronome.isRunning()) {
            metronome.stop();
        }
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        if (dismissListener != null) {
            dismissListener.onDismiss();
        }
    }
    
    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }
    
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}

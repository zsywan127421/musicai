package com.example.musicai.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicai.R;
import com.example.musicai.model.Effect;

import java.util.List;

public class EffectAdapter extends RecyclerView.Adapter<EffectAdapter.EffectViewHolder> {
    
    public interface OnEffectActionListener {
        void onEffectEnabledChanged(int position, boolean enabled);
        void onEffectDeleted(int position);
        void onEffectParamChanged(int position);
    }
    
    private Context context;
    private List<Effect> effects;
    private OnEffectActionListener listener;
    
    public EffectAdapter(Context context, List<Effect> effects, OnEffectActionListener listener) {
        this.context = context;
        this.effects = effects;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public EffectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_effect, parent, false);
        return new EffectViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull EffectViewHolder holder, int position) {
        holder.bind(effects.get(position), position);
    }
    
    @Override
    public int getItemCount() {
        return effects.size();
    }
    
    class EffectViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvEffectName;
        private TextView tvEffectParams;
        private TextView tvEnable;
        private ImageButton btnDelete;
        private LinearLayout llParams;
        private LinearLayout llParamLevel;
        private LinearLayout llParamFrequency;
        private LinearLayout llParamGain;
        private LinearLayout llParamQ;
        private LinearLayout llParamThreshold;
        private LinearLayout llParamRatio;
        private LinearLayout llParamAttack;
        private LinearLayout llParamRelease;
        private LinearLayout llParamRoomSize;
        private LinearLayout llParamDamping;
        private LinearLayout llParamWet;
        
        private SeekBar seekbarLevel;
        private SeekBar seekbarFrequency;
        private SeekBar seekbarGain;
        private SeekBar seekbarQ;
        private SeekBar seekbarThreshold;
        private SeekBar seekbarRatio;
        private SeekBar seekbarAttack;
        private SeekBar seekbarRelease;
        private SeekBar seekbarRoomSize;
        private SeekBar seekbarDamping;
        private SeekBar seekbarWet;
        
        private TextView tvLevelValue;
        private TextView tvFrequencyValue;
        private TextView tvGainValue;
        private TextView tvQValue;
        private TextView tvThresholdValue;
        private TextView tvRatioValue;
        private TextView tvAttackValue;
        private TextView tvReleaseValue;
        private TextView tvRoomSizeValue;
        private TextView tvDampingValue;
        private TextView tvWetValue;
        
        private boolean isParamsVisible = false;
        
        EffectViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvEffectName = itemView.findViewById(R.id.tv_effect_name);
            tvEffectParams = itemView.findViewById(R.id.tv_effect_params);
            tvEnable = itemView.findViewById(R.id.tv_enable);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            llParams = itemView.findViewById(R.id.ll_params);
            
            llParamLevel = itemView.findViewById(R.id.ll_param_level);
            llParamFrequency = itemView.findViewById(R.id.ll_param_frequency);
            llParamGain = itemView.findViewById(R.id.ll_param_gain);
            llParamQ = itemView.findViewById(R.id.ll_param_q);
            llParamThreshold = itemView.findViewById(R.id.ll_param_threshold);
            llParamRatio = itemView.findViewById(R.id.ll_param_ratio);
            llParamAttack = itemView.findViewById(R.id.ll_param_attack);
            llParamRelease = itemView.findViewById(R.id.ll_param_release);
            llParamRoomSize = itemView.findViewById(R.id.ll_param_room_size);
            llParamDamping = itemView.findViewById(R.id.ll_param_damping);
            llParamWet = itemView.findViewById(R.id.ll_param_wet);
            
            seekbarLevel = itemView.findViewById(R.id.seekbar_level);
            seekbarFrequency = itemView.findViewById(R.id.seekbar_frequency);
            seekbarGain = itemView.findViewById(R.id.seekbar_gain);
            seekbarQ = itemView.findViewById(R.id.seekbar_q);
            seekbarThreshold = itemView.findViewById(R.id.seekbar_threshold);
            seekbarRatio = itemView.findViewById(R.id.seekbar_ratio);
            seekbarAttack = itemView.findViewById(R.id.seekbar_attack);
            seekbarRelease = itemView.findViewById(R.id.seekbar_release);
            seekbarRoomSize = itemView.findViewById(R.id.seekbar_room_size);
            seekbarDamping = itemView.findViewById(R.id.seekbar_damping);
            seekbarWet = itemView.findViewById(R.id.seekbar_wet);
            
            tvLevelValue = itemView.findViewById(R.id.tv_level_value);
            tvFrequencyValue = itemView.findViewById(R.id.tv_frequency_value);
            tvGainValue = itemView.findViewById(R.id.tv_gain_value);
            tvQValue = itemView.findViewById(R.id.tv_q_value);
            tvThresholdValue = itemView.findViewById(R.id.tv_threshold_value);
            tvRatioValue = itemView.findViewById(R.id.tv_ratio_value);
            tvAttackValue = itemView.findViewById(R.id.tv_attack_value);
            tvReleaseValue = itemView.findViewById(R.id.tv_release_value);
            tvRoomSizeValue = itemView.findViewById(R.id.tv_room_size_value);
            tvDampingValue = itemView.findViewById(R.id.tv_damping_value);
            tvWetValue = itemView.findViewById(R.id.tv_wet_value);
        }
        
        void bind(Effect effect, int position) {
            tvEffectName.setText(effect.getDisplayName());
            tvEffectParams.setText(effect.getParamsDisplay());
            
            updateEnabledButton(effect.enabled);
            
            tvEnable.setOnClickListener(v -> {
                effect.enabled = !effect.enabled;
                updateEnabledButton(effect.enabled);
                if (listener != null) {
                    listener.onEffectEnabledChanged(position, effect.enabled);
                }
            });
            
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEffectDeleted(position);
                }
            });
            
            itemView.setOnClickListener(v -> {
                isParamsVisible = !isParamsVisible;
                llParams.setVisibility(isParamsVisible ? View.VISIBLE : View.GONE);
                setupParams(effect, position);
            });
            
            if (isParamsVisible) {
                llParams.setVisibility(View.VISIBLE);
                setupParams(effect, position);
            } else {
                llParams.setVisibility(View.GONE);
            }
        }
        
        private void updateEnabledButton(boolean enabled) {
            if (enabled) {
                tvEnable.setBackgroundResource(R.drawable.apple_button_primary_bg);
                tvEnable.setTextColor(context.getResources().getColor(R.color.apple_white));
                tvEnable.setText("启用");
            } else {
                tvEnable.setBackgroundResource(R.drawable.apple_button_minor_bg);
                tvEnable.setTextColor(context.getResources().getColor(R.color.apple_text_secondary));
                tvEnable.setText("禁用");
            }
        }
        
        private void setupParams(Effect effect, int position) {
            llParamLevel.setVisibility(View.GONE);
            llParamFrequency.setVisibility(View.GONE);
            llParamGain.setVisibility(View.GONE);
            llParamQ.setVisibility(View.GONE);
            llParamThreshold.setVisibility(View.GONE);
            llParamRatio.setVisibility(View.GONE);
            llParamAttack.setVisibility(View.GONE);
            llParamRelease.setVisibility(View.GONE);
            llParamRoomSize.setVisibility(View.GONE);
            llParamDamping.setVisibility(View.GONE);
            llParamWet.setVisibility(View.GONE);
            
            switch (effect.type) {
                case Effect.TYPE_GAIN:
                    setupGainParams(effect, position);
                    break;
                case Effect.TYPE_EQ:
                    setupEQParams(effect, position);
                    break;
                case Effect.TYPE_COMPRESSOR:
                    setupCompressorParams(effect, position);
                    break;
                case Effect.TYPE_REVERB:
                    setupReverbParams(effect, position);
                    break;
                case Effect.TYPE_HIGH_PASS:
                case Effect.TYPE_LOW_PASS:
                    setupFilterParams(effect, position);
                    break;
            }
        }
        
        private void setupGainParams(Effect effect, int position) {
            llParamLevel.setVisibility(View.VISIBLE);
            seekbarLevel.setMax(240);
            seekbarLevel.setProgress((int)(effect.level + 120));
            tvLevelValue.setText(String.format("%.1fdB", effect.level));
            
            seekbarLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    effect.level = progress - 120;
                    tvLevelValue.setText(String.format("%.1fdB", effect.level));
                    tvEffectParams.setText(effect.getParamsDisplay());
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (listener != null) {
                        listener.onEffectParamChanged(position);
                    }
                }
            });
        }
        
        private void setupEQParams(Effect effect, int position) {
            llParamFrequency.setVisibility(View.VISIBLE);
            llParamGain.setVisibility(View.VISIBLE);
            llParamQ.setVisibility(View.VISIBLE);
            
            seekbarFrequency.setMax(1000);
            seekbarFrequency.setProgress((int)(effect.frequency / 20.0 - 1));
            tvFrequencyValue.setText(String.format("%.0fHz", effect.frequency));
            
            seekbarGain.setMax(240);
            seekbarGain.setProgress((int)(effect.gain + 120));
            tvGainValue.setText(String.format("%.1fdB", effect.gain));
            
            seekbarQ.setMax(100);
            seekbarQ.setProgress((int)(effect.q * 10));
            tvQValue.setText(String.format("%.1f", effect.q));
            
            SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (seekBar == seekbarFrequency) {
                        effect.frequency = (progress + 1) * 20f;
                        tvFrequencyValue.setText(String.format("%.0fHz", effect.frequency));
                    } else if (seekBar == seekbarGain) {
                        effect.gain = progress - 120;
                        tvGainValue.setText(String.format("%.1fdB", effect.gain));
                    } else if (seekBar == seekbarQ) {
                        effect.q = progress / 10f;
                        tvQValue.setText(String.format("%.1f", effect.q));
                    }
                    tvEffectParams.setText(effect.getParamsDisplay());
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (EffectAdapter.this.listener != null) {
                        EffectAdapter.this.listener.onEffectParamChanged(position);
                    }
                }
            };
            
            seekbarFrequency.setOnSeekBarChangeListener(listener);
            seekbarGain.setOnSeekBarChangeListener(listener);
            seekbarQ.setOnSeekBarChangeListener(listener);
        }
        
        private void setupCompressorParams(Effect effect, int position) {
            llParamThreshold.setVisibility(View.VISIBLE);
            llParamRatio.setVisibility(View.VISIBLE);
            llParamAttack.setVisibility(View.VISIBLE);
            llParamRelease.setVisibility(View.VISIBLE);
            
            seekbarThreshold.setMax(400);
            seekbarThreshold.setProgress((int)(-effect.threshold));
            tvThresholdValue.setText(String.format("%.0fdB", effect.threshold));
            
            seekbarRatio.setMax(190);
            seekbarRatio.setProgress((int)(effect.ratio - 1));
            tvRatioValue.setText(String.format("%.1f:1", effect.ratio));
            
            seekbarAttack.setMax(100);
            seekbarAttack.setProgress((int)effect.attack);
            tvAttackValue.setText(String.format("%.0fms", effect.attack));
            
            seekbarRelease.setMax(1000);
            seekbarRelease.setProgress((int)effect.release);
            tvReleaseValue.setText(String.format("%.0fms", effect.release));
            
            SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (seekBar == seekbarThreshold) {
                        effect.threshold = -progress;
                        tvThresholdValue.setText(String.format("%.0fdB", effect.threshold));
                    } else if (seekBar == seekbarRatio) {
                        effect.ratio = progress + 1;
                        tvRatioValue.setText(String.format("%.1f:1", effect.ratio));
                    } else if (seekBar == seekbarAttack) {
                        effect.attack = progress;
                        tvAttackValue.setText(String.format("%.0fms", effect.attack));
                    } else if (seekBar == seekbarRelease) {
                        effect.release = progress;
                        tvReleaseValue.setText(String.format("%.0fms", effect.release));
                    }
                    tvEffectParams.setText(effect.getParamsDisplay());
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (EffectAdapter.this.listener != null) {
                        EffectAdapter.this.listener.onEffectParamChanged(position);
                    }
                }
            };
            
            seekbarThreshold.setOnSeekBarChangeListener(listener);
            seekbarRatio.setOnSeekBarChangeListener(listener);
            seekbarAttack.setOnSeekBarChangeListener(listener);
            seekbarRelease.setOnSeekBarChangeListener(listener);
        }
        
        private void setupReverbParams(Effect effect, int position) {
            llParamRoomSize.setVisibility(View.VISIBLE);
            llParamDamping.setVisibility(View.VISIBLE);
            llParamWet.setVisibility(View.VISIBLE);
            
            seekbarRoomSize.setMax(100);
            seekbarRoomSize.setProgress((int)effect.roomSize);
            tvRoomSizeValue.setText(String.format("%.0f%%", effect.roomSize));
            
            seekbarDamping.setMax(100);
            seekbarDamping.setProgress((int)effect.damping);
            tvDampingValue.setText(String.format("%.0f%%", effect.damping));
            
            seekbarWet.setMax(100);
            seekbarWet.setProgress((int)effect.wet);
            tvWetValue.setText(String.format("%.0f%%", effect.wet));
            
            SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (seekBar == seekbarRoomSize) {
                        effect.roomSize = progress;
                        tvRoomSizeValue.setText(String.format("%.0f%%", effect.roomSize));
                    } else if (seekBar == seekbarDamping) {
                        effect.damping = progress;
                        tvDampingValue.setText(String.format("%.0f%%", effect.damping));
                    } else if (seekBar == seekbarWet) {
                        effect.wet = progress;
                        tvWetValue.setText(String.format("%.0f%%", effect.wet));
                    }
                    tvEffectParams.setText(effect.getParamsDisplay());
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (EffectAdapter.this.listener != null) {
                        EffectAdapter.this.listener.onEffectParamChanged(position);
                    }
                }
            };
            
            seekbarRoomSize.setOnSeekBarChangeListener(listener);
            seekbarDamping.setOnSeekBarChangeListener(listener);
            seekbarWet.setOnSeekBarChangeListener(listener);
        }
        
        private void setupFilterParams(Effect effect, int position) {
            llParamFrequency.setVisibility(View.VISIBLE);
            
            seekbarFrequency.setMax(1000);
            seekbarFrequency.setProgress((int)(effect.frequency / 20.0 - 1));
            tvFrequencyValue.setText(String.format("%.0fHz", effect.frequency));
            
            seekbarFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    effect.frequency = (progress + 1) * 20f;
                    tvFrequencyValue.setText(String.format("%.0fHz", effect.frequency));
                    tvEffectParams.setText(effect.getParamsDisplay());
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (listener != null) {
                        listener.onEffectParamChanged(position);
                    }
                }
            });
        }
    }
}

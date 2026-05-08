package com.example.musicai.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicai.R;
import com.example.musicai.Track;
import com.example.musicai.model.Effect;

import java.util.Collections;
import java.util.List;

public class EffectChainBottomSheet {
    
    public interface OnEffectChainChangedListener {
        void onEffectChainChanged(List<Effect> effects);
    }
    
    private Dialog dialog;
    private Track track;
    private EffectAdapter adapter;
    private OnEffectChainChangedListener listener;
    
    public static void show(Context context, Track track, OnEffectChainChangedListener listener) {
        EffectChainBottomSheet bottomSheet = new EffectChainBottomSheet();
        bottomSheet.init(context, track, listener);
    }
    
    private void init(Context context, Track track, OnEffectChainChangedListener listener) {
        this.track = track;
        this.listener = listener;
        
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet_effect_chain);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setWindowAnimations(android.R.style.Animation_Dialog);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.4f);
        }
        
        setupViews(context);
        dialog.show();
    }
    
    private void setupViews(Context context) {
        TextView tvTrackName = dialog.findViewById(R.id.tv_track_name);
        tvTrackName.setText("轨道: " + track.name);
        
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dismiss());
        
        Spinner spEffectType = dialog.findViewById(R.id.sp_effect_type);
        ArrayAdapter<String> effectAdapter = new ArrayAdapter<>(
            context,
            R.layout.spinner_item,
            Effect.EFFECT_TYPES
        );
        effectAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spEffectType.setAdapter(effectAdapter);
        
        Button btnAddEffect = dialog.findViewById(R.id.btn_add_effect);
        btnAddEffect.setOnClickListener(v -> {
            String selectedType = Effect.EFFECT_TYPES[spEffectType.getSelectedItemPosition()];
            Effect newEffect = new Effect(selectedType);
            track.effects.add(newEffect);
            updateEffectList();
            if (listener != null) {
                listener.onEffectChainChanged(track.effects);
            }
        });
        
        RecyclerView rvEffects = dialog.findViewById(R.id.rv_effects);
        TextView tvEmptyEffects = dialog.findViewById(R.id.tv_empty_effects);
        
        adapter = new EffectAdapter(context, track.effects, new EffectAdapter.OnEffectActionListener() {
            @Override
            public void onEffectEnabledChanged(int position, boolean enabled) {
                track.effects.get(position).enabled = enabled;
                if (listener != null) {
                    listener.onEffectChainChanged(track.effects);
                }
            }
            
            @Override
            public void onEffectDeleted(int position) {
                ConfirmDialog.showDelete(context, track.effects.get(position).getDisplayName(), () -> {
                    track.effects.remove(position);
                    updateEffectList();
                    if (listener != null) {
                        listener.onEffectChainChanged(track.effects);
                    }
                });
            }
            
            @Override
            public void onEffectParamChanged(int position) {
                if (listener != null) {
                    listener.onEffectChainChanged(track.effects);
                }
            }
        });
        
        rvEffects.setLayoutManager(new LinearLayoutManager(context));
        rvEffects.setAdapter(adapter);
        
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                Collections.swap(track.effects, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                if (listener != null) {
                    listener.onEffectChainChanged(track.effects);
                }
                return true;
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        });
        touchHelper.attachToRecyclerView(rvEffects);
        
        Button btnDone = dialog.findViewById(R.id.btn_done);
        btnDone.setOnClickListener(v -> dismiss());
        
        updateEffectList();
    }
    
    private void updateEffectList() {
        TextView tvEmptyEffects = dialog.findViewById(R.id.tv_empty_effects);
        RecyclerView rvEffects = dialog.findViewById(R.id.rv_effects);
        
        if (track.effects.isEmpty()) {
            tvEmptyEffects.setVisibility(View.VISIBLE);
            rvEffects.setVisibility(View.GONE);
        } else {
            tvEmptyEffects.setVisibility(View.GONE);
            rvEffects.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }
    
    private void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}

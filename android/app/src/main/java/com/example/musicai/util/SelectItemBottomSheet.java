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

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicai.R;

import java.util.List;

public class SelectItemBottomSheet {
    
    public interface OnItemSelectedListener {
        void onItemSelected(int index);
    }
    
    public static void showNoteSelection(Context context, List<String> items, OnItemSelectedListener listener) {
        show(context, "选择要编辑的音符", items, listener);
    }
    
    public static void showChordSelection(Context context, List<String> items, OnItemSelectedListener listener) {
        show(context, "选择要编辑的和弦", items, listener);
    }
    
    public static void show(Context context, List<String> items, OnItemSelectedListener listener) {
        show(context, "选择项目", items, listener);
    }
    
    public static void show(Context context, String title, List<String> items, OnItemSelectedListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet_select_item);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setWindowAnimations(android.R.style.Animation_Dialog);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.4f);
        }
        
        TextView tvTitle = dialog.findViewById(R.id.tv_title);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        RecyclerView rvItems = dialog.findViewById(R.id.rv_items);
        View btnCancel = dialog.findViewById(R.id.btn_cancel);
        
        tvTitle.setText(title);
        
        SelectItemAdapter adapter = new SelectItemAdapter(items);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        rvItems.setLayoutManager(layoutManager);
        rvItems.setAdapter(adapter);
        rvItems.setHasFixedSize(true);
        rvItems.setItemViewCacheSize(20);
        layoutManager.setItemPrefetchEnabled(true);
        rvItems.setNestedScrollingEnabled(false);
        
        adapter.setOnItemClickListener((position) -> {
            dialog.dismiss();
            listener.onItemSelected(position);
        });
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
}

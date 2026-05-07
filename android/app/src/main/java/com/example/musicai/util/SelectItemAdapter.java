package com.example.musicai.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicai.R;

import java.util.List;

public class SelectItemAdapter extends RecyclerView.Adapter<SelectItemAdapter.ViewHolder> {
    
    private List<String> items;
    private OnItemClickListener listener;
    
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    
    public SelectItemAdapter(List<String> items) {
        this.items = items;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_select, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }
    
    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvIndex;
        private TextView tvContent;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tv_index);
            tvContent = itemView.findViewById(R.id.tv_content);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(getAdapterPosition());
                }
            });
        }
        
        void bind(String content, int position) {
            tvIndex.setText((position + 1) + ".");
            tvContent.setText(content);
        }
    }
}

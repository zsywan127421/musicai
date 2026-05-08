package com.example.musicai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicai.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class LibraryItemAdapter extends RecyclerView.Adapter<LibraryItemAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onItemClick(int position);
        void onItemDelete(int position);
        void onItemPlay(int position);
    }

    private List<?> items;
    private int itemType;
    private OnItemActionListener listener;

    public LibraryItemAdapter(List<?> items, int itemType) {
        this.items = items != null ? items : new ArrayList<>();
        this.itemType = itemType;
    }

    public void updateData(List<?> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_library_content, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), position);
    }

    private Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout cardContainer;
        private TextView tvTitle;
        private TextView tvSubtitle;
        private TextView tvTime;
        private TextView tvBadge;
        private ImageButton btnPlay;
        private ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.card_container);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvBadge = itemView.findViewById(R.id.tv_badge);
            btnPlay = itemView.findViewById(R.id.btn_play);
            btnDelete = itemView.findViewById(R.id.btn_delete);

            cardContainer.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(pos);
                }
            });

            btnPlay.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemPlay(pos);
                }
            });

            btnDelete.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemDelete(pos);
                }
            });
        }

        void bind(Object item, int position) {
            if (itemType == LibraryTabsActivity.TYPE_MELODY) {
                MusicRepository.MelodyEntry entry = (MusicRepository.MelodyEntry) item;
                tvTitle.setText(entry.name);
                tvSubtitle.setText(entry.style + " · " + entry.notes.size() + " 个音符");
                tvTime.setText(TimeUtils.formatRelativeTime(entry.createdAt));
                tvBadge.setText("旋律");
                tvBadge.setVisibility(View.VISIBLE);
            } else if (itemType == LibraryTabsActivity.TYPE_CHORD) {
                MusicRepository.ChordEntry entry = (MusicRepository.ChordEntry) item;
                tvTitle.setText(entry.name);
                tvSubtitle.setText(entry.style + " · " + entry.chords.size() + " 个和弦");
                tvTime.setText(TimeUtils.formatRelativeTime(entry.createdAt));
                tvBadge.setText("和弦");
                tvBadge.setVisibility(View.VISIBLE);
            } else if (itemType == LibraryTabsActivity.TYPE_SONG) {
                SongEntry entry = (SongEntry) item;
                tvTitle.setText(entry.name);
                tvSubtitle.setText(entry.style + " · " + entry.segments.size() + " 段落");
                tvTime.setText(TimeUtils.formatRelativeTime(entry.createdAt));
                tvBadge.setText("歌曲");
                tvBadge.setVisibility(View.VISIBLE);
            }
        }
    }
}

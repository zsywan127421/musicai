package com.example.musicai.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.musicai.MusicRepository;
import com.example.musicai.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LibraryAdapter extends BaseAdapter {
    
    private Context context;
    private List<Object> items;
    private List<String> itemIds;
    private boolean isMelody;
    private LayoutInflater inflater;
    
    public LibraryAdapter(Context context, List<?> items, boolean isMelody) {
        this.context = context;
        this.items = new ArrayList<>(items);
        this.isMelody = isMelody;
        this.inflater = LayoutInflater.from(context);
        this.itemIds = new ArrayList<>();
        
        for (Object item : items) {
            if (item instanceof MusicRepository.MelodyEntry) {
                itemIds.add(((MusicRepository.MelodyEntry) item).id);
            } else if (item instanceof MusicRepository.ChordEntry) {
                itemIds.add(((MusicRepository.ChordEntry) item).id);
            } else {
                itemIds.add(null);
            }
        }
    }
    
    public String getItemStringId(int position) {
        if (position >= 0 && position < itemIds.size()) {
            return itemIds.get(position);
        }
        return null;
    }
    
    @Override
    public int getCount() {
        return items.size();
    }
    
    @Override
    public Object getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_library, parent, false);
            holder = new ViewHolder();
            holder.tvName = convertView.findViewById(R.id.tv_name);
            holder.tvInfo = convertView.findViewById(R.id.tv_info);
            holder.tvStyle = convertView.findViewById(R.id.tv_style);
            holder.tvCount = convertView.findViewById(R.id.tv_count);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        Object rawItem = items.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        
        if (rawItem instanceof MusicRepository.MelodyEntry) {
            MusicRepository.MelodyEntry entry = (MusicRepository.MelodyEntry) rawItem;
            holder.tvName.setText(entry.name);
            holder.tvStyle.setText(entry.style);
            holder.tvCount.setText(entry.notes.size() + " 音符");
            holder.tvInfo.setText(sdf.format(new Date(entry.createdAt)));
        } else if (rawItem instanceof MusicRepository.ChordEntry) {
            MusicRepository.ChordEntry entry = (MusicRepository.ChordEntry) rawItem;
            holder.tvName.setText(entry.name);
            holder.tvStyle.setText(entry.style);
            holder.tvCount.setText(entry.chords.size() + " 和弦");
            holder.tvInfo.setText(sdf.format(new Date(entry.createdAt)));
        }
        
        return convertView;
    }
    
    private static class ViewHolder {
        TextView tvName;
        TextView tvInfo;
        TextView tvStyle;
        TextView tvCount;
    }
}

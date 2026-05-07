package com.example.musicai.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.musicai.MusicData;
import com.example.musicai.R;

import java.util.List;

public class LibraryAdapter extends BaseAdapter {
    
    private Context context;
    private List<?> items;
    private boolean isMelody;
    private LayoutInflater inflater;
    
    public LibraryAdapter(Context context, List<?> items, boolean isMelody) {
        this.context = context;
        this.items = items;
        this.isMelody = isMelody;
        this.inflater = LayoutInflater.from(context);
    }
    
    @Override
    public int getCount() {
        return items.size();
    }
    
    @Override
    public Object getItem(int position) {
        return items.get(position);
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
        
        if (rawItem instanceof MusicData.Melody) {
            MusicData.Melody melody = (MusicData.Melody) rawItem;
            holder.tvName.setText(melody.name);
            holder.tvStyle.setText(melody.style);
            holder.tvCount.setText(melody.notes.size() + " 音符");
            holder.tvInfo.setText(String.valueOf(melody.createdAt));
        } else if (rawItem instanceof MusicData.ChordProgression) {
            MusicData.ChordProgression chord = (MusicData.ChordProgression) rawItem;
            holder.tvName.setText(chord.name);
            holder.tvStyle.setText(chord.style);
            holder.tvCount.setText(chord.chords.size() + " 和弦");
            holder.tvInfo.setText(String.valueOf(chord.createdAt));
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

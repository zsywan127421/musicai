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
    private List<? extends MusicData> items;
    private boolean isMelody;
    private LayoutInflater inflater;
    
    public LibraryAdapter(Context context, List<? extends MusicData> items, boolean isMelody) {
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
        
        MusicData item = items.get(position);
        holder.tvName.setText(item.name);
        holder.tvStyle.setText(item.style);
        
        if (isMelody && item instanceof MusicData.Melody) {
            MusicData.Melody melody = (MusicData.Melody) item;
            holder.tvCount.setText(melody.notes.size() + " 音符");
            holder.tvInfo.setText(item.createdAt);
        } else if (!isMelody && item instanceof MusicData.ChordProgression) {
            MusicData.ChordProgression chord = (MusicData.ChordProgression) item;
            holder.tvCount.setText(chord.chords.size() + " 和弦");
            holder.tvInfo.setText(item.createdAt);
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

package com.example.musicai;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class HighlightedAdapter extends ArrayAdapter<String> {
    
    private int highlightIndex = -1;
    private int normalBgColor = Color.parseColor("#1C1C1E");
    private int highlightBgColor = Color.parseColor("#0A84FF");
    private int normalTextColor = Color.parseColor("#FFFFFF");
    private int highlightTextColor = Color.parseColor("#FFFFFF");
    
    public HighlightedAdapter(Context context, List<String> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);
    }
    
    public void setHighlightIndex(int index) {
        this.highlightIndex = index;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        
        if (position == highlightIndex) {
            view.setBackgroundColor(highlightBgColor);
            textView.setTextColor(highlightTextColor);
            textView.setTextSize(15);
            textView.setPadding(24, 16, 16, 16);
        } else {
            view.setBackgroundColor(normalBgColor);
            
            String text = getItem(position);
            if (text != null && text.startsWith("---")) {
                textView.setTextColor(Color.parseColor("#8E8E93"));
                textView.setTextSize(13);
            } else if (text != null && (text.contains("标题:") || text.contains("艺术家:") || text.contains("风格:"))) {
                textView.setTextColor(Color.parseColor("#FFD60A"));
                textView.setTextSize(14);
            } else {
                textView.setTextColor(normalTextColor);
                textView.setTextSize(14);
            }
            textView.setPadding(24, 12, 16, 12);
        }
        
        return view;
    }
}

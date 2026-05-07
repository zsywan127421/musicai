package com.example.musicai;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

public class HighlightedAdapter extends ArrayAdapter<String> {
    
    private int highlightIndex = -1;
    private int normalBgColor;
    private int highlightBgColor;
    private int normalTextColor;
    private int highlightTextColor;
    private int labelTextColor;
    
    public HighlightedAdapter(Context context, List<String> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);
        normalBgColor = ContextCompat.getColor(context, R.color.apple_card_bg);
        highlightBgColor = ContextCompat.getColor(context, R.color.apple_accent);
        normalTextColor = ContextCompat.getColor(context, R.color.apple_text);
        highlightTextColor = Color.WHITE;
        labelTextColor = ContextCompat.getColor(context, R.color.apple_accent);
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
                textView.setTextColor(ContextCompat.getColor(getContext(), R.color.apple_text_tertiary));
                textView.setTextSize(13);
            } else if (text != null && (text.contains("标题:") || text.contains("艺术家:") || text.contains("风格:"))) {
                textView.setTextColor(labelTextColor);
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

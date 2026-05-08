package com.example.musicai.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.musicai.R;

import java.util.ArrayList;
import java.util.List;

public class ToolbarHelper {
    
    public interface OnMenuItemClickListener {
        void onMenuItemClick(int itemId);
    }
    
    public static class MenuItemData {
        public int id;
        public String title;
        public int iconRes;
        public boolean isDanger;
        
        public MenuItemData(int id, String title) {
            this.id = id;
            this.title = title;
        }
        
        public MenuItemData(int id, String title, int iconRes) {
            this.id = id;
            this.title = title;
            this.iconRes = iconRes;
        }
        
        public MenuItemData(int id, String title, int iconRes, boolean isDanger) {
            this.id = id;
            this.title = title;
            this.iconRes = iconRes;
            this.isDanger = isDanger;
        }
    }
    
    private View toolbarView;
    private ImageButton btnBack;
    private ImageButton btnMenu;
    private TextView tvTitle;
    private OnMenuItemClickListener menuListener;
    
    public ToolbarHelper(Activity activity, View containerView) {
        this.toolbarView = containerView;
        this.btnBack = containerView.findViewById(R.id.btn_back);
        this.btnMenu = containerView.findViewById(R.id.btn_menu);
        this.tvTitle = containerView.findViewById(R.id.tv_title);
    }
    
    public ToolbarHelper setTitle(String title) {
        tvTitle.setText(title);
        return this;
    }
    
    public ToolbarHelper setBackVisible(boolean visible) {
        btnBack.setVisibility(visible ? View.VISIBLE : View.GONE);
        return this;
    }
    
    public ToolbarHelper setBackIcon(Drawable icon) {
        btnBack.setImageDrawable(icon);
        return this;
    }
    
    public ToolbarHelper setMenuVisible(boolean visible) {
        btnMenu.setVisibility(visible ? View.VISIBLE : View.GONE);
        return this;
    }
    
    public ToolbarHelper setOnBackClickListener(View.OnClickListener listener) {
        btnBack.setOnClickListener(listener);
        return this;
    }
    
    public ToolbarHelper setMenuItems(List<MenuItemData> items, OnMenuItemClickListener listener) {
        this.menuListener = listener;
        btnMenu.setOnClickListener(v -> showPopupMenu(v.getContext(), items));
        return this;
    }
    
    private void showPopupMenu(Context context, List<MenuItemData> items) {
        PopupMenu popup = new PopupMenu(context, btnMenu, Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0);
        Menu menu = popup.getMenu();
        
        for (MenuItemData item : items) {
            MenuItem menuItem = menu.add(Menu.NONE, item.id, Menu.NONE, item.title);
            if (item.iconRes != 0) {
                menuItem.setIcon(item.iconRes);
            }
            if (item.isDanger) {
                menuItem.setTitleTextColor(ContextCompat.getColor(context, R.color.apple_danger));
            }
        }
        
        popup.setOnMenuItemClickListener(item -> {
            if (menuListener != null) {
                menuListener.onMenuItemClick(item.getItemId());
            }
            return true;
        });
        
        popup.show();
    }
    
    public void destroy() {
        btnBack.setOnClickListener(null);
        btnMenu.setOnClickListener(null);
    }
}

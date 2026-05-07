package com.example.musicai.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtils {
    
    public static final String DATE_FORMAT_FULL = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT_SHORT = "yyyy-MM-dd";
    
    public static String formatTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return "未知时间";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_FULL, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public static String formatTimestampShort(long timestamp) {
        if (timestamp <= 0) {
            return "未知时间";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_SHORT, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public static String formatRelativeTime(long timestamp) {
        if (timestamp <= 0) {
            return "未知时间";
        }
        
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 0) {
            return formatTimestamp(timestamp);
        }
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        if (seconds < 60) {
            return "刚刚";
        }
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        if (minutes < 60) {
            return minutes + "分钟前";
        }
        
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        if (hours < 24) {
            return hours + "小时前";
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (days < 7) {
            return days + "天前";
        }
        
        if (days < 30) {
            long weeks = days / 7;
            return weeks + "周前";
        }
        
        if (days < 365) {
            long months = days / 30;
            return months + "个月前";
        }
        
        long years = days / 365;
        return years + "年前";
    }
}

package com.example.musicai.util;

import java.util.ArrayList;
import java.util.List;

public class DataChangeObserver {
    
    public interface OnDataChangeListener {
        void onMelodyChanged();
        void onChordChanged();
        void onSongChanged();
        void onAllChanged();
    }
    
    private static DataChangeObserver instance;
    private List<OnDataChangeListener> listeners = new ArrayList<>();
    
    private DataChangeObserver() {}
    
    public static synchronized DataChangeObserver getInstance() {
        if (instance == null) {
            instance = new DataChangeObserver();
        }
        return instance;
    }
    
    public void registerListener(OnDataChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void unregisterListener(OnDataChangeListener listener) {
        listeners.remove(listener);
    }
    
    public void notifyMelodyChanged() {
        for (OnDataChangeListener listener : listeners) {
            listener.onMelodyChanged();
        }
    }
    
    public void notifyChordChanged() {
        for (OnDataChangeListener listener : listeners) {
            listener.onChordChanged();
        }
    }
    
    public void notifySongChanged() {
        for (OnDataChangeListener listener : listeners) {
            listener.onSongChanged();
        }
    }
    
    public void notifyAllChanged() {
        for (OnDataChangeListener listener : listeners) {
            listener.onAllChanged();
        }
    }
}

package com.example.musicai;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Project {
    
    public String id;
    public String name;
    public int bpm = 120;
    public String timeSignature = "4/4";
    public String keySignature = "C";
    public List<Track> tracks;
    public long createdAt;
    public long updatedAt;
    
    public static final String[] TIME_SIGNATURES = {
        "4/4", "3/4", "6/8", "2/4", "5/4", "7/8", "12/8"
    };
    
    public static final String[] KEY_SIGNATURES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B",
        "Am", "A#m", "Bm", "Cm", "C#m", "Dm", "D#m", "Em", "Fm", "F#m", "Gm", "G#m"
    };
    
    public Project() {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = "New Project";
        this.bpm = 120;
        this.timeSignature = "4/4";
        this.keySignature = "C";
        this.tracks = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Project(String name) {
        this();
        this.name = name;
    }
    
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);
        obj.put("bpm", bpm);
        obj.put("timeSignature", timeSignature);
        obj.put("keySignature", keySignature);
        
        JSONArray tracksArray = new JSONArray();
        for (Track track : tracks) {
            tracksArray.put(track.toJson());
        }
        obj.put("tracks", tracksArray);
        
        obj.put("createdAt", createdAt);
        obj.put("updatedAt", updatedAt);
        
        return obj;
    }
    
    public static Project fromJson(JSONObject obj) throws JSONException {
        Project project = new Project();
        project.id = obj.optString("id", java.util.UUID.randomUUID().toString());
        project.name = obj.optString("name", "New Project");
        project.bpm = obj.optInt("bpm", 120);
        project.timeSignature = obj.optString("timeSignature", "4/4");
        project.keySignature = obj.optString("keySignature", "C");
        
        project.tracks = new ArrayList<>();
        if (obj.has("tracks")) {
            JSONArray tracksArray = obj.getJSONArray("tracks");
            for (int i = 0; i < tracksArray.length(); i++) {
                project.tracks.add(Track.fromJson(tracksArray.getJSONObject(i)));
            }
        }
        
        project.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
        project.updatedAt = obj.optLong("updatedAt", System.currentTimeMillis());
        
        return project;
    }
    
    public void addTrack(Track track) {
        if (tracks == null) {
            tracks = new ArrayList<>();
        }
        tracks.add(track);
        updatedAt = System.currentTimeMillis();
    }
    
    public void removeTrack(String trackId) {
        if (tracks != null) {
            tracks.removeIf(track -> track.id.equals(trackId));
            updatedAt = System.currentTimeMillis();
        }
    }
    
    public Track getTrack(String trackId) {
        if (tracks != null) {
            for (Track track : tracks) {
                if (track.id.equals(trackId)) {
                    return track;
                }
            }
        }
        return null;
    }
    
    public int getTrackCount() {
        return tracks != null ? tracks.size() : 0;
    }
    
    public boolean hasSoloTrack() {
        if (tracks != null) {
            for (Track track : tracks) {
                if (track.isSolo) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean shouldPlayTrack(Track track) {
        if (hasSoloTrack()) {
            return track.isSolo;
        }
        return !track.isMuted;
    }
    
    public void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Project copy() {
        Project copy = new Project();
        copy.id = java.util.UUID.randomUUID().toString();
        copy.name = this.name + " (Copy)";
        copy.bpm = this.bpm;
        copy.timeSignature = this.timeSignature;
        copy.keySignature = this.keySignature;
        copy.tracks = new ArrayList<>();
        for (Track track : this.tracks) {
            copy.tracks.add(track.copy());
        }
        copy.createdAt = System.currentTimeMillis();
        copy.updatedAt = System.currentTimeMillis();
        return copy;
    }
}

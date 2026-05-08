package com.example.musicai;

import com.example.musicai.model.Effect;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Track {
    
    public String id;
    public String name;
    public String instrument;
    public MusicData.Melody melody;
    public boolean isMuted;
    public boolean isSolo;
    public float volume = 1.0f;
    public float pan = 0.0f;
    public List<Effect> effects;
    
    public static final String[] INSTRUMENTS = {
        "Piano", "Guitar", "Bass", "Strings",
        "Synth Lead", "Synth Pad", "Drums", "Organ"
    };
    
    public Track() {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = "New Track";
        this.instrument = INSTRUMENTS[0];
        this.melody = new MusicData.Melody();
        this.isMuted = false;
        this.isSolo = false;
        this.volume = 1.0f;
        this.pan = 0.0f;
        this.effects = new ArrayList<>();
    }
    
    public Track(String name, String instrument) {
        this();
        this.name = name;
        this.instrument = instrument;
    }
    
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);
        obj.put("instrument", instrument);
        obj.put("melody", melody != null ? melody.toJson() : new JSONObject());
        obj.put("isMuted", isMuted);
        obj.put("isSolo", isSolo);
        obj.put("volume", volume);
        obj.put("pan", pan);
        
        JSONArray effectsArray = new JSONArray();
        for (Effect effect : effects) {
            effectsArray.put(effect.toJson());
        }
        obj.put("effects", effectsArray);
        
        return obj;
    }
    
    public static Track fromJson(JSONObject obj) throws JSONException {
        Track track = new Track();
        track.id = obj.optString("id", java.util.UUID.randomUUID().toString());
        track.name = obj.optString("name", "New Track");
        track.instrument = obj.optString("instrument", INSTRUMENTS[0]);
        track.melody = obj.has("melody") ? MusicData.Melody.fromJson(obj.getJSONObject("melody")) : new MusicData.Melody();
        track.isMuted = obj.optBoolean("isMuted", false);
        track.isSolo = obj.optBoolean("isSolo", false);
        track.volume = (float) obj.optDouble("volume", 1.0);
        track.pan = (float) obj.optDouble("pan", 0.0);
        
        track.effects = new ArrayList<>();
        if (obj.has("effects")) {
            JSONArray effectsArray = obj.getJSONArray("effects");
            for (int i = 0; i < effectsArray.length(); i++) {
                track.effects.add(Effect.fromJson(effectsArray.getJSONObject(i)));
            }
        }
        
        return track;
    }
    
    public Track copy() {
        Track copy = new Track();
        copy.id = java.util.UUID.randomUUID().toString();
        copy.name = this.name;
        copy.instrument = this.instrument;
        copy.melody = this.melody;
        copy.isMuted = this.isMuted;
        copy.isSolo = this.isSolo;
        copy.volume = this.volume;
        copy.pan = this.pan;
        copy.effects = new ArrayList<>(this.effects);
        return copy;
    }
}

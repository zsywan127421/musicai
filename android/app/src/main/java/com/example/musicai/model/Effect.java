package com.example.musicai.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Effect {
    
    public String id;
    public String type;
    public boolean enabled;
    public float level;
    public float frequency;
    public float gain;
    public float q;
    public float threshold;
    public float ratio;
    public float attack;
    public float release;
    public float roomSize;
    public float damping;
    public float wet;
    
    public static final String TYPE_GAIN = "Gain";
    public static final String TYPE_EQ = "EQ";
    public static final String TYPE_COMPRESSOR = "Compressor";
    public static final String TYPE_REVERB = "Reverb";
    public static final String TYPE_HIGH_PASS = "HighPass";
    public static final String TYPE_LOW_PASS = "LowPass";
    
    public static final String[] EFFECT_TYPES = {
        TYPE_GAIN, TYPE_EQ, TYPE_COMPRESSOR, TYPE_REVERB, TYPE_HIGH_PASS, TYPE_LOW_PASS
    };
    
    public Effect() {
        this.id = java.util.UUID.randomUUID().toString();
        this.type = TYPE_GAIN;
        this.enabled = true;
        setDefaultParams(TYPE_GAIN);
    }
    
    public Effect(String type) {
        this.id = java.util.UUID.randomUUID().toString();
        this.type = type;
        this.enabled = true;
        setDefaultParams(type);
    }
    
    private void setDefaultParams(String type) {
        switch (type) {
            case TYPE_GAIN:
                this.level = 0f;
                break;
            case TYPE_EQ:
                this.frequency = 1000f;
                this.gain = 0f;
                this.q = 1f;
                break;
            case TYPE_COMPRESSOR:
                this.threshold = -20f;
                this.ratio = 4f;
                this.attack = 10f;
                this.release = 100f;
                break;
            case TYPE_REVERB:
                this.roomSize = 50f;
                this.damping = 50f;
                this.wet = 30f;
                break;
            case TYPE_HIGH_PASS:
            case TYPE_LOW_PASS:
                this.frequency = 1000f;
                break;
        }
    }
    
    public void setParamsByType(String type) {
        this.type = type;
        setDefaultParams(type);
    }
    
    public float[] process(float[] input) {
        if (!enabled || input == null || input.length == 0) {
            return input;
        }
        
        float[] output = new float[input.length];
        System.arraycopy(input, 0, output, 0, input.length);
        
        switch (type) {
            case TYPE_GAIN:
                output = applyGain(output);
                break;
            case TYPE_EQ:
                output = applyEQ(output);
                break;
            case TYPE_COMPRESSOR:
                output = applyCompressor(output);
                break;
            case TYPE_REVERB:
                output = applyReverb(output);
                break;
            case TYPE_HIGH_PASS:
                output = applyHighPass(output);
                break;
            case TYPE_LOW_PASS:
                output = applyLowPass(output);
                break;
        }
        
        return output;
    }
    
    private float[] applyGain(float[] input) {
        float gainLinear = (float) Math.pow(10, level / 20.0);
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = input[i] * gainLinear;
        }
        return output;
    }
    
    private float[] applyEQ(float[] input) {
        float[] output = new float[input.length];
        float omega = (float) (2 * Math.PI * frequency / 44100.0);
        float alpha = (float) (Math.sin(omega) / (2 * q));
        float cosOmega = (float) Math.cos(omega);
        
        float b0 = (float) (1 + alpha * Math.pow(10, gain / 40.0));
        float b1 = (float) (-2 * cosOmega);
        float b2 = (float) (1 - alpha * Math.pow(10, gain / 40.0));
        float a0 = (float) (1 + alpha / Math.pow(10, gain / 40.0));
        float a1 = (float) (-2 * cosOmega);
        float a2 = (float) (1 - alpha / Math.pow(10, gain / 40.0));
        
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;
        
        float x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int i = 0; i < input.length; i++) {
            float x0 = input[i];
            float y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            output[i] = y0;
            x2 = x1;
            x1 = x0;
            y2 = y1;
            y1 = y0;
        }
        return output;
    }
    
    private float[] applyCompressor(float[] input) {
        float[] output = new float[input.length];
        float attackCoef = (float) Math.exp(-1.0 / (attack * 44100.0 / 1000.0));
        float releaseCoef = (float) Math.exp(-1.0 / (release * 44100.0 / 1000.0));
        float envelope = 0;
        float thresholdLinear = (float) Math.pow(10, threshold / 20.0);
        
        for (int i = 0; i < input.length; i++) {
            float absInput = Math.abs(input[i]);
            if (absInput > envelope) {
                envelope = attackCoef * envelope + (1 - attackCoef) * absInput;
            } else {
                envelope = releaseCoef * envelope + (1 - releaseCoef) * absInput;
            }
            
            float gainReduction = 1.0f;
            if (envelope > thresholdLinear) {
                float overThreshold = envelope / thresholdLinear;
                float compressedOverThreshold = (float) Math.pow(overThreshold, 1.0 / ratio);
                gainReduction = compressedOverThreshold / overThreshold;
            }
            
            output[i] = input[i] * gainReduction;
        }
        return output;
    }
    
    private float[] applyReverb(float[] input) {
        int numDelays = 8;
        int[] delays = {
            (int)(1557 * roomSize / 100.0),
            (int)(1617 * roomSize / 100.0),
            (int)(1491 * roomSize / 100.0),
            (int)(1422 * roomSize / 100.0),
            (int)(1277 * roomSize / 100.0),
            (int)(1356 * roomSize / 100.0),
            (int)(1188 * roomSize / 100.0),
            (int)(1116 * roomSize / 100.0)
        };
        
        float[] decay = new float[numDelays];
        for (int i = 0; i < numDelays; i++) {
            decay[i] = (float)(0.84 * Math.pow(roomSize / 100.0, damping / 100.0));
        }
        
        float wetLinear = wet / 100.0f;
        float dryLinear = 1.0f - wetLinear;
        
        float[][] delayBuffers = new float[numDelays][];
        int[] delayIndices = new int[numDelays];
        for (int i = 0; i < numDelays; i++) {
            delayBuffers[i] = new float[delays[i]];
        }
        
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            float wetSample = 0;
            for (int d = 0; d < numDelays; d++) {
                int delayIndex = delayIndices[d];
                float delayedSample = delayBuffers[d][delayIndex];
                wetSample += delayedSample * decay[d];
                delayBuffers[d][delayIndex] = input[i];
                delayIndices[d] = (delayIndex + 1) % delays[d];
            }
            
            output[i] = input[i] * dryLinear + wetSample * wetLinear / numDelays;
        }
        
        return output;
    }
    
    private float[] applyHighPass(float[] input) {
        float[] output = new float[input.length];
        float cutoff = frequency;
        float rc = 1.0f / (2.0f * (float) Math.PI * cutoff);
        float dt = 1.0f / 44100.0f;
        float alpha = rc / (rc + dt);
        
        float prevInput = 0;
        float prevOutput = 0;
        
        for (int i = 0; i < input.length; i++) {
            output[i] = alpha * (prevOutput + input[i] - prevInput);
            prevInput = input[i];
            prevOutput = output[i];
        }
        return output;
    }
    
    private float[] applyLowPass(float[] input) {
        float[] output = new float[input.length];
        float cutoff = frequency;
        float rc = 1.0f / (2.0f * (float) Math.PI * cutoff);
        float dt = 1.0f / 44100.0f;
        float alpha = dt / (rc + dt);
        
        float prevOutput = 0;
        
        for (int i = 0; i < input.length; i++) {
            output[i] = prevOutput + alpha * (input[i] - prevOutput);
            prevOutput = output[i];
        }
        return output;
    }
    
    public String getDisplayName() {
        return type;
    }
    
    public String getParamsDisplay() {
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case TYPE_GAIN:
                sb.append(String.format("%.1fdB", level));
                break;
            case TYPE_EQ:
                sb.append(String.format("%.0fHz %.1fdB Q%.1f", frequency, gain, q));
                break;
            case TYPE_COMPRESSOR:
                sb.append(String.format("%.0fdB %.1f:1", threshold, ratio));
                break;
            case TYPE_REVERB:
                sb.append(String.format("Size:%.0f%% Wet:%.0f%%", roomSize, wet));
                break;
            case TYPE_HIGH_PASS:
            case TYPE_LOW_PASS:
                sb.append(String.format("%.0fHz", frequency));
                break;
        }
        return sb.toString();
    }
    
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("type", type);
        obj.put("enabled", enabled);
        obj.put("level", level);
        obj.put("frequency", frequency);
        obj.put("gain", gain);
        obj.put("q", q);
        obj.put("threshold", threshold);
        obj.put("ratio", ratio);
        obj.put("attack", attack);
        obj.put("release", release);
        obj.put("roomSize", roomSize);
        obj.put("damping", damping);
        obj.put("wet", wet);
        return obj;
    }
    
    public static Effect fromJson(JSONObject obj) throws JSONException {
        Effect effect = new Effect();
        effect.id = obj.optString("id", java.util.UUID.randomUUID().toString());
        effect.type = obj.optString("type", TYPE_GAIN);
        effect.enabled = obj.optBoolean("enabled", true);
        effect.level = (float) obj.optDouble("level", 0);
        effect.frequency = (float) obj.optDouble("frequency", 1000);
        effect.gain = (float) obj.optDouble("gain", 0);
        effect.q = (float) obj.optDouble("q", 1);
        effect.threshold = (float) obj.optDouble("threshold", -20);
        effect.ratio = (float) obj.optDouble("ratio", 4);
        effect.attack = (float) obj.optDouble("attack", 10);
        effect.release = (float) obj.optDouble("release", 100);
        effect.roomSize = (float) obj.optDouble("roomSize", 50);
        effect.damping = (float) obj.optDouble("damping", 50);
        effect.wet = (float) obj.optDouble("wet", 30);
        return effect;
    }
    
    public Effect copy() {
        Effect copy = new Effect();
        copy.id = java.util.UUID.randomUUID().toString();
        copy.type = this.type;
        copy.enabled = this.enabled;
        copy.level = this.level;
        copy.frequency = this.frequency;
        copy.gain = this.gain;
        copy.q = this.q;
        copy.threshold = this.threshold;
        copy.ratio = this.ratio;
        copy.attack = this.attack;
        copy.release = this.release;
        copy.roomSize = this.roomSize;
        copy.damping = this.damping;
        copy.wet = this.wet;
        return copy;
    }
}

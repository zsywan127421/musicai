package com.example.musicai;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ModelConfig {
    
    private static final String PREFS_NAME = "MusicAIConfig";
    private static final String KEY_API_URL = "api_url";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL_NAME = "model_name";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_MAX_TOKENS = "max_tokens";
    
    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL_NAME = "gpt-3.5-turbo";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 500;
    
    private Context context;
    private SharedPreferences prefs;
    
    public ModelConfig(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public String getApiUrl() {
        return prefs.getString(KEY_API_URL, DEFAULT_API_URL);
    }
    
    public void setApiUrl(String url) {
        prefs.edit().putString(KEY_API_URL, url).apply();
    }
    
    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }
    
    public void setApiKey(String key) {
        prefs.edit().putString(KEY_API_KEY, key).apply();
    }
    
    public String getModelName() {
        return prefs.getString(KEY_MODEL_NAME, DEFAULT_MODEL_NAME);
    }
    
    public void setModelName(String name) {
        prefs.edit().putString(KEY_MODEL_NAME, name).apply();
    }
    
    public double getTemperature() {
        return (double) prefs.getFloat(KEY_TEMPERATURE, (float) DEFAULT_TEMPERATURE);
    }
    
    public void setTemperature(double temperature) {
        prefs.edit().putFloat(KEY_TEMPERATURE, (float) temperature).apply();
    }
    
    public int getMaxTokens() {
        return prefs.getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS);
    }
    
    public void setMaxTokens(int tokens) {
        prefs.edit().putInt(KEY_MAX_TOKENS, tokens).apply();
    }
    
    public void resetToDefaults() {
        prefs.edit()
            .putString(KEY_API_URL, DEFAULT_API_URL)
            .putString(KEY_API_KEY, "")
            .putString(KEY_MODEL_NAME, DEFAULT_MODEL_NAME)
            .putFloat(KEY_TEMPERATURE, (float) DEFAULT_TEMPERATURE)
            .putInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS)
            .apply();
    }
    
    public String generateContent(String prompt) throws IOException {
        String apiUrl = getApiUrl();
        String apiKey = getApiKey();
        String modelName = getModelName();
        double temperature = getTemperature();
        int maxTokens = getMaxTokens();
        
        if (apiKey.isEmpty()) {
            throw new IOException("API key not configured");
        }
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", modelName);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            
            org.json.JSONArray messages = new org.json.JSONArray();
            messages.put(message);
            requestBody.put("messages", messages);
        } catch (JSONException e) {
            throw new IOException("Failed to build request", e);
        }
        
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(java.util.concurrent.TimeUnit.MINUTES.toMillis(1), java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(java.util.concurrent.TimeUnit.MINUTES.toMillis(1), java.util.concurrent.TimeUnit.MILLISECONDS)
            .build();
        
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .post(body)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("API request failed: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body() != null ? response.body().string() : "";
            try {
                JSONObject jsonResponse = new JSONObject(responseBody);
                org.json.JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    return message.getString("content").trim();
                }
            } catch (JSONException e) {
                throw new IOException("Failed to parse response", e);
            }
        }
        
        return "";
    }
}

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
    
    private static final String TAG = "ModelConfig";
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
    
    /**
     * 获取完整的 API URL
     * 自动处理 DeepSeek 等 API 的 base URL 补全
     */
    private String getFullApiUrl() {
        String apiUrl = getApiUrl().trim();
        
        // 如果 URL 已经包含 /chat/completions，直接使用
        if (apiUrl.contains("/chat/completions")) {
            return apiUrl;
        }
        
        // 处理 DeepSeek API
        if (apiUrl.contains("deepseek.com")) {
            // 确保使用 https
            if (apiUrl.startsWith("http://")) {
                apiUrl = apiUrl.replace("http://", "https://");
            }
            
            // 如果 URL 以 /v1 结尾，添加 /chat/completions
            if (apiUrl.endsWith("/v1")) {
                return apiUrl + "/chat/completions";
            }
            
            // 如果 URL 是 https://api.deepseek.com，添加 /v1/chat/completions
            if (apiUrl.equals("https://api.deepseek.com") || 
                apiUrl.equals("https://api.deepseek.com/")) {
                return "https://api.deepseek.com/v1/chat/completions";
            }
            
            // 其他情况，确保以 /v1/chat/completions 结尾
            if (!apiUrl.contains("/v1/")) {
                if (apiUrl.endsWith("/")) {
                    return apiUrl + "v1/chat/completions";
                } else {
                    return apiUrl + "/v1/chat/completions";
                }
            }
        }
        
        // 对于其他 API，如果 URL 不以 /chat/completions 结尾，尝试添加
        if (!apiUrl.endsWith("/chat/completions")) {
            if (apiUrl.endsWith("/")) {
                return apiUrl + "chat/completions";
            } else {
                return apiUrl + "/chat/completions";
            }
        }
        
        return apiUrl;
    }
    
    /**
     * 获取适合当前 API 的模型名称
     */
    private String getAppropriateModelName() {
        String modelName = getModelName();
        String apiUrl = getApiUrl();
        
        // 如果是 DeepSeek API，使用 DeepSeek 模型
        if (apiUrl.contains("deepseek")) {
            // 如果用户没有指定 DeepSeek 模型，使用 deepseek-v4-flash
            if (!modelName.contains("deepseek")) {
                return "deepseek-v4-flash";
            }
            // 确保使用 v4 模型
            if (modelName.equals("deepseek-chat")) {
                return "deepseek-v4-flash";
            }
        }
        
        return modelName;
    }
    
    public String generateContent(String prompt) throws IOException {
        String apiUrl = getFullApiUrl();
        String apiKey = getApiKey();
        String modelName = getAppropriateModelName();
        double temperature = getTemperature();
        int maxTokens = getMaxTokens();
        
        Log.d(TAG, "API URL: " + apiUrl);
        Log.d(TAG, "Model: " + modelName);
        
        if (apiKey.isEmpty()) {
            throw new IOException("API key not configured");
        }
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", modelName);
            
            // 对于需要严格格式输出的场景，使用较低的 temperature
            double effectiveTemperature = temperature;
            if (apiUrl.contains("deepseek")) {
                effectiveTemperature = Math.min(0.3, temperature);
            }
            requestBody.put("temperature", effectiveTemperature);
            
            requestBody.put("max_tokens", maxTokens);
            
            // 对于 DeepSeek API，不设置 reasoning_effort
            if (!apiUrl.contains("deepseek")) {
                requestBody.put("reasoning_effort", "none");
            }
            
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
        
        // 构建请求
        Request request = new Request.Builder()
            .url(apiUrl)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();
        
        Log.d(TAG, "Request URL: " + apiUrl);
        Log.d(TAG, "Request Body: " + requestBody.toString());
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                Log.e(TAG, "API request failed: " + response.code() + " - " + responseBody);
                throw new IOException("API request failed: " + response.code() + " - " + responseBody);
            }
            
            Log.d(TAG, "API response: " + responseBody);
            
            try {
                JSONObject jsonResponse = new JSONObject(responseBody);
                
                // 检查是否有错误信息
                if (jsonResponse.has("error")) {
                    JSONObject error = jsonResponse.getJSONObject("error");
                    String errorMessage = error.optString("message", "Unknown API error");
                    throw new IOException("API error: " + errorMessage);
                }
                
                org.json.JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    return message.getString("content").trim();
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse response: " + responseBody, e);
                throw new IOException("Failed to parse response: " + e.getMessage(), e);
            }
        }
        
        return "";
    }
}

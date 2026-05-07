package com.example.musicai;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

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
    
    private String getFullApiUrl() {
        String apiUrl = getApiUrl().trim();
        
        if (apiUrl.contains("/chat/completions")) {
            return apiUrl;
        }
        
        if (apiUrl.contains("deepseek.com")) {
            if (apiUrl.startsWith("http://")) {
                apiUrl = apiUrl.replace("http://", "https://");
            }
            
            if (apiUrl.endsWith("/v1")) {
                return apiUrl + "/chat/completions";
            }
            
            if (apiUrl.equals("https://api.deepseek.com") || 
                apiUrl.equals("https://api.deepseek.com/")) {
                return "https://api.deepseek.com/v1/chat/completions";
            }
            
            if (!apiUrl.contains("/v1/")) {
                if (apiUrl.endsWith("/")) {
                    return apiUrl + "v1/chat/completions";
                } else {
                    return apiUrl + "/v1/chat/completions";
                }
            }
        }
        
        if (!apiUrl.endsWith("/chat/completions")) {
            if (apiUrl.endsWith("/")) {
                return apiUrl + "chat/completions";
            } else {
                return apiUrl + "/chat/completions";
            }
        }
        
        return apiUrl;
    }
    
    private String getAppropriateModelName() {
        String modelName = getModelName();
        String apiUrl = getApiUrl();
        
        if (apiUrl.contains("deepseek")) {
            if (!modelName.contains("deepseek")) {
                return "deepseek-v4-flash";
            }
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
            
            double effectiveTemperature = temperature;
            if (apiUrl.contains("deepseek")) {
                effectiveTemperature = Math.min(0.3, temperature);
            }
            requestBody.put("temperature", effectiveTemperature);
            
            requestBody.put("max_tokens", maxTokens);
            
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
                throw new IOException("Failed to parse response: " + e.getMessage());
            }
        }
        
        return "";
    }
    
    public String testConnection() throws IOException {
        String apiUrl = getApiUrl();
        String apiKey = getApiKey();
        String modelName = getAppropriateModelName();
        
        Log.d(TAG, "Testing connection to: " + apiUrl);
        Log.d(TAG, "Model: " + modelName);
        
        if (apiKey.isEmpty()) {
            return "ERROR:API Key 不能为空";
        }
        
        if (apiUrl.isEmpty()) {
            return "ERROR:API 地址不能为空";
        }
        
        String fullUrl = getFullApiUrl();
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", modelName);
            requestBody.put("max_tokens", 10);
            requestBody.put("temperature", 0.1);
            
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", "Hi");
            
            org.json.JSONArray messages = new org.json.JSONArray();
            messages.put(message);
            requestBody.put("messages", messages);
        } catch (JSONException e) {
            return "ERROR:构建请求失败: " + e.getMessage();
        }
        
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
            .url(fullUrl)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            int statusCode = response.code();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            Log.d(TAG, "Response code: " + statusCode);
            Log.d(TAG, "Response body: " + responseBody);
            
            if (statusCode == 200) {
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.has("model")) {
                        String responseModel = jsonResponse.getString("model");
                        return "SUCCESS:连接成功！模型: " + responseModel;
                    }
                    return "SUCCESS:连接成功！";
                } catch (JSONException e) {
                    return "SUCCESS:连接成功（响应解析异常）";
                }
            } else if (statusCode == 401) {
                return "ERROR:401 认证失败 - API Key 无效或已过期";
            } else if (statusCode == 403) {
                return "ERROR:403 禁止访问 - 权限不足";
            } else if (statusCode == 404) {
                return "ERROR:404 地址错误 - 请检查 API 地址是否正确";
            } else if (statusCode == 429) {
                return "ERROR:429 请求过多 - 请稍后再试";
            } else if (statusCode >= 500) {
                return "ERROR:" + statusCode + " 服务器错误 - 请检查 API 服务状态";
            } else {
                return "ERROR:" + statusCode + " - " + responseBody;
            }
        } catch (SocketTimeoutException e) {
            return "ERROR:连接超时 - 网络缓慢或服务器无响应";
        } catch (UnknownHostException e) {
            return "ERROR:地址解析失败 - 请检查 API 地址是否正确";
        } catch (ConnectException e) {
            return "ERROR:连接失败 - 无法连接到服务器";
        } catch (Exception e) {
            return "ERROR:连接异常 - " + e.getMessage();
        }
    }
}

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
    
    private static final String DEFAULT_API_URL = "https://api.deepseek.com/v1";
    private static final String DEFAULT_MODEL_NAME = "deepseek-chat";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 4096;
    
    private static final int TIMEOUT_SECONDS = 60;
    private static final int MAX_RETRIES = 2;
    
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
        
        return apiUrl;
    }
    
    public AIResponse requestAI(String userPrompt) {
        return requestAIWithSystemPrompt(userPrompt, null);
    }
    
    public AIResponse requestAIWithSystemPrompt(String userPrompt, String systemPrompt) {
        String apiUrl = getFullApiUrl();
        String apiKey = getApiKey();
        String modelName = getModelName();
        double temperature = getTemperature();
        int maxTokens = getMaxTokens();
        
        Log.d(TAG, "API URL: " + apiUrl);
        Log.d(TAG, "Model: " + modelName);
        Log.d(TAG, "Max tokens: " + maxTokens);
        
        if (apiKey.isEmpty()) {
            return new AIResponse(AIResponse.ErrorType.CONFIG_ERROR, "API Key 未配置，请在设置中配置");
        }
        
        if (apiUrl.isEmpty()) {
            return new AIResponse(AIResponse.ErrorType.CONFIG_ERROR, "API 地址未配置");
        }
        
        int retryCount = 0;
        String lastError = "";
        
        while (retryCount <= MAX_RETRIES) {
            AIResponse response = doRequest(apiUrl, apiKey, modelName, temperature, maxTokens, userPrompt, systemPrompt);
            
            if (response.isSuccess()) {
                return response;
            }
            
            lastError = response.errorMessage;
            
            if (response.errorType == AIResponse.ErrorType.API_ERROR ||
                response.errorType == AIResponse.ErrorType.CONFIG_ERROR ||
                response.errorType == AIResponse.ErrorType.NETWORK_ERROR) {
                return response;
            }
            
            retryCount++;
            if (retryCount <= MAX_RETRIES) {
                Log.w(TAG, "Request failed, retrying... (" + retryCount + "/" + MAX_RETRIES + ")");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        return new AIResponse(AIResponse.ErrorType.EMPTY_CONTENT, "生成失败（已重试" + MAX_RETRIES + "次）: " + lastError);
    }
    
    private AIResponse doRequest(String apiUrl, String apiKey, String modelName, double temperature, 
                                 int maxTokens, String userPrompt, String systemPrompt) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", modelName);
            requestBody.put("temperature", Math.min(0.3, temperature));
            requestBody.put("max_tokens", maxTokens);
            
            org.json.JSONArray messages = new org.json.JSONArray();
            
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JSONObject sysMsg = new JSONObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                messages.put(sysMsg);
            }
            
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.put(userMsg);
            
            requestBody.put("messages", messages);
            
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .build();
            
            RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
            );
            
            Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .post(body);
            
            Log.d(TAG, "Request URL: " + apiUrl);
            Log.d(TAG, "Request Body: " + requestBody.toString().substring(0, Math.min(500, requestBody.toString().length())));
            
            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response body length: " + responseBody.length());
                
                if (!response.isSuccessful()) {
                    String errorMsg = getApiErrorMessage(response.code(), responseBody);
                    return new AIResponse(AIResponse.ErrorType.API_ERROR, errorMsg);
                }
                
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    if (jsonResponse.has("error")) {
                        JSONObject error = jsonResponse.getJSONObject("error");
                        String errorMessage = error.optString("message", "未知API错误");
                        return new AIResponse(AIResponse.ErrorType.API_ERROR, "API错误: " + errorMessage);
                    }
                    
                    org.json.JSONArray choices = jsonResponse.optJSONArray("choices");
                    if (choices == null || choices.length() == 0) {
                        return new AIResponse(AIResponse.ErrorType.EMPTY_CONTENT, "AI未返回有效内容，请重试");
                    }
                    
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.optJSONObject("message");
                    if (message == null) {
                        return new AIResponse(AIResponse.ErrorType.PARSE_ERROR, "AI返回格式异常：缺少message字段");
                    }
                    
                    String content = message.optString("content", "").trim();
                    
                    if (content.isEmpty()) {
                        return new AIResponse(AIResponse.ErrorType.EMPTY_CONTENT, "AI生成为空内容，请重试");
                    }
                    
                    Log.d(TAG, "Content length: " + content.length());
                    return new AIResponse(content);
                    
                } catch (JSONException e) {
                    Log.e(TAG, "Parse error: " + responseBody, e);
                    return new AIResponse(AIResponse.ErrorType.PARSE_ERROR, "解析AI返回失败: " + e.getMessage());
                }
            }
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Connection timeout", e);
            return new AIResponse(AIResponse.ErrorType.NETWORK_ERROR, "网络连接超时（" + TIMEOUT_SECONDS + "秒），请检查网络后重试");
        } catch (UnknownHostException e) {
            Log.e(TAG, "Unknown host", e);
            return new AIResponse(AIResponse.ErrorType.NETWORK_ERROR, "无法解析地址，请检查API地址是否正确");
        } catch (ConnectException e) {
            Log.e(TAG, "Connection failed", e);
            return new AIResponse(AIResponse.ErrorType.NETWORK_ERROR, "连接服务器失败，请检查网络或API地址");
        } catch (IOException e) {
            Log.e(TAG, "IO exception", e);
            return new AIResponse(AIResponse.ErrorType.NETWORK_ERROR, "网络异常: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
            return new AIResponse(AIResponse.ErrorType.PARSE_ERROR, "发生错误: " + e.getMessage());
        }
    }
    
    private String getApiErrorMessage(int statusCode, String responseBody) {
        String detail = "";
        try {
            if (responseBody != null && !responseBody.isEmpty()) {
                JSONObject errorJson = new JSONObject(responseBody);
                if (errorJson.has("error")) {
                    JSONObject error = errorJson.getJSONObject("error");
                    detail = error.optString("message", "");
                }
            }
        } catch (Exception e) {
        }
        
        switch (statusCode) {
            case 401:
                return "401 认证失败 - API Key无效或已过期，请检查设置";
            case 403:
                return "403 禁止访问 - 权限不足或账号异常";
            case 404:
                return "404 地址错误 - API地址配置不正确";
            case 429:
                return "429 请求过于频繁 - 请稍后重试";
            case 500:
                return "500 服务器错误 - DeepSeek服务暂时不可用";
            case 502:
            case 503:
                return statusCode + " 服务异常 - 请稍后重试";
            default:
                return statusCode + (detail.isEmpty() ? "" : ": " + detail);
        }
    }
    
    public String generateContent(String prompt) throws IOException {
        AIResponse response = requestAI(prompt);
        if (response.isSuccess()) {
            return response.content;
        } else {
            throw new IOException(response.errorMessage);
        }
    }
    
    public String testConnection() {
        String apiUrl = getApiUrl();
        String apiKey = getApiKey();
        String modelName = getModelName();
        
        Log.d(TAG, "Testing connection to: " + apiUrl);
        
        if (apiKey.isEmpty()) {
            return "ERROR:API Key不能为空";
        }
        
        if (apiUrl.isEmpty()) {
            return "ERROR:API地址不能为空";
        }
        
        String fullUrl = getFullApiUrl();
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", modelName);
            requestBody.put("max_tokens", 50);
            requestBody.put("temperature", 0.1);
            
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", "Say 'OK' if you can hear me.");
            
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
            
            Log.d(TAG, "Test Response code: " + statusCode);
            Log.d(TAG, "Test Response body: " + responseBody);
            
            if (statusCode == 200) {
                return "SUCCESS:连接成功！模型: " + modelName;
            } else {
                return "ERROR:" + getApiErrorMessage(statusCode, responseBody);
            }
        } catch (SocketTimeoutException e) {
            return "ERROR:连接超时 - 网络缓慢或服务器无响应";
        } catch (UnknownHostException e) {
            return "ERROR:地址解析失败 - 请检查API地址";
        } catch (ConnectException e) {
            return "ERROR:连接失败 - 无法连接到服务器";
        } catch (Exception e) {
            return "ERROR:连接异常 - " + e.getMessage();
        }
    }
}

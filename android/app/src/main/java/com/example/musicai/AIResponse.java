package com.example.musicai;

public class AIResponse {
    public enum ErrorType {
        NONE,
        NETWORK_ERROR,
        API_ERROR,
        PARSE_ERROR,
        EMPTY_CONTENT,
        CONFIG_ERROR
    }
    
    public String content;
    public ErrorType errorType;
    public String errorMessage;
    
    public AIResponse() {
        this.content = "";
        this.errorType = ErrorType.NONE;
        this.errorMessage = "";
    }
    
    public AIResponse(String content) {
        this.content = content;
        this.errorType = ErrorType.NONE;
        this.errorMessage = "";
    }
    
    public AIResponse(ErrorType errorType, String errorMessage) {
        this.content = "";
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }
    
    public boolean isSuccess() {
        return errorType == ErrorType.NONE && content != null && !content.trim().isEmpty();
    }
}

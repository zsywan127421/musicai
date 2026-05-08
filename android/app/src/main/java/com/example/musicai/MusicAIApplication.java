package com.example.musicai;

import android.app.Application;
import android.util.Log;
import android.util.AndroidRuntimeException;

public class MusicAIApplication extends Application {
    
    private static final String TAG = "MusicAIApplication";
    private static MusicAIApplication instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception in thread: " + thread.getName(), throwable);
            
            if (throwable instanceof OutOfMemoryError) {
                System.gc();
                Log.w(TAG, "Memory pressure detected, running GC");
            }
            
            if (!(throwable instanceof AndroidRuntimeException && 
                  throwable.getMessage() != null && 
                  throwable.getMessage().contains("finish"))) {
                Log.e(TAG, "Unhandled exception, saving crash log");
                saveCrashLog(throwable);
            }
            
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });
        
        Log.d(TAG, "MusicAI Application initialized");
    }
    
    private void saveCrashLog(Throwable throwable) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Crash at: ").append(java.text.DateFormat.getDateTimeInstance().format(new java.util.Date())).append("\n");
            sb.append("Thread: ").append(Thread.currentThread().getName()).append("\n\n");
            
            Throwable cause = throwable;
            while (cause != null) {
                sb.append("Caused by: ").append(cause.getClass().getName()).append("\n");
                sb.append("Message: ").append(cause.getMessage()).append("\n\n");
                
                for (StackTraceElement element : cause.getStackTrace()) {
                    if (element.getFileName() != null) {
                        sb.append("  at ").append(element.getClassName())
                          .append(".").append(element.getMethodName())
                          .append("(").append(element.getFileName())
                          .append(":").append(element.getLineNumber()).append(")\n");
                    }
                }
                cause = cause.getCause();
                if (cause != null) {
                    sb.append("\n");
                }
            }
            
            java.io.File crashFile = new java.io.File(getExternalFilesDir(null), "crash_" + System.currentTimeMillis() + ".log");
            java.io.FileWriter writer = new java.io.FileWriter(crashFile);
            writer.write(sb.toString());
            writer.close();
            Log.d(TAG, "Crash log saved to: " + crashFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save crash log", e);
        }
    }
    
    public static MusicAIApplication getInstance() {
        return instance;
    }
}

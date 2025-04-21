package com.example.circlea;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

public class CircleAApplication extends Application {
    private static final String TAG = "CircleAApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "Application onCreate started");
        Toast.makeText(this, "CircleA Application Started", Toast.LENGTH_LONG).show();
        
        try {
            // 檢查 Google Play Services
            int playServicesAvailable = com.google.android.gms.common.GoogleApiAvailability
                .getInstance()
                .isGooglePlayServicesAvailable(this);
            Log.d(TAG, "Google Play Services status: " + playServicesAvailable);
            
            // 初始化 Firebase
            Log.d(TAG, "Initializing Firebase...");
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");
            
            // 檢查 Firebase 實例
            if (FirebaseApp.getInstance() != null) {
                Log.d(TAG, "Firebase instance exists");
            } else {
                Log.e(TAG, "Firebase instance is null");
            }
            
            // 獲取 FCM token
            Log.d(TAG, "Requesting FCM token...");
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Fetching FCM registration token failed", task.getException());
                            Toast.makeText(this, 
                                "FCM Token 獲取失敗: " + task.getException().getMessage(), 
                                Toast.LENGTH_LONG).show();
                            return;
                        }

                        String token = task.getResult();
                        Log.d(TAG, "FCM Token successfully retrieved: " + token);
                        Toast.makeText(this, "FCM Token 獲取成功", Toast.LENGTH_SHORT).show();
                        
                        // 保存 token 到 SharedPreferences
                        getSharedPreferences("fcm_token_pref", MODE_PRIVATE)
                            .edit()
                            .putString("fcm_token", token)
                            .apply();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get FCM token", e);
                        Toast.makeText(this, 
                            "FCM Token 獲取失敗: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    });
                    
            // 訂閱默認主題
            FirebaseMessaging.getInstance().subscribeToTopic("all")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Subscribed to 'all' topic");
                        } else {
                            Log.e(TAG, "Failed to subscribe to 'all' topic", task.getException());
                        }
                    });
                    
        } catch (Exception e) {
            Log.e(TAG, "Error during Firebase initialization", e);
            Toast.makeText(this, "Firebase 初始化錯誤: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void sendTokenToServer(String token) {
        // TODO: 實現將 token 發送到伺服器的邏輯
        Log.d(TAG, "Sending FCM token to server...");
        // 在這裡添加將 token 發送到伺服器的代碼
    }
} 
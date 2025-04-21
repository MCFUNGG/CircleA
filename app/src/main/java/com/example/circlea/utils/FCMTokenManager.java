package com.example.circlea.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.example.circlea.Config;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FCMTokenManager {
    private static final String TAG = "FCMTokenManager";
    private static final String FCM_TOKEN_PREF = "fcm_token_pref";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    // 獲取當前的FCM令牌
    public static void getFCMToken(Context context, final TokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        if (callback != null) {
                            callback.onFailure("Failed to get FCM token");
                        }
                        return;
                    }

                    // 獲取新令牌
                    String token = task.getResult();
                    
                    // 保存令牌到SharedPreferences
                    saveTokenToPrefs(context, token);
                    
                    Log.d(TAG, "FCM Token: " + token);
                    if (callback != null) {
                        callback.onSuccess(token);
                    }
                });
    }

    // 保存令牌到SharedPreferences
    private static void saveTokenToPrefs(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(FCM_TOKEN_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_FCM_TOKEN, token);
        editor.apply();
    }

    // 從SharedPreferences獲取令牌
    public static String getTokenFromPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(FCM_TOKEN_PREF, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FCM_TOKEN, "");
    }

    // 將令牌發送到服務器
    public static void sendTokenToServer(Context context, String memberId, String token) {
        Log.d(TAG, "Sending FCM token to server - Member ID: " + memberId + ", Token: " + token);
        
        String url = Config.BASE_URL + "save_fcm_token.php";
        Log.d(TAG, "Request URL: " + url);

        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(
                com.android.volley.Request.Method.POST, 
                url,
                response -> {
                    Log.d(TAG, "Server response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.getString("message");
                        
                        Log.d(TAG, "Response parsed - Success: " + success + ", Message: " + message);
                        
                        if (jsonResponse.has("debug")) {
                            Log.d(TAG, "Debug info: " + jsonResponse.getString("debug"));
                        }
                        
                        if (success) {
                            Log.d(TAG, "Token successfully saved to server");
                        } else {
                            Log.e(TAG, "Failed to save token: " + message);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing server response: " + e.getMessage());
                        Log.e(TAG, "Raw response: " + response);
                    }
                },
                error -> {
                    Log.e(TAG, "Network error occurred");
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Error status code: " + error.networkResponse.statusCode);
                        Log.e(TAG, "Error data: " + new String(error.networkResponse.data));
                    }
                    Log.e(TAG, "Error details: " + error.toString());
                    if (error.getCause() != null) {
                        Log.e(TAG, "Error cause: " + error.getCause().getMessage());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("member_id", memberId);
                params.put("token", token);
                
                Log.d(TAG, "Request parameters - Member ID: " + memberId);
                Log.d(TAG, "Request parameters - Token length: " + token.length());
                
                return params;
            }
        };

        Log.d(TAG, "Adding request to queue");
        queue.add(stringRequest);
    }

    // 回調接口
    public interface TokenCallback {
        void onSuccess(String token);
        void onFailure(String errorMessage);
    }
} 
package com.example.circlea.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.circlea.Home;
import com.example.circlea.R;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;
import java.util.Map;

public class FCMService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "CircleA_Channel";
    private static final String CHANNEL_NAME = "CircleA Notifications";
    private static final String CHANNEL_DESC = "Notifications for CircleA application";

    static {
        Log.d(TAG, "=== FCMService class loaded ===");
        Log.d(TAG, "Static initialization block executed");
    }

    public FCMService() {
        super();
        Log.d(TAG, "=== FCMService constructor called ===");
        Log.d(TAG, "Service instance created");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "=== FCMService Created ===");
        
        try {
            createNotificationChannel();
            Log.d(TAG, "Notification channel created successfully in onCreate");
            
            // 檢查通知權限
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Notification permission not granted");
                } else {
                    Log.d(TAG, "Notification permission granted");
                }
            }
            
            // 獲取當前 FCM Token
            FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d(TAG, "Current FCM Token in Service: " + token);
                    } else {
                        Log.e(TAG, "Failed to get FCM token in service", task.getException());
                    }
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "=== New Token Received in Service ===");
        Log.d(TAG, "Token: " + token);
        
        try {
            // 保存新令牌到SharedPreferences
            getSharedPreferences("fcm_token_pref", Context.MODE_PRIVATE)
                    .edit()
                    .putString("fcm_token", token)
                    .apply();
            
            Log.d(TAG, "Token saved to SharedPreferences");
            
            // 如果用戶已登錄，將新令牌發送到服務器
            String memberId = getSharedPreferences("CircleA", MODE_PRIVATE)
                    .getString("member_id", null);
            
            Log.d(TAG, "Current member ID: " + memberId);
            
            if (memberId != null && !memberId.isEmpty()) {
                FCMTokenManager.sendTokenToServer(this, memberId, token);
                Log.d(TAG, "Sending new token to server for member: " + memberId);
            } else {
                Log.d(TAG, "No member ID found, token will be saved later when user logs in");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onNewToken: " + e.getMessage(), e);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "=== Message Received in Service ===");
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        
        try {
            // 檢查消息類型
            if (remoteMessage.getData().size() > 0) {
                Log.d(TAG, "Message data payload: " + remoteMessage.getData());
                handleDataMessage(remoteMessage.getData());
            }

            // 檢查通知
            if (remoteMessage.getNotification() != null) {
                Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
                
                // 立即顯示通知
                sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    remoteMessage.getData()
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing message: " + e.getMessage(), e);
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        try {
            Log.d(TAG, "=== Handling Data Message ===");
            Log.d(TAG, "Raw data received: " + new JSONObject(data).toString(2));
            
            // 檢查是否包含 debug 信息
            String fcmDebug = data.get("fcm_debug");
            if (fcmDebug != null) {
                Log.d(TAG, "FCM Debug Info: " + fcmDebug);
            }
            
            // 檢查是否包含錯誤信息
            String error = data.get("error");
            if (error != null) {
                Log.e(TAG, "Error from server: " + error);
            }
            
            String type = data.get("type");
            String matchId = data.get("match_id");
            String studentName = data.get("student_name");
            
            Log.d(TAG, "Message Type: " + type);
            Log.d(TAG, "Match ID: " + matchId);
            Log.d(TAG, "Student Name: " + studentName);
            
            // 檢查成功狀態
            String success = data.get("success");
            if (success != null) {
                Log.d(TAG, "Success status: " + success);
            }
            
            // 檢查消息
            String message = data.get("message");
            if (message != null) {
                Log.d(TAG, "Server message: " + message);
            }
            
            if ("new_request".equals(type)) {
                Log.d(TAG, "Processing new request notification");
                String title = "新補習請求";
                String notificationMessage = studentName != null ? 
                    studentName + " 向您發送了補習請求" : 
                    "您收到了一個新的補習請求";
                
                Log.d(TAG, "Preparing notification - Title: " + title);
                Log.d(TAG, "Preparing notification - Message: " + notificationMessage);
                
                // 立即顯示通知
                sendNotification(title, notificationMessage, data);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling data message: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
                
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created: " + CHANNEL_ID);
            }
        }
    }

    private void sendNotification(String title, String messageBody, Map<String, String> data) {
        try {
            Intent intent = new Intent(this, Home.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            // 添加數據到 intent
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
            );

            String channelId = CHANNEL_ID;
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            
            NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle(title)
                    .setContentText(messageBody)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.notify(0, notificationBuilder.build());
                Log.d(TAG, "Notification sent successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification: " + e.getMessage(), e);
        }
    }
} 
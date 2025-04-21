package com.example.circlea.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.example.circlea.Config;
import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FCMTestActivity extends AppCompatActivity {
    private static final String TAG = "FCMTestActivity";
    private TextView logTextView;
    private ScrollView scrollView;
    private String currentToken = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 創建並設置佈局
        createTestLayout();
        
        // 初始化 Firebase
        initializeFirebase();
    }
    
    private void initializeFirebase() {
        log("正在初始化 Firebase...");
        
        // 獲取當前 FCM Token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentToken = task.getResult();
                        log("FCM Token 獲取成功：" + currentToken);
                    } else {
                        log("FCM Token 獲取失敗：" + task.getException().getMessage());
                    }
                });
    }
    
    private void createTestLayout() {
        // 創建一個垂直線性佈局
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);
        
        // 添加標題
        TextView titleView = new TextView(this);
        titleView.setText("FCM 測試面板");
        titleView.setTextSize(20);
        titleView.setPadding(0, 0, 0, 16);
        layout.addView(titleView);
        
        // 添加測試按鈕
        addButton(layout, "獲取當前 Token", this::getCurrentToken);
        addButton(layout, "創建本地通知", this::createLocalNotification);
        addButton(layout, "發送測試 FCM 消息", this::sendTestFCMMessage);
        addButton(layout, "檢查通知權限", this::checkNotificationPermission);
        addButton(layout, "清除日誌", () -> logTextView.setText(""));
        
        // 添加日誌視圖
        scrollView = new ScrollView(this);
        logTextView = new TextView(this);
        logTextView.setPadding(8, 8, 8, 8);
        scrollView.addView(logTextView);
        layout.addView(scrollView);
        
        // 設置佈局
        setContentView(layout);
    }
    
    private void addButton(LinearLayout layout, String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(v -> action.run());
        layout.addView(button);
    }
    
    private void getCurrentToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        log("當前 FCM Token：" + token);
                        // 複製到剪貼板
                        android.content.ClipboardManager clipboard = 
                            (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = 
                            android.content.ClipData.newPlainText("FCM Token", token);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Token 已複製到剪貼板", Toast.LENGTH_SHORT).show();
                    } else {
                        log("獲取 Token 失敗：" + task.getException().getMessage());
                    }
                });
    }
    
    private void createLocalNotification() {
        log("創建本地通知...");
        
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            
        // 為 Android O 及更高版本創建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                Config.NOTIFICATION_CHANNEL_ID,
                Config.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(Config.NOTIFICATION_CHANNEL_DESC);
            notificationManager.createNotificationChannel(channel);
        }
        
        // 創建通知
        NotificationCompat.Builder builder = 
            new NotificationCompat.Builder(this, Config.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("測試通知")
                .setContentText("這是一條本地測試通知")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                
        // 發送通知
        notificationManager.notify(1, builder.build());
        log("本地通知已發送");
    }
    
    private void sendTestFCMMessage() {
        log("發送 FCM 測試消息...");
        
        if (currentToken.isEmpty()) {
            log("錯誤：FCM Token 為空");
            return;
        }
        
        // 準備 FCM 消息
        try {
            // 使用與其他活動相同的 URL 構建方式
            String serverUrl = "http://" + IPConfig.getIP() + "/FYP/php/send_fcm_test.php";
            log("發送請求到：" + serverUrl);
            
            // 準備請求數據
            JSONObject requestData = new JSONObject();
            requestData.put("token", currentToken);
            requestData.put("title", "測試通知");
            requestData.put("message", "這是一條測試消息");
            requestData.put("type", "test");
            
            log("請求數據：" + requestData.toString());
            
            // 發送請求
            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), 
                requestData.toString()
            );
            
            Request request = new Request.Builder()
                .url(serverUrl)
                .post(body)
                .build();
                
            log("發送請求中...");
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log("發送失敗：" + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    log("伺服器響應：" + responseBody);
                    
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        boolean success = jsonResponse.optBoolean("success", false);
                        String message = jsonResponse.optString("message", "未知錯誤");
                        
                        if (success) {
                            log("測試消息發送成功：" + message);
                        } else {
                            log("測試消息發送失敗：" + message);
                        }
                        
                        // 檢查是否有調試信息
                        if (jsonResponse.has("debug")) {
                            log("調試信息：" + jsonResponse.getString("debug"));
                        }
                    } catch (Exception e) {
                        log("解析響應失敗：" + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            log("錯誤：" + e.getMessage());
        }
    }
    
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            NotificationChannel channel = manager.getNotificationChannel(Config.NOTIFICATION_CHANNEL_ID);
            if (channel != null) {
                log("通知渠道狀態：");
                log("名稱：" + channel.getName());
                log("描述：" + channel.getDescription());
                log("重要性：" + channel.getImportance());
            } else {
                log("通知渠道未創建");
            }
        }
        
        // 檢查通知權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                log("已獲得通知權限");
            } else {
                log("未獲得通知權限");
                requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 
                    1);
            }
        } else {
            log("設備版本低於 Android 13，無需請求通知權限");
        }
    }
    
    private void log(String message) {
        runOnUiThread(() -> {
            Log.d(TAG, message);
            logTextView.append(message + "\n");
            // 滾動到底部
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }
} 
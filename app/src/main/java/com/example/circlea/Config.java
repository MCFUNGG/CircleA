package com.example.circlea;

public class Config {
    // 基礎 URL
    public static final String BASE_URL = "http://" + IPConfig.getIP() + "/FYP/php/";
    
    // FCM 相關配置
    public static final String FCM_SERVER_KEY = "AIzaSyBdB0YGOehtQfgfwG1o4uBKG2gqUC8f1wE";
    
    // API 端點
    public static final String API_SAVE_FCM_TOKEN = BASE_URL + "save_fcm_token.php";
    public static final String API_SEND_FCM_TEST = BASE_URL + "send_fcm_test.php";
    
    // 通知渠道配置
    public static final String NOTIFICATION_CHANNEL_ID = "CircleA_Channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "CircleA Notifications";
    public static final String NOTIFICATION_CHANNEL_DESC = "Notifications for CircleA application";
    
    // SharedPreferences 配置
    public static final String PREF_NAME = "CircleA";
    public static final String PREF_FCM_TOKEN = "fcm_token";
    public static final String PREF_MEMBER_ID = "member_id";
} 
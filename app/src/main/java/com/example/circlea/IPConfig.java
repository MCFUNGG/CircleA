package com.example.circlea;

import android.os.Build;

public class IPConfig {

    //remember also set the network security config.xml

    // 服务器配置
    private static final String EMULATOR_IP = "10.0.2.2";        // 模擬器使用
    private static final String REAL_DEVICE_IP = "192.168.0.10";  // 實體設備使用
    private static final String INFINITYFREE_DOMAIN = "circlea-app-a7e9dnbxg0ggerhz.eastasia-01.azurewebsites.net"; 
    
    // 環境控制
    private static final boolean USE_PRODUCTION = false; // true使用在線服務器，false使用本地開發

    public static String getIP() {
        if (USE_PRODUCTION) {
            return INFINITYFREE_DOMAIN;
        } else {
            return isEmulator() ? EMULATOR_IP : REAL_DEVICE_IP;
        }
    }

    // 檢測是否在模擬器上運行
    private static boolean isEmulator() {
        return Build.PRODUCT.contains("sdk") || 
               Build.PRODUCT.contains("google_sdk") || 
               Build.PRODUCT.contains("emulator") || 
               Build.PRODUCT.contains("simulator") ||
               Build.FINGERPRINT.startsWith("generic") ||
               Build.FINGERPRINT.startsWith("unknown") ||
               Build.MODEL.contains("google_sdk") ||
               Build.MODEL.contains("Emulator") ||
               Build.MODEL.contains("Android SDK built for x86");
    }

    public static String getBaseUrl() {
        String protocol = USE_PRODUCTION ? "https://" : "http://";
        return protocol + getIP();
    }

    public static String getApiPath() {
        return "/FYP/php";
    }

    public static String getFullApiUrl(String endpoint) {
        return getBaseUrl() + getApiPath() + endpoint;
    }

    public static String getImageUrl(String imagePath) {
        if(imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        return getBaseUrl() + "/uploads/" + imagePath;
    }
}
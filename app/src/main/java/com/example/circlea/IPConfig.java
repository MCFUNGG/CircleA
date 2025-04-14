package com.example.circlea;

public class IPConfig {

    //remember also set the network security config.xml

    // 服务器配置
    private static final String DEVELOPMENT_IP = "10.0.2.2"; // 本地开发
    private static final String INFINITYFREE_DOMAIN = "circlea-app-a7e9dnbxg0ggerhz.eastasia-01.azurewebsites.net"; // 从InfinityFree获取的域名
    
    // 环境控制
    private static final boolean USE_PRODUCTION = false; // true使用在线服务器，false使用本地开发

    public static String getIP() {
        return USE_PRODUCTION ? INFINITYFREE_DOMAIN : DEVELOPMENT_IP;
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
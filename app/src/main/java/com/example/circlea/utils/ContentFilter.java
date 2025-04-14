package com.example.circlea.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 內容過濾器 - 用於檢測和過濾描述中的敏感詞彙和個人信息
 */
public class ContentFilter {

    
    // 電話號碼模式（香港格式）
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?(?:852|853|86)[-\\s]?)?(?:\\d{4}[-\\s]?\\d{4}|\\d{8})");
    
    // 電子郵件模式
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    
    // WhatsApp ID模式
    private static final Pattern WHATSAPP_PATTERN = Pattern.compile("(?:whatsapp|wa|wechat|微信):?\\s*[a-zA-Z0-9_]+|[a-zA-Z0-9_]+\\s*(?:whatsapp|wa|wechat|微信)");
    
    // 地址模式（簡單版本，可能需要調整）
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            // 中文地址格式
            "(?:香港|九龍|新界|離島)(?:[\\u4e00-\\u9fa5]|\\w)+(?:路|街|道|里|村|大廈|花園|邨|苑|樓)\\s*(?:\\d+[a-zA-Z]?號?|\\d+樓)?|" +

                    // 英文地址格式 - 街道
                    "\\d+\\s+[A-Za-z\\s]+(?:Street|Road|Avenue|Lane|Path|Drive|Highway|Terrace|Close|Square)|" +

                    // 英文地址格式 - 建築物/屋苑
                    "[A-Za-z\\s]+(?:Building|Tower|Estate|Garden|House|Court|Mansion|Centre|Center|Plaza|Heights)|" +

                    // 英文地址格式 - 香港區域
                    "(?:Central|Wan Chai|Causeway Bay|North Point|Quarry Bay|Chai Wan|Kennedy Town|Sheung Wan|" +
                    "Sai Ying Pun|Pok Fu Lam|Aberdeen|Ap Lei Chau|Stanley|Repulse Bay|Tsim Sha Tsui|" +
                    "Yau Ma Tei|Mong Kok|Sham Shui Po|Cheung Sha Wan|Lai Chi Kok|Mei Foo|Kowloon Tong|" +
                    "Diamond Hill|Wong Tai Sin|Ngau Tau Kok|Kwun Tong|Lam Tin|Yau Tong|Tseung Kwan O|" +
                    "Hang Hau|Tiu Keng Leng|Sai Kung|Clear Water Bay|Sha Tin|Tai Wai|Fo Tan|" +
                    "Ma On Shan|Tai Po|Fanling|Sheung Shui|Yuen Long|Tin Shui Wai|Tuen Mun|" +
                    "Tsuen Wan|Kwai Chung|Tsing Yi|Tung Chung|Discovery Bay|Ma Wan|Cheung Chau|" +
                    "Lantau Island|Lamma Island)"
    );

    /**
     * 檢查內容是否包含敏感詞或個人信息
     * @param content 要檢查的內容
     * @return 包含敏感內容的結果對象
     */
    public static ContentCheckResult checkContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ContentCheckResult(false, false, null);
        }
        
        // 檢查敏感詞
        boolean hasBadWords = false;
        String badWord = null;
        

        
        // 檢查個人信息
        boolean hasPersonalInfo = PHONE_PATTERN.matcher(content).find() ||
                                  EMAIL_PATTERN.matcher(content).find() ||
                                  WHATSAPP_PATTERN.matcher(content).find() ||
                                  ADDRESS_PATTERN.matcher(content).find();
        
        return new ContentCheckResult(hasBadWords, hasPersonalInfo, badWord);
    }
    
    /**
     * 過濾內容中的敏感詞和個人信息
     * @param content 原始內容
     * @return 過濾後的內容
     */
    public static String filterContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        String filtered = content;
        

        
        // 過濾電話號碼
        filtered = PHONE_PATTERN.matcher(filtered).replaceAll("***-****");
        
        // 過濾電子郵件
        filtered = EMAIL_PATTERN.matcher(filtered).replaceAll("***@***.com");
        
        // 過濾WhatsApp ID
        filtered = WHATSAPP_PATTERN.matcher(filtered).replaceAll("WA:***");
        
        // 過濾地址
        filtered = ADDRESS_PATTERN.matcher(filtered).replaceAll("***地址***");
        
        return filtered;
    }
    
    /**
     * 產生星號字符串替代敏感詞
     * @param length 長度
     * @return 星號字符串
     */
    private static String generateStars(int length) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < length; i++) {
            stars.append("*");
        }
        return stars.toString();
    }
    
    /**
     * 獲取默認的警告消息
     * @param result 內容檢查結果
     * @return 警告消息
     */
    public static String getWarningMessage(ContentCheckResult result) {
        if (result.hasBadWords && result.hasPersonalInfo) {
            return "描述包含不適當詞彙和個人信息。請修改後再提交。";
        } else if (result.hasBadWords) {
            return "描述包含不適當詞彙：" + result.badWord + "。請修改後再提交。";
        } else if (result.hasPersonalInfo) {
            return "描述包含個人信息。為保護隱私，請勿在描述中包含電話號碼、電子郵件或地址等。";
        }
        return null;
    }
    
    /**
     * 內容檢查結果類
     */
    public static class ContentCheckResult {
        public final boolean hasBadWords;
        public final boolean hasPersonalInfo;
        public final String badWord;
        
        public ContentCheckResult(boolean hasBadWords, boolean hasPersonalInfo, String badWord) {
            this.hasBadWords = hasBadWords;
            this.hasPersonalInfo = hasPersonalInfo;
            this.badWord = badWord;
        }
        
        public boolean isClean() {
            return !hasBadWords && !hasPersonalInfo;
        }
    }
} 
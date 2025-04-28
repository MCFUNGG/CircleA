package com.example.circlea.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * 帮助翻译数据库中的英文内容到用户界面显示的语言
 */
public class TranslationHelper {
    private static final String PREFS_NAME = "LanguagePrefs";
    private static final String LANGUAGE_KEY = "SelectedLanguage";
    
    private static Map<String, String> districtTranslations = new HashMap<>();
    private static Map<String, String> subjectTranslations = new HashMap<>();
    private static Map<String, String> classLevelTranslations = new HashMap<>();
    private static Map<String, String> statusTranslations = new HashMap<>();
    
    // 初始化翻译映射表
    static {
        // 地区名称翻译
        districtTranslations.put("Central and Western", "中西區");
        districtTranslations.put("Eastern", "東區");
        districtTranslations.put("Southern", "南區");
        districtTranslations.put("Wan Chai", "灣仔");
        districtTranslations.put("Kowloon City", "九龍城");
        districtTranslations.put("Yau Tsim Mong", "油尖旺");
        districtTranslations.put("Sham Shui Po", "深水埗");
        districtTranslations.put("Wong Tai Sin", "黃大仙");
        districtTranslations.put("Kwun Tong", "觀塘");
        districtTranslations.put("Tai Po", "大埔");
        districtTranslations.put("Yuen Long", "元朗");
        districtTranslations.put("Tuen Mun", "屯門");
        districtTranslations.put("North", "北區");
        districtTranslations.put("Sai Kung", "西貢");
        districtTranslations.put("Sha Tin", "沙田");
        districtTranslations.put("Tsuen Wan", "荃灣");
        districtTranslations.put("Kwai Tsing", "葵青");
        districtTranslations.put("Islands", "離島");
        
        // 科目名称翻译
        subjectTranslations.put("Chinese Language", "中國語文");
        subjectTranslations.put("English Language", "英國語文");
        subjectTranslations.put("Mathematics", "數學");
        subjectTranslations.put("Liberal Studies", "通識教育");
        subjectTranslations.put("Physics", "物理");
        subjectTranslations.put("Chemistry", "化學");
        subjectTranslations.put("Biology", "生物");
        subjectTranslations.put("Combined Science", "組合科學");
        subjectTranslations.put("Geography", "地理");
        subjectTranslations.put("History", "歷史");
        subjectTranslations.put("Chinese History", "中國歷史");
        subjectTranslations.put("Economics", "經濟");
        subjectTranslations.put("Business, Accounting and Financial Studies", "企業、會計與財務概論");
        subjectTranslations.put("Information and Communication Technology", "資訊及通訊科技");
        subjectTranslations.put("Citizenship and Social Development", "公民與社會發展");
        subjectTranslations.put("Tourism and Hospitality Studies", "旅遊與款待");
        subjectTranslations.put("Literature in English", "英國文學");
        subjectTranslations.put("Chinese Literature", "中國文學");
        subjectTranslations.put("Ethics and Religious Studies", "倫理與宗教");
        subjectTranslations.put("Design and Applied Technology", "設計與應用科技");
        subjectTranslations.put("Health Management and Social Care", "健康管理與社會關懷");
        subjectTranslations.put("Technology and Living", "科技與生活");
        subjectTranslations.put("Music", "音樂");
        subjectTranslations.put("Visual Arts", "視覺藝術");
        subjectTranslations.put("Physical Education", "體育");
        subjectTranslations.put("General Studies", "常識");
        
        // 班级水平翻译
        classLevelTranslations.put("Kindergarten - K.1", "幼稚園 - K.1");
        classLevelTranslations.put("Kindergarten - K.2", "幼稚園 - K.2");
        classLevelTranslations.put("Kindergarten - K.3", "幼稚園 - K.3");
        classLevelTranslations.put("Primary - P.1", "小學 - P.1");
        classLevelTranslations.put("Primary - P.2", "小學 - P.2");
        classLevelTranslations.put("Primary - P.3", "小學 - P.3");
        classLevelTranslations.put("Primary - P.4", "小學 - P.4");
        classLevelTranslations.put("Primary - P.5", "小學 - P.5");
        classLevelTranslations.put("Primary - P.6", "小學 - P.6");
        classLevelTranslations.put("Secondary - S.1", "中學 - S.1");
        classLevelTranslations.put("Secondary - S.2", "中學 - S.2");
        classLevelTranslations.put("Secondary - S.3", "中學 - S.3");
        classLevelTranslations.put("Secondary - S.4", "中學 - S.4");
        classLevelTranslations.put("Secondary - S.5", "中學 - S.5");
        classLevelTranslations.put("Secondary - S.6", "中學 - S.6");
        classLevelTranslations.put("Primary school - P.1", "小學 - P.1");
        classLevelTranslations.put("Primary school - P.2", "小學 - P.2");
        classLevelTranslations.put("Primary school - P.3", "小學 - P.3");
        classLevelTranslations.put("Primary school - P.4", "小學 - P.4");
        classLevelTranslations.put("Primary school - P.5", "小學 - P.5");
        classLevelTranslations.put("Primary school - P.6", "小學 - P.6");
        classLevelTranslations.put("Secondary School - F.1", "中學 - F.1");
        classLevelTranslations.put("Secondary School - F.2", "中學 - F.2");
        classLevelTranslations.put("Secondary School - F.3", "中學 - F.3");
        classLevelTranslations.put("Secondary School - F.4", "中學 - F.4");
        classLevelTranslations.put("Secondary School - F.5", "中學 - F.5");
        classLevelTranslations.put("Secondary School - F.6", "中學 - F.6");
        classLevelTranslations.put("University - College freshman", "大學 - 大一");
        classLevelTranslations.put("University - Sophomore", "大學 - 大二");
        classLevelTranslations.put("University - Third year", "大學 - 大三");
        classLevelTranslations.put("University - Senior year", "大學 - 大四");
        
        // 状态翻译
        statusTranslations.put("Pending", "待處理");
        statusTranslations.put("Approved", "已批准");
        statusTranslations.put("Rejected", "已拒絕");
        statusTranslations.put("Completed", "已完成");
        statusTranslations.put("Incomplete", "未完成");
    }
    
    /**
     * 根据当前语言设置翻译文本
     * @param context 上下文
     * @param englishText 英文文本
     * @param textType 文本类型 (district, subject, class_level, status)
     * @return 翻译后的文本
     */
    public static String translateText(Context context, String englishText, String textType) {
        if (englishText == null || englishText.isEmpty()) {
            return "";
        }
        
        // 获取当前语言设置
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String currentLanguage = prefs.getString(LANGUAGE_KEY, "en");
        
        // 如果是英文，直接返回原文本
        if (currentLanguage.equals("en")) {
            return englishText;
        }
        
        // 根据文本类型选择对应的翻译映射
        Map<String, String> translationMap;
        switch (textType) {
            case "district":
                translationMap = districtTranslations;
                break;
            case "subject":
                translationMap = subjectTranslations;
                break;
            case "class_level":
                translationMap = classLevelTranslations;
                break;
            case "status":
                translationMap = statusTranslations;
                break;
            default:
                return englishText;
        }
        
        // 查找翻译
        String translatedText = translationMap.get(englishText);
        return (translatedText != null) ? translatedText : englishText;
    }
    
    /**
     * 翻译地区名称
     */
    public static String translateDistrict(Context context, String englishName) {
        return translateText(context, englishName, "district");
    }
    
    /**
     * 翻译科目名称
     */
    public static String translateSubject(Context context, String englishName) {
        return translateText(context, englishName, "subject");
    }
    
    /**
     * 翻译班级水平
     */
    public static String translateClassLevel(Context context, String englishName) {
        return translateText(context, englishName, "class_level");
    }
    
    /**
     * 翻译状态
     */
    public static String translateStatus(Context context, String englishStatus) {
        return translateText(context, englishStatus, "status");
    }
} 
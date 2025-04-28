package com.example.circlea.profile;

import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TutorProfileActivity extends AppCompatActivity {

    private static final String DEBUG_TAG = "TutorProfileActivity";

    /**
     * 过滤香港大学信息，使用更精确的匹配逻辑
     */
    private String filterHKUniversities(String education) {
        if (education == null || education.isEmpty() || education.equals("null")) {
            return null;
        }
        
        // 大学名称映射（完整名称和缩写 -> 标准中文名）
        Map<String, String> universityMap = new HashMap<>();
        universityMap.put("香港大學", "香港大學");
        universityMap.put("University of Hong Kong", "香港大學");
        universityMap.put("HKU", "香港大學");
        universityMap.put("香港中文大學", "香港中文大學");
        universityMap.put("Chinese University of Hong Kong", "香港中文大學");
        universityMap.put("CUHK", "香港中文大學");
        universityMap.put("香港科技大學", "香港科技大學");
        universityMap.put("Hong Kong University of Science and Technology", "香港科技大學");
        universityMap.put("HKUST", "香港科技大學");
        universityMap.put("香港理工大學", "香港理工大學");
        universityMap.put("Hong Kong Polytechnic University", "香港理工大學");
        universityMap.put("PolyU", "香港理工大學");
        universityMap.put("香港城市大學", "香港城市大學");
        universityMap.put("City University of Hong Kong", "香港城市大學");
        universityMap.put("CityU", "香港城市大學");
        universityMap.put("香港浸會大學", "香港浸會大學");
        universityMap.put("Hong Kong Baptist University", "香港浸會大學");
        universityMap.put("HKBU", "香港浸會大學");
        universityMap.put("嶺南大學", "嶺南大學");
        universityMap.put("Lingnan University", "嶺南大學");
        universityMap.put("LU", "嶺南大學");
        universityMap.put("香港教育大學", "香港教育大學");
        universityMap.put("Education University of Hong Kong", "香港教育大學");
        universityMap.put("EdUHK", "香港教育大學");
        universityMap.put("香港樹仁大學", "香港樹仁大學");
        universityMap.put("Hong Kong Shue Yan University", "香港樹仁大學");
        universityMap.put("HKSYU", "香港樹仁大學");
        
        // 精确匹配检查（全词匹配）
        String[] words = education.split("\\s+|,|\\.|;|\\(|\\)|\\[|\\]|\\{|\\}");
        for (String word : words) {
            String trimmedWord = word.trim();
            if (!trimmedWord.isEmpty() && universityMap.containsKey(trimmedWord)) {
                Log.d(DEBUG_TAG, "Found exact match for: " + trimmedWord);
                return universityMap.get(trimmedWord);
            }
        }
        
        // 按缩写长度降序排序（确保HKUST在HKU之前检查）
        List<String> acronyms = new ArrayList<>();
        List<String> fullNames = new ArrayList<>();
        
        for (String uniName : universityMap.keySet()) {
            if (uniName.matches("[A-Z]+")) {
                acronyms.add(uniName);
            } else {
                fullNames.add(uniName);
            }
        }
        
        // 按长度降序排序缩写，确保较长的缩写(如HKUST)在较短的缩写(如HKU)之前检查
        Collections.sort(acronyms, (a, b) -> b.length() - a.length());
        
        // 首先检查缩写的词边界匹配
        for (String acronym : acronyms) {
            String pattern = "\\b" + acronym + "\\b";
            if (Pattern.compile(pattern).matcher(education).find()) {
                Log.d(DEBUG_TAG, "Found acronym match for: " + acronym);
                return universityMap.get(acronym);
            }
        }
        
        // 然后检查全名匹配
        Collections.sort(fullNames, (a, b) -> b.length() - a.length());
        for (String fullName : fullNames) {
            if (education.contains(fullName)) {
                Log.d(DEBUG_TAG, "Found full name match for: " + fullName);
                return universityMap.get(fullName);
            }
        }
        
        // 如果没有找到匹配，返回原始信息
        return education;
    }
} 
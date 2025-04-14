package com.example.circlea.home;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.circlea.IPConfig;
import com.example.circlea.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TutorProfileActivity extends AppCompatActivity {

    private static final String TAG = "TutorProfileActivity";
    
    // UI 元素
    private TextView nameTextView;
    private TextView profileDescTextView;
    private TextView educationTextView;
    private TextView aboutMeTextView;
    private ImageButton backButton;
    private de.hdodenhof.circleimageview.CircleImageView tutorImageView;
    
    // 數據
    private String tutorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_profile);
        
        // 獲取傳遞的導師ID
        tutorId = getIntent().getStringExtra("tutor_id");
        
        // 初始化視圖
        initViews();
        
        // 設置返回按鈕點擊事件
        backButton.setOnClickListener(v -> finish());
        
        // 如果有導師ID，則獲取導師資料
        if (tutorId != null && !tutorId.isEmpty()) {
            Log.d(TAG, "Fetching tutor data for ID: " + tutorId);
            fetchTutorData(tutorId);
        } else {
            Log.e(TAG, "No tutor ID provided!");
            Toast.makeText(this, "無法顯示導師資料，導師ID不存在", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void initViews() {
        nameTextView = findViewById(R.id.tutor_name);
        profileDescTextView = findViewById(R.id.tutor_profile_desc);
        educationTextView = findViewById(R.id.tutor_education);
        aboutMeTextView = findViewById(R.id.about_me_text);
        backButton = findViewById(R.id.back_button);
        tutorImageView = findViewById(R.id.tutor_image);
    }
    
    private void fetchTutorData(String tutorId) {
        OkHttpClient client = new OkHttpClient();
        
        // 使用會員詳情API獲取導師基本資料
        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_member_detail.php";
        
        Log.d(TAG, "Fetching tutor data for ID: " + tutorId + " from URL: " + url);
        
        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", tutorId)
                .build();
                
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
                
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch tutor data: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(TutorProfileActivity.this, 
                            "無法獲取導師資料，請檢查網絡連接", Toast.LENGTH_SHORT).show();
                    // 無法獲取數據時顯示基本資訊
                    displayBasicInfo(tutorId);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Server error: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(TutorProfileActivity.this, 
                                "服務器錯誤，請稍後再試", Toast.LENGTH_SHORT).show();
                        // 服務器錯誤時顯示基本資訊
                        displayBasicInfo(tutorId);
                    });
                    return;
                }
                
                String jsonResponse = response.body().string();
                Log.d(TAG, "Server response: " + jsonResponse);
                
                try {
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    
                    if (!jsonObject.optBoolean("success", false)) {
                        String message = jsonObject.optString("message", "未知錯誤");
                        Log.e(TAG, "API error: " + message);
                        runOnUiThread(() -> {
                            Toast.makeText(TutorProfileActivity.this, 
                                    "無法獲取導師資料: " + message, Toast.LENGTH_SHORT).show();
                            // API錯誤時顯示基本資訊
                            displayBasicInfo(tutorId);
                        });
                        return;
                    }
                    
                    // 獲取導師數據 - 注意API返回的是數組
                    JSONArray dataArray = jsonObject.optJSONArray("data");
                    if (dataArray == null || dataArray.length() == 0) {
                        runOnUiThread(() -> {
                            Toast.makeText(TutorProfileActivity.this, 
                                    "未找到導師資料", Toast.LENGTH_SHORT).show();
                            displayBasicInfo(tutorId);
                        });
                        return;
                    }
                    
                    // 獲取第一條記錄（最新的會員詳情記錄）
                    JSONObject data = dataArray.getJSONObject(0);
                    
                    // 從會員表獲取用戶名
                    String name = getIntent().getStringExtra("tutorName"); // 從Intent獲取
                    
                    // 從會員詳情表提取字段
                    final String profileDesc = data.optString("description", "");
                    final String gender = data.optString("Gender", "");
                    
                    // 獲取頭像URL
                    final String profileIcon = data.optString("profile", "");
                    
                    // 如果有頭像URL，加載頭像
                    if (profileIcon != null && !profileIcon.isEmpty()) {
                        runOnUiThread(() -> {
                            String fullProfileUrl = "http://" + IPConfig.getIP() + profileIcon;
                            Log.d(TAG, "Loading profile icon from: " + fullProfileUrl);
                            // 使用Glide加載頭像
                            Glide.with(TutorProfileActivity.this)
                                    .load(fullProfileUrl)
                                    .placeholder(R.drawable.circle_background)
                                    .error(R.drawable.circle_background)
                                    .into(tutorImageView);
                        });
                    }
                    
                    // 然後獲取導師CV數據
                    fetchTutorCV(tutorId, name, profileDesc);
                    
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(TutorProfileActivity.this, 
                                "解析導師資料時出錯", Toast.LENGTH_SHORT).show();
                        // JSON解析錯誤時顯示基本資訊
                        displayBasicInfo(tutorId);
                    });
                }
            }
        });
    }
    
    private void fetchTutorCV(String tutorId, String name, String profileDesc) {
        OkHttpClient client = new OkHttpClient();
        
        // 使用自定義的API路徑獲取CV數據
        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_cv_data.php";
        Log.d(TAG, "Fetching tutor CV for ID: " + tutorId + " from URL: " + url);
        
        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", tutorId)
                .build();
                
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
                
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch CV data: " + e.getMessage());
                // CV數據獲取失敗時顯示部分信息
                displayPartialInfo(name, profileDesc, "資料載入中...", "", "", "");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Server error when fetching CV: " + response.code());
                    displayPartialInfo(name, profileDesc, "資料載入失敗", "", "", "");
                    return;
                }
                
                String jsonResponse = response.body().string();
                Log.d(TAG, "CV data response: " + jsonResponse);
                
                try {
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    
                    if (!jsonObject.optBoolean("success", false)) {
                        String message = jsonObject.optString("message", "未知錯誤");
                        Log.e(TAG, "API error when fetching CV: " + message);
                        displayPartialInfo(name, profileDesc, "未找到CV資料", "", "", "");
                        return;
                    }
                    
                    // 檢查是否有CV數據
                    JSONArray dataArray = jsonObject.optJSONArray("cv_data");
                    if (dataArray == null || dataArray.length() == 0) {
                        Log.d(TAG, "No CV data found");
                        displayPartialInfo(name, profileDesc, "未找到CV資料", "", "", "");
                        return;
                    }
                    
                    // 獲取CV數據
                    JSONObject cvData = dataArray.getJSONObject(0);
                    
                    String education = cvData.optString("education", "");
                    String skills = cvData.optString("skills", "");
                    String language = cvData.optString("language", "");
                    String other = cvData.optString("other", "");
                    
                    // 過濾出重點大學信息
                    String filteredEducation = filterHKUniversities(education);
                    
                    // 顯示資料
                    displayPartialInfo(name, profileDesc, filteredEducation, skills, language, other);
                    
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error for CV data: " + e.getMessage());
                    displayPartialInfo(name, profileDesc, "解析CV資料時出錯", "", "", "");
                }
            }
        });
    }
    
    private void displayPartialInfo(String name, String profileDesc, String education,
                                   String skills, String language, String other) {
        runOnUiThread(() -> {
            // 顯示用戶名
            if (name != null && !name.isEmpty()) {
                nameTextView.setText(name);
            } else {
                nameTextView.setText("導師");
            }
            
            // 顯示個人描述
            if (profileDesc != null && !profileDesc.isEmpty()) {
                profileDescTextView.setText(profileDesc);
            } else {
                profileDescTextView.setText("此導師暫無自我介紹");
            }
            
            // 顯示教育背景
            if (education != null && !education.isEmpty() && !education.equals("null")) {
                educationTextView.setText(education);
            } else {
                educationTextView.setText("暫無教育背景資料");
            }
            
            // 組合顯示完整的「關於我」部分
            StringBuilder aboutMeBuilder = new StringBuilder();
            
            // 添加技能
            if (skills != null && !skills.isEmpty() && !skills.equals("null")) {
                aboutMeBuilder.append("【專業技能】\n").append(skills).append("\n\n");
            }
            
            // 添加語言能力
            if (language != null && !language.isEmpty() && !language.equals("null")) {
                aboutMeBuilder.append("【語言能力】\n").append(language).append("\n\n");
            }
            
            // 添加其他資訊
            if (other != null && !other.isEmpty() && !other.equals("null")) {
                aboutMeBuilder.append("【其他資訊】\n").append(other);
            }
            
            // 如果有內容，顯示；否則顯示默認文字
            if (aboutMeBuilder.length() > 0) {
                aboutMeTextView.setText(aboutMeBuilder.toString());
            } else {
                aboutMeTextView.setText("此導師暫未提供詳細資料");
            }
        });
    }
    
    private String filterHKUniversities(String education) {
        // 如果教育資訊為空，返回預設值
        if (education == null || education.isEmpty() || education.equals("null")) {
            return "暫無教育背景資料";
        }
        
        // 檢查香港知名大學
        String[] universities = {
            "香港大學", "University of Hong Kong", "HKU", 
            "香港中文大學", "Chinese University of Hong Kong", "CUHK",
            "香港科技大學", "Hong Kong University of Science and Technology", "HKUST", 
            "香港理工大學", "Hong Kong Polytechnic University", "PolyU",
            "香港城市大學", "City University of Hong Kong", "CityU",
            "香港浸會大學", "Hong Kong Baptist University", "HKBU",
            "嶺南大學", "Lingnan University", "LU",
            "香港教育大學", "Education University of Hong Kong", "EdUHK",
            "香港樹仁大學", "Hong Kong Shue Yan University", "HKSYU"
        };
        
        // 大學名稱對應表（英文縮寫 -> 中文全名）
        String[][] universityMapping = {
            {"HKU", "香港大學"},
            {"CUHK", "香港中文大學"},
            {"HKUST", "香港科技大學"},
            {"PolyU", "香港理工大學"},
            {"CityU", "香港城市大學"},
            {"HKBU", "香港浸會大學"},
            {"LU", "嶺南大學"},
            {"EdUHK", "香港教育大學"},
            {"HKSYU", "香港樹仁大學"}
        };
        
        for (String uni : universities) {
            if (education.contains(uni)) {
                // 找到大學名稱，返回標準化的中文名稱
                
                // 如果找到的是英文縮寫，返回對應的中文全名
                for (String[] mapping : universityMapping) {
                    if (uni.equals(mapping[0]) || uni.contains(mapping[0])) {
                        return mapping[1];
                    }
                }
                
                // 如果找到的是中文全名，直接返回
                for (String[] mapping : universityMapping) {
                    if (uni.equals(mapping[1]) || uni.contains(mapping[1])) {
                        return mapping[1];
                    }
                }
                
                // 如果都不匹配，返回找到的大學名稱
                return uni;
            }
        }
        
        // 如果沒有找到特定大學，返回原始信息
        return education;
    }
    
    private void displayBasicInfo(String tutorId) {
        runOnUiThread(() -> {
            // 顯示用戶名
            String name = getIntent().getStringExtra("tutorName");
            if (name != null && !name.isEmpty()) {
                nameTextView.setText(name);
            } else {
                nameTextView.setText("導師");
            }
            
            // 設置預設描述
            profileDescTextView.setText("資料載入中...");
            educationTextView.setText("資料載入中...");
            aboutMeTextView.setText("導師資料暫時無法獲取，請稍後再試。");
        });
    }
} 
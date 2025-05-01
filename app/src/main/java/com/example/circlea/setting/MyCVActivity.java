package com.example.circlea.setting;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.circlea.IPConfig;
import com.example.circlea.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyCVActivity extends AppCompatActivity {
    
    private static final String TAG = "MyCVActivity";
    private RecyclerView recyclerView;
    private CVAdapter adapter;
    private List<CVItem> cvList = new ArrayList<>();
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private Button createCVButton;
    private String memberId;
    private CardView currentCVCard;
    private TextView currentCVDate, currentCVContact, currentCVEducation, 
            currentCVSkills, currentCVLanguage, currentCVOther;
    private Button editCurrentCVButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_cv);
        
        // 獲取用戶ID
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        memberId = sharedPreferences.getString("member_id", "");
        
        if (memberId.isEmpty()) {
            Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始化視圖
        initViews();
        
        // 設置返回按鈕
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        
        // 設置創建CV按鈕
        createCVButton.setOnClickListener(v -> {
            Intent intent = new Intent(MyCVActivity.this, ScanCV.class);
            startActivity(intent);
        });
        
        // 設置編輯當前CV按鈕
        editCurrentCVButton.setOnClickListener(v -> {
            if (!cvList.isEmpty()) {
                CVItem currentCV = getCurrentCV();
                Intent intent = new Intent(MyCVActivity.this, ScanCV.class);
                intent.putExtra("cv_id", currentCV.getId());
                intent.putExtra("contact", currentCV.getContact());
                intent.putExtra("education", currentCV.getEducation());
                intent.putExtra("skills", currentCV.getSkills());
                intent.putExtra("language", currentCV.getLanguage());
                intent.putExtra("other", currentCV.getOther());
                startActivity(intent);
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到頁面時重新加載數據
        loadCVData();
    }
    
    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        createCVButton = findViewById(R.id.createCVButton);
        recyclerView = findViewById(R.id.recyclerViewCVHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CVAdapter(cvList, this::handleCVItemClick);
        recyclerView.setAdapter(adapter);
        
        // 當前CV卡片區域
        currentCVCard = findViewById(R.id.currentCVCard);
        currentCVDate = findViewById(R.id.currentCVDate);
        currentCVContact = findViewById(R.id.currentCVContact);
        currentCVEducation = findViewById(R.id.currentCVEducation);
        currentCVSkills = findViewById(R.id.currentCVSkills);
        currentCVLanguage = findViewById(R.id.currentCVLanguage);
        currentCVOther = findViewById(R.id.currentCVOther);
        editCurrentCVButton = findViewById(R.id.editCurrentCVButton);
    }
    
    private void loadCVData() {
        progressBar.setVisibility(View.VISIBLE);
        if (currentCVCard.getVisibility() == View.VISIBLE) {
            currentCVCard.setVisibility(View.GONE);
        }
        
        OkHttpClient client = new OkHttpClient();
        
        RequestBody formBody = new FormBody.Builder()
                .add("member_id", memberId)
                .build();
        
        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/get_cv_data.php")
                .post(formBody)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to fetch CV data: " + e.getMessage());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MyCVActivity.this, "獲取CV資料失敗", Toast.LENGTH_SHORT).show();
                    showEmptyView();
                });
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        boolean success = jsonResponse.getBoolean("success");
                        
                        if (success) {
                            cvList.clear();
                            JSONArray cvArray = jsonResponse.getJSONArray("cv_data");
                            
                            for (int i = 0; i < cvArray.length(); i++) {
                                JSONObject cvObject = cvArray.getJSONObject(i);
                                
                                String status = i == 0 ? "A" : "P"; // 最新的CV設為活躍狀態
                                
                                CVItem cvItem = new CVItem(
                                        cvObject.getInt("cv_id"),
                                        cvObject.getString("contact"),
                                        cvObject.getString("skills"),
                                        cvObject.getString("education"),
                                        cvObject.getString("language"),
                                        cvObject.getString("other"),
                                        cvObject.getString("created_at"),
                                        status
                                );
                                
                                cvList.add(cvItem);
                            }
                            
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                if (cvList.isEmpty()) {
                                    showEmptyView();
                                } else {
                                    showCVData();
                                }
                            });
                            
                        } else {
                            String message = jsonResponse.getString("message");
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(MyCVActivity.this, message, Toast.LENGTH_SHORT).show();
                                showEmptyView();
                            });
                        }
                        
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(MyCVActivity.this, "數據解析錯誤", Toast.LENGTH_SHORT).show();
                            showEmptyView();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MyCVActivity.this, "伺服器錯誤", Toast.LENGTH_SHORT).show();
                        showEmptyView();
                    });
                }
            }
        });
    }
    
    private void showEmptyView() {
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        currentCVCard.setVisibility(View.GONE);
    }
    
    private void showCVData() {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
        
        // 顯示當前使用的CV (狀態為A的CV)
        updateCurrentCVCard();
    }
    
    private void updateCurrentCVCard() {
        CVItem currentCV = getCurrentCV();
        
        if (currentCV != null) {
            currentCVCard.setVisibility(View.VISIBLE);
            
            // 格式化日期
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = inputFormat.parse(currentCV.getCreatedAt());
                
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
                String formattedDate = outputFormat.format(date);
                currentCVDate.setText(formattedDate);
            } catch (ParseException e) {
                currentCVDate.setText(currentCV.getCreatedAt());
            }
            
            currentCVContact.setText(currentCV.getContact());
            currentCVEducation.setText(currentCV.getEducation());
            currentCVSkills.setText(currentCV.getSkills());
            currentCVLanguage.setText(currentCV.getLanguage());
            currentCVOther.setText(currentCV.getOther());
        } else {
            currentCVCard.setVisibility(View.GONE);
        }
    }
    
    private CVItem getCurrentCV() {
        // 由於我們使用cv_data表，而不使用status字段，直接返回最新的CV
        if (!cvList.isEmpty()) {
            return cvList.get(0);  // 假設列表已按時間排序（最新的在前）
        }
        return null;
    }
    
    // 添加一個方法以根據CV ID獲取CV項目
    private CVItem getCVById(int cvId) {
        for (CVItem item : cvList) {
            if (item.getId() == cvId) {
                return item;
            }
        }
        return null;
    }
    
    private void handleCVItemClick(CVItem cvItem) {
        // 更新當前顯示的CV為用戶選擇的CV
        for (CVItem item : cvList) {
            item.setStatus(item.getId() == cvItem.getId() ? "A" : "P");
        }
        updateCurrentCVCard();
        adapter.notifyDataSetChanged(); // 通知適配器數據已更改
        Toast.makeText(this, "已顯示選中的CV", Toast.LENGTH_SHORT).show();
    }
    
    // CV項目類，用於存儲CV數據
    public static class CVItem {
        private final int id;
        private final String contact;
        private final String skills;
        private final String education;
        private final String language;
        private final String other;
        private final String createdAt;
        private String status;
        
        public CVItem(int id, String contact, String skills, String education, String language, String other, String createdAt, String status) {
            this.id = id;
            this.contact = contact;
            this.skills = skills;
            this.education = education;
            this.language = language;
            this.other = other;
            this.createdAt = createdAt;
            this.status = status;
        }
        
        public int getId() {
            return id;
        }
        
        public String getContact() {
            return contact;
        }
        
        public String getSkills() {
            return skills;
        }
        
        public String getEducation() {
            return education;
        }
        
        public String getLanguage() {
            return language;
        }
        
        public String getOther() {
            return other;
        }
        
        public String getCreatedAt() {
            return createdAt;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
    }
    
    // CV適配器，用於顯示CV歷史記錄
    private class CVAdapter extends RecyclerView.Adapter<CVAdapter.CVViewHolder> {
        
        private final List<CVItem> cvItems;
        private final OnCVItemClickListener listener;
        
        public CVAdapter(List<CVItem> cvItems, OnCVItemClickListener listener) {
            this.cvItems = cvItems;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public CVViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_cv_history, parent, false);
            return new CVViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull CVViewHolder holder, int position) {
            CVItem cvItem = cvItems.get(position);
            
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = inputFormat.parse(cvItem.getCreatedAt());
                
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
                String formattedDate = outputFormat.format(date);
                holder.dateTextView.setText(formattedDate);
            } catch (ParseException e) {
                holder.dateTextView.setText(cvItem.getCreatedAt());
            }
            
            // 顯示CV狀態
            if (cvItem.getStatus().equals("A")) {
                holder.statusTextView.setText(getString(R.string.currently_using));
                holder.statusTextView.setVisibility(View.VISIBLE);
            } else {
                holder.statusTextView.setVisibility(View.GONE);
            }
            
            // 設置點擊事件
            holder.itemView.setOnClickListener(v -> listener.onCVItemClick(cvItem));
            
            // 設置按鈕點擊事件
            holder.itemView.findViewById(R.id.buttonViewCV).setOnClickListener(v -> {
                listener.onCVItemClick(cvItem);
            });
            
            holder.itemView.findViewById(R.id.buttonActivateCV).setOnClickListener(v -> {
                listener.onCVItemClick(cvItem);
            });
        }
        
        @Override
        public int getItemCount() {
            return cvItems.size();
        }
        
        class CVViewHolder extends RecyclerView.ViewHolder {
            TextView dateTextView;
            TextView statusTextView;
            
            CVViewHolder(@NonNull View itemView) {
                super(itemView);
                dateTextView = itemView.findViewById(R.id.textViewCVDate);
                statusTextView = itemView.findViewById(R.id.statusTextView);
            }
        }
    }
    
    // CV項目點擊監聽器
    interface OnCVItemClickListener {
        void onCVItemClick(CVItem cvItem);
    }
} 
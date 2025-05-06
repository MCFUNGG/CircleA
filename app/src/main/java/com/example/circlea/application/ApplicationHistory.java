package com.example.circlea.application;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.example.circlea.utils.ContentFilter;
import com.google.android.material.button.MaterialButton;
import com.example.circlea.LanguageManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApplicationHistory extends AppCompatActivity {
    private LinearLayout applicationsContainer;
    private OkHttpClient client;
    private String currentRole = "T"; // Default role to Tutor
    private MaterialButton btnTutor;
    private MaterialButton btnPS;
    private static final String TAG = "ApplicationHistory";
    private LanguageManager languageManager;

    // Lists to store subject, district, and level data
    private List<String> subjects;
    private List<String> districts;
    private List<String> studentLevels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_application);

        // Initialize views
        applicationsContainer = findViewById(R.id.history_application_container);
        client = new OkHttpClient();
        btnTutor = findViewById(R.id.button_tutor);
        btnPS = findViewById(R.id.button_ps);
        ImageButton exitButton = findViewById(R.id.exitButton);
        languageManager = new LanguageManager(this);

        setupButtons();
        
        // 先顯示加載提示
        showLoading(true);
        
        // 先獲取數據列表，完成後再獲取應用數據
        fetchLevelsAndSubjects(() -> {
            // 數據加載完成後獲取應用數據
            fetchApplicationData();
        });
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            // 清空舊數據
            if (show) {
                applicationsContainer.removeAllViews();
                
                // 添加加載中提示
                TextView loadingText = new TextView(this);
                loadingText.setText(R.string.loading);
                loadingText.setTextSize(18);
                loadingText.setGravity(android.view.Gravity.CENTER);
                loadingText.setPadding(0, 50, 0, 0);
                applicationsContainer.addView(loadingText);
            }
        });
    }

    private void fetchLevelsAndSubjects(Runnable onComplete) {
        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_studentLevels&Subject&District.php";
        
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(ApplicationHistory.this, getString(R.string.error), Toast.LENGTH_SHORT).show();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseStr = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseStr);
                        
                        // 檢查 JSON 是否包含 success 欄位
                        if (jsonObject.has("success")) {
                            boolean success = jsonObject.getBoolean("success");
                            if (!success) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ApplicationHistory.this, getString(R.string.failed_to_fetch_data), Toast.LENGTH_SHORT).show();
                                });
                                if (onComplete != null) {
                                    runOnUiThread(onComplete);
                                }
                                return;
                            }
                        } else {
                            // 如果沒有 success 欄位，檢查是否有其他必要欄位
                            if (!jsonObject.has("levels") || !jsonObject.has("subjects") || !jsonObject.has("districts")) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ApplicationHistory.this, getString(R.string.invalid_response_format), Toast.LENGTH_SHORT).show();
                                });
                                if (onComplete != null) {
                                    runOnUiThread(onComplete);
                                }
                                return;
                            }
                        }
                        
                        // 解析學生等級、科目和地區
                        parseSubjectsAndLevels(jsonObject);

                        if (onComplete != null) {
                            runOnUiThread(onComplete);
                        }
                        
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(ApplicationHistory.this, getString(R.string.json_parsing_error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            if (onComplete != null) {
                                onComplete.run();
                            }
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ApplicationHistory.this, getString(R.string.server_error) + ": " + response.code(), Toast.LENGTH_SHORT).show();
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
                }
            }
        });
    }

    private void parseSubjectsAndLevels(JSONObject jsonObject) throws JSONException {
        subjects = new ArrayList<>();
        districts = new ArrayList<>();
        studentLevels = new ArrayList<>();

        JSONArray subjectArray = jsonObject.getJSONArray("subjects");
        for (int i = 0; i < subjectArray.length(); i++) {
            JSONObject subject = subjectArray.getJSONObject(i);
            subjects.add(subject.getString("subject_name"));
        }

        JSONArray districtArray = jsonObject.getJSONArray("districts");
        for (int i = 0; i < districtArray.length(); i++) {
            JSONObject district = districtArray.getJSONObject(i);
            districts.add(district.getString("district_name"));
        }

        JSONArray levelArray = jsonObject.getJSONArray("levels");
        for (int i = 0; i < levelArray.length(); i++) {
            JSONObject level = levelArray.getJSONObject(i);
            studentLevels.add(level.getString("class_level_name"));
        }
    }

    private void fetchApplicationData() {
        if (currentRole.equals("T")) {
            fetchTutorApplicationData();
        } else {
            fetchPsApplicationData();
        }
    }

    private void setupButtons() {
        updateButtonStates(true); // true means Tutor is selected

        btnTutor.setOnClickListener(v -> {
            if (!currentRole.equals("T")) {
                currentRole = "T";
                updateButtonStates(true);
                fetchTutorApplicationData();
            }
        });

        btnPS.setOnClickListener(v -> {
            if (!currentRole.equals("PS")) {
                currentRole = "PS";
                updateButtonStates(false);
                fetchPsApplicationData();
            }
        });

        findViewById(R.id.exitButton).setOnClickListener(v -> finish());
    }

    private void updateButtonStates(boolean isTutorSelected) {
        btnTutor.setBackgroundColor(getColor(isTutorSelected ? R.color.green_500 : android.R.color.transparent));
        btnPS.setBackgroundColor(getColor(isTutorSelected ? android.R.color.transparent : R.color.purple_500));
    }

    private void fetchPsApplicationData() {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);
        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_member_own_PS_application_data.php";

        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", memberId)
                .add("role", "PS")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ApplicationHistory.this,
                        getString(R.string.failed_to_fetch_ps_applications), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    processApplicationResponse(response);
                }
            }
        });
    }

    private void fetchTutorApplicationData() {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);
        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_member_own_T_application_data.php";

        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", memberId)
                .add("role", "T")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ApplicationHistory.this,
                        getString(R.string.failed_to_fetch_tutor_applications), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    processApplicationResponse(response);
                }
            }
        });
    }

    private void processApplicationResponse(Response response) throws IOException {
        String jsonResponse = response.body().string();
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            if (jsonObject.getBoolean("success")) {
                JSONArray dataArray = jsonObject.getJSONArray("data");
                runOnUiThread(() -> {
                    // 關閉加載提示
                    showLoading(false);
                    // 清空舊的數據
                    applicationsContainer.removeAllViews();
                    
                    if (dataArray.length() == 0) {
                        // 沒有找到任何應用
                        TextView noDataText = new TextView(this);
                        noDataText.setText(getString(R.string.no_applications_found));
                        noDataText.setTextSize(16);
                        noDataText.setGravity(android.view.Gravity.CENTER);
                        noDataText.setPadding(0, 100, 0, 0);
                        applicationsContainer.addView(noDataText);
                    } else {
                        // 添加應用列表
                        for (int i = 0; i < dataArray.length(); i++) {
                            try {
                                addApplicationView(dataArray.getJSONObject(i));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            } else {
                String message = jsonObject.optString("message", getString(R.string.unknown_error));
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ApplicationHistory.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(ApplicationHistory.this,
                        getString(R.string.error_processing_data), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void addApplicationView(JSONObject data) throws JSONException {
        View applicationView = LayoutInflater.from(this)
                .inflate(R.layout.history_application_item, applicationsContainer, false);

        String appId = data.optString("app_id", getString(R.string.n_a));
        String studentLevel = data.optString("class_level_name", getString(R.string.n_a));
        String fee = data.optString("feePerHr", getString(R.string.n_a));
        String status = data.optString("status", getString(R.string.n_a));
        String description = data.optString("description", "");
        boolean isCompleted = data.optBoolean("is_completed", false);

        // 翻译学生级别
        studentLevel = languageManager.translateDatabaseField(studentLevel, "class_level");

        // Process arrays
        JSONArray subjectNamesArray = data.optJSONArray("subject_names");
        JSONArray districtNamesArray = data.optJSONArray("district_names");
        
        String subjectsStr = processArrayToString(subjectNamesArray);
        String districtsStr = processArrayToString(districtNamesArray);
        
        // 翻译科目和地区列表
        subjectsStr = languageManager.translateSubjectList(subjectsStr);
        districtsStr = languageManager.translateDistrictList(districtsStr);

        // Set values to the view
        ((TextView) applicationView.findViewById(R.id.app_id)).setText(
                String.format(getString(R.string.app_id_format), appId));
        ((TextView) applicationView.findViewById(R.id.subject_text)).setText(subjectsStr);
        ((TextView) applicationView.findViewById(R.id.student_level_text)).setText(studentLevel);
        ((TextView) applicationView.findViewById(R.id.fee_text)).setText(
                String.format(getString(R.string.fee_format), fee));
        ((TextView) applicationView.findViewById(R.id.district_text)).setText(districtsStr);

        // Handle status
        TextView statusTextView = applicationView.findViewById(R.id.status_text);
        if (isCompleted) {
            statusTextView.setText(getString(R.string.completed));
            statusTextView.setBackgroundResource(R.drawable.status_completed_pill);
            // 已完成应用不可点击
            applicationView.setOnClickListener(null);
            applicationView.setAlpha(0.7f); // 设置透明度以表示不可点击
        } else if (status.equals("P")) {
            statusTextView.setText(getString(R.string.pending));
            statusTextView.setBackgroundResource(R.drawable.status_pending_pill);
            // 添加点击监听器来打开编辑对话框
            setupClickListener(applicationView, appId, studentLevel, subjectsStr, districtsStr, fee, description, status);
        } else if (status.equals("A")) {
            statusTextView.setText(getString(R.string.approved));
            statusTextView.setBackgroundResource(R.drawable.status_approved_pill);
            // 添加点击监听器来打开编辑对话框
            setupClickListener(applicationView, appId, studentLevel, subjectsStr, districtsStr, fee, description, status);
        } else if (status.equals("R")) {
            statusTextView.setText(getString(R.string.rejected));
            statusTextView.setBackgroundResource(R.drawable.status_rejected_pill);
            // 添加点击监听器来打开编辑对话框
            setupClickListener(applicationView, appId, studentLevel, subjectsStr, districtsStr, fee, description, status);
        }

        applicationsContainer.addView(applicationView);
        }

    // 提取点击监听器逻辑到单独的方法
    private void setupClickListener(View applicationView, String appId, String studentLevel, 
                                   String subjectsStr, String districtsStr, String fee, 
                                   String description, String status) {
        applicationView.setOnClickListener(v -> {
            if (subjects == null || districts == null || studentLevels == null) {
                // 如果数据尚未加载完成，显示进度对话框并重新加载
                showDataLoadingDialog(appId, studentLevel, subjectsStr, districtsStr, fee, description, status);
                return;
            }
            showEditDialog(appId, studentLevel, subjectsStr, districtsStr, fee, description, status);
        });
    }

    // 顯示數據加載中的對話框，並嘗試重新獲取數據
    private void showDataLoadingDialog(String appId, String studentLevel, String subjects, 
                                     String districts, String fee, String description, String status) {
        Dialog loadingDialog = new Dialog(this);
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(true);
        
        Button retryButton = loadingDialog.findViewById(R.id.retry_button);
        Button cancelButton = loadingDialog.findViewById(R.id.cancel_button);
        
        retryButton.setOnClickListener(v -> {
            loadingDialog.dismiss();
            // 重新獲取數據
            fetchLevelsAndSubjects(() -> {
                // 數據加載成功後，在主線程上顯示編輯對話框或 Toast 訊息
                runOnUiThread(() -> {
                    if (this.subjects != null && this.districts != null && this.studentLevels != null) {
                        showEditDialog(appId, studentLevel, subjects, districts, fee, description, status);
                    } else {
                        Toast.makeText(ApplicationHistory.this, 
                                      getString(R.string.failed_to_fetch_levels_and_subjects), 
                                      Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
        
        cancelButton.setOnClickListener(v -> loadingDialog.dismiss());
        
        loadingDialog.show();
    }

    private void showEditDialog(String appId, String studentLevel, String subjects, 
                               String districts, String fee, String description, String status) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_application);
        dialog.setCancelable(true);

        // 調整對話框窗口大小
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        // Initialize views in dialog
        Spinner levelSpinner = dialog.findViewById(R.id.student_level_spinner);
        EditText feeInput = dialog.findViewById(R.id.fee_input);
        EditText descriptionInput = dialog.findViewById(R.id.description_input);
        LinearLayout subjectContainer = dialog.findViewById(R.id.subject_container);
        LinearLayout districtContainer = dialog.findViewById(R.id.district_container);
        Button saveButton = dialog.findViewById(R.id.save_button);
        Button cancelButton = dialog.findViewById(R.id.cancel_button);
        Button deleteButton = dialog.findViewById(R.id.delete_button);

        // 创建包含翻译后学生级别的列表供下拉菜单使用
        List<String> translatedLevels = new ArrayList<>();
        for (String level : this.studentLevels) {
            translatedLevels.add(languageManager.translateDatabaseField(level, "class_level"));
        }

        // Set up student level spinner with translated levels
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_dropdown_item, translatedLevels);
        levelSpinner.setAdapter(levelAdapter);
        
        // 选择当前级别（已经是翻译后的）
        int levelPosition = translatedLevels.indexOf(studentLevel);
        if (levelPosition >= 0) {
            levelSpinner.setSelection(levelPosition);
        }

        // Set fee and description
        feeInput.setText(fee);
        descriptionInput.setText(description);

        // Set up subject checkboxes
        List<String> selectedSubjects = new ArrayList<>();
        for (String subject : subjects.split(", ")) {
            selectedSubjects.add(subject); // 这些科目已经是翻译后的
        }
        
        // 为每个科目添加复选框，使用翻译后的名称
        for (String subject : this.subjects) {
            CheckBox checkBox = new CheckBox(this);
            String translatedSubject = languageManager.translateDatabaseField(subject, "subject");
            checkBox.setText(translatedSubject);
            checkBox.setChecked(selectedSubjects.contains(translatedSubject));
            
            // 為 CheckBox 添加底部間距
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dpToPx(4); // 4dp 的底部間距
            checkBox.setLayoutParams(params);
            
            subjectContainer.addView(checkBox);
        }

        // Set up district checkboxes
        List<String> selectedDistricts = new ArrayList<>();
        for (String district : districts.split(", ")) {
            selectedDistricts.add(district); // 这些地区已经是翻译后的
        }
        
        // 为每个地区添加复选框，使用翻译后的名称
        for (String district : this.districts) {
            CheckBox checkBox = new CheckBox(this);
            String translatedDistrict = languageManager.translateDatabaseField(district, "district");
            checkBox.setText(translatedDistrict);
            checkBox.setChecked(selectedDistricts.contains(translatedDistrict));
            
            // 為 CheckBox 添加底部間距
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dpToPx(4); // 4dp 的底部間距
            checkBox.setLayoutParams(params);
            
            districtContainer.addView(checkBox);
        }

        // Set click listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        // 刪除按鈕點擊監聽器
        deleteButton.setOnClickListener(v -> {
            showDeleteConfirmationDialog(appId, dialog);
        });
        
        saveButton.setOnClickListener(v -> {
            // Collect data from dialog
            // 获取用户在下拉菜单中选择的翻译后的级别
            String translatedNewLevel = levelSpinner.getSelectedItem().toString();
            // 从翻译后的级别找回原始级别
            String newLevel = "";
            for (int i = 0; i < translatedLevels.size(); i++) {
                if (translatedLevels.get(i).equals(translatedNewLevel)) {
                    newLevel = this.studentLevels.get(i);
                    break;
                }
            }
            
            String newFee = feeInput.getText().toString();
            String newDescription = descriptionInput.getText().toString();
            
            // 收集选中的翻译后科目，并找回原始科目
            List<String> newSelectedSubjects = new ArrayList<>();
            List<String> translatedNewSelectedSubjects = new ArrayList<>();
            for (int i = 0; i < subjectContainer.getChildCount(); i++) {
                CheckBox checkBox = (CheckBox) subjectContainer.getChildAt(i);
                if (checkBox.isChecked()) {
                    String translatedSubject = checkBox.getText().toString();
                    translatedNewSelectedSubjects.add(translatedSubject);
                    
                    // 从翻译后科目找回原始科目
                    for (String originalSubject : this.subjects) {
                        if (languageManager.translateDatabaseField(originalSubject, "subject").equals(translatedSubject)) {
                            newSelectedSubjects.add(originalSubject);
                            break;
                        }
                    }
                }
            }
            
            // 收集选中的翻译后地区，并找回原始地区
            List<String> newSelectedDistricts = new ArrayList<>();
            List<String> translatedNewSelectedDistricts = new ArrayList<>();
            for (int i = 0; i < districtContainer.getChildCount(); i++) {
                CheckBox checkBox = (CheckBox) districtContainer.getChildAt(i);
                if (checkBox.isChecked()) {
                    String translatedDistrict = checkBox.getText().toString();
                    translatedNewSelectedDistricts.add(translatedDistrict);
                    
                    // 从翻译后地区找回原始地区
                    for (String originalDistrict : this.districts) {
                        if (languageManager.translateDatabaseField(originalDistrict, "district").equals(translatedDistrict)) {
                            newSelectedDistricts.add(originalDistrict);
                            break;
                        }
                    }
                }
            }
            
            // Validate inputs
            if (newSelectedSubjects.isEmpty()) {
                Toast.makeText(this, "Please select at least one subject", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (newSelectedDistricts.isEmpty()) {
                Toast.makeText(this, "Please select at least one district", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (newFee.isEmpty()) {
                Toast.makeText(this, "Please enter fee", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 檢查描述內容是否包含敏感詞或個人信息
            if (!newDescription.isEmpty()) {
                ContentFilter.ContentCheckResult checkResult = 
                    ContentFilter.checkContent(newDescription);
                
                if (!checkResult.isClean()) {
                    String warningMessage = ContentFilter.getWarningMessage(checkResult);
                    Toast.makeText(this, warningMessage, Toast.LENGTH_LONG).show();
                    return;
                }
            }
            
            // 檢查是否有實際修改內容，比较原始数据
            boolean hasChanges = !newLevel.equals(getOriginalLevelFromTranslated(studentLevel)) || 
                                !newFee.equals(fee) || 
                                !newDescription.equals(description) ||
                                !compareSubjects(newSelectedSubjects, getOriginalSubjectsFromTranslated(subjects.split(", "))) ||
                                !compareDistricts(newSelectedDistricts, getOriginalDistrictsFromTranslated(districts.split(", ")));
            
            // 只有在有修改且原狀態為批准時才變更狀態
            String newStatus = (hasChanges && status.equals("A")) ? "P" : status;
            
            // 如果沒有修改，提示用戶並詢問是否仍要保存
            if (!hasChanges) {
                dialog.dismiss();
                Toast.makeText(this, "沒有任何修改", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Update application
            updateApplication(appId, newLevel, newSelectedSubjects, newSelectedDistricts, 
                    newFee, newDescription, newStatus);
            dialog.dismiss();
        });

        dialog.show();
    }

    // 辅助方法，根据翻译后的学生级别查找原始级别
    private String getOriginalLevelFromTranslated(String translatedLevel) {
        for (String originalLevel : studentLevels) {
            if (languageManager.translateDatabaseField(originalLevel, "class_level").equals(translatedLevel)) {
                return originalLevel;
            }
        }
        return translatedLevel; // 如果找不到，返回翻译后的级别作为备选
    }

    // 辅助方法，根据翻译后的科目列表查找原始科目列表
    private String[] getOriginalSubjectsFromTranslated(String[] translatedSubjects) {
        String[] originalSubjects = new String[translatedSubjects.length];
        for (int i = 0; i < translatedSubjects.length; i++) {
            originalSubjects[i] = getOriginalSubjectFromTranslated(translatedSubjects[i]);
        }
        return originalSubjects;
    }

    // 辅助方法，根据翻译后的单个科目查找原始科目
    private String getOriginalSubjectFromTranslated(String translatedSubject) {
        for (String originalSubject : subjects) {
            if (languageManager.translateDatabaseField(originalSubject, "subject").equals(translatedSubject)) {
                return originalSubject;
            }
        }
        return translatedSubject; // 如果找不到，返回翻译后的科目作为备选
    }

    // 辅助方法，根据翻译后的地区列表查找原始地区列表
    private String[] getOriginalDistrictsFromTranslated(String[] translatedDistricts) {
        String[] originalDistricts = new String[translatedDistricts.length];
        for (int i = 0; i < translatedDistricts.length; i++) {
            originalDistricts[i] = getOriginalDistrictFromTranslated(translatedDistricts[i]);
        }
        return originalDistricts;
    }

    // 辅助方法，根据翻译后的单个地区查找原始地区
    private String getOriginalDistrictFromTranslated(String translatedDistrict) {
        for (String originalDistrict : districts) {
            if (languageManager.translateDatabaseField(originalDistrict, "district").equals(translatedDistrict)) {
                return originalDistrict;
            }
        }
        return translatedDistrict; // 如果找不到，返回翻译后的地区作为备选
    }

    // 顯示刪除確認對話框
    private void showDeleteConfirmationDialog(String appId, Dialog parentDialog) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.delete_confirmation))
                .setMessage(getString(R.string.delete_application_confirmation_message))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    parentDialog.dismiss();
                    deleteApplication(appId);
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    // 刪除申請
    private void deleteApplication(String appId) {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);

        // 日誌記錄參數
        Log.d("DeleteApplication", "Starting delete with parameters:");
        Log.d("DeleteApplication", "app_id: " + appId);
        Log.d("DeleteApplication", "member_id: " + memberId);
        Log.d("DeleteApplication", "app_creator: " + currentRole);

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("app_id", appId)
                .add("member_id", memberId)
                .add("app_creator", currentRole);

        RequestBody requestBody = formBuilder.build();

        String url = "http://" + IPConfig.getIP() + "/FYP/php/delete_application.php";
        Log.d("DeleteApplication", "Request URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // 顯示進度提示
        runOnUiThread(() -> {
            Toast.makeText(this, getString(R.string.deleting_application), Toast.LENGTH_SHORT).show();
        });

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DeleteApplication", "Network error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(ApplicationHistory.this, 
                            getString(R.string.failed_to_delete_application) + ": " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d("DeleteApplication", "Response: " + responseBody);
                
                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    boolean success = jsonObject.getBoolean("success");
                    String message = jsonObject.getString("message");
                    
                    Log.d("DeleteApplication", "Success: " + success + ", Message: " + message);

                    runOnUiThread(() -> {
                        Toast.makeText(ApplicationHistory.this, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            // 刷新應用列表
                            fetchApplicationData();
                        }
                    });
                } catch (JSONException e) {
                    Log.e("DeleteApplication", "JSON parsing error: " + e.getMessage(), e);
                    Log.e("DeleteApplication", "Response body: " + responseBody);
                    
                    runOnUiThread(() -> {
                        Toast.makeText(ApplicationHistory.this, 
                                getString(R.string.error_processing_data) + ": " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    // 比較兩個科目列表是否相同
    private boolean compareSubjects(List<String> list1, String[] array) {
        if (list1.size() != array.length) return false;
        
        for (String item : array) {
            if (!list1.contains(item.trim())) {
                return false;
            }
        }
        return true;
    }
    
    // 比較兩個地區列表是否相同
    private boolean compareDistricts(List<String> list1, String[] array) {
        if (list1.size() != array.length) return false;
        
        for (String item : array) {
            if (!list1.contains(item.trim())) {
                return false;
            }
        }
        return true;
    }

    // 輔助方法：將 dp 轉換為像素
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void updateApplication(String appId, String studentLevel, List<String> subjects, 
                                  List<String> districts, String fee, String description, String status) {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);

        // Convert lists to JSON arrays
        JSONArray subjectArray = new JSONArray();
        for (String subject : subjects) {
            subjectArray.put(subject);
        }

        JSONArray districtArray = new JSONArray();
        for (String district : districts) {
            districtArray.put(district);
        }

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("app_id", appId)
                .add("member_id", memberId)
                .add("class_level_name", studentLevel)
                .add("subjects", subjectArray.toString())
                .add("districts", districtArray.toString())
                .add("fee", fee)
                .add("description", description)
                .add("app_creator", currentRole)
                .add("status", status);

        RequestBody requestBody = formBuilder.build();

        String url = "http://" + IPConfig.getIP() + "/FYP/php/update_application.php";

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(ApplicationHistory.this, 
                            "Failed to update application: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    boolean success = jsonObject.getBoolean("success");
                    String message = jsonObject.getString("message");

                    runOnUiThread(() -> {
                        Toast.makeText(ApplicationHistory.this, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            // Refresh the list
                            fetchApplicationData();
                        }
                    });
                } catch (JSONException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(ApplicationHistory.this, 
                                getString(R.string.error_processing_data), 
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private String processArrayToString(JSONArray array) throws JSONException {
        if (array == null || array.length() == 0) return getString(R.string.n_a);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            if (i > 0) result.append(", ");
            result.append(array.getString(i));
        }
        return result.toString();
    }
}
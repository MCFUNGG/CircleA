package com.example.circlea.home;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PSAppDetail extends AppCompatActivity {
    private TextView appIdTextView, memberIdTextView, subjectTextView, classLevelTextView,
            feeTextView, districtTextView, matchingScoreTextView, usernameTextView;
    private ImageButton exitBtn;
    private ImageView profileIconImageView;
    private ImageView genderIcon;
    private Button applyButton;
    private Dialog applyDialog;
    private String tutorId;
    private OkHttpClient client;
    private LinearLayout applicationsContainer;
    private String selectedAppId = "";
    private String psAppId = "";
    private String psId = "";
    private String psUsername = "";
    private String psAvatarUrl = "";
    private Map<String, String> appStatusMap = new HashMap<>();
    
    private static final String DEBUG_TAG = "PSAppDetail";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_ps_app_detail);

        client = new OkHttpClient();
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        tutorId = sharedPreferences.getString("member_id", null);

        // 获取从Intent传递过来的数据
        psAppId = getIntent().getStringExtra("psAppId");
        psId = getIntent().getStringExtra("ps_id");
        psUsername = getIntent().getStringExtra("ps_username");
        psAvatarUrl = getIntent().getStringExtra("ps_avatar_url");
        
        // 记录获取到的值
        Log.d("PSAppDetail", "Received psAppId: " + psAppId);
        Log.d("PSAppDetail", "Received psId: " + psId);
        Log.d("PSAppDetail", "Received psUsername: " + psUsername);
        Log.d("PSAppDetail", "Received psAvatarUrl: " + psAvatarUrl);

        initializeViews();
        initializeApplyDialog();
        setClickListeners();
        displayIntentData();
        fetchJsonData();
        
        // 获取目标用户的性别信息
        fetchTargetGenderInfo();
    }

    private void initializeViews() {
        appIdTextView = findViewById(R.id.appIdTextView);
       // memberIdTextView = findViewById(R.id.memberIdTextView);
        subjectTextView = findViewById(R.id.subjectTextView);
        classLevelTextView = findViewById(R.id.classLevelTextView);
        feeTextView = findViewById(R.id.feeTextView);
        districtTextView = findViewById(R.id.districtTextView);
        matchingScoreTextView = findViewById(R.id.matchingScoreTextView);
        usernameTextView = findViewById(R.id.usernameTextView);
        profileIconImageView = findViewById(R.id.profileIconImageView);
        genderIcon = findViewById(R.id.genderIcon);
        exitBtn = findViewById(R.id.exitButton);
        applyButton = findViewById(R.id.applyButton);
    }

    private void initializeApplyDialog() {
        applyDialog = new Dialog(this);
        applyDialog.setContentView(R.layout.dialog_matching_details);

        applyDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(applyDialog.getWindow().getAttributes());
        lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        applyDialog.getWindow().setAttributes(lp);

        applicationsContainer = applyDialog.findViewById(R.id.applicationsContainer);

        Button cancelButton = applyDialog.findViewById(R.id.dialogCancelButton);
        Button confirmButton = applyDialog.findViewById(R.id.dialogConfirmButton);

        cancelButton.setOnClickListener(v -> applyDialog.dismiss());
        confirmButton.setOnClickListener(v -> {
            if (selectedAppId.isEmpty()) {
                Toast.makeText(PSAppDetail.this, "Please select an application first", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String status = appStatusMap.get(selectedAppId);
            if (status == null || !status.equals("A")) {
                Toast.makeText(PSAppDetail.this, "This application has not been approved yet", Toast.LENGTH_SHORT).show();
                return;
            }
            
            checkIfMatchExist();
            applyDialog.dismiss();
        });
    }

    private void setClickListeners() {
        exitBtn.setOnClickListener(v -> finish());
        applyButton.setOnClickListener(v -> {
            fetchTutorApplicationData();
            showApplyDialog();
        });
    }

    private void displayIntentData() {
        String psAppId = getIntent().getStringExtra("psAppId");
        String memberId = getIntent().getStringExtra("ps_id");
        String classLevel = getIntent().getStringExtra("classLevel");
        String fee = getIntent().getStringExtra("fee");
        ArrayList<String> subjects = getIntent().getStringArrayListExtra("subjects");
        ArrayList<String> districts = getIntent().getStringArrayListExtra("districts");

        // 设置用户头像
        if (psAvatarUrl != null && !psAvatarUrl.isEmpty()) {
            Log.d("PSAppDetail", "Loading avatar URL: " + psAvatarUrl);
            Glide.with(this)
                 .load(psAvatarUrl)
                 .placeholder(R.drawable.default_avatar)
                 .error(R.drawable.default_avatar)
                 .circleCrop()
                 .into(profileIconImageView);
        } else {
            profileIconImageView.setImageResource(R.drawable.default_avatar);
            Log.d("PSAppDetail", "No avatar URL provided, using default");
        }

        // 设置用户名
        if (psUsername != null && !psUsername.isEmpty()) {
            usernameTextView.setText(psUsername);
            Log.d("PSAppDetail", "Setting username: " + psUsername);
        } else {
            usernameTextView.setText(getString(R.string.unknown_student));
            Log.d("PSAppDetail", "No username provided, using default");
        }
        
        // 设置应用ID为小文本
        if (psAppId != null) {
            appIdTextView.setText(getString(R.string.app_id_prefix, psAppId));
        }
        
        if (classLevel != null) {
            classLevelTextView.setText(classLevel);
        }
        
        if (fee != null) {
            feeTextView.setText(getString(R.string.fee_hourly_format, fee));
        }

        if (subjects != null && !subjects.isEmpty()) {
            subjectTextView.setText(String.join(", ", subjects));
        } else {
            subjectTextView.setText(getString(R.string.n_a));
        }

        if (districts != null && !districts.isEmpty()) {
            districtTextView.setText(String.join(", ", districts));
        } else {
            districtTextView.setText(getString(R.string.n_a));
        }
    }

    private void fetchTutorApplicationData() {
        String url = "http://"+ IPConfig.getIP()+"/FYP/php/get_member_own_T_application_data.php";

        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", tutorId)
                .add("role", "Tutor")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("TutorSendRequestToPS", "Request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(PSAppDetail.this, "Failed to fetch data", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    Log.d("TutorSendRequestToPS", "Server response: " + jsonResponse);

                    try {
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        if (jsonObject.getBoolean("success")) {
                            JSONArray dataArray = jsonObject.getJSONArray("data");

                            runOnUiThread(() -> applicationsContainer.removeAllViews());
                            
                            appStatusMap.clear();

                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject data = dataArray.getJSONObject(i);
                                
                                String status = data.optString("status", "");
                                
                                if (!"A".equals(status)) {
                                    Log.d("TutorSendRequestToPS", "Skipping non-approved application: " + data.optString("app_id"));
                                    continue;
                                }
                                
                                View applicationView = LayoutInflater.from(PSAppDetail.this)
                                        .inflate(R.layout.history_application_item, applicationsContainer, false);

                                final String appId = data.optString("app_id", "N/A");
                                String studentLevel = data.optString("class_level_name", "N/A");
                                String fee = data.optString("feePerHr", "N/A");
                                String description = data.optString("description", "N/A");
                                
                                appStatusMap.put(appId, status);

                                JSONArray subjectNames = data.optJSONArray("subject_names");
                                StringBuilder subjectsStr = new StringBuilder();
                                if (subjectNames != null && subjectNames.length() > 0) {
                                    for (int j = 0; j < subjectNames.length(); j++) {
                                        if (j > 0) subjectsStr.append(", ");
                                        subjectsStr.append(subjectNames.getString(j));
                                    }
                                } else {
                                    subjectsStr.append("N/A");
                                }

                                JSONArray districtNames = data.optJSONArray("district_names");
                                StringBuilder districtsStr = new StringBuilder();
                                if (districtNames != null && districtNames.length() > 0) {
                                    for (int j = 0; j < districtNames.length(); j++) {
                                        if (j > 0) districtsStr.append(", ");
                                        districtsStr.append(districtNames.getString(j));
                                    }
                                } else {
                                    districtsStr.append("N/A");
                                }

                                final String subjects = subjectsStr.toString();
                                final String districts = districtsStr.toString();

                                ((TextView) applicationView.findViewById(R.id.app_id)).setText(appId);
                                ((TextView) applicationView.findViewById(R.id.subject_text)).setText(getString(R.string.subject_prefix, subjects));
                                ((TextView) applicationView.findViewById(R.id.student_level_text)).setText(studentLevel);
                                ((TextView) applicationView.findViewById(R.id.fee_text)).setText(getString(R.string.fee_prefix, fee));
                                ((TextView) applicationView.findViewById(R.id.district_text)).setText(getString(R.string.district_prefix, districts));
                                
                                TextView statusView = applicationView.findViewById(R.id.status_text);
                                if (statusView != null) {
                                    if (status.equals("P")) {
                                        statusView.setText("Pending");
                                        statusView.setBackgroundResource(R.drawable.status_pending_pill);
                                    } else if (status.equals("A")) {
                                        statusView.setText("Approved");
                                        statusView.setBackgroundResource(R.drawable.status_approved_pill);
                                    } else if (status.equals("R")) {
                                        statusView.setText("Rejected");
                                        statusView.setBackgroundResource(R.drawable.status_rejected_pill);
                                    }
                                }

                                applicationView.setOnClickListener(v -> {
                                    for (int j = 0; j < applicationsContainer.getChildCount(); j++) {
                                        applicationsContainer.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
                                    }
                                    v.setBackgroundColor(Color.parseColor("#E8F5E9"));
                                    selectedAppId = appId;

                                    Log.d("TutorSendRequestToPS", "Selected Tutor Application ID: " + selectedAppId);
                                    Toast.makeText(PSAppDetail.this,
                                            "Selected Application: " + appId,
                                            Toast.LENGTH_SHORT).show();
                                });

                                final View finalView = applicationView;
                                runOnUiThread(() -> applicationsContainer.addView(finalView));
                            }
                            
                            if (appStatusMap.isEmpty()) {
                                runOnUiThread(() -> {
                                    View noAppsView = LayoutInflater.from(PSAppDetail.this)
                                            .inflate(android.R.layout.simple_list_item_1, applicationsContainer, false);
                                    ((TextView) noAppsView).setText(getString(R.string.no_approved_applications));
                                    ((TextView) noAppsView).setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    applicationsContainer.addView(noAppsView);
                                });
                            }
                            
                        } else {
                            String message = jsonObject.optString("message", "Unknown error");
                            runOnUiThread(() -> Toast.makeText(PSAppDetail.this,
                                    message, Toast.LENGTH_SHORT).show());
                        }
                    } catch (JSONException e) {
                        Log.e("TutorSendRequestToPS", "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(PSAppDetail.this,
                                "Error processing data", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private void showApplyDialog() {
        if (applyDialog != null) {
            TextView messageText = applyDialog.findViewById(R.id.dialogMessageText);
            messageText.setText(getString(R.string.confirm_send_request) +
                    classLevelTextView.getText() + "\n" +
                    subjectTextView.getText() + "\n" +
                    feeTextView.getText());

            applyDialog.show();
        }
    }

    private void checkIfMatchExist() {
        if (selectedAppId.isEmpty() || psAppId == null) {
            Log.e("TutorSendRequestToPS", "Missing app IDs - Tutor App ID: " + selectedAppId + ", PS App ID: " + psAppId);
            Toast.makeText(this, "Error: Please select an application first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String status = appStatusMap.get(selectedAppId);
        if (status == null || !status.equals("A")) {
            Toast.makeText(this, "This application has not been approved yet", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody formBody = new FormBody.Builder()
                .add("ps_app_id", psAppId)
                .add("tutor_app_id", selectedAppId)
                .build();

        Log.d("TutorSendRequestToPS", "Sending request - PS App ID: " + psAppId + ", Tutor App ID: " + selectedAppId);

        Request request = new Request.Builder()
                .url("http://"+IPConfig.getIP()+"/FYP/php/check_if_match_exist.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("TutorSendRequestToPS", "Network request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(PSAppDetail.this,
                        "Failed to check match existence. Please try again.",
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("TutorSendRequestToPS", "Raw server response: " + responseData);

                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    final boolean success = jsonResponse.getBoolean("success");
                    final String message = jsonResponse.getString("message");

                    runOnUiThread(() -> {
                        Toast.makeText(PSAppDetail.this, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            Log.d("TutorSendRequestToPS", "Match exists - dismissing dialog");
                            applyDialog.dismiss();
                        } else {
                            Log.d("TutorSendRequestToPS", "No match exists - proceeding with confirmation");
                            handleApplyConfirmation();
                        }
                    });
                } catch (JSONException e) {
                    Log.e("TutorSendRequestToPS", "JSON parsing error: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(PSAppDetail.this,
                            "Error processing server response",
                            Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void handleApplyConfirmation() {
        String scoreText = matchingScoreTextView.getText().toString();
        String matchMark = extractScore(scoreText);

        if (selectedAppId.isEmpty()) {
            Toast.makeText(this, "Please select your application first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (psAppId == null) {
            Toast.makeText(this, "Error: PS application information is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String status = appStatusMap.get(selectedAppId);
        if (status == null || !status.equals("A")) {
            Toast.makeText(this, "This application has not been approved yet", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("TutorSendRequestToPS", "Tutor App ID: " + selectedAppId);
        Log.d("TutorSendRequestToPS", "PS App ID: " + psAppId);
        Log.d("TutorSendRequestToPS", "Match Mark: " + matchMark);

        // 构建请求参数
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("ps_app_id", psAppId)
                .add("tutor_app_id", selectedAppId)
                .add("ps_id", psId)
                .add("tutor_id", tutorId);
                
        // 添加匹配分数
        if (matchMark != null && !matchMark.isEmpty()) {
            formBuilder.add("match_mark", matchMark);
        } else {
            formBuilder.add("match_mark", "50%"); // 默认匹配分数
        }
        
        RequestBody formBody = formBuilder.build();

        // 构建请求
        Request request = new Request.Builder()
                .url("http://"+IPConfig.getIP()+"/FYP/php/post_match_request_from_T.php")
                .post(formBody)
                .build();

        // 发送请求并处理响应
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("TutorSendRequestToPS", "Request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(PSAppDetail.this,
                        "Failed to submit request. Please try again.",
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("TutorSendRequestToPS", "Server response: " + responseData);
                
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    final boolean success = jsonResponse.getBoolean("success");
                    final String message = jsonResponse.getString("message");

                    runOnUiThread(() -> {
                        Toast.makeText(PSAppDetail.this, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            applyDialog.dismiss();
                            finish();
                        }
                    });
                } catch (JSONException e) {
                    Log.e("TutorSendRequestToPS", "JSON parsing error: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(PSAppDetail.this,
                            "Error processing server response",
                            Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void fetchJsonData() {
        Request request = new Request.Builder()
                .url("http://"+IPConfig.getIP()+"/Matching/get_json.php")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(PSAppDetail.this,
                        "Failed to fetch data. Please check your internet connection.",
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    runOnUiThread(() -> updateUIWithJson(jsonResponse));
                } else {
                    runOnUiThread(() -> Toast.makeText(PSAppDetail.this,
                            "Server error. Please try again later.",
                            Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private String extractScore(String scoreText) {
        if (scoreText == null || scoreText.isEmpty()) {
            Log.e("TutorSendRequestToPS", "Score text is null or empty");
            return "50%"; // 默认匹配分数
        }
        
        Log.d("TutorSendRequestToPS", "Raw score text: " + scoreText);
        
        try {
            // 尝试从"Score X: YY%" 格式提取
            String[] parts = scoreText.split(":");
            if (parts.length > 1) {
                String scorePart = parts[1].trim();
                Log.d("TutorSendRequestToPS", "Extracted score part: " + scorePart);
                return scorePart;
            }
            
            // 如果不包含冒号，可能只是单纯的百分比
            if (scoreText.contains("%")) {
                Log.d("TutorSendRequestToPS", "Using raw percentage: " + scoreText);
                return scoreText.trim();
            }
            
            // 其他情况，尝试直接使用文本
            Log.d("TutorSendRequestToPS", "Using raw text: " + scoreText);
            return scoreText;
        } catch (Exception e) {
            Log.e("TutorSendRequestToPS", "Error extracting score: " + e.getMessage());
            return "50%"; // 发生错误时使用默认匹配分数
        }
    }

    private void updateUIWithJson(String jsonResponse) {
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            StringBuilder scores = new StringBuilder();
            String topScore = "";

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.has("score")) {
                    String score = jsonObject.getString("score");
                    if (i == 0) {
                        topScore = score;
                    }
                    scores.append("Score ").append(i + 1).append(": ").append(score).append("\n");
                }
            }

            if (!topScore.isEmpty()) {
                // 在顶部右侧显示第一个分数作为主要分数
                matchingScoreTextView.setText(topScore);
            } else if (scores.length() > 0) {
                // 如果没有单独的主要分数，则显示所有分数
                matchingScoreTextView.setText(scores.toString());
            } else {
                matchingScoreTextView.setText(getString(R.string.n_a_score));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            matchingScoreTextView.setText(getString(R.string.error_score));
        }
    }

    private void displayGenderIcon(String gender) {
        runOnUiThread(() -> {
            if (gender != null && !gender.isEmpty()) {
                if (gender.equals("M")) {
                    genderIcon.setImageResource(R.drawable.ic_male);
                    genderIcon.setBackgroundResource(R.drawable.circle_background);
                    genderIcon.setVisibility(View.VISIBLE);
                } else if (gender.equals("F")) {
                    genderIcon.setImageResource(R.drawable.ic_female);
                    genderIcon.setBackgroundResource(R.drawable.circle_background);
                    genderIcon.setVisibility(View.VISIBLE);
                } else {
                    genderIcon.setVisibility(View.GONE);
                }
            } else {
                genderIcon.setVisibility(View.GONE);
            }
        });
    }

    private void fetchTargetGenderInfo() {
        if (isFinishing()) return;
        
        if (psId == null || psId.isEmpty()) {
            Log.e(DEBUG_TAG, "无法获取目标会员ID");
            return;
        }
        
        OkHttpClient client = new OkHttpClient();
        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_member_detail.php";
        
        RequestBody requestBody = new FormBody.Builder()
            .add("member_id", psId)
            .build();
            
        Request request = new Request.Builder()
            .url(url)
            .post(requestBody)
            .build();
            
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(DEBUG_TAG, "获取性别信息失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(DEBUG_TAG, "获取性别信息服务器错误: " + response.code());
                    return;
                }
                
                String jsonResponse = response.body().string();
                Log.d(DEBUG_TAG, "性别信息响应: " + jsonResponse);
                
                try {
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    
                    if (!jsonObject.optBoolean("success", false)) {
                        // 如果API返回失败，就隐藏性别图标
                        runOnUiThread(() -> genderIcon.setVisibility(View.GONE));
                        return;
                    }
                    
                    JSONArray dataArray = jsonObject.optJSONArray("data");
                    if (dataArray == null || dataArray.length() == 0) {
                        // 如果没有数据，就隐藏性别图标
                        runOnUiThread(() -> genderIcon.setVisibility(View.GONE));
                        return;
                    }
                    
                    // 获取第一条会员详情记录
                    JSONObject memberDetail = dataArray.getJSONObject(0);
                    String gender = memberDetail.optString("Gender", "");
                    
                    // 显示性别图标
                    displayGenderIcon(gender);
                    
                } catch (JSONException e) {
                    Log.e(DEBUG_TAG, "解析性别信息JSON出错: " + e.getMessage());
                    runOnUiThread(() -> genderIcon.setVisibility(View.GONE));
                }
            }
        });
    }
}
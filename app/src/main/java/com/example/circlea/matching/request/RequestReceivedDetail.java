package com.example.circlea.matching.request;

import static android.app.PendingIntent.getActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RequestReceivedDetail extends AppCompatActivity {
    private ImageButton exitButton;
    private MaterialButton acceptButton, rejectButton;
    private ImageView profileIcon;
    private TextView psAppIdTextView, psSubjectTextView, psClassLevelTextView,
            psFeeTextView, psDistrictTextView, psMemberIdTextView,
            tutorAppIdTextView, tutorSubjectTextView, tutorClassLevelTextView,requestMessageTextView,
            tutorFeeTextView, tutorDistrictTextView, tutorMemberIdTextView;
    private  String matchId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.matching_request_detail);

        matchId = getIntent().getStringExtra("match_id");
        Log.d("RequestDetail", "matchId: " + matchId);

        try {
            initializeViews();
            setupClickListeners();
            loadIntentData();
            // 获取当前用户的应用信息填充到顶部卡片
            loadCurrentUserApplication();
            // 检查是否已经显示了完整信息，如果没有则通过API加载
            if (requestMessageTextView.getText().toString().contains("N/A")) {
                getTutorApplicationByMatchId();
            }
        } catch (Exception e) {
            Log.e("RequestDetail", "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error initializing view", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        exitButton = findViewById(R.id.exitButton);
        acceptButton = findViewById(R.id.accept_Button);
        rejectButton = findViewById(R.id.reject_Button);

        requestMessageTextView = findViewById(R.id.requestMessageTextView);

        // PS Details
        psAppIdTextView = findViewById(R.id.psAppIdTextView);
        psSubjectTextView = findViewById(R.id.psSubjectTextView);
        psClassLevelTextView = findViewById(R.id.psClassLevelTextView);
        psFeeTextView = findViewById(R.id.psFeeTextView);
        psDistrictTextView = findViewById(R.id.psDistrictTextView);
        psMemberIdTextView = findViewById(R.id.psMemberIdTextView);

        // Tutor Details
        tutorAppIdTextView = findViewById(R.id.tutorAppIdTextView);
        tutorSubjectTextView = findViewById(R.id.tutorSubjectTextView);
        tutorClassLevelTextView = findViewById(R.id.tutorClassLevelTextView);
        tutorFeeTextView = findViewById(R.id.tutorFeeTextView);
        tutorDistrictTextView = findViewById(R.id.tutorDistrictTextView);
    }

    private void setupClickListeners() {
        exitButton.setOnClickListener(v -> finish());
        rejectButton.setOnClickListener(v -> rejectMatchingRequest(matchId));
        acceptButton.setOnClickListener(v -> acceptMatchingRequest(matchId));
    }

    private void loadIntentData() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            try {
                String psAppId = extras.getString("ps_app_id", "N/A");
                String psUsername = extras.getString("psUsername", "N/A");
                String fee = extras.getString("fee", "N/A");
                String classLevel = extras.getString("class_level", "N/A");
                String subjects = extras.getString("subjects", "N/A");
                String districts = extras.getString("districts", "N/A");
                String profileIconUrl = extras.getString("profile_icon", "");
                String lessonPerWeek = extras.getString("lesson_per_week", "N/A");
                String target_profileUrl = extras.getString("target_profileUrl", "");
                // Set PS details
                psAppIdTextView.setText("Application ID: " + psAppId);
                psSubjectTextView.setText("Subjects: " + subjects);
                psClassLevelTextView.setText("Class Level: " + classLevel);
                psFeeTextView.setText("Fee: HK$" + fee);
                psDistrictTextView.setText("Districts: " + districts);
                psMemberIdTextView.setText("Username: " + psUsername);

                String requestMessage = psUsername + " sent you a request";
                requestMessageTextView.setText(requestMessage);

                // Load profile image if available
                if (target_profileUrl != null && !target_profileUrl.isEmpty() && !target_profileUrl.equals("N/A")) {
                    String fullProfileUrl = "http://" + IPConfig.getIP() + target_profileUrl;
                    Log.d("RequestDetail", "fullProfileUrl:" + fullProfileUrl);
                    Glide.with(this)
                            .load(fullProfileUrl)
                            .error(R.drawable.circle_background)
                            .placeholder(R.drawable.circle_background)
                            .circleCrop()
                            .into(profileIcon);
                } else {
                    // Fallback to just using the profile_icon
                    if (profileIconUrl != null && !profileIconUrl.isEmpty() && !profileIconUrl.equals("N/A")) {
                        String fullProfileUrl = "http://" + IPConfig.getIP() + profileIconUrl;
                        Log.d("RequestDetail", "Using fallback profileIconUrl:" + fullProfileUrl);
                        Glide.with(this)
                                .load(fullProfileUrl)
                                .error(R.drawable.circle_background)
                                .placeholder(R.drawable.circle_background)
                                .circleCrop()
                                .into(profileIcon);
                    } else {
                        // No valid URL, use default image
                        profileIcon.setImageResource(R.drawable.circle_background);
                    }
                }

            } catch (Exception e) {
                Log.e("RequestDetail", "Error loading data: " + e.getMessage());
                Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getTutorApplicationByMatchId() {
        if (matchId == null || matchId.isEmpty()) {
            Log.e("RequestDetail", "Match ID is null or empty");
            return;
        }

        // 获取当前用户ID
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", "");

        RequestBody formBody = new FormBody.Builder()
                .add("match_id", matchId)
                .build();

        Request request = new Request.Builder()
                .url("http://"+ IPConfig.getIP()+"/FYP/php/get_tutor_application_by_match_id.php")
                .post(formBody)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("RequestDetail", "Failed to get application: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(RequestReceivedDetail.this,
                        "Failed to load details", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("RequestDetail", "Raw response: " + responseData);
                try {
                    JSONObject json = new JSONObject(responseData);
                    if (json.getBoolean("success")) {
                        JSONArray dataArray = json.getJSONArray("data");
                        if (dataArray.length() > 0) {
                            JSONObject data = dataArray.getJSONObject(0); // Get first application
                            Log.d("RequestDetail", "data: " + data);

                            runOnUiThread(() -> {
                                try {
                                    String appId = data.optString("app_id", "N/A");
                                    String username = data.optString("username", "N/A");
                                    String classLevelName = data.optString("class_level_name", "N/A");
                                    String fee = data.optString("feePerHr", "N/A");
                                    String memberId = data.optString("member_id", "N/A");

                                    // Handle subject names array
                                    JSONArray subjectNames = data.optJSONArray("subject_names");
                                    String subjects = "N/A";
                                    if (subjectNames != null && subjectNames.length() > 0) {
                                        List<String> subjectList = new ArrayList<>();
                                        for (int i = 0; i < subjectNames.length(); i++) {
                                            subjectList.add(subjectNames.getString(i));
                                        }
                                        subjects = TextUtils.join(", ", subjectList);
                                    }

                                    // Handle district names array
                                    JSONArray districtNames = data.optJSONArray("district_names");
                                    String districts = "N/A";
                                    if (districtNames != null && districtNames.length() > 0) {
                                        List<String> districtList = new ArrayList<>();
                                        for (int i = 0; i < districtNames.length(); i++) {
                                            districtList.add(districtNames.getString(i));
                                        }
                                        districts = TextUtils.join(", ", districtList);
                                    }

                                    // 更新底部卡片信息（发送请求方信息）
                                    psAppIdTextView.setText("Application ID: " + appId);
                                    psSubjectTextView.setText("Subjects: " + subjects);
                                    psClassLevelTextView.setText("Class Level: " + classLevelName);
                                    psFeeTextView.setText("Fee: HK$" + fee);
                                    psDistrictTextView.setText("Districts: " + districts);
                                    psMemberIdTextView.setText("Username: " + username);
                                    
                                    // 更新请求消息
                                    if (username != null && !username.equals("N/A")) {
                                        String requestMessage = username + " sent you a request";
                                        requestMessageTextView.setText(requestMessage);
                                    }

                                    // Load profile icon if available
                                    String profileIcon = data.optString("profile_icon", "");
                                    if (!profileIcon.isEmpty() && RequestReceivedDetail.this.profileIcon != null) {
                                        String fullProfileUrl = "http://" + IPConfig.getIP() + profileIcon;
                                        Log.d("RequestDetail", "Loading profile icon from: " + fullProfileUrl);
                                        Glide.with(RequestReceivedDetail.this)
                                                .load(fullProfileUrl)
                                                .error(R.drawable.circle_background)
                                                .placeholder(R.drawable.circle_background)
                                                .circleCrop()
                                                .into(RequestReceivedDetail.this.profileIcon);
                                    }

                                } catch (Exception e) {
                                    Log.e("RequestDetail", "Error updating UI: " + e.getMessage());
                                    Log.e("RequestDetail", "Response data: " + responseData);
                                    Toast.makeText(RequestReceivedDetail.this,
                                            "Error loading details", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Log.e("RequestDetail", "API returned error: " + json.getString("message"));
                        runOnUiThread(() -> Toast.makeText(RequestReceivedDetail.this,
                                json.optString("message", "Error loading details"),
                                Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    Log.e("RequestDetail", "Error parsing JSON response: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(RequestReceivedDetail.this,
                            "Error loading details", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void acceptMatchingRequest(String matchId) {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);

        RequestBody formBody = new FormBody.Builder()
                .add("match_id", matchId)
                .add("member_id", memberId)
                .build();

        Request request = new Request.Builder()
                .url("http://"+IPConfig.getIP()+"/FYP/php/update_match_status_to_Pending.php")
                .post(formBody)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("RequestDetail", "Failed to update status: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(RequestReceivedDetail.this,
                        "Failed to update status", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("RequestDetail", "Raw response: " + responseData);
                try {
                    JSONObject json = new JSONObject(responseData);
                    boolean success = json.getBoolean("success");
                    final String message = json.getString("message");

                    runOnUiThread(() -> {
                        Toast.makeText(RequestReceivedDetail.this, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            // If update was successful, close the activity or update UI
                            finish();
                        }
                    });
                } catch (JSONException e) {
                    Log.e("RequestDetail", "Error parsing JSON response: " + e.getMessage());
                    Log.e("RequestDetail", "Invalid JSON response content: " + responseData);
                    runOnUiThread(() -> Toast.makeText(RequestReceivedDetail.this,
                            "Error processing response", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void  rejectMatchingRequest(String matchId) {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);

        RequestBody formBody = new FormBody.Builder()
                .add("match_id", matchId)
                .add("member_id", memberId)
                .build();
        Request request = new Request.Builder()
                .url("http://"+IPConfig.getIP()+"/FYP/php/update_match_status_to_Reject.php")
                .post(formBody)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("RequestDetail", "Failed to update status: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(RequestReceivedDetail.this,
                        "Failed to update status", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                try {
                    JSONObject json = new JSONObject(responseData);
                    boolean success = json.getBoolean("success");
                    final String message = json.getString("message");

                    runOnUiThread(() -> {
                        Toast.makeText(RequestReceivedDetail.this, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            // If update was successful, close the activity or update UI
                            finish();
                        }
                    });
                } catch (JSONException e) {
                    Log.e("RequestDetail", "Error parsing JSON response: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(RequestReceivedDetail.this,
                            "Error processing response", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // 加载当前用户的应用信息
    private void loadCurrentUserApplication() {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", "");
        
        if (matchId == null || matchId.isEmpty() || memberId.isEmpty()) {
            return;
        }
        
        // 构建请求
        RequestBody formBody = new FormBody.Builder()
                .add("match_id", matchId)
                .build();
                
        // 使用获取当前用户应用信息的API
        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/get_tutor_application_by_match_id.php")
                .post(formBody)
                .build();
                
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("RequestDetail", "Failed to load current user application: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("RequestDetail", "Current user app response: " + responseData);
                
                try {
                    JSONObject json = new JSONObject(responseData);
                    if (json.getBoolean("success")) {
                        JSONArray dataArray = json.getJSONArray("data");
                        if (dataArray.length() > 0) {
                            JSONObject data = dataArray.getJSONObject(0);
                            
                            runOnUiThread(() -> {
                                try {
                                    // 获取应用信息
                                    String appId = data.optString("app_id", "");
                                    String classLevel = data.optString("class_level_name", "");
                                    
                                    // 处理科目
                                    JSONArray subjectNames = data.optJSONArray("subject_names");
                                    String subjects = "";
                                    if (subjectNames != null && subjectNames.length() > 0) {
                                        List<String> subjectList = new ArrayList<>();
                                        for (int i = 0; i < subjectNames.length(); i++) {
                                            subjectList.add(subjectNames.getString(i));
                                        }
                                        subjects = TextUtils.join(", ", subjectList);
                                    }
                                    
                                    // 处理地区
                                    JSONArray districtNames = data.optJSONArray("district_names");
                                    String districts = "";
                                    if (districtNames != null && districtNames.length() > 0) {
                                        List<String> districtList = new ArrayList<>();
                                        for (int i = 0; i < districtNames.length(); i++) {
                                            districtList.add(districtNames.getString(i));
                                        }
                                        districts = TextUtils.join(", ", districtList);
                                    }
                                    
                                    // 更新顶部卡片（当前用户信息）
                                    if (!appId.isEmpty()) {
                                        tutorAppIdTextView.setText("Application ID: " + appId);
                                    }
                                    if (!classLevel.isEmpty()) {
                                        tutorClassLevelTextView.setText("Class Level: " + classLevel);
                                    }
                                    if (!subjects.isEmpty()) {
                                        tutorSubjectTextView.setText("Subjects: " + subjects);
                                    }
                                    if (!districts.isEmpty()) {
                                        tutorDistrictTextView.setText("Districts: " + districts);
                                    }
                                    
                                } catch (Exception e) {
                                    Log.e("RequestDetail", "Error updating top card: " + e.getMessage());
                                }
                            });
                        }
                    }
                } catch (JSONException e) {
                    Log.e("RequestDetail", "Error parsing current user app JSON: " + e.getMessage());
                }
            }
        });
    }
}
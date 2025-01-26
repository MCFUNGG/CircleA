package com.example.circlea.matching;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
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

public class RequestDetail extends AppCompatActivity {
    private ImageButton exitButton;
    private MaterialButton acceptButton, rejectButton;
    private ImageView profileIcon;
    private TextView psAppIdTextView, psSubjectTextView, psClassLevelTextView,
            psFeeTextView, psDistrictTextView, psMemberIdTextView,
            tutorAppIdTextView, tutorSubjectTextView, tutorClassLevelTextView,
            tutorFeeTextView, tutorDistrictTextView, tutorMemberIdTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.matching_request_detail);

        try {
            initializeViews();
            setupClickListeners();
            loadIntentData();
            getTutorApplicationByMatchId();
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
        profileIcon = findViewById(R.id.profileIcon);

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
        tutorMemberIdTextView = findViewById(R.id.tutorMemberIdTextView);
    }

    private void setupClickListeners() {
        exitButton.setOnClickListener(v -> finish());
        rejectButton.setOnClickListener(v -> finish());
        acceptButton.setOnClickListener(v -> finish());
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

                // Set PS details
                psAppIdTextView.setText("PS Application ID: " + psAppId);
                psSubjectTextView.setText("Subjects: " + subjects);
                psClassLevelTextView.setText("Class Level: " + classLevel);
                psFeeTextView.setText("Fee: HK$" + fee);
                psDistrictTextView.setText("Districts: " + districts);
                psMemberIdTextView.setText("Username: " + psUsername);

                // Load profile image if available
                if (!profileIconUrl.isEmpty() && profileIcon != null) {
                    String fullProfileUrl = "http://10.0.2.2" + profileIconUrl;
                    Glide.with(this)
                            .load(fullProfileUrl)
                            .error(R.drawable.circle_background)
                            .placeholder(R.drawable.circle_background)
                            .circleCrop()
                            .into(profileIcon);
                }
            } catch (Exception e) {
                Log.e("RequestDetail", "Error loading data: " + e.getMessage());
                Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getTutorApplicationByMatchId() {
        String matchId = getIntent().getStringExtra("match_id");
        Log.d("RequestDetail", "matchId: " + matchId);

        if (matchId == null || matchId.isEmpty()) {
            Log.e("RequestDetail", "Match ID is null or empty");
            return;
        }

        RequestBody formBody = new FormBody.Builder()
                .add("match_id", matchId)
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.2.2/FYP/php/get_tutor_application_by_match_id.php")
                .post(formBody)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("RequestDetail", "Failed to get tutor application: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(RequestDetail.this,
                        "Failed to load tutor details", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                try {
                    JSONObject json = new JSONObject(responseData);
                    if (json.getBoolean("success")) {
                        JSONObject data = json.getJSONObject("data");
                        Log.d("RequestDetail", "data: " + data);

                        runOnUiThread(() -> {
                            try {
                                String appId = data.optString("app_id", "N/A");
                                String username = data.optString("username", "N/A");
                                String classLevel = data.optString("class_level", "N/A");
                                String fee = data.optString("fee", "N/A");
                                String description = data.optString("description", "N/A");

                                // Handle subjects array
                                JSONArray subjectsArray = data.optJSONArray("subjects");
                                List<String> subjectNames = new ArrayList<>();
                                if (subjectsArray != null) {
                                    for (int i = 0; i < subjectsArray.length(); i++) {
                                        JSONObject subject = subjectsArray.getJSONObject(i);
                                        subjectNames.add(subject.getString("subject_name"));
                                    }
                                }
                                String subjects = subjectNames.isEmpty() ? "N/A" : TextUtils.join(", ", subjectNames);

                                // Handle districts array
                                JSONArray districtsArray = data.optJSONArray("districts");
                                List<String> districtNames = new ArrayList<>();
                                if (districtsArray != null) {
                                    for (int i = 0; i < districtsArray.length(); i++) {
                                        JSONObject district = districtsArray.getJSONObject(i);
                                        districtNames.add(district.getString("district_name"));
                                    }
                                }
                                String districts = districtNames.isEmpty() ? "N/A" : TextUtils.join(", ", districtNames);

                                // Update UI
                                if (tutorAppIdTextView != null)
                                    tutorAppIdTextView.setText("Tutor Application ID: " + appId);
                                if (tutorSubjectTextView != null)
                                    tutorSubjectTextView.setText("Subjects: " + subjects);
                                if (tutorClassLevelTextView != null)
                                    tutorClassLevelTextView.setText("Class Level: " + classLevel);
                                if (tutorFeeTextView != null)
                                    tutorFeeTextView.setText("Fee: HK$" + fee);
                                if (tutorDistrictTextView != null)
                                    tutorDistrictTextView.setText("Districts: " + districts);
                                if (tutorMemberIdTextView != null)
                                    tutorMemberIdTextView.setText("Username: " + username);

                            } catch (Exception e) {
                                Log.e("RequestDetail", "Error updating UI: " + e.getMessage());
                                Toast.makeText(RequestDetail.this,
                                        "Error loading tutor details", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Log.e("RequestDetail", "API returned error: " + json.getString("message"));
                        runOnUiThread(() -> Toast.makeText(RequestDetail.this,
                                json.optString("message", "Error loading tutor details"),
                                Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    Log.e("RequestDetail", "Error parsing JSON response: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(RequestDetail.this,
                            "Error loading tutor details", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
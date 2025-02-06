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

                // Set PS details
                psAppIdTextView.setText("PS Application ID: " + psAppId);
                psSubjectTextView.setText("Subjects: " + subjects);
                psClassLevelTextView.setText("Class Level: " + classLevel);
                psFeeTextView.setText("Fee: HK$" + fee);
                psDistrictTextView.setText("Districts: " + districts);
                psMemberIdTextView.setText("Username: " + psUsername);

                String requestMessage = psUsername + " sent you a request";
                requestMessageTextView.setText(requestMessage);

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


        if (matchId == null || matchId.isEmpty()) {
            Log.e("RequestDetail", "Match ID is null or empty");
            return;
        }

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
                Log.e("RequestDetail", "Failed to get tutor application: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(RequestReceivedDetail.this,
                        "Failed to load tutor details", Toast.LENGTH_SHORT).show());
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
                            JSONObject data = dataArray.getJSONObject(0); // Get first tutor application
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

                                    // Update UI
                                    if (tutorAppIdTextView != null)
                                        tutorAppIdTextView.setText("Tutor Application ID: " + appId);
                                    if (tutorSubjectTextView != null)
                                        tutorSubjectTextView.setText("Subjects: " + subjects);
                                    if (tutorClassLevelTextView != null)
                                        tutorClassLevelTextView.setText("Class Level: " + classLevelName);
                                    if (tutorFeeTextView != null)
                                        tutorFeeTextView.setText("Fee: HK$" + fee);
                                    if (tutorDistrictTextView != null)
                                        tutorDistrictTextView.setText("Districts: " + districts);
                                    if (tutorMemberIdTextView != null)
                                        tutorMemberIdTextView.setText("Username: " + username);

                                    // Load profile icon if available
                                    String profileIcon = data.optString("profile_icon", "");
                                    if (!profileIcon.isEmpty() && RequestReceivedDetail.this.profileIcon != null) {
                                        String fullProfileUrl = "http://10.0.2.2" + profileIcon;
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
                                            "Error loading tutor details", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Log.e("RequestDetail", "API returned error: " + json.getString("message"));
                        runOnUiThread(() -> Toast.makeText(RequestReceivedDetail.this,
                                json.optString("message", "Error loading tutor details"),
                                Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    Log.e("RequestDetail", "Error parsing JSON response: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(RequestReceivedDetail.this,
                            "Error loading tutor details", Toast.LENGTH_SHORT).show());
                }
            }
        });
}

    private void acceptMatchingRequest(String matchId) {
        RequestBody formBody = new FormBody.Builder()
                .add("match_id", matchId)
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

    private void  rejectMatchingRequest(String matchId) {
        RequestBody formBody = new FormBody.Builder()
                .add("match_id", matchId)
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



}
package com.example.circlea.home;

import static android.app.PendingIntent.getActivity;
import static android.content.ContentValues.TAG;

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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.IPConfig;
import com.example.circlea.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;

public class TutorAppDetail extends AppCompatActivity {

    private TextView appIdTextView, memberIdTextView, subjectTextView, classLevelTextView,
            feeTextView, districtTextView, matchingScoreTextView,educationText;
    private ImageButton exitBtn;
    private Button applyButton;
    private Dialog applyDialog;
    private String psId ="";
    private OkHttpClient client;
    private LinearLayout applicationsContainer;
    private String selectedAppId = "";
    private String tutorAppId = "";
    String tutorId = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_t_app_detail);

        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        psId = sharedPreferences.getString("member_id", null);

        tutorAppId = getIntent().getStringExtra("tutotAppId");
        tutorId = getIntent().getStringExtra("tutor_id");

        // Initialize views
        initializeViews();
        initializeApplyDialog();
        setClickListeners();

        // Get and display intent data
        displayIntentData();

        // Fetch latest data from get_json.php
        fetchJsonData();
    }

    private void initializeViews() {
        appIdTextView = findViewById(R.id.appIdTextView);
        //memberIdTextView = findViewById(R.id.memberIdTextView);
        subjectTextView = findViewById(R.id.subjectTextView);
        classLevelTextView = findViewById(R.id.classLevelTextView);
        feeTextView = findViewById(R.id.feeTextView);
        districtTextView = findViewById(R.id.districtTextView);
        matchingScoreTextView = findViewById(R.id.matchingScoreTextView);
        exitBtn = findViewById(R.id.exitButton);
        applyButton = findViewById(R.id.applyButton);
        educationText = findViewById(R.id.educationText);
        if (matchingScoreTextView == null) {
            Log.e("TutorAppDetail", "Failed to initialize matchingScoreTextView");
        }
    }

    private void initializeApplyDialog() {
        if (!isFinishing()) {  // Check if activity is not finishing
            applyDialog = new Dialog(this);
            applyDialog.setContentView(R.layout.dialog_matching_details);

            // Make dialog background transparent
            if (applyDialog.getWindow() != null) {
                applyDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                // Set dialog width to 90% of screen width
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(applyDialog.getWindow().getAttributes());
                lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
                applyDialog.getWindow().setAttributes(lp);
            }
            applyDialog = new Dialog(this);
            applyDialog.setContentView(R.layout.dialog_matching_details);  // Use the correct layout

            // Make dialog background transparent
            applyDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Set dialog width to 90% of screen width
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(applyDialog.getWindow().getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            applyDialog.getWindow().setAttributes(lp);

            applicationsContainer = applyDialog.findViewById(R.id.applicationsContainer);
            // Find dialog buttons
            Button cancelButton = applyDialog.findViewById(R.id.dialogCancelButton);
            Button confirmButton = applyDialog.findViewById(R.id.dialogConfirmButton);

            // Set click listeners for dialog buttons
            cancelButton.setOnClickListener(v -> applyDialog.dismiss());
            confirmButton.setOnClickListener(v -> {
                checkIfMatchExist();

                applyDialog.dismiss();
            });
        }
    }

    private void setClickListeners() {
        exitBtn.setOnClickListener(v -> finish());
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchPsApplicationData();
                showApplyDialog();
            }
        });
    }
    // show tutor application detail
    private void displayIntentData() {
        // Get data from intent
        String tutotAppId = getIntent().getStringExtra("tutotAppId");
        String memberId = getIntent().getStringExtra("member_id");
        String classLevel = getIntent().getStringExtra("classLevel");
        String fee = getIntent().getStringExtra("fee");
        String education = getIntent().getStringExtra("education");
        // Add debug logs to check all intent data
        Log.d("DEBUG_INTENT", "All extras in intent: " + getIntent().getExtras());
        Log.d("DEBUG_INTENT", "Tutor ID: " + getIntent().getStringExtra("tutor_id"));
        Log.d("TEST_EDUCATION", "Raw education data: [" + education + "]");
        Log.d("Test123","education data after intent in TutorAppDetail.java : "+education);
        ArrayList<String> subjects = getIntent().getStringArrayListExtra("subjects");
        ArrayList<String> districts = getIntent().getStringArrayListExtra("districts");

        // Display the data with null checks
        if (tutotAppId != null) {
            appIdTextView.setText("Application ID: " + tutotAppId);
        }
        if (memberId != null) {
            memberIdTextView.setText("Member ID: " + memberId);
        }
        if (classLevel != null) {
            classLevelTextView.setText("Class Level: " + classLevel);
        }
        if (fee != null) {
            feeTextView.setText(String.format(getString(R.string.fee_per_hour_format), fee));
        }

        if (education != null) {
            educationText.setText(education);
            Log.d(TAG,"educationtextview: "+education);
        }

        // Display subjects
        if (subjects != null && !subjects.isEmpty()) {
            subjectTextView.setText(String.format(getString(R.string.subjects_format), String.join(", ", subjects)));
        } else {
            subjectTextView.setText(getString(R.string.subjects_na));
        }

        // Display districts
        if (districts != null && !districts.isEmpty()) {
            districtTextView.setText(String.format(getString(R.string.districts_format), String.join(", ", districts)));
        } else {
            districtTextView.setText(getString(R.string.districts_na));
        }

        if (education != null && !education.isEmpty()) {
            educationText.setText(education);
            Log.d("DEBUG_EDUCATION", "Setting education text: " + education);
        } else {
            educationText.setText(getString(R.string.no_education_background));
            Log.d("DEBUG_EDUCATION", "No education data available");
        }
    }

    private void fetchPsApplicationData() {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);
        String url = "http://"+ IPConfig.getIP()+"/FYP/php/get_member_own_PS_application_data.php";
        client = new OkHttpClient();

        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", memberId)
                .add("role", "PS")  // Specifically requesting PS role
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("PsSendRequestToT", "Request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(TutorAppDetail.this, "Failed to fetch data", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    Log.d("PsSendRequestToT", "Server response: " + jsonResponse);

                    try {
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        if (jsonObject.getBoolean("success")) {
                            JSONArray dataArray = jsonObject.getJSONArray("data");

                            // Clear previous views
                            runOnUiThread(() -> applicationsContainer.removeAllViews());

                            // Iterate through the application array
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject data = dataArray.getJSONObject(i);
                                View applicationView = LayoutInflater.from(TutorAppDetail.this)
                                        .inflate(R.layout.history_application_item, applicationsContainer, false);

                                // Get basic info
                                final String appId = data.optString("app_id", "N/A");
                                String studentLevel = data.optString("class_level_name", "N/A");
                                String fee = data.optString("feePerHr", "N/A");
                                String description = data.optString("description", "N/A");
                                String status = data.optString("status", "N/A");


                                TextView statusTextView = applicationView.findViewById(R.id.status_tv);
                                if (statusTextView != null) {
                                    if (status.equals("P")) {
                                        statusTextView.setText("Pending");
                                        statusTextView.setBackgroundResource(R.drawable.status_pending_pill);
                                    } else if (status.equals("A")) {
                                        statusTextView.setText("Approved");
                                        statusTextView.setBackgroundResource(R.drawable.status_approved_pill);
                                    } else if (status.equals("R")) {
                                        statusTextView.setText("Rejected");
                                        statusTextView.setBackgroundResource(R.drawable.status_rejected_pill);
                                    }
                                } else {
                                    // 尝试使用我们在PSAppDetail.java中使用的ID
                                    statusTextView = applicationView.findViewById(R.id.status_text);
                                    if (statusTextView != null) {
                                        if (status.equals("P")) {
                                            statusTextView.setText("Pending");
                                            statusTextView.setBackgroundResource(R.drawable.status_pending_pill);
                                        } else if (status.equals("A")) {
                                            statusTextView.setText("Approved");
                                            statusTextView.setBackgroundResource(R.drawable.status_approved_pill);
                                        } else if (status.equals("R")) {
                                            statusTextView.setText("Rejected");
                                            statusTextView.setBackgroundResource(R.drawable.status_rejected_pill);
                                        }
                                    }
                                    // 如果两个ID都找不到，就忽略状态显示
                                }
                                // Handle subject names array
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

                                // Handle district names array
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

                                // Set values
                                ((TextView) applicationView.findViewById(R.id.app_id)).setText(appId);
                                ((TextView) applicationView.findViewById(R.id.subject_text)).setText("Subject: " + subjects);
                                ((TextView) applicationView.findViewById(R.id.student_level_text)).setText(studentLevel);
                                ((TextView) applicationView.findViewById(R.id.fee_text)).setText("$" + fee);
                                ((TextView) applicationView.findViewById(R.id.district_text)).setText("District: " + districts);
                                // ((TextView) applicationView.findViewById(R.id.description_text)).setText(description);

                                // Add click listener for selection
                                applicationView.setOnClickListener(v -> {
                                    // Deselect all views
                                    for (int j = 0; j < applicationsContainer.getChildCount(); j++) {
                                        applicationsContainer.getChildAt(j)
                                                .setBackgroundColor(Color.TRANSPARENT);
                                    }
                                    // Select clicked view
                                    v.setBackgroundColor(Color.parseColor("#E8F5E9"));
                                    selectedAppId = appId;

                                    Log.d("PsSendRequestToT", "Selected PS Application ID: " + selectedAppId);
                                    Toast.makeText(TutorAppDetail.this,
                                            "Selected Application: " + appId,
                                            Toast.LENGTH_SHORT).show();
                                });

                                // Add to container
                                final View finalView = applicationView;
                                runOnUiThread(() -> applicationsContainer.addView(finalView));
                            }
                        } else {
                            String message = jsonObject.optString("message", "Unknown error");
                            runOnUiThread(() -> Toast.makeText(TutorAppDetail.this,
                                    message, Toast.LENGTH_SHORT).show());
                        }
                    } catch (JSONException e) {
                        Log.e("PsSendRequestToT", "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(TutorAppDetail.this,
                                "Error processing data", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dismiss dialog if it's showing
        if (applyDialog != null && applyDialog.isShowing()) {
            applyDialog.dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Dismiss dialog if it's showing
        if (applyDialog != null && applyDialog.isShowing()) {
            applyDialog.dismiss();
        }
    }

    private void showApplyDialog() {
        if (applyDialog != null && !isFinishing()) {  // Check if activity is not finishing
            // Update dialog message with current details
            TextView messageText = applyDialog.findViewById(R.id.dialogMessageText);
            messageText.setText("Are you sure you want to apply for this tutor?\n\n" +
                    classLevelTextView.getText() + "\n" +
                    subjectTextView.getText() + "\n" +
                    feeTextView.getText());

            applyDialog.show();
        }
    }
    private void checkIfMatchExist() {
        String tutorId = getIntent().getStringExtra("member_id");

        if (selectedAppId == null || tutorAppId == null) {  // Changed condition to check actual app IDs
            Log.e("PsSendRequestToT", "Missing app IDs - PS App ID: " + selectedAppId + ", Tutor App ID: " + tutorAppId);
            Toast.makeText(this, "Error: Please select an application first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create OkHttpClient for the request
        OkHttpClient client = new OkHttpClient();

        // Create the request body with the required parameters
        RequestBody formBody = new FormBody.Builder()
                .add("ps_app_id", selectedAppId)
                .add("tutor_app_id", tutorAppId)
                .build();

        // Log the request parameters
        Log.d("PsSendRequestToT", "Sending request - PS App ID: " + selectedAppId + ", Tutor App ID: " + tutorAppId);

        // Create the request
        Request request = new Request.Builder()
                .url("http://"+IPConfig.getIP()+"/FYP/php/check_if_match_exist.php")
                .post(formBody)
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("PsSendRequestToT", "Network request failed: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(TutorAppDetail.this,
                            "Failed to check match existence. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("PsSendRequestToT", "Raw server response: " + responseData);

                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    final boolean success = jsonResponse.getBoolean("success");
                    final String message = jsonResponse.getString("message");

                    Log.d("PsSendRequestToT", "Parsed response - Success: " + success +
                            ", Message: " + message +
                            ", PS App ID: " + selectedAppId +
                            ", Tutor App ID: " + tutorAppId);

                    runOnUiThread(() -> {
                        Toast.makeText(TutorAppDetail.this, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            Log.d("PsSendRequestToT", "Match exists - dismissing dialog");
                            applyDialog.dismiss();
                        } else {
                            Log.d("PsSendRequestToT", "No match exists - proceeding with confirmation");
                            handleApplyConfirmation();
                        }
                    });
                } catch (JSONException e) {
                    Log.e("PsSendRequestToT", "JSON parsing error: " + e.getMessage() +
                            "\nResponse data: " + responseData);
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(TutorAppDetail.this,
                                "Error processing server response",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    //post match request from PS
    private void handleApplyConfirmation() {
        // Get the matching score
        String scoreText = matchingScoreTextView.getText().toString();
        String matchMark = extractScore(scoreText);

        // Check if we have all required information
        if (selectedAppId == null) {
            Toast.makeText(this, "Please select your application first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tutorAppId == null) {
            Toast.makeText(this, "Error: Tutor application information is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        if (scoreText == null) {
            Toast.makeText(this, "Error: Matching score is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        // Log the values for debugging
        Log.d("PsSendRequestToT", "PS App ID: " + selectedAppId);
        Log.d("PsSendRequestToT", "Tutor App ID: " + tutorAppId);
        Log.d("PsSendRequestToT", "Match Mark: " + scoreText);

        // Create OkHttpClient for the request
        OkHttpClient client = new OkHttpClient();

        // Create the request body with the required parameters
        RequestBody formBody = new FormBody.Builder()
                .add("ps_app_id", selectedAppId)
                .add("tutor_app_id", tutorAppId)
                .add("ps_id", psId)
                .add("tutor_id", tutorId)
                .add("match_mark", scoreText)
                .build();

        // Create the request
        Request request = new Request.Builder()
                .url("http://"+IPConfig.getIP()+"/FYP/php/post_match_request_from_PS.php")
                .post(formBody)
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("PsSendRequestToT", "Request failed: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(TutorAppDetail.this,
                            "Failed to submit application. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("PsSendRequestToT", "Raw response: " + responseData);

                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    boolean success = jsonResponse.getBoolean("success");
                    
                    // Log FCM debug information if available
                    if (jsonResponse.has("fcm_debug")) {
                        JSONObject fcmDebug = jsonResponse.getJSONObject("fcm_debug");
                        Log.d("FCM_Debug", "=== FCM Debug Information ===");
                        
                        // Log debug logs if available
                        if (fcmDebug.has("debug_logs")) {
                            JSONArray debugLogs = fcmDebug.getJSONArray("debug_logs");
                            for (int i = 0; i < debugLogs.length(); i++) {
                                Log.d("FCM_Debug", debugLogs.getString(i));
                            }
                        }
                        
                        // Log FCM request details
                        if (fcmDebug.has("fcm_request")) {
                            Log.d("FCM_Debug", "Request: " + fcmDebug.getJSONObject("fcm_request").toString(2));
                        }
                        
                        // Log FCM response
                        if (fcmDebug.has("fcm_response")) {
                            Log.d("FCM_Debug", "Response: " + fcmDebug.get("fcm_response").toString());
                        }
                        
                        // Log any FCM errors
                        if (fcmDebug.has("fcm_error")) {
                            Log.e("FCM_Debug", "Error: " + fcmDebug.getString("fcm_error"));
                        }
                    }

                    final String message = jsonResponse.optString("message", "Unknown response");
                    Log.d("PsSendRequestToT", "Success: " + success + ", Message: " + message);

                    runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(TutorAppDetail.this,
                                    "Application submitted successfully!",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(TutorAppDetail.this,
                                    "Failed to submit application: " + message,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (JSONException e) {
                    Log.e("PsSendRequestToT", "JSON parsing error: " + e.getMessage() +
                            "\nResponse data: " + responseData);
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(TutorAppDetail.this,
                                "Error processing server response",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    //score part


    private void updateUIWithJson(String jsonResponse) {
        if (isFinishing()) {
            Log.e("TutorAppDetail", "Activity is finishing");
            return;
        }

        runOnUiThread(() -> {
            // Check if view is still valid
            if (matchingScoreTextView == null) {
                Log.e("TutorAppDetail", "matchingScoreTextView is null");
                return;
            }

            try {
                JSONArray jsonArray = new JSONArray(jsonResponse);
                StringBuilder scores = new StringBuilder();


                JSONObject jsonObject = jsonArray.getJSONObject(0);
                if (jsonObject.has("score")) {
                    String score = jsonObject.getString("score");
                    scores.append(score);
                }


                final String finalScore = scores.length() > 0 ?
                        scores.toString() : "No matching scores found.";

                // Final null check before setting text
                if (matchingScoreTextView != null) {
                    matchingScoreTextView.setText(finalScore);
                }

            } catch (JSONException e) {
                Log.e("TutorAppDetail", "JSON parsing error: " + e.getMessage());
                // Final null check before setting error text
                if (matchingScoreTextView != null) {
                    matchingScoreTextView.setText("Error parsing matching scores.");
                }
            }
        });
    }

    // Also update the fetchJsonData method to include additional checks
    private void fetchJsonData() {
        if (isFinishing()) return;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://"+IPConfig.getIP()+"/Matching/get_json.php")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (isFinishing()) return;

                runOnUiThread(() -> {
                    if (!isFinishing() && matchingScoreTextView != null) {
                        matchingScoreTextView.setText("Failed to fetch data");
                        Toast.makeText(TutorAppDetail.this,
                                "Failed to fetch data. Please check your internet connection.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (isFinishing()) return;

                if (response.isSuccessful()) {
                    final String jsonResponse = response.body().string();
                    if (!isFinishing()) {
                        updateUIWithJson(jsonResponse);
                    }
                } else {
                    runOnUiThread(() -> {
                        if (!isFinishing() && matchingScoreTextView != null) {
                            matchingScoreTextView.setText("Server error");
                            Toast.makeText(TutorAppDetail.this,
                                    "Server error. Please try again later.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private String extractScore(String scoreText) {
        if (scoreText == null) {
            Log.e("TutorAppDetail", "scoreText is null");
            return null;
        }

        String[] parts = scoreText.split(":");
        if (parts.length > 1) {
            String scorePart = parts[1].trim(); // Get the part after the colon
            return scorePart; // Remove the '%' and trim whitespace
        }
        return null; // Return null if the format is unexpected
    }

}
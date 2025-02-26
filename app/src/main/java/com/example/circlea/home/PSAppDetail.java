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

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PSAppDetail extends AppCompatActivity {
    private TextView appIdTextView, memberIdTextView, subjectTextView, classLevelTextView,
            feeTextView, districtTextView, matchingScoreTextView;
    private ImageButton exitBtn;
    private Button applyButton;
    private Dialog applyDialog;
    private String tutorId;
    private OkHttpClient client;
    private LinearLayout applicationsContainer;
    private String selectedAppId = "";
    private String psAppId = "";
    private String psId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_ps_app_detail);

        client = new OkHttpClient();
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        tutorId = sharedPreferences.getString("member_id", null);

        psAppId = getIntent().getStringExtra("psAppId");
        psId = getIntent().getStringExtra("ps_id");

        initializeViews();
        initializeApplyDialog();
        setClickListeners();
        displayIntentData();
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

        if (psAppId != null) appIdTextView.setText("Application ID: " + psAppId);
        //if (memberId != null) memberIdTextView.setText("Member ID: " + memberId);
        if (classLevel != null) classLevelTextView.setText("Class Level: " + classLevel);
        if (fee != null) feeTextView.setText("Fee: $" + fee + " /hr");

        if (subjects != null && !subjects.isEmpty()) {
            subjectTextView.setText("Subjects: " + String.join(", ", subjects));
        } else {
            subjectTextView.setText("Subjects: N/A");
        }

        if (districts != null && !districts.isEmpty()) {
            districtTextView.setText("Districts: " + String.join(", ", districts));
        } else {
            districtTextView.setText("Districts: N/A");
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

                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject data = dataArray.getJSONObject(i);
                                View applicationView = LayoutInflater.from(PSAppDetail.this)
                                        .inflate(R.layout.history_application_item, applicationsContainer, false);

                                final String appId = data.optString("app_id", "N/A");
                                String studentLevel = data.optString("class_level_name", "N/A");
                                String fee = data.optString("feePerHr", "N/A");
                                String description = data.optString("description", "N/A");

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
                                ((TextView) applicationView.findViewById(R.id.subject_text)).setText("Subject: " + subjects);
                                ((TextView) applicationView.findViewById(R.id.student_level_text)).setText(studentLevel);
                                ((TextView) applicationView.findViewById(R.id.fee_text)).setText("$" + fee);
                                ((TextView) applicationView.findViewById(R.id.district_text)).setText("District: " + districts);
                                // ((TextView) applicationView.findViewById(R.id.description_text)).setText(description);

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
            messageText.setText("Are you sure you want to send request to this student?\n\n" +
                    classLevelTextView.getText() + "\n" +
                    subjectTextView.getText() + "\n" +
                    feeTextView.getText());

            applyDialog.show();
        }
    }

    private void checkIfMatchExist() {
        if (selectedAppId == null || psAppId == null) {
            Log.e("TutorSendRequestToPS", "Missing app IDs - Tutor App ID: " + selectedAppId + ", PS App ID: " + psAppId);
            Toast.makeText(this, "Error: Please select an application first", Toast.LENGTH_SHORT).show();
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

        if (selectedAppId == null) {
            Toast.makeText(this, "Please select your application first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (psAppId == null) {
            Toast.makeText(this, "Error: PS application information is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        if (matchMark == null) {
            Toast.makeText(this, "Error: Matching score is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("TutorSendRequestToPS", "Tutor App ID: " + selectedAppId);
        Log.d("TutorSendRequestToPS", "PS App ID: " + psAppId);
        Log.d("TutorSendRequestToPS", "Match Mark: " + matchMark);

        RequestBody formBody = new FormBody.Builder()
                .add("ps_app_id", psAppId)
                .add("tutor_app_id", selectedAppId)
                .add("ps_id", psId)
                .add("tutor_id", tutorId)
                .add("match_mark", matchMark)
                .build();

        Request request = new Request.Builder()
                .url("http://"+IPConfig.getIP()+"/FYP/php/post_match_request_from_T.php")
                .post(formBody)
                .build();

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
        String[] parts = scoreText.split(":");
        if (parts.length > 1) {
            return parts[1].trim();
        }
        return null;
    }

    private void updateUIWithJson(String jsonResponse) {
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            StringBuilder scores = new StringBuilder();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.has("score")) {
                    String score = jsonObject.getString("score");
                    scores.append("Score ").append(i + 1).append(": ").append(score).append("\n");
                }
            }

            if (scores.length() > 0) {
                matchingScoreTextView.setText(scores.toString());
            } else {
                matchingScoreTextView.setText("No matching scores found.");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            matchingScoreTextView.setText("Error parsing matching scores.");
        }
    }
}
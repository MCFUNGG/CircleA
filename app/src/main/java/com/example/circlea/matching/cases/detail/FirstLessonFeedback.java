package com.example.circlea.matching.cases.detail;

import android.os.Bundle;
import android.util.Log;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

public class FirstLessonFeedback extends AppCompatActivity {

    private String matchId;
    private String studentId;
    private String tutorId;
    private String tutorAppId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_lesson_feedback);
        Log.d("CurrentJava", "FirstLessonFeedback");
        // Get data from intent
        matchId = getIntent().getStringExtra("case_id");
        studentId = getIntent().getStringExtra("student_id");
        tutorId = getIntent().getStringExtra("tutor_id");

        getTutorAppId();
    }

    private void getTutorAppId() {
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("match_id", matchId)
                .build();

        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/get_tutor_application_by_match_id.php")
                .post(formBody)
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FirstLessonFeedback", "Failed to get tutor_app_id: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(FirstLessonFeedback.this, "Failed to load necessary data", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    Log.d("FirstLessonFeedback", "getTutorAppId response: " + responseData);

                    if (jsonResponse.getBoolean("success")) {
                        JSONObject data = jsonResponse.getJSONArray("data").getJSONObject(0);
                        tutorAppId = data.getString("app_id");
                        Log.d("FirstLessonFeedback", "Got tutor_app_id: " + tutorAppId);
                        
                        runOnUiThread(() -> initializeViews());
                    } else {
                        String message = jsonResponse.optString("message", "Failed to get application data");
                        Log.e("FirstLessonFeedback", message);
                        runOnUiThread(() -> {
                            Toast.makeText(FirstLessonFeedback.this, message, Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                } catch (Exception e) {
                    Log.e("FirstLessonFeedback", "Error parsing response: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(FirstLessonFeedback.this, "Error loading data", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }
        });
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        RatingBar ratingBar = findViewById(R.id.ratingBar);
        TextInputEditText feedbackInput = findViewById(R.id.feedbackInput);
        MaterialButton submitButton = findViewById(R.id.submitButton);
        MaterialTextView skipButton = findViewById(R.id.skipButton);

        // Remove navigation icon
        toolbar.setNavigationIcon(null);

        // Setup submit button
        submitButton.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String feedback = feedbackInput.getText() != null ?
                    feedbackInput.getText().toString().trim() : "";

            if (rating == 0) {
                Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            submitFeedback(rating, feedback);
        });

        // Setup skip button
        skipButton.setOnClickListener(v -> {
            Toast.makeText(this, "Thank you for your time", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void submitFeedback(float rating, String feedback) {
        if (tutorAppId == null) {
            Toast.makeText(this, "Missing application data, please try again", Toast.LENGTH_SHORT).show();
            return;
        }
        
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("member_id", studentId)
                .add("application_id", tutorAppId)
                .add("role", "parent")
                .add("rate_score", String.valueOf(rating))
                .add("comment", feedback)
                .build();

        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/submit_feedback.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(FirstLessonFeedback.this,
                        "Connection failed. Please try again", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    boolean success = jsonResponse.getBoolean("success");
                    final String message = jsonResponse.optString("message",
                            "Feedback submitted successfully");

                    runOnUiThread(() -> {
                        Toast.makeText(FirstLessonFeedback.this,
                                message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            finish();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(FirstLessonFeedback.this,
                            "Error processing response", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
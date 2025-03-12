package com.example.circlea.matching.cases.detail;

import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_lesson_feedback);

        // Get data from intent
        matchId = getIntent().getStringExtra("case_id");
        studentId = getIntent().getStringExtra("student_id");
        tutorId = getIntent().getStringExtra("tutor_id");

        initializeViews();
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
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("match_id", matchId)
                .add("student_id", studentId)
                .add("tutor_id", tutorId)
                .add("rating", String.valueOf(rating))
                .add("feedback", feedback)
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
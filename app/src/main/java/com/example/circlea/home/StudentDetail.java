package com.example.circlea.home;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;

public class StudentDetail extends AppCompatActivity {

    private TextView appIdTextView, memberIdTextView, subjectTextView, classLevelTextView, feeTextView, districtTextView, matchingScoreTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.student_detail);

        appIdTextView = findViewById(R.id.appIdTextView);
        memberIdTextView = findViewById(R.id.memberIdTextView);
        subjectTextView = findViewById(R.id.subjectTextView);
        classLevelTextView = findViewById(R.id.classLevelTextView);
        feeTextView = findViewById(R.id.feeTextView);
        districtTextView = findViewById(R.id.districtTextView);
        matchingScoreTextView = findViewById(R.id.matchingScoreTextView);

        // Get the appId passed from the previous activity
        String appId = getIntent().getStringExtra("appId");
        String memberId = getIntent().getStringExtra("member_id");
        String classLevel = getIntent().getStringExtra("classLevel");
        String fee = getIntent().getStringExtra("fee");

        // Retrieve subjects and districts as ArrayList
        ArrayList<String> subjects = getIntent().getStringArrayListExtra("subjects");
        ArrayList<String> districts = getIntent().getStringArrayListExtra("districts");

        // Display the received data with null checks
        if (appId != null) {
            appIdTextView.setText("Application ID: " + appId);
        }
        if (memberId != null) {
            memberIdTextView.setText("Member ID: " + memberId);
        }
        if (classLevel != null) {
            classLevelTextView.setText("Class Level: " + classLevel);
        }
        if (fee != null) {
            feeTextView.setText("Fee: $" + fee + " /hr");
        }

        // Display subjects
        if (subjects != null && !subjects.isEmpty()) {
            subjectTextView.setText("Subjects: " + String.join(", ", subjects));
        } else {
            subjectTextView.setText("Subjects: N/A");
        }

        // Display districts
        if (districts != null && !districts.isEmpty()) {
            districtTextView.setText("Districts: " + String.join(", ", districts));
        } else {
            districtTextView.setText("Districts: N/A");
        }

        // Fetch latest data from get_json.php
        fetchJsonData();
    }

    private void fetchJsonData() {
        OkHttpClient client = new OkHttpClient();

        // Create a GET request to get_json.php
        Request request = new Request.Builder()
                .url("http://10.0.2.2/Matching/get_json.php") // Replace with your PHP URL
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(StudentDetail.this, "Failed to fetch data. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Parse the JSON response
                    String jsonResponse = response.body().string();
                    runOnUiThread(() -> {
                        // Update the UI with the fetched JSON data
                        updateUIWithJson(jsonResponse);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(StudentDetail.this, "Server error. Please try again later.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    // Method to update the UI with the fetched JSON data
    private void updateUIWithJson(String jsonResponse) {
        try {
            // Parse the JSON response
            JSONArray jsonArray = new JSONArray(jsonResponse);

            // Assuming the "score" field is present in each JSON object in the array
            StringBuilder scores = new StringBuilder();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.has("score")) {
                    String score = jsonObject.getString("score");
                    scores.append("Score ").append(i + 1).append(": ").append(score).append("\n");
                }
            }

            // Display the scores in the TextView
            if (scores.length() > 0) {
                matchingScoreTextView.setText(scores.toString());
            } else {
                matchingScoreTextView.setText("No matching scores found.");
            }

        } catch (JSONException e) {
            // Handle JSON parsing errors
            e.printStackTrace();
            matchingScoreTextView.setText("Error parsing matching scores.");
        }
    }
}
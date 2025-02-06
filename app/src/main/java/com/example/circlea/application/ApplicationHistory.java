package com.example.circlea.application;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.FormBody;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.circlea.IPConfig;
import com.example.circlea.R;

public class ApplicationHistory extends AppCompatActivity {

    private LinearLayout applicationsContainer;
    private OkHttpClient client;
    private String currentRole = "T"; // Default role to Tutor

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_application); // Ensure this layout file contains the container

        // Initialize LinearLayout container
        applicationsContainer = findViewById(R.id.history_application_container);
        client = new OkHttpClient(); // Initialize OkHttpClient

        // Set up buttons
        Button btnTutor = findViewById(R.id.button_tutor);
        Button btnPS = findViewById(R.id.button_ps);

        btnTutor.setOnClickListener(v -> {
            currentRole = "T"; // Set role to Tutor
            fetchApplicationData();
        });

        btnPS.setOnClickListener(v -> {
            currentRole = "PS"; // Set role to Parent/Student
            fetchApplicationData();
        });

        Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> finish());

        // Fetch initial application data as Tutor
        fetchApplicationData();
    }

    private void fetchApplicationData() {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);
        String url = "http://"+ IPConfig.getIP()+"/FYP/php/get_member_own_application_data.php";

        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", memberId)
                .add("role", currentRole) // Send the current role to the server
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchApplicationData", "Request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ApplicationHistory.this, "Failed to fetch data", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    Log.d("FetchApplicationData", "Server response: " + jsonResponse);

                    try {
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        if (jsonObject.getBoolean("success")) {
                            JSONArray dataArray = jsonObject.getJSONArray("data");

                            // Clear previous views
                            runOnUiThread(() -> applicationsContainer.removeAllViews());

                            // Iterate through the application array
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject data = dataArray.getJSONObject(i);
                                LayoutInflater inflater = LayoutInflater.from(ApplicationHistory.this);
                                View applicationView = inflater.inflate(R.layout.history_application_item, applicationsContainer, false);

                                // Get basic info
                                String appId = data.optString("app_id", "N/A");
                                String studentLevel = data.optString("class_level_name", "N/A");
                                String fee = data.optString("feePerHr", "N/A");
                                String description = data.optString("description", "N/A");

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

                                // Set values
                                final String subjects = subjectsStr.toString();
                                final String districts = districtsStr.toString();

                                runOnUiThread(() -> {
                                    ((TextView) applicationView.findViewById(R.id.app_id)).setText("App ID: " + appId);
                                    ((TextView) applicationView.findViewById(R.id.subject_text)).setText("Subject: " + subjects);
                                    ((TextView) applicationView.findViewById(R.id.student_level_text)).setText("" + studentLevel);
                                    ((TextView) applicationView.findViewById(R.id.fee_text)).setText("$" + fee);
                                    ((TextView) applicationView.findViewById(R.id.district_text)).setText("District: " + districts);
                                    //((TextView) applicationView.findViewById(R.id.description_text)).setText("Description: " + description);

                                    applicationsContainer.addView(applicationView);
                                });
                            }
                        } else {
                            String message = jsonObject.optString("message", "Unknown error");
                            runOnUiThread(() -> Toast.makeText(ApplicationHistory.this, message, Toast.LENGTH_SHORT).show());
                        }
                    } catch (JSONException e) {
                        Log.e("FetchApplicationData", "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(ApplicationHistory.this, "Error processing data", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("FetchApplicationData", "Request failed, response code: " + response.code());
                    runOnUiThread(() -> Toast.makeText(ApplicationHistory.this, "Failed to fetch application data", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
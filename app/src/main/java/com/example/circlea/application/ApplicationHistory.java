package com.example.circlea.application;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
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
    private MaterialButton btnTutor;
    private MaterialButton btnPS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_application);

        // Initialize views
        applicationsContainer = findViewById(R.id.history_application_container);
        client = new OkHttpClient();

        // Set up buttons with MaterialButton
        btnTutor = findViewById(R.id.button_tutor);
        btnPS = findViewById(R.id.button_ps);
        ImageButton exitButton = findViewById(R.id.exitButton);

        setupButtons();

        // Fetch initial application data as Tutor
        fetchApplicationData();
    }

    private void setupButtons() {
        // Set initial button states
        updateButtonStates(true); // true means Tutor is selected

        btnTutor.setOnClickListener(v -> {
            currentRole = "T";
            updateButtonStates(true);
            fetchApplicationData();
        });

        btnPS.setOnClickListener(v -> {
            currentRole = "PS";
            updateButtonStates(false);
            fetchApplicationData();
        });

        findViewById(R.id.exitButton).setOnClickListener(v -> finish());
    }

    private void updateButtonStates(boolean isTutorSelected) {
        if (isTutorSelected) {
            btnTutor.setBackgroundColor(getColor(R.color.green_500));
            btnPS.setBackgroundColor(getColor(android.R.color.transparent));
        } else {
            btnPS.setBackgroundColor(getColor(R.color.purple_500));
            btnTutor.setBackgroundColor(getColor(android.R.color.transparent));
        }
    }

    private void fetchApplicationData() {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);
        String url = "http://"+ IPConfig.getIP()+"/FYP/php/get_member_own_application_data.php";

        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", memberId)
                .add("role", currentRole)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchApplicationData", "Request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ApplicationHistory.this,
                        "Failed to fetch data", Toast.LENGTH_SHORT).show());
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
                            runOnUiThread(() -> applicationsContainer.removeAllViews());

                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject data = dataArray.getJSONObject(i);
                                processApplicationData(data);
                            }
                        } else {
                            String message = jsonObject.optString("message", "Unknown error");
                            runOnUiThread(() -> Toast.makeText(ApplicationHistory.this,
                                    message, Toast.LENGTH_SHORT).show());
                        }
                    } catch (JSONException e) {
                        Log.e("FetchApplicationData", "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(ApplicationHistory.this,
                                "Error processing data", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("FetchApplicationData", "Request failed, response code: " + response.code());
                    runOnUiThread(() -> Toast.makeText(ApplicationHistory.this,
                            "Failed to fetch application data", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void processApplicationData(JSONObject data) throws JSONException {
        LayoutInflater inflater = LayoutInflater.from(this);
        View applicationView = inflater.inflate(R.layout.history_application_item,
                applicationsContainer, false);

        String appId = data.optString("app_id", "N/A");
        String studentLevel = data.optString("class_level_name", "N/A");
        String fee = data.optString("feePerHr", "N/A");

        // Process subjects
        JSONArray subjectNames = data.optJSONArray("subject_names");
        String subjects = processArrayToString(subjectNames);

        // Process districts
        JSONArray districtNames = data.optJSONArray("district_names");
        String districts = processArrayToString(districtNames);

        runOnUiThread(() -> {
            try {
                // Using the correct IDs from your layout
                TextView studentLevelView = applicationView.findViewById(R.id.student_level_text);
                TextView subjectView = applicationView.findViewById(R.id.subject_text);
                TextView districtView = applicationView.findViewById(R.id.district_text);
                TextView feeView = applicationView.findViewById(R.id.fee_text);
                TextView appIdView = applicationView.findViewById(R.id.app_id);

                if (studentLevelView != null) {
                    studentLevelView.setText(studentLevel);
                }
                if (subjectView != null) {
                    subjectView.setText(subjects);
                }
                if (districtView != null) {
                    districtView.setText(districts);
                }
                if (feeView != null) {
                    feeView.setText(String.format("HK$%s/hr", fee));
                }
                if (appIdView != null) {
                    appIdView.setText(String.format("ID: %s", appId));
                }

                applicationsContainer.addView(applicationView);
            } catch (Exception e) {
                Log.e("ProcessApplicationData", "Error setting view data: " + e.getMessage());
                Toast.makeText(ApplicationHistory.this,
                        "Error displaying application data", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private String processArrayToString(JSONArray array) throws JSONException {
        if (array == null || array.length() == 0) return "N/A";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            if (i > 0) result.append(", ");
            result.append(array.getString(i));
        }
        return result.toString();
    }
}
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
        btnTutor = findViewById(R.id.button_tutor);
        btnPS = findViewById(R.id.button_ps);
        ImageButton exitButton = findViewById(R.id.exitButton);

        setupButtons();
        fetchApplicationData(); // Initial fetch as Tutor
    }

    private void fetchApplicationData() {
        // Remove this method and directly call the appropriate method
        if (currentRole.equals("T")) {
            fetchTutorApplicationData();
        } else {
            fetchPsApplicationData();
        }
    }

    private void setupButtons() {
        updateButtonStates(true); // true means Tutor is selected

        btnTutor.setOnClickListener(v -> {
            if (!currentRole.equals("T")) {
                currentRole = "T";
                updateButtonStates(true);
                fetchTutorApplicationData();
            }
        });

        btnPS.setOnClickListener(v -> {
            if (!currentRole.equals("PS")) {
                currentRole = "PS";
                updateButtonStates(false);
                fetchPsApplicationData();
            }
        });

        findViewById(R.id.exitButton).setOnClickListener(v -> finish());
    }

    private void updateButtonStates(boolean isTutorSelected) {
        btnTutor.setBackgroundColor(getColor(isTutorSelected ? R.color.green_500 : android.R.color.transparent));
        btnPS.setBackgroundColor(getColor(isTutorSelected ? android.R.color.transparent : R.color.purple_500));
    }

    private void fetchPsApplicationData() {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);
        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_member_own_PS_application_data.php";

        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", memberId)
                .add("role", "PS")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ApplicationHistory.this,
                        "Failed to fetch PS applications", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    processApplicationResponse(response);
                }
            }
        });
    }

    private void fetchTutorApplicationData() {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);
        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_member_own_T_application_data.php";

        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", memberId)
                .add("role", "T")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ApplicationHistory.this,
                        "Failed to fetch tutor applications", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    processApplicationResponse(response);
                }
            }
        });
    }

    private void processApplicationResponse(Response response) throws IOException {
        String jsonResponse = response.body().string();
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            if (jsonObject.getBoolean("success")) {
                JSONArray dataArray = jsonObject.getJSONArray("data");
                runOnUiThread(() -> {
                    applicationsContainer.removeAllViews();
                    for (int i = 0; i < dataArray.length(); i++) {
                        try {
                            addApplicationView(dataArray.getJSONObject(i));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                String message = jsonObject.optString("message", "Unknown error");
                runOnUiThread(() -> Toast.makeText(ApplicationHistory.this,
                        message, Toast.LENGTH_SHORT).show());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(ApplicationHistory.this,
                    "Error processing data", Toast.LENGTH_SHORT).show());
        }
    }

    private void addApplicationView(JSONObject data) throws JSONException {
        View applicationView = LayoutInflater.from(this)
                .inflate(R.layout.history_application_item, applicationsContainer, false);

        String appId = data.optString("app_id", "N/A");
        String studentLevel = data.optString("class_level_name", "N/A");
        String fee = data.optString("feePerHr", "N/A");
        String status = data.optString("status", "N/A");

        // Process arrays
        String subjects = processArrayToString(data.optJSONArray("subject_names"));
        String districts = processArrayToString(data.optJSONArray("district_names"));

        // Set values
        ((TextView) applicationView.findViewById(R.id.app_id)).setText("ID: " + appId);
        ((TextView) applicationView.findViewById(R.id.subject_text)).setText(subjects);
        ((TextView) applicationView.findViewById(R.id.student_level_text)).setText(studentLevel);
        ((TextView) applicationView.findViewById(R.id.fee_text)).setText(String.format("HK$%s/hr", fee));
        ((TextView) applicationView.findViewById(R.id.district_text)).setText(districts);

        // Handle status
        TextView statusTextView = applicationView.findViewById(R.id.status_tv);
        if (status.equals("P")) {
            statusTextView.setText("Pending");
            statusTextView.setBackgroundResource(R.drawable.status_pending_pill);
        } else if (status.equals("A")) {
            statusTextView.setText("Approved");
            statusTextView.setBackgroundResource(R.drawable.status_approved_pill);
        }else if (status.equals("R")){
            statusTextView.setText("Rejected");
            statusTextView.setBackgroundResource(R.drawable.status_rejected_pill);
        }

        applicationsContainer.addView(applicationView);
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
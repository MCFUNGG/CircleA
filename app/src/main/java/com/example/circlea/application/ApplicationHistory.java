package com.example.circlea.application;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

import com.example.circlea.R;

public class ApplicationHistory extends AppCompatActivity {

    private LinearLayout applicationsContainer;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_application); // Ensure this layout file contains the container

        // Initialize LinearLayout container
        applicationsContainer = findViewById(R.id.history_application_container);
        client = new OkHttpClient(); // Initialize OkHttpClient

        // Fetch application data
        fetchApplicationData();
    }

    private void fetchApplicationData() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);

        String url = "http://10.0.2.2/FYP/php/get_application_data.php";

        // Use the retrieved member_id
        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", memberId) // Use the member_id from SharedPreferences
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
                                View applicationView = inflater.inflate(R.layout.application_item, applicationsContainer, false);

                                // Safely retrieve values
                                String role = data.optString("app_creator", "N/A");
                                String subject = data.optString("subject_id", "N/A");
                                String studentLevel = data.optString("class_level_id", "N/A");
                                String fee = data.optString("feePerHr", "N/A");
                                String district = data.optString("district_id", "N/A");
                                String description = data.optString("description", "N/A");

                                // Set values
                                ((TextView) applicationView.findViewById(R.id.role_text)).setText(role);
                                ((TextView) applicationView.findViewById(R.id.subject_text)).setText(subject);
                                ((TextView) applicationView.findViewById(R.id.student_level_text)).setText(studentLevel);
                                ((TextView) applicationView.findViewById(R.id.fee_text)).setText(fee);
                                ((TextView) applicationView.findViewById(R.id.district_text)).setText(district);
                                ((TextView) applicationView.findViewById(R.id.description_text)).setText(description);

                                // Add to the container
                                runOnUiThread(() -> applicationsContainer.addView(applicationView));
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
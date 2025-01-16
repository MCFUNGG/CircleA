package com.example.circlea.setting;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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

public class MemberCart extends AppCompatActivity {

    private LinearLayout applicationsContainer;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.member_cart);

        applicationsContainer = findViewById(R.id.history_application_container);
        client = new OkHttpClient();

        // Fetch application data
        fetchApplicationData();

        Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> finish());
    }

    private void fetchApplicationData() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE);
        Set<String> appIds = sharedPreferences.getStringSet("selected_app_ids", new HashSet<>());
        String url = "http://10.0.2.2/FYP/php/get_member_cart.php";

        if (appIds.isEmpty()) {
            Toast.makeText(this, "No applications saved", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert Set to a comma-separated string to send to PHP
        String appIdString = TextUtils.join(",", appIds);

        RequestBody requestBody = new FormBody.Builder()
                .add("app_ids", appIdString) // Send all application IDs
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("CartFetchApplicationData", "Request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(MemberCart.this, "Failed to fetch data", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    Log.d("CartFetchApplicationData", "Server response: " + jsonResponse);

                    try {
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        if (jsonObject.getBoolean("success")) {
                            JSONArray dataArray = jsonObject.getJSONArray("data");

                            runOnUiThread(() -> applicationsContainer.removeAllViews());

                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject data = dataArray.getJSONObject(i);
                                LayoutInflater inflater = LayoutInflater.from(MemberCart.this);
                                View applicationView = inflater.inflate(R.layout.application_item, applicationsContainer, false);

                                // Safely retrieve values
                                String subject = data.optString("subject_name", "N/A");
                                String studentLevel = data.optString("class_level_name", "N/A");
                                String fee = data.optString("fee_per_hour", "N/A");
                                String district = data.optString("district_name", "N/A");

                                // Set values
                                ((TextView) applicationView.findViewById(R.id.subject_text)).setText(subject);
                                ((TextView) applicationView.findViewById(R.id.student_level_text)).setText(studentLevel);
                                ((TextView) applicationView.findViewById(R.id.fee_text)).setText(fee);
                                ((TextView) applicationView.findViewById(R.id.district_text)).setText(district);

                                runOnUiThread(() -> applicationsContainer.addView(applicationView));
                            }
                        } else {
                            String message = jsonObject.optString("message", "Unknown error");
                            runOnUiThread(() -> Toast.makeText(MemberCart.this, message, Toast.LENGTH_SHORT).show());
                        }
                    } catch (JSONException e) {
                        Log.e("CartFetchApplicationData", "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(MemberCart.this, "Error processing data", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("CartFetchApplicationData", "Request failed, response code: " + response.code());
                    runOnUiThread(() -> Toast.makeText(MemberCart.this, "Failed to fetch application data", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
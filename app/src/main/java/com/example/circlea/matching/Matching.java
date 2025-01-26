    package com.example.circlea.matching;

    import android.content.SharedPreferences;
    import android.os.Bundle;
    import android.util.Log;
    import android.widget.Button;
    import android.widget.Toast;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;
    import com.example.circlea.R;
    import org.json.JSONArray;
    import org.json.JSONException;
    import org.json.JSONObject;
    import java.io.IOException;
    import java.util.ArrayList;
    import java.util.List;
    import okhttp3.*;

    public class Matching extends AppCompatActivity {
        private RecyclerView matchingRequestRecyclerView;
        private MatchingRequestAdapter adapter;
        private List<MatchingRequest> requestList;
        private Button btnRequest, btnCase;
        private String selectedRequestId = null;
        private String memberId = "";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.matching);

            SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
            memberId = sharedPreferences.getString("member_id", null);

            initializeViews();
            setupRecyclerView();
            loadRequestData();
        }

        private void initializeViews() {
            matchingRequestRecyclerView = findViewById(R.id.matchingRequestRecyclerView);
            btnRequest = findViewById(R.id.btnRequest);
            btnCase = findViewById(R.id.btnCase);
            requestList = new ArrayList<>();
        }

        private void setupRecyclerView() {
            adapter = new MatchingRequestAdapter(this, requestList);
            matchingRequestRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            matchingRequestRecyclerView.setAdapter(adapter);

            adapter.setOnItemClickListener((request, position) -> {
                selectedRequestId = request.getRequestId();
                Toast.makeText(this, "Selected request: " + selectedRequestId, Toast.LENGTH_SHORT).show();
            });
        }

        private void loadRequestData() {
            if (memberId == null) {
                Toast.makeText(this, "Error: Missing user information", Toast.LENGTH_SHORT).show();
                return;
            }

            OkHttpClient client = new OkHttpClient();

            RequestBody formBody = new FormBody.Builder()
                    .add("tutor_id", memberId)
                    .build();

            Request request = new Request.Builder()
                    .url("http://10.0.2.2/FYP/php/get_match_request_received.php")
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("Matching", "Network request failed: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(Matching.this,
                                "Failed to load match requests. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    Log.d("Matching", "Server response: " + responseData);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray dataArray = jsonResponse.getJSONArray("data");

                            runOnUiThread(() -> {
                                requestList.clear();
                                for (int i = 0; i < dataArray.length(); i++) {
                                    try {
                                        JSONObject item = dataArray.getJSONObject(i);
                                        JSONObject appDetails = item.getJSONObject("application_details");

                                        // Extract data from JSON
                                        String matchId = item.optString("match_id", "N/A");
                                        String psAppId = item.optString("ps_app_id", "N/A");
                                        String matchMark = item.optString("match_mark", "N/A");
                                        String profileIcon = item.optString("profile_icon", "N/A");
                                        String psUsername = item.optString("ps_username", "N/A");
                                        // Extract application details
                                        String classLevel = appDetails.optString("class_level_name", "N/A");
                                        JSONArray subjectsArray = appDetails.getJSONArray("subject_names");
                                        JSONArray districtsArray = appDetails.getJSONArray("district_names");
                                        String fee = appDetails.optString("feePerHr", "N/A");

                                        // Convert arrays to strings
                                        StringBuilder subjects = new StringBuilder();
                                        for (int j = 0; j < subjectsArray.length(); j++) {
                                            if (j > 0) subjects.append(", ");
                                            subjects.append(subjectsArray.getString(j));
                                        }

                                        StringBuilder districts = new StringBuilder();
                                        for (int j = 0; j < districtsArray.length(); j++) {
                                            if (j > 0) districts.append(", ");
                                            districts.append(districtsArray.getString(j));
                                        }

                                        // Create MatchingRequest object
                                        MatchingRequest matchRequest = new MatchingRequest(
                                                matchId,
                                                psAppId,
                                                psUsername,
                                                fee,
                                                classLevel,
                                                subjects.toString(),
                                                districts.toString(),
                                                matchMark,
                                                profileIcon
                                        );
                                        requestList.add(matchRequest);
                                        Log.e("Matching", "matchRequest: " + matchRequest);
                                    } catch (JSONException e) {
                                        Log.e("Matching", "JSON parsing error: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                                adapter.notifyDataSetChanged();
                            });
                        } else {
                            String message = jsonResponse.optString("message", "No matching requests found");
                            runOnUiThread(() -> {
                                Toast.makeText(Matching.this, message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("Matching", "JSON parsing error: " + e.getMessage());
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(Matching.this,
                                    "Error processing server response",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }

        // Optional: Add method to handle button clicks
        private void setupButtons() {
            btnRequest.setOnClickListener(v -> {
                // Handle request button click
                loadRequestData();
            });

            btnCase.setOnClickListener(v -> {
                // Handle case button click
                // Add your case handling logic here
            });
        }
    }
package com.example.circlea.setting;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.circlea.Login;
import com.example.circlea.R;
import com.example.circlea.application.ParentApplicationFillDetail;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SettingFragment extends Fragment {

    private OkHttpClient client;
    private TextView userEmailTextView;
    private TextView userPhoneTextView,usernameTextView,logOutTextView;
    private Button userOwnDetailbtn, userOwnCartbtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        client = new OkHttpClient();

        // Initialize TextViews
        usernameTextView = view.findViewById(R.id.username);
        userEmailTextView = view.findViewById(R.id.user_email);
        userPhoneTextView = view.findViewById(R.id.user_phone);
        userOwnDetailbtn = view.findViewById(R.id.user_own_detail_button);
        userOwnCartbtn = view.findViewById(R.id.cart_button);
        logOutTextView = view.findViewById(R.id.log_out_tv);

        // Set up button click listener
        userOwnDetailbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MemberDetail.class);
                startActivity(intent);
            }
        });

        userOwnCartbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MemberCart.class);
                startActivity(intent);
            }
        });

        logOutTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear all data from SharedPreferences
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("CircleA", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear(); // This will remove all entries
                editor.apply(); // Apply the changes asynchronously

                // Start the Login activity
                Intent intent = new Intent(getActivity(), Login.class);
                startActivity(intent);
                // Optionally, you may want to finish the current activity
                getActivity().finish();
            }
        });



        // Fetch setting data when the fragment is created
        fetchSettingData();

        return view;
    }

    private void fetchSettingData() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);

        if (memberId == null) {
            Toast.makeText(getActivity(), "Member ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "http://10.0.2.2/FYP/php/get_member_own_profile.php"; // Update with your URL

        // Create the request body
        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", memberId) // Use the member_id from SharedPreferences
                .build();

        // Build the request
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // Send the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchSettingData", "Request failed: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Failed to fetch data", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    Log.d("FetchSettingData", "Server response: " + jsonResponse);

                    try {
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        if (jsonObject.getBoolean("success")) {
                            JSONArray dataArray = jsonObject.getJSONArray("data");

                            // Check if dataArray has at least one entry
                            if (dataArray.length() > 0) {
                                JSONObject data = dataArray.getJSONObject(0); // Get the first object
                                String email = data.optString("email", "N/A"); // Get email
                                String phone = data.optString("phone", "N/A"); // Get phone
                                String username = data.optString("username", "N/A");
                                // Update UI with user data
                                requireActivity().runOnUiThread(() -> {
                                    userEmailTextView.setText(email); // Set email
                                    userPhoneTextView.setText(phone);
                                    usernameTextView.setText(username);// Set phone
                                });

                                // Log the data for debugging
                                Log.d("SettingData", "Email: " + email + ", Phone: " + phone);
                            } else {
                                Log.d("SettingData", "Data array is empty");
                                requireActivity().runOnUiThread(() -> {
                                    userEmailTextView.setText("N/A");
                                    userPhoneTextView.setText("N/A");
                                    usernameTextView.setText("N/A");
                                });
                            }
                        } else {
                            String message = jsonObject.optString("message", "Unknown error");
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (JSONException e) {
                        Log.e("FetchSettingData", "JSON parsing error: " + e.getMessage());
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Error processing data", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    Log.e("FetchSettingData", "Request failed, response code: " + response.code());
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Failed to fetch setting data", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}
package com.example.circlea.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.circlea.Home;
import com.example.circlea.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeFragment extends Fragment {

    private RecyclerView horizontalRecyclerView, verticalRecyclerView, findingTutorsRecyclerView, findingStudentsRecyclerView;
    private OkHttpClient client;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        client = new OkHttpClient();
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        horizontalRecyclerView = view.findViewById(R.id.horizontalRecyclerView);
        verticalRecyclerView = view.findViewById(R.id.verticalRecyclerView);
        findingTutorsRecyclerView = view.findViewById(R.id.findingTutorsRecyclerView);
        findingStudentsRecyclerView = view.findViewById(R.id.findingStudentsRecyclerView);
        ImageButton menuButton = view.findViewById(R.id.menuButton);

        menuButton.setOnClickListener(v -> {
            ((Home) getActivity()).openDrawer(); // Call method from Home activity
        });

        // Set up horizontal RecyclerView
        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        horizontalRecyclerView.setLayoutManager(horizontalLayoutManager);
        ArrayList<String> horizontalData = new ArrayList<>();
        horizontalData.add("1");
        horizontalData.add("2");
        horizontalData.add("3");
        HorizontalAdapter horizontalAdapter = new HorizontalAdapter(horizontalData);
        horizontalRecyclerView.setAdapter(horizontalAdapter);

        // Set up vertical RecyclerView
        LinearLayoutManager verticalLayoutManager = new LinearLayoutManager(getContext());
        verticalRecyclerView.setLayoutManager(verticalLayoutManager);
        ArrayList<String> verticalData = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            verticalData.add("Tutor " + i);
        }
        VerticalAdapter verticalAdapter = new VerticalAdapter(verticalData);
        verticalRecyclerView.setAdapter(verticalAdapter);

        // Set up finding tutors RecyclerView
        LinearLayoutManager findingTutorsLayoutManager = new LinearLayoutManager(getContext());
        findingTutorsRecyclerView.setLayoutManager(findingTutorsLayoutManager);

        // Set up finding students RecyclerView
        LinearLayoutManager findingStudentsLayoutManager = new LinearLayoutManager(getContext());
        findingStudentsRecyclerView.setLayoutManager(findingStudentsLayoutManager);

        // Fetch application data
        fetchTutorsApplicationData();
        fetchStudentsApplicationData();

        return view;
    }

    private void fetchTutorsApplicationData() {
        String url = "http://10.0.2.2/FYP/php/get_T_application_data.php";

        // Create a GET request
        Request request = new Request.Builder()
                .url(url)
                .build();

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchApplicationData", "Request failed: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show());
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

                            // Create a list to hold the application data
                            ArrayList<ApplicationItem> applicationsList = new ArrayList<>();

                            // Iterate through the application array
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject data = dataArray.getJSONObject(i);
                                String appId = data.optString("app_id", "N/A");
                                String memberId = data.optString("member_id", "N/A");
                                String classLevel = data.optString("class_level_name", "N/A");
                                String fee = data.optString("feePerHr", "N/A");

                                // Fetch subjects
                                JSONArray subjectsArray = data.optJSONArray("subject_names");
                                ArrayList<String> subjects = new ArrayList<>();
                                if (subjectsArray != null) {
                                    for (int j = 0; j < subjectsArray.length(); j++) {
                                        subjects.add(subjectsArray.optString(j, "N/A"));
                                    }
                                }

                                // Fetch districts
                                JSONArray districtsArray = data.optJSONArray("district_names");
                                ArrayList<String> districts = new ArrayList<>();
                                if (districtsArray != null) {
                                    for (int k = 0; k < districtsArray.length(); k++) {
                                        districts.add(districtsArray.optString(k, "N/A"));
                                    }
                                }

                                // Fetch profile icon
                                String profileIcon = data.optString("profile_icon", ""); // Fetch the profile icon URL

                                // Create ApplicationItem object and add it to the list
                                applicationsList.add(new ApplicationItem(appId, subjects, classLevel, fee, districts, memberId, profileIcon));
                            }

                            // Update UI on the main thread using FindingStudentsAdapter
                            requireActivity().runOnUiThread(() -> {
                                FindingStudentsAdapter findingStudentsAdapter = new FindingStudentsAdapter(applicationsList, getContext());
                                findingStudentsRecyclerView.setAdapter(findingStudentsAdapter);
                            });
                        } else {
                            String message = jsonObject.optString("message", "Unknown error");
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
                        }
                    } catch (JSONException e) {
                        Log.e("FetchApplicationData", "JSON parsing error: " + e.getMessage());
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error processing data", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("FetchApplicationData", "Request failed, response code: " + response.code());
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to fetch application data", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void fetchStudentsApplicationData() {
        String url = "http://10.0.2.2/FYP/php/get_PS_application_data.php";

        // Create a GET request
        Request request = new Request.Builder()
                .url(url)
                .build();

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchApplicationData", "Request failed: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show());
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

                            // Create a list to hold the application data
                            ArrayList<ApplicationItem> applicationsList = new ArrayList<>();

                            // Iterate through the application array
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject data = dataArray.getJSONObject(i);
                                String appId = data.optString("app_id", "N/A");
                                String memberId = data.optString("member_id", "N/A");
                                String classLevel = data.optString("class_level_name", "N/A");
                                String fee = data.optString("feePerHr", "N/A");
                                String district = data.optString("district_name", "N/A");

                                // Fetch subjects
                                JSONArray subjectsArray = data.optJSONArray("subject_names");
                                ArrayList<String> subjects = new ArrayList<>();
                                if (subjectsArray != null) {
                                    for (int j = 0; j < subjectsArray.length(); j++) {
                                        subjects.add(subjectsArray.optString(j, "N/A"));
                                    }
                                }

                                // Fetch profile icon (if applicable)
                                String profileIcon = data.optString("profile_icon", ""); // Fetch the profile icon URL

                                // Create ApplicationItem object and add it to the list
                                applicationsList.add(new ApplicationItem(appId, subjects, classLevel, fee, new ArrayList<>(List.of(district)), memberId, profileIcon));
                            }

                            // Update UI on the main thread using ApplicationAdapter
                            requireActivity().runOnUiThread(() -> {
                                ApplicationAdapter findingTutorsAdapter = new ApplicationAdapter(applicationsList);
                                findingTutorsRecyclerView.setAdapter(findingTutorsAdapter);
                            });
                        } else {
                            String message = jsonObject.optString("message", "Unknown error");
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
                        }
                    } catch (JSONException e) {
                        Log.e("FetchApplicationData", "JSON parsing error: " + e.getMessage());
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error processing data", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("FetchApplicationData", "Request failed, response code: " + response.code());
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to fetch application data", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
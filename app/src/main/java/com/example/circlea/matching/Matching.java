package com.example.circlea.matching;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
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
import okhttp3.*;
public class Matching extends Fragment {
    private RecyclerView matchingRequestRecyclerView;
    private MatchingRequestReceivedAdapter receivedAdapter;
    private MatchingRequestSentAdapter sentAdapter;
    private List<MatchingRequest> requestList;
    private Button btnRequest, btnCase, btnRequestReceived, btnRequestSent;
    private View requestSubButtonsLayout;
    private String selectedRequestId = null;
    private String memberId = "";
    private View rootView;
    private ImageButton menuButton;
    private boolean isReceivedMode = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.matching, container, false);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("CircleA", Context.MODE_PRIVATE);
        memberId = sharedPreferences.getString("member_id", null);

        initializeViews();
        setupRecyclerView();
        setupButtons();
        setDefaultState();

        menuButton.setOnClickListener(v -> {
            ((Home) getActivity()).openDrawer();
        });

        return rootView;
    }

    private void initializeViews() {
        matchingRequestRecyclerView = rootView.findViewById(R.id.matchingRequestRecyclerView);
        btnRequest = rootView.findViewById(R.id.btnRequest);
        btnCase = rootView.findViewById(R.id.btnCase);
        btnRequestReceived = rootView.findViewById(R.id.btnRequestReceived);
        btnRequestSent = rootView.findViewById(R.id.btnRequestSent);
        requestSubButtonsLayout = rootView.findViewById(R.id.requestSubButtonsLayout);
        menuButton = rootView.findViewById(R.id.menuButton);
        requestList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        receivedAdapter = new MatchingRequestReceivedAdapter(requireContext(), requestList);
        sentAdapter = new MatchingRequestSentAdapter(requireContext(), requestList);
        matchingRequestRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        matchingRequestRecyclerView.setAdapter(receivedAdapter);

        MatchingRequestReceivedAdapter.OnItemClickListener receivedClickListener =
                (request, position) -> {
                    selectedRequestId = request.getRequestId();
                    Toast.makeText(requireContext(), "Selected received request: " + selectedRequestId, Toast.LENGTH_SHORT).show();
                };

        MatchingRequestSentAdapter.OnItemClickListener sentClickListener =
                (request, position) -> {
                    selectedRequestId = request.getRequestId();
                    Toast.makeText(requireContext(), "Selected sent request: " + selectedRequestId, Toast.LENGTH_SHORT).show();
                };

        receivedAdapter.setOnItemClickListener(receivedClickListener);
        sentAdapter.setOnItemClickListener(sentClickListener);
    }

    private void setupButtons() {
        btnRequest.setOnClickListener(v -> {
            requestSubButtonsLayout.setVisibility(View.VISIBLE);
            matchingRequestRecyclerView.setVisibility(View.GONE);
            updateMainButtonStyles(true);
        });

        btnCase.setOnClickListener(v -> {
            requestSubButtonsLayout.setVisibility(View.GONE);
            matchingRequestRecyclerView.setVisibility(View.GONE);
            updateMainButtonStyles(false);
        });

        btnRequestReceived.setOnClickListener(v -> {
            isReceivedMode = true;
            updateSubButtonStyles(true);
            matchingRequestRecyclerView.setVisibility(View.VISIBLE);
            matchingRequestRecyclerView.setAdapter(receivedAdapter);
            loadRequestData(true);
        });

        btnRequestSent.setOnClickListener(v -> {
            isReceivedMode = false;
            updateSubButtonStyles(false);
            matchingRequestRecyclerView.setVisibility(View.VISIBLE);
            matchingRequestRecyclerView.setAdapter(sentAdapter);
            loadRequestData(false);
        });
    }

    private void setDefaultState() {
        requestSubButtonsLayout.setVisibility(View.VISIBLE);
        updateMainButtonStyles(true);
        updateSubButtonStyles(true);
        matchingRequestRecyclerView.setVisibility(View.VISIBLE);
        loadRequestData(true);
    }

    private void updateMainButtonStyles(boolean isRequestSelected) {
        btnRequest.setTextColor(getResources().getColor(
                isRequestSelected ? android.R.color.holo_orange_light : android.R.color.darker_gray));
        btnCase.setTextColor(getResources().getColor(
                isRequestSelected ? android.R.color.darker_gray : android.R.color.holo_orange_light));
    }

    private void updateSubButtonStyles(boolean isReceivedSelected) {
        btnRequestReceived.setTextColor(getResources().getColor(
                isReceivedSelected ? android.R.color.holo_orange_light : android.R.color.darker_gray));
        btnRequestSent.setTextColor(getResources().getColor(
                isReceivedSelected ? android.R.color.darker_gray : android.R.color.holo_orange_light));
    }

    private void loadRequestData(boolean isReceived) {
        if (memberId == null) {
            Toast.makeText(requireContext(), "Error: Missing user information", Toast.LENGTH_SHORT).show();
            return;
        }

        requestList.clear();
        if (isReceived) {
            receivedAdapter.notifyDataSetChanged();
        } else {
            sentAdapter.notifyDataSetChanged();
        }

        OkHttpClient client = new OkHttpClient();

        RequestBody formBody;
        if (isReceived) {
            formBody = new FormBody.Builder()
                    .add("tutor_id", memberId)
                    .build();
        } else {
            formBody = new FormBody.Builder()
                    .add("ps_id", memberId)
                    .build();
        }

        String url = isReceived ?
                "http://10.0.2.2/FYP/php/get_match_request_received.php" :
                "http://10.0.2.2/FYP/php/get_match_request_sent.php";

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Matching", "Network request failed: " + e.getMessage());
                showToast("Failed to load match requests. Please try again.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("Matching", "Server response: " + responseData);

                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    if (jsonResponse.getBoolean("success")) {
                        JSONArray dataArray = jsonResponse.getJSONArray("data");
                        processMatchRequests(dataArray, isReceived);
                    } else {
                        String message = jsonResponse.optString("message", "No matching requests found");
                        showToast(message);
                    }
                } catch (JSONException e) {
                    Log.e("Matching", "JSON parsing error: " + e.getMessage());
                    e.printStackTrace();
                    showToast("Error processing server response");
                }
            }
        });
    }

    private void processMatchRequests(JSONArray dataArray, boolean isReceived) {
        requireActivity().runOnUiThread(() -> {
            requestList.clear();
            for (int i = 0; i < dataArray.length(); i++) {
                try {
                    JSONObject item = dataArray.getJSONObject(i);
                    if (isReceived) {
                        processReceivedRequest(item);
                    } else {
                        processSentRequest(item);
                    }
                } catch (JSONException e) {
                    Log.e("Matching", "JSON parsing error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            if (isReceived) {
                receivedAdapter.notifyDataSetChanged();
            } else {
                sentAdapter.notifyDataSetChanged();
            }
        });
    }

    private void processReceivedRequest(JSONObject item) throws JSONException {
        JSONObject appDetails = item.getJSONObject("application_details");

        String matchId = item.optString("match_id", "N/A");
        String psAppId = item.optString("ps_app_id", "N/A");
        String psUsername = item.optString("ps_username", "N/A");
        String matchMark = item.optString("match_mark", "N/A");
        String profileIcon = item.optString("profile_icon", "N/A");

        String classLevel = appDetails.optString("class_level_name", "N/A");
        String fee = appDetails.optString("feePerHr", "N/A");

        String subjects = processArray(appDetails.getJSONArray("subject_names"));
        String districts = processArray(appDetails.getJSONArray("district_names"));

        MatchingRequest request = new MatchingRequest(
                matchId,
                psAppId,
                psUsername,
                fee,
                classLevel,
                subjects,
                districts,
                matchMark,
                profileIcon
        );
        requestList.add(request);
    }

    private void processSentRequest(JSONObject item) throws JSONException {
        JSONObject appDetails = item.getJSONObject("application_details");

        String matchId = item.optString("match_id", "N/A");
        String psAppId = item.optString("ps_app_id", "N/A");
        String username = item.optString("username", "N/A");
        String matchMark = item.optString("match_mark", "N/A");
        String profileIcon = item.optString("profile_icon", "N/A");

        String classLevel = appDetails.optString("class_level_name", "N/A");
        String fee = appDetails.optString("feePerHr", "N/A");

        String subjects = processArray(appDetails.getJSONArray("subject_names"));
        String districts = processArray(appDetails.getJSONArray("district_names"));

        MatchingRequest request = new MatchingRequest(
                matchId,
                psAppId,
                username,
                fee,
                classLevel,
                subjects,
                districts,
                matchMark,
                profileIcon
        );
        requestList.add(request);
    }

    private String processArray(JSONArray array) throws JSONException {
        if (array == null || array.length() == 0) {
            return "N/A";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            if (i > 0) result.append(", ");
            result.append(array.getString(i));
        }
        return result.toString();
    }

    private void showToast(String message) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }
}
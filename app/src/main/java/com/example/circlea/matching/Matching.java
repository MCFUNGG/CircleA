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
import android.widget.TextView;
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
    private RecyclerView matchingRequestByTutorRecyclerView, matchingRequestByPsRecyclerView;
    private View matchingRequestByTutorLayout, matchingRequestByPsLayout;
    private MatchingRequestReceivedAdapter receivedAdapter, receivedPsAdapter;
    private MatchingRequestSentAdapter sentAdapter, sentPsAdapter;
    private List<MatchingRequest> receivedPSList, receivedTutorList;
    private List<MatchingRequest> sentPSList, sentTutorList;
    private Button btnRequest, btnCase, btnRequestReceived, btnRequestSent;
    private View requestSubButtonsLayout;
    private String selectedRequestId = null;
    private String memberId = "";
    private String username = "";
    private View rootView;
    private ImageButton menuButton;
    private boolean isReceivedMode = true;
    private TextView tvRequestFromPs, tvRequestFromTutor;
    private View dividerPs, dividerTutor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.matching, container, false);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("CircleA", Context.MODE_PRIVATE);
        memberId = sharedPreferences.getString("member_id", null);
        username = sharedPreferences.getString("username", null);

        initializeViews();
        setupRecyclerViews();
        setupButtons();
        setDefaultState();

        menuButton.setOnClickListener(v -> {
            ((Home) getActivity()).openDrawer();
        });

        return rootView;
    }

    private void initializeViews() {
        matchingRequestByTutorRecyclerView = rootView.findViewById(R.id.matchingRequestByTutorRecyclerView);
        matchingRequestByPsRecyclerView = rootView.findViewById(R.id.matchingRequestByPsRecyclerView);
        matchingRequestByTutorLayout = rootView.findViewById(R.id.matchingRequestByTutorLinearLayout);
        matchingRequestByPsLayout = rootView.findViewById(R.id.matchingRequestByPsLinearLayout);
        btnRequest = rootView.findViewById(R.id.btnRequest);
        btnCase = rootView.findViewById(R.id.btnCase);
        btnRequestReceived = rootView.findViewById(R.id.btnRequestReceived);
        btnRequestSent = rootView.findViewById(R.id.btnRequestSent);
        requestSubButtonsLayout = rootView.findViewById(R.id.requestSubButtonsLayout);
        tvRequestFromPs = rootView.findViewById(R.id.tvRequestFromPs);
        tvRequestFromTutor = rootView.findViewById(R.id.tvRequestFromTutor);
        dividerPs = rootView.findViewById(R.id.dividerPs);
        dividerTutor = rootView.findViewById(R.id.dividerT);
        menuButton = rootView.findViewById(R.id.menuButton);

        receivedPSList = new ArrayList<>();
        receivedTutorList = new ArrayList<>();
        sentPSList = new ArrayList<>();
        sentTutorList = new ArrayList<>();
    }

    private void setupRecyclerViews() {
        receivedAdapter = new MatchingRequestReceivedAdapter(requireContext(), receivedPSList, "PS");
        sentAdapter = new MatchingRequestSentAdapter(requireContext(), sentPSList);
        matchingRequestByTutorRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        receivedPsAdapter = new MatchingRequestReceivedAdapter(requireContext(), receivedTutorList, "TUTOR");
        sentPsAdapter = new MatchingRequestSentAdapter(requireContext(), sentTutorList);
        matchingRequestByPsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        setupClickListeners();
        updateAdapters(true);
    }
    private void setupClickListeners() {
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
        receivedPsAdapter.setOnItemClickListener(receivedClickListener);
        sentPsAdapter.setOnItemClickListener(sentClickListener);
    }

    private void updateRequestHeaderTexts(boolean isReceived) {
        if (isReceived) {
            tvRequestFromPs.setText("Request From Parent/Student");
            tvRequestFromTutor.setText("Request From Tutor");
        } else {
            tvRequestFromPs.setText("Request Sent As Parent/Student");
            tvRequestFromTutor.setText("Request Sent As Tutor");
        }
    }

    private void setupButtons() {
        btnRequest.setOnClickListener(v -> {
            requestSubButtonsLayout.setVisibility(View.VISIBLE);
            hideAllRequestLayouts();
            updateMainButtonStyles(true);
        });

        btnCase.setOnClickListener(v -> {
            requestSubButtonsLayout.setVisibility(View.GONE);
            hideAllRequestLayouts();
            updateMainButtonStyles(false);
        });

        btnRequestReceived.setOnClickListener(v -> {
            isReceivedMode = true;
            updateSubButtonStyles(true);
            showRequestLayouts();
            updateAdapters(true);
            updateRequestHeaderTexts(true);
            loadRequestData(true);
        });

        btnRequestSent.setOnClickListener(v -> {
            isReceivedMode = false;
            updateSubButtonStyles(false);
            showRequestLayouts();
            updateAdapters(false);
            updateRequestHeaderTexts(false);
            loadRequestData(false);
        });
    }

    private void hideAllRequestLayouts() {
        matchingRequestByTutorLayout.setVisibility(View.GONE);
        matchingRequestByPsLayout.setVisibility(View.GONE);
        tvRequestFromPs.setVisibility(View.GONE);
        tvRequestFromTutor.setVisibility(View.GONE);
        dividerPs.setVisibility(View.GONE);
        dividerTutor.setVisibility(View.GONE);
    }

    private void showRequestLayouts() {
        matchingRequestByTutorLayout.setVisibility(View.VISIBLE);
        matchingRequestByPsLayout.setVisibility(View.VISIBLE);
        tvRequestFromPs.setVisibility(View.VISIBLE);
        tvRequestFromTutor.setVisibility(View.VISIBLE);
        dividerPs.setVisibility(View.VISIBLE);
        dividerTutor.setVisibility(View.VISIBLE);
    }

    private void updateAdapters(boolean isReceived) {
        if (isReceived) {
            matchingRequestByPsRecyclerView.setAdapter(receivedPsAdapter);    // From PS
            matchingRequestByTutorRecyclerView.setAdapter(receivedAdapter);   // From Tutor
        } else {
            matchingRequestByPsRecyclerView.setAdapter(sentAdapter);     // Sent as Tutor
            matchingRequestByTutorRecyclerView.setAdapter(sentPsAdapter);     // Sent as PS
        }

        // Make sure RecyclerViews are visible
        matchingRequestByPsRecyclerView.setVisibility(View.VISIBLE);
        matchingRequestByTutorRecyclerView.setVisibility(View.VISIBLE);

        // Set layout managers if not already set
        if (matchingRequestByPsRecyclerView.getLayoutManager() == null) {
            matchingRequestByPsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
        if (matchingRequestByTutorRecyclerView.getLayoutManager() == null) {
            matchingRequestByTutorRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
    }

    private void setDefaultState() {
        requestSubButtonsLayout.setVisibility(View.VISIBLE);
        hideAllRequestLayouts();
        updateMainButtonStyles(true);
        updateSubButtonStyles(true);
        updateRequestHeaderTexts(true);
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

        // Get username from SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("CircleA", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");

        // Clear appropriate lists based on mode
        if (isReceived) {
            receivedPSList.clear();
            receivedTutorList.clear();
        } else {
            sentPSList.clear();
            sentTutorList.clear();
        }

        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("member_id", memberId)
                .add("is_received", String.valueOf(isReceived))
                .build();

        // Use different URLs for received and sent requests
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
                        requireActivity().runOnUiThread(() -> hideAllRequestLayouts());
                    }
                } catch (JSONException e) {
                    Log.e("Matching", "JSON parsing error: " + e.getMessage());
                    showToast("Error processing server response");
                }
            }
        });
    }

    private void processMatchRequests(JSONArray dataArray, boolean isReceived) {
        requireActivity().runOnUiThread(() -> {
            try {
                // Clear appropriate lists based on mode
                if (isReceived) {
                    receivedPSList.clear();
                    receivedTutorList.clear();
                    Log.d("Matching", "Cleared received lists");
                } else {
                    sentPSList.clear();
                    sentTutorList.clear();
                    Log.d("Matching", "Cleared sent lists");
                }

                Log.d("Matching", "Total requests received from server: " + dataArray.length());

                for (int i = 0; i < dataArray.length(); i++) {
                    try {
                        JSONObject item = dataArray.getJSONObject(i);
                        String senderRole = item.optString("sender_role", "");
                        String matchId = item.optString("match_id", "");
                        String creator = item.optString("match_creator", "");

                        Log.d("Matching", String.format("Processing request %d: match_id=%s, sender_role=%s, creator=%s",
                                i, matchId, senderRole, creator));

                        MatchingRequest request = processRequest(item, isReceived);

                        if (isReceived) {
                            // For received requests, sort by who created the request
                            if (creator.equals("PS")) {
                                receivedPSList.add(request);
                                Log.d("Matching", "Added to receivedPSList: " + matchId);
                            } else if (creator.equals("T")) {
                                receivedTutorList.add(request);
                                Log.d("Matching", "Added to receivedTutorList: " + matchId);
                            }
                        } else {
                            // For sent requests, sort by sender_role
                            if (senderRole.equals("T")) {
                                sentTutorList.add(request);
                                Log.d("Matching", "Added to sentTutorList: " + matchId);
                            } else if (senderRole.equals("PS")) {
                                sentPSList.add(request);
                                Log.d("Matching", "Added to sentPSList: " + matchId);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("Matching", "Error processing request: " + e.getMessage());
                    }
                }

                Log.d("Matching", String.format("Final list sizes - Received(PS: %d, Tutor: %d), Sent(PS: %d, Tutor: %d)",
                        receivedPSList.size(), receivedTutorList.size(), sentPSList.size(), sentTutorList.size()));

                // Update adapters
                if (isReceived) {
                    receivedAdapter.notifyDataSetChanged();
                    receivedPsAdapter.notifyDataSetChanged();
                    Log.d("Matching", "Updated received adapters");
                } else {
                    sentAdapter.notifyDataSetChanged();
                    sentPsAdapter.notifyDataSetChanged();
                    Log.d("Matching", "Updated sent adapters");
                }

                // Get current lists based on mode
                List<MatchingRequest> currentPSList = isReceived ? receivedPSList : sentPSList;
                List<MatchingRequest> currentTutorList = isReceived ? receivedTutorList : sentTutorList;

                // Update visibility of layouts
                matchingRequestByTutorRecyclerView.setVisibility(currentPSList.isEmpty() ? View.GONE : View.VISIBLE);
                tvRequestFromPs.setVisibility(currentPSList.isEmpty() ? View.GONE : View.VISIBLE);
                dividerPs.setVisibility(currentPSList.isEmpty() ? View.GONE : View.VISIBLE);

                matchingRequestByPsRecyclerView.setVisibility(currentTutorList.isEmpty() ? View.GONE : View.VISIBLE);
                tvRequestFromTutor.setVisibility(currentTutorList.isEmpty() ? View.GONE : View.VISIBLE);
                dividerTutor.setVisibility(currentTutorList.isEmpty() ? View.GONE : View.VISIBLE);

                Log.d("Matching", String.format("Visibility updated - PS Layout: %s, Tutor Layout: %s",
                        currentPSList.isEmpty() ? "GONE" : "VISIBLE",
                        currentTutorList.isEmpty() ? "GONE" : "VISIBLE"));

                // If both lists are empty, hide all layouts
                if (currentPSList.isEmpty() && currentTutorList.isEmpty()) {
                    hideAllRequestLayouts();
                    Log.d("Matching", "Both lists empty, hiding all layouts");
                }

            } catch (Exception e) {
                Log.e("Matching", "Error in processMatchRequests: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    private MatchingRequest processRequest(JSONObject item, boolean isReceived) throws JSONException {
        JSONObject appDetails = item.getJSONObject("application_details");
        String creator = item.optString("match_creator", "");

        String matchId = item.optString("match_id", "N/A");
        String psAppId = item.optString("ps_app_id", "N/A");
        String tutorAppId = item.optString("tutor_app_id", "N/A");
        String psUsername = item.optString("ps_username", "N/A");
        String tutorUsername = item.optString("tutor_username", "N/A");
        String matchMark = item.optString("match_mark", "N/A");
        String profileIcon = item.optString("profile_icon", "N/A");

        String classLevel = appDetails.optString("class_level_name", "N/A");
        String fee = appDetails.optString("feePerHr", "N/A");
        String subjects = processArray(appDetails.getJSONArray("subject_names"));
        String districts = processArray(appDetails.getJSONArray("district_names"));

        // Log the request details for debugging
        Log.d("Matching", String.format("Processing request: ID=%s, Creator=%s, PS=%s, Tutor=%s",
                matchId, creator, psUsername, tutorUsername));

        return new MatchingRequest(
                matchId, psAppId, tutorAppId, psUsername, tutorUsername,
                fee, classLevel, subjects, districts, matchMark,
                profileIcon, creator
        );
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

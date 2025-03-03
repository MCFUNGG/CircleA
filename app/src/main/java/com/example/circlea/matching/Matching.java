package com.example.circlea.matching;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

import com.example.circlea.matching.cases.MatchingCase;
import com.example.circlea.matching.cases.MatchingCaseAdapter;
import com.example.circlea.matching.request.MatchingRequest;
import com.example.circlea.matching.request.MatchingRequestReceivedAdapter;
import com.example.circlea.matching.request.MatchingRequestSentAdapter;
import com.google.android.material.button.MaterialButton;

import com.example.circlea.Home;
import com.example.circlea.IPConfig;
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
    private Button btnRequest, btnCase;
    private MaterialButton btnRequestReceived, btnRequestSent;
    private View requestSubButtonsLayout;
    private String selectedRequestId = null;
    private String memberId = "";
    private String username = "";
    private View rootView;
    private ImageButton menuButton;
    private boolean isReceivedMode = true;
    private TextView tvRequestFromPs, tvRequestFromTutor;
    private View dividerPs, dividerTutor;

    private RecyclerView caseRecyclerView;
    private MatchingCaseAdapter caseAdapter;
    private List<MatchingCase> caseList;
    private View caseSectionLayout;

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


        caseSectionLayout = rootView.findViewById(R.id.caseSectionLayout);
        caseRecyclerView = rootView.findViewById(R.id.caseRecyclerView);
        caseList = new ArrayList<>();
        caseAdapter = new MatchingCaseAdapter(requireContext(), caseList);
        caseRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        caseRecyclerView.setAdapter(caseAdapter);

        receivedPSList = new ArrayList<>();
        receivedTutorList = new ArrayList<>();
        sentPSList = new ArrayList<>();
        sentTutorList = new ArrayList<>();
    }
    private void setupButtons() {
        btnRequest.setOnClickListener(v -> {
            requestSubButtonsLayout.setVisibility(View.VISIBLE);
            hideAllRequestLayouts();
            caseSectionLayout.setVisibility(View.GONE);
            showRequestLayouts();
            updateMainButtonStyles(true);
        });

        btnCase.setOnClickListener(v -> {
            requestSubButtonsLayout.setVisibility(View.GONE);
            hideAllRequestLayouts();
            updateMainButtonStyles(false);
            caseSectionLayout.setVisibility(View.VISIBLE);
            loadCaseData(); // New method to load cases
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

    private void updateMainButtonStyles(boolean isRequestSelected) {
        btnRequest.setTextColor(getResources().getColor(
                isRequestSelected ? R.color.orange : R.color.text_secondary));
        btnCase.setTextColor(getResources().getColor(
                isRequestSelected ? R.color.text_secondary : R.color.orange));
    }

    private void updateSubButtonStyles(boolean isReceivedSelected) {
        if (isReceivedSelected) {
            btnRequestReceived.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.purple_500)));
            btnRequestReceived.setTextColor(Color.WHITE);
            btnRequestSent.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.gray)));
            btnRequestSent.setTextColor(Color.parseColor("#80FFFFFF"));
        } else {
            btnRequestSent.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.purple_500)));
            btnRequestSent.setTextColor(Color.WHITE);
            btnRequestReceived.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.gray)));
            btnRequestReceived.setTextColor(Color.parseColor("#80FFFFFF"));
        }
    }

    private void hideAllRequestLayouts() {
        matchingRequestByTutorLayout.setVisibility(View.GONE);
        matchingRequestByPsLayout.setVisibility(View.GONE);
        tvRequestFromPs.setVisibility(View.GONE);
        tvRequestFromTutor.setVisibility(View.GONE);
        dividerPs.setVisibility(View.GONE);
        dividerTutor.setVisibility(View.GONE);
        caseSectionLayout.setVisibility(View.GONE);
    }

    private void showRequestLayouts() {
        matchingRequestByTutorLayout.setVisibility(View.VISIBLE);
        matchingRequestByPsLayout.setVisibility(View.VISIBLE);
        tvRequestFromPs.setVisibility(View.VISIBLE);
        tvRequestFromTutor.setVisibility(View.VISIBLE);
        dividerPs.setVisibility(View.VISIBLE);
        dividerTutor.setVisibility(View.VISIBLE);
    }

    private void setDefaultState() {
        // Show the request sub buttons layout
        requestSubButtonsLayout.setVisibility(View.VISIBLE);

        // Set Request tab as selected
        updateMainButtonStyles(true);

        // Set Received button as selected and update its style
        isReceivedMode = true;
        updateSubButtonStyles(true);

        // Show the layouts
        showRequestLayouts();

        // Update the header texts for received mode
        updateRequestHeaderTexts(true);

        // Set initial button colors
        btnRequestReceived.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.purple_500)));
        btnRequestReceived.setTextColor(Color.WHITE);
        btnRequestSent.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.gray)));
        btnRequestSent.setTextColor(Color.parseColor("#80FFFFFF"));

        // Load the received data
        loadRequestData(true);
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

        caseAdapter.setOnItemClickListener((matchingCase, position) -> {
            String caseId = matchingCase.getMatchId();
            String status = matchingCase.getStatus();

            if (status.equalsIgnoreCase("P")) {
                Toast.makeText(requireContext(), "Waiting for admin approve", Toast.LENGTH_SHORT).show();
            } else if (status.equalsIgnoreCase("A")) {
                Toast.makeText(requireContext(), "Selected case: " + caseId, Toast.LENGTH_SHORT).show();
                // Add navigation to case details here if needed
            }
        });

        receivedAdapter.setOnItemClickListener(receivedClickListener);
        sentAdapter.setOnItemClickListener(sentClickListener);
        receivedPsAdapter.setOnItemClickListener(receivedClickListener);
        sentPsAdapter.setOnItemClickListener(sentClickListener);
    }

    private void updateAdapters(boolean isReceived) {
        if (isReceived) {
            matchingRequestByPsRecyclerView.setAdapter(receivedPsAdapter);    // From PS
            matchingRequestByTutorRecyclerView.setAdapter(receivedAdapter);   // From Tutor
        } else {
            // Fix: Swap these two lines for sent requests
            matchingRequestByPsRecyclerView.setAdapter(sentPsAdapter);     // Sent as PS
            matchingRequestByTutorRecyclerView.setAdapter(sentAdapter);    // Sent as Tutor
        }

        matchingRequestByPsRecyclerView.setVisibility(View.VISIBLE);
        matchingRequestByTutorRecyclerView.setVisibility(View.VISIBLE);

        if (matchingRequestByPsRecyclerView.getLayoutManager() == null) {
            matchingRequestByPsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
        if (matchingRequestByTutorRecyclerView.getLayoutManager() == null) {
            matchingRequestByTutorRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
    }

    private void loadCaseData() {
        if (memberId == null) {
            Toast.makeText(requireContext(), "Error: Missing user information", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("member_id", memberId)
                .build();

        Request request = new Request.Builder()
                    .url("http://" + IPConfig.getIP() + "/FYP/php/get_match_cases.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Matching", "Failed to load cases: " + e.getMessage());
                showToast("Failed to load cases. Please try again.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    if (jsonResponse.getBoolean("success")) {
                        JSONArray dataArray = jsonResponse.getJSONArray("data");
                        processCases(dataArray);
                    } else {
                        String message = jsonResponse.optString("message", "No cases found");
                        showToast(message);
                    }
                } catch (JSONException e) {
                    Log.e("Matching", "JSON parsing error: " + e.getMessage());
                    showToast("Error processing server response");
                }
            }
        });
    }
    private void processCases(JSONArray dataArray) {
        requireActivity().runOnUiThread(() -> {
            try {
                caseList.clear();
                Log.d("Matching", "Processing " + dataArray.length() + " cases");

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    JSONObject appDetails = item.getJSONObject("application_details");

                    String matchId = item.optString("match_id", "N/A");
                    String psAppId = item.optString("ps_app_id", "N/A");
                    String tutorAppId = item.optString("tutor_app_id", "N/A");
                    String psUsername = item.optString("ps_username", "N/A");
                    String tutorUsername = item.optString("tutor_username", "N/A");
                    String status = item.optString("status", "Pending");
                    String profileIcon = item.optString("profile_icon", "N/A");
                    String matchCreator = item.optString("match_creator", "");

                    String classLevel = appDetails.optString("class_level_name", "N/A");
                    String fee = appDetails.optString("feePerHr", "N/A");
                    String subjects = processArray(appDetails.getJSONArray("subject_names"));
                    String districts = processArray(appDetails.getJSONArray("district_names"));

                    MatchingCase matchingCase = new MatchingCase(
                            matchId, psAppId, tutorAppId, psUsername, tutorUsername,
                            fee, classLevel, subjects, districts, status,
                            profileIcon, matchCreator
                    );

                    caseList.add(matchingCase);
                    Log.d("Matching", "Added case: " + matchId);
                }

                caseAdapter.notifyDataSetChanged();

            } catch (Exception e) {
                Log.e("Matching", "Error processing cases: " + e.getMessage());
                e.printStackTrace();
                showToast("Error processing cases");
            }
        });
    }


    private void loadRequestData(boolean isReceived) {
        if (memberId == null) {
            Toast.makeText(requireContext(), "Error: Missing user information", Toast.LENGTH_SHORT).show();
            return;
        }

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

        String url = isReceived ?
                "http://"+ IPConfig.getIP()+"/FYP/php/get_match_request_received.php" :
                "http://"+IPConfig.getIP()+"/FYP/php/get_match_request_sent.php";

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
                    JSONObject item = dataArray.getJSONObject(i);
                    String senderRole = item.optString("sender_role", "");
                    String matchId = item.optString("match_id", "");
                    String creator = item.optString("match_creator", "");

                    Log.d("Matching", String.format("Processing request %d: match_id=%s, sender_role=%s, creator=%s",
                            i, matchId, senderRole, creator));

                    MatchingRequest request = processRequest(item, isReceived);

                    if (isReceived) {
                        if (creator.equals("PS")) {
                            receivedPSList.add(request);
                            Log.d("Matching", "Added to receivedPSList: " + matchId);
                        } else if (creator.equals("T")) {
                            receivedTutorList.add(request);
                            Log.d("Matching", "Added to receivedTutorList: " + matchId);
                        }
                    } else {
                        if (senderRole.equals("T")) {
                            sentTutorList.add(request);
                            Log.d("Matching", "Added to sentTutorList: " + matchId);
                        } else if (senderRole.equals("PS")) {
                            sentPSList.add(request);
                            Log.d("Matching", "Added to sentPSList: " + matchId);
                        }
                    }
                }

                // Update adapters
                if (isReceived) {
                    receivedAdapter.notifyDataSetChanged();
                    receivedPsAdapter.notifyDataSetChanged();
                } else {
                    sentAdapter.notifyDataSetChanged();
                    sentPsAdapter.notifyDataSetChanged();
                }

                // Get current lists based on mode
                List<MatchingRequest> currentPSList = isReceived ? receivedPSList : sentPSList;
                List<MatchingRequest> currentTutorList = isReceived ? receivedTutorList : sentTutorList;

                // Update visibility
                updateLayoutVisibility(currentPSList, currentTutorList);

            } catch (Exception e) {
                Log.e("Matching", "Error in processMatchRequests: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void updateLayoutVisibility(List<MatchingRequest> psList, List<MatchingRequest> tutorList) {
        matchingRequestByTutorRecyclerView.setVisibility(psList.isEmpty() ? View.GONE : View.VISIBLE);
        tvRequestFromPs.setVisibility(psList.isEmpty() ? View.GONE : View.VISIBLE);
        dividerPs.setVisibility(psList.isEmpty() ? View.GONE : View.VISIBLE);

        matchingRequestByPsRecyclerView.setVisibility(tutorList.isEmpty() ? View.GONE : View.VISIBLE);
        tvRequestFromTutor.setVisibility(tutorList.isEmpty() ? View.GONE : View.VISIBLE);
        dividerTutor.setVisibility(tutorList.isEmpty() ? View.GONE : View.VISIBLE);

        if (psList.isEmpty() && tutorList.isEmpty()) {
            hideAllRequestLayouts();
        }
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
        requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        );
    }
}
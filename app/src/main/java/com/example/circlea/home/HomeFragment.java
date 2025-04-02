    package com.example.circlea.home;

    import static android.content.ContentValues.TAG;
    import static android.content.Context.MODE_PRIVATE;

    import android.content.SharedPreferences;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.ImageButton;
    import android.widget.LinearLayout;
    import android.widget.Toast;

    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.fragment.app.Fragment;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.bumptech.glide.Glide;
    import com.example.circlea.Advertisement;
    import com.example.circlea.Home;
    import com.example.circlea.IPConfig;
    import com.example.circlea.R;

    import org.json.JSONArray;
    import org.json.JSONException;
    import org.json.JSONObject;

    import java.io.IOException;
    import java.util.ArrayList;

    import okhttp3.Call;
    import okhttp3.Callback;
    import okhttp3.FormBody;
    import okhttp3.OkHttpClient;
    import okhttp3.Request;
    import okhttp3.RequestBody;
    import okhttp3.Response;

    public class HomeFragment extends Fragment {

        private RecyclerView horizontalRecyclerView, verticalRecyclerView,
                findingTutorsRecyclerView, findingStudentsRecyclerView;
        private OkHttpClient client;
        private LinearLayout highRatedSection, tutorApplicationSection, studentApplicationSection;
        private Button btnHighRated, btnTutorApp, btnStudentApp;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            if (!isAdded()) {
                return null;
            }

            client = new OkHttpClient();
            View view = inflater.inflate(R.layout.fragment_home, container, false);

            initializeViews(view);
            setupRecyclerViews();
            setupButtons();

            ImageButton menuButton = view.findViewById(R.id.menuButton);
            menuButton.setOnClickListener(v -> {
                if (isAdded() && getActivity() instanceof Home) {
                    ((Home) getActivity()).openDrawer();
                }
            });

            // Set up horizontal RecyclerView data
            ArrayList<String> horizontalData = new ArrayList<>();
            horizontalData.add("1");
            horizontalData.add("2");
            horizontalData.add("3");
            HorizontalAdapter horizontalAdapter = new HorizontalAdapter(horizontalData);
            horizontalRecyclerView.setAdapter(horizontalAdapter);
            getAdsData();

            // Set up vertical RecyclerView data
            ArrayList<String> verticalData = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                verticalData.add("Tutor " + i);
            }
            VerticalAdapter verticalAdapter = new VerticalAdapter(verticalData);
            verticalRecyclerView.setAdapter(verticalAdapter);

            // Fetch application data
            fetchTutorsApplicationData();
            fetchStudentsApplicationData();

            // Show high rated section by default
            showSection(highRatedSection);
            btnHighRated.setSelected(true);

            return view;
        }

        private void initializeViews(View view) {
            horizontalRecyclerView = view.findViewById(R.id.horizontalRecyclerView);
            verticalRecyclerView = view.findViewById(R.id.verticalRecyclerView);
            findingTutorsRecyclerView = view.findViewById(R.id.findingTutorsRecyclerView);
            findingStudentsRecyclerView = view.findViewById(R.id.findingStudentsRecyclerView);

            highRatedSection = view.findViewById(R.id.highRatedSection);
            tutorApplicationSection = view.findViewById(R.id.tutorApplicationSection);
            studentApplicationSection = view.findViewById(R.id.studentApplicationSection);

            btnHighRated = view.findViewById(R.id.btnHighRated);
            btnTutorApp = view.findViewById(R.id.btnTutorApp);
            btnStudentApp = view.findViewById(R.id.btnStudentApp);
        }

        private void setupRecyclerViews() {
            if (!isAdded()) return;

            LinearLayoutManager horizontalLayout = new LinearLayoutManager(requireContext(),
                    LinearLayoutManager.HORIZONTAL, false);
            LinearLayoutManager verticalLayout = new LinearLayoutManager(requireContext());
            LinearLayoutManager tutorsLayout = new LinearLayoutManager(requireContext());
            LinearLayoutManager studentsLayout = new LinearLayoutManager(requireContext());

            horizontalRecyclerView.setLayoutManager(horizontalLayout);
            verticalRecyclerView.setLayoutManager(verticalLayout);
            findingTutorsRecyclerView.setLayoutManager(tutorsLayout);
            findingStudentsRecyclerView.setLayoutManager(studentsLayout);

            horizontalRecyclerView.setNestedScrollingEnabled(false);
            verticalRecyclerView.setNestedScrollingEnabled(false);
            findingTutorsRecyclerView.setNestedScrollingEnabled(false);
            findingStudentsRecyclerView.setNestedScrollingEnabled(false);

            horizontalRecyclerView.setHasFixedSize(true);
            verticalRecyclerView.setHasFixedSize(true);
            findingTutorsRecyclerView.setHasFixedSize(true);
            findingStudentsRecyclerView.setHasFixedSize(true);
        }

        private void setupButtons() {
            View.OnClickListener buttonClickListener = v -> {
                if (!isAdded()) return;

                Button clickedButton = (Button) v;
                updateButtonStates(clickedButton);

                if (v.getId() == R.id.btnHighRated) {
                    showSection(highRatedSection);
                } else if (v.getId() == R.id.btnTutorApp) {
                    showSection(tutorApplicationSection);
                } else if (v.getId() == R.id.btnStudentApp) {
                    showSection(studentApplicationSection);
                }
            };

            btnHighRated.setOnClickListener(buttonClickListener);
            btnTutorApp.setOnClickListener(buttonClickListener);
            btnStudentApp.setOnClickListener(buttonClickListener);

            updateButtonStates(btnHighRated);
        }

        private void updateButtonStates(Button selectedButton) {
            if (!isAdded()) return;

            btnHighRated.setBackgroundResource(android.R.color.transparent);
            btnHighRated.setTextColor(requireContext().getResources().getColor(R.color.text_secondary));
            btnTutorApp.setBackgroundResource(android.R.color.transparent);
            btnTutorApp.setTextColor(requireContext().getResources().getColor(R.color.text_secondary));
            btnStudentApp.setBackgroundResource(android.R.color.transparent);
            btnStudentApp.setTextColor(requireContext().getResources().getColor(R.color.text_secondary));

            selectedButton.setBackgroundResource(R.drawable.selected_tab_background);
            selectedButton.setTextColor(requireContext().getResources().getColor(R.color.orange));
        }

        private void getAdsData() {
            String url = "http://" + IPConfig.getIP() + "/FYP/php/get_ads_data.php";

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("FetchAdsData", "Request failed: " + e.getMessage());
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Failed to fetch advertisements", Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String jsonResponse = response.body().string();
                        Log.d("FetchAdsData", "Server response: " + jsonResponse);

                        try {
                            JSONObject jsonObject = new JSONObject(jsonResponse);
                            if (jsonObject.getBoolean("success")) {
                                JSONArray dataArray = jsonObject.getJSONArray("data");
                                ArrayList<Advertisement> advertisements = new ArrayList<>();

                                for (int i = 0; i < dataArray.length(); i++) {
                                    JSONObject adObject = dataArray.getJSONObject(i);
                                    String id = adObject.getString("ad_id");
                                    String imageUrl = "http://" + IPConfig.getIP() + adObject.getString("image_url");
                                    advertisements.add(new Advertisement(id, imageUrl));
                                }

                                requireActivity().runOnUiThread(() -> {
                                    // Get the existing adapter and update it
                                    HorizontalAdapter adapter = (HorizontalAdapter) horizontalRecyclerView.getAdapter();
                                    if (adapter != null) {
                                        adapter.setAdvertisements(advertisements);
                                    } else {
                                        adapter = new HorizontalAdapter(new ArrayList<>());
                                        adapter.setAdvertisements(advertisements);
                                        horizontalRecyclerView.setAdapter(adapter);
                                    }
                                });

                            } else {
                                String errorMessage = jsonObject.optString("message", "Unknown error occurred");
                                Log.e("FetchAdsData", "API Error: " + errorMessage);
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show()
                                );
                            }
                        } catch (JSONException e) {
                            Log.e("FetchAdsData", "JSON parsing error: " + e.getMessage());
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getActivity(), "Error parsing advertisement data", Toast.LENGTH_SHORT).show()
                            );
                        }
                    } else {
                        Log.e("FetchAdsData", "Server Error: " + response.code());
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Server error: " + response.code(), Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            });
        }
        private void showSection(LinearLayout sectionToShow) {
            if (!isAdded()) return;

            highRatedSection.setVisibility(View.GONE);
            tutorApplicationSection.setVisibility(View.GONE);
            studentApplicationSection.setVisibility(View.GONE);
            sectionToShow.setVisibility(View.VISIBLE);
        }
        private void fetchTutorsApplicationData() {
            if (!isAdded()) {
                Log.e("HomeFragment", "Fragment not attached to activity");
                return;
            }

            try {
                SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("CircleA", MODE_PRIVATE);
                String memberId = sharedPreferences.getString("member_id", null);
                String url = "http://" + IPConfig.getIP() + "/FYP/php/get_T_application_data.php";

                RequestBody requestBody = new FormBody.Builder()
                        .add("member_id", memberId)
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e("FetchApplicationData", "Request failed: " + e.getMessage());
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), getString(R.string.failed_to_fetch_data), Toast.LENGTH_SHORT).show()
                            );
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!isAdded()) return;

                        if (response.isSuccessful()) {
                            String jsonResponse = response.body().string();
                            Log.d("FetchApplicationData", "Server response: " + jsonResponse);

                            try {
                                JSONObject jsonObject = new JSONObject(jsonResponse);
                                if (jsonObject.getBoolean("success")) {
                                    JSONArray dataArray = jsonObject.getJSONArray("data");
                                    ArrayList<ApplicationItem> applicationsList = new ArrayList<>();

                                    for (int i = 0; i < dataArray.length(); i++) {
                                        JSONObject data = dataArray.getJSONObject(i);
                                        String appId = data.optString("app_id", "N/A");
                                        String memberId = data.optString("member_id", "N/A");
                                        String classLevel = data.optString("class_level_name", "N/A");
                                        String fee = data.optString("feePerHr", "N/A");
                                        String username = data.optString("username", "N/A");
                                        String profileIcon = data.optString("profile_icon", "");
                                        String education = data.optString("education", "");
                                        Log.d("EDUCATION_DEBUG", "Education raw JSON data for app_id " +
                                                appId + ": [" + data.optString("education", "NULL_VALUE") + "]");

                                        // Check if education is null or empty
                                        if (education == null || education.isEmpty()) {
                                            Log.w("EDUCATION_DEBUG", "Empty education for app_id: " + appId);
                                            education = "No education information available";
                                        }

                                        // Verify education value before creating ApplicationItem
                                        Log.d("EDUCATION_DEBUG", "Final education value: [" + education + "]");


                                        // Handle subjects
                                        JSONArray subjectsArray = data.optJSONArray("subject_names");
                                        ArrayList<String> subjects = new ArrayList<>();
                                        if (subjectsArray != null) {
                                            for (int j = 0; j < subjectsArray.length(); j++) {
                                                subjects.add(subjectsArray.optString(j, "N/A"));
                                            }
                                        }

                                        // Handle districts
                                        JSONArray districtsArray = data.optJSONArray("district_names");
                                        ArrayList<String> districts = new ArrayList<>();
                                        if (districtsArray != null) {
                                            for (int k = 0; k < districtsArray.length(); k++) {
                                                districts.add(districtsArray.optString(k, "N/A"));
                                            }
                                        }


                                        Log.d("Test123","education data  in homefragment.java : "+education);
                                        applicationsList.add(new ApplicationItem(
                                                appId, subjects, classLevel, fee, districts,
                                                memberId, profileIcon, username, "student", education));
                                    }

                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(() -> {
                                            if (isAdded()) {
                                                FindingStudentsAdapter findingStudentsAdapter =
                                                        new FindingStudentsAdapter(applicationsList, requireContext());
                                                findingStudentsRecyclerView.setAdapter(findingStudentsAdapter);
                                            }
                                        });
                                    }
                                } else {
                                    String message = jsonObject.optString("message", getString(R.string.unknown_error));
                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e("FetchApplicationData", "JSON parsing error: " + e.getMessage());
                                if (isAdded()) {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), getString(R.string.error_processing_data), Toast.LENGTH_SHORT).show()
                                    );
                                }
                            }
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("HomeFragment", "Error in fetchTutorsApplicationData: " + e.getMessage());
            }
        }

        private void fetchStudentsApplicationData() {
            if (!isAdded()) {
                Log.e("HomeFragment", "Fragment not attached to activity");
                return;
            }

            try {
                SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("CircleA", MODE_PRIVATE);
                String memberId = sharedPreferences.getString("member_id", null);
                String url = "http://" + IPConfig.getIP() + "/FYP/php/get_PS_application_data.php";

                RequestBody requestBody = new FormBody.Builder()
                        .add("member_id", memberId)
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e("FetchApplicationData", "Request failed: " + e.getMessage());
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), getString(R.string.failed_to_fetch_data), Toast.LENGTH_SHORT).show()
                            );
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!isAdded()) return;

                        if (response.isSuccessful()) {
                            String jsonResponse = response.body().string();
                            Log.d("FetchApplicationData", "Server response: " + jsonResponse);

                            try {
                                JSONObject jsonObject = new JSONObject(jsonResponse);
                                if (jsonObject.getBoolean("success")) {
                                    JSONArray dataArray = jsonObject.getJSONArray("data");
                                    ArrayList<ApplicationItem> applicationsList = new ArrayList<>();

                                    for (int i = 0; i < dataArray.length(); i++) {
                                        JSONObject data = dataArray.getJSONObject(i);
                                        String appId = data.optString("app_id", "N/A");
                                        String memberId = data.optString("member_id", "N/A");
                                        String classLevel = data.optString("class_level_name", "N/A");
                                        String fee = data.optString("feePerHr", "N/A");
                                        String username = data.optString("username", "N/A");

                                        JSONArray subjectsArray = data.optJSONArray("subject_names");
                                        ArrayList<String> subjects = new ArrayList<>();
                                        if (subjectsArray != null) {
                                            for (int j = 0; j < subjectsArray.length(); j++) {
                                                subjects.add(subjectsArray.optString(j, "N/A"));
                                            }
                                        }

                                        JSONArray districtsArray = data.optJSONArray("district_names");
                                        ArrayList<String> districts = new ArrayList<>();
                                        if (districtsArray != null) {
                                            for (int k = 0; k < districtsArray.length(); k++) {
                                                districts.add(districtsArray.optString(k, "N/A"));
                                            }
                                        }

                                        String profileIcon = data.optString("profile_icon", "");
                                        String education = data.optString("education", "");
                                        applicationsList.add(new ApplicationItem(
                                                appId, subjects, classLevel, fee, districts,
                                                memberId, profileIcon, username, "tutor", education));
                                    }

                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(() -> {
                                            if (isAdded()) {
                                                ApplicationAdapter findingTutorsAdapter =
                                                        new ApplicationAdapter(applicationsList, requireContext());
                                                findingTutorsRecyclerView.setAdapter(findingTutorsAdapter);
                                            }
                                        });
                                    }
                                } else {
                                    String message = jsonObject.optString("message", getString(R.string.unknown_error));
                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e("FetchApplicationData", "JSON parsing error: " + e.getMessage());
                                if (isAdded()) {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), getString(R.string.error_processing_data), Toast.LENGTH_SHORT).show()
                                    );
                                }
                            }
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("HomeFragment", "Error in fetchStudentsApplicationData: " + e.getMessage());
            }
        }
    }
package com.example.circlea.home;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.bumptech.glide.Glide;
import com.example.circlea.Home;
import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.example.circlea.home.TutorProfileActivity;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {

    private RecyclerView horizontalRecyclerView, verticalRecyclerView,
            findingTutorsRecyclerView, findingStudentsRecyclerView,
            searchResultsRecyclerView;
    private OkHttpClient client;
    private LinearLayout highRatedSection, tutorApplicationSection, studentApplicationSection,
            searchResultsContainer, mainContentContainer;
    private Button btnHighRated, btnTutorApp, btnStudentApp, closeSearchResultsButton;
    private View tutorFilterLayout, studentFilterLayout;
    private FilterHelper tutorFilterHelper, studentFilterHelper;
    private ArrayList<ApplicationItem> originalTutorApplications, originalStudentApplications, 
            searchResults;
    private SearchResultsAdapter searchResultsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (!isAdded()) {
            return null;
        }
        //-----------TESTING----------------
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d("FCM", "Token: " + token);
                        // 顯示或保存這個 token
                    }
                });
        //----------------------------------
        client = new OkHttpClient();
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(view);
        setupRecyclerViews();
        setupButtons();
        setupSearchBar(view);

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

        // 初始化過濾器佈局
        tutorFilterLayout = view.findViewById(R.id.tutor_filter);
        studentFilterLayout = view.findViewById(R.id.student_filter);
        
        // 初始化過濾器助手
        tutorFilterHelper = new FilterHelper(tutorFilterLayout, getContext());
        studentFilterHelper = new FilterHelper(studentFilterLayout, getContext());
        
        // 設置過濾器監聽器
        setupFilterListeners();

        // Fetch application data first
        fetchHighRatedTutors();
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
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView);

        highRatedSection = view.findViewById(R.id.highRatedSection);
        tutorApplicationSection = view.findViewById(R.id.tutorApplicationSection);
        studentApplicationSection = view.findViewById(R.id.studentApplicationSection);
        searchResultsContainer = view.findViewById(R.id.searchResultsContainer);
        mainContentContainer = view.findViewById(R.id.mainContentContainer);

        btnHighRated = view.findViewById(R.id.btnHighRated);
        btnTutorApp = view.findViewById(R.id.btnTutorApp);
        btnStudentApp = view.findViewById(R.id.btnStudentApp);
        closeSearchResultsButton = view.findViewById(R.id.closeSearchResultsButton);
        
        // 初始化列表數據
        originalTutorApplications = new ArrayList<>();
        originalStudentApplications = new ArrayList<>();
        searchResults = new ArrayList<>();
        
        // 设置关闭搜索结果按钮的点击事件
        closeSearchResultsButton.setOnClickListener(v -> {
            searchResultsContainer.setVisibility(View.GONE);
            mainContentContainer.setVisibility(View.VISIBLE);
        });
    }

    private void setupRecyclerViews() {
        if (!isAdded()) return;

        // 设置水平列表
        LinearLayoutManager horizontalLayout = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false);
        horizontalRecyclerView.setLayoutManager(horizontalLayout);
        horizontalRecyclerView.setNestedScrollingEnabled(false);

        // 设置垂直列表（高评分导师）
        LinearLayoutManager verticalLayout = new LinearLayoutManager(requireContext());
        verticalRecyclerView.setLayoutManager(verticalLayout);
        verticalRecyclerView.setNestedScrollingEnabled(false);
        verticalRecyclerView.setHasFixedSize(true);
        // 添加分割线
        verticalRecyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        // 设置其他列表
        LinearLayoutManager tutorsLayout = new LinearLayoutManager(requireContext());
        LinearLayoutManager studentsLayout = new LinearLayoutManager(requireContext());
        LinearLayoutManager searchResultsLayout = new LinearLayoutManager(requireContext());

        findingTutorsRecyclerView.setLayoutManager(tutorsLayout);
        findingStudentsRecyclerView.setLayoutManager(studentsLayout);
        searchResultsRecyclerView.setLayoutManager(searchResultsLayout);

        findingTutorsRecyclerView.setNestedScrollingEnabled(false);
        findingStudentsRecyclerView.setNestedScrollingEnabled(false);
        searchResultsRecyclerView.setNestedScrollingEnabled(false);
        
        // 初始化搜索结果适配器
        searchResultsAdapter = new SearchResultsAdapter(searchResults, requireContext());
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);
        // 为搜索结果添加分隔线
        searchResultsRecyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
    }

    private void setupButtons() {
        View.OnClickListener buttonClickListener = v -> {
            if (!isAdded()) return;

            Button clickedButton = (Button) v;
            updateButtonStates(clickedButton);

            if (v.getId() == R.id.btnHighRated) {
                showSection(highRatedSection);
                // 高评分导师标签不显示过滤器
                tutorFilterLayout.setVisibility(View.GONE);
                studentFilterLayout.setVisibility(View.GONE);
            } else if (v.getId() == R.id.btnTutorApp) {
                showSection(tutorApplicationSection);
                // 显示导师应用过滤器
                tutorFilterLayout.setVisibility(View.VISIBLE);
                studentFilterLayout.setVisibility(View.GONE);
            } else if (v.getId() == R.id.btnStudentApp) {
                showSection(studentApplicationSection);
                // 显示学生应用过滤器
                studentFilterLayout.setVisibility(View.VISIBLE);
                tutorFilterLayout.setVisibility(View.GONE);
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
                                
                                // 确保正确读取link_url字段
                                String linkUrl = null;
                                if (!adObject.isNull("link_url")) {
                                    linkUrl = adObject.getString("link_url");
                                    if (linkUrl.equalsIgnoreCase("null")) {
                                        linkUrl = null;
                                    }
                                }
                                
                                Log.d("FetchAdsData", "广告ID: " + id + ", 链接URL: " + linkUrl);
                                
                                // 根据链接是否为null决定使用哪个构造函数
                                if (linkUrl == null || linkUrl.isEmpty()) {
                                    Log.d("FetchAdsData", "添加无链接广告");
                                    advertisements.add(new Advertisement(id, imageUrl));
                                } else {
                                    Log.d("FetchAdsData", "添加带链接广告: " + linkUrl);
                                    advertisements.add(new Advertisement(id, imageUrl, linkUrl));
                                }
                            }

                            requireActivity().runOnUiThread(() -> {
                                // Get the existing adapter and update it
                                HorizontalAdapter adapter = (HorizontalAdapter) horizontalRecyclerView.getAdapter();
                                
                                // 添加调试信息，检查所有广告链接
                                Log.d("FetchAdsData", "广告加载完成，总数: " + advertisements.size());
                                for (Advertisement ad : advertisements) {
                                    Log.d("FetchAdsData", "广告ID: " + ad.getId() + 
                                            ", 图片: " + ad.getImageUrl() + 
                                            ", 链接: " + ad.getLinkUrl());
                                }
                                
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
                                            // 更新原始數據列表
                                            originalTutorApplications.clear();
                                            originalTutorApplications.addAll(applicationsList);
                                            
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
                                            // 更新原始數據列表
                                            originalStudentApplications.clear();
                                            originalStudentApplications.addAll(applicationsList);
                                            
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

    private void fetchHighRatedTutors() {
        if (!isAdded()) {
            Log.e("HomeFragment", "Fragment not attached to activity");
            return;
        }

        try {
            String url = "http://" + IPConfig.getIP() + "/FYP/php/get_tutor_applications.php";

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("FetchHighRatedTutors", "Request failed: " + e.getMessage());
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> Toast
                                .makeText(requireContext(), "Failed to fetch high rated tutors", Toast.LENGTH_SHORT)
                                .show());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!isAdded())
                        return;

                    if (response.isSuccessful()) {
                        String jsonResponse = response.body().string();
                        Log.d("FetchHighRatedTutors", "Server response: " + jsonResponse);

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
                                    String rating = data.optString("rating", "0.0");

                                    JSONArray subjectsArray = data.optJSONArray("subjects");
                                    ArrayList<String> subjects = new ArrayList<>();
                                    if (subjectsArray != null) {
                                        for (int j = 0; j < subjectsArray.length(); j++) {
                                            subjects.add(subjectsArray.optString(j, "N/A"));
                                        }
                                    }

                                    JSONArray districtsArray = data.optJSONArray("districts");
                                    ArrayList<String> districts = new ArrayList<>();
                                    if (districtsArray != null) {
                                        for (int k = 0; k < districtsArray.length(); k++) {
                                            districts.add(districtsArray.optString(k, "N/A"));
                                        }
                                    }

                                    String profileIcon = data.optString("profile", "");

                                    ApplicationItem item = new ApplicationItem(
                                            appId, subjects, classLevel, fee, districts,
                                            memberId, profileIcon, username, "tutor", rating);
                                    applicationsList.add(item);
                                }

                                // Sort by rating in descending order
                                Collections.sort(applicationsList, (a, b) -> {
                                    double ratingA = Double.parseDouble(a.getRating());
                                    double ratingB = Double.parseDouble(b.getRating());
                                    return Double.compare(ratingB, ratingA);
                                });

                                // Convert sorted ApplicationItems to strings
                                ArrayList<String> tutorStrings = new ArrayList<>();
                                for (ApplicationItem item : applicationsList) {
                                    tutorStrings.add(item.getUsername()); // Just use username for now
                                }

                                if (isAdded()) {
                                    requireActivity().runOnUiThread(() -> {
                                        if (isAdded()) {
                                            VerticalAdapter verticalAdapter = new VerticalAdapter(
                                                    applicationsList, requireContext());
                                            
                                            // 為高評分導師添加點擊事件處理
                                            verticalAdapter.setOnItemClickListener(new VerticalAdapter.OnItemClickListener() {
                                                @Override
                                                public void onItemClick(int position, ApplicationItem item) {
                                                    // 打開導師個人資料頁面
                                                    Intent intent = new Intent(requireContext(), TutorProfileActivity.class);
                                                    intent.putExtra("tutor_id", item.getMemberId());
                                                    intent.putExtra("tutorName", item.getUsername());
                                                    startActivity(intent);
                                                }
                                            });
                                            
                                            verticalRecyclerView.setAdapter(verticalAdapter);
                                        }
                                    });
                                }
                            } else {
                                String message = jsonObject.optString("message", "Unknown error");
                                if (isAdded()) {
                                    requireActivity().runOnUiThread(
                                            () -> Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
                                }
                            }
                        } catch (JSONException e) {
                            Log.e("FetchHighRatedTutors", "JSON parsing error: " + e.getMessage());
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> Toast
                                        .makeText(requireContext(), "Error processing data", Toast.LENGTH_SHORT)
                                        .show());
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e("HomeFragment", "Error in fetchHighRatedTutors: " + e.getMessage());
        }
    }

    // 添加临时测试数据方法
    private void showTestData() {
        Log.d("HomeFragment", "Showing test data");
        ArrayList<ApplicationItem> testData = new ArrayList<>();
        ArrayList<String> subjects = new ArrayList<>();
        subjects.add("Math");
        subjects.add("English");
        
        ArrayList<String> districts = new ArrayList<>();
        districts.add("Central");
        districts.add("North");

        for (int i = 1; i <= 5; i++) {
            ApplicationItem tutor = new ApplicationItem(
                String.valueOf(i),
                subjects,
                "Level " + i,
                "100",
                districts,
                String.valueOf(i),
                "",
                "Tutor " + i,
                "tutor",
                "4." + i,
                "Bachelor's Degree"
            );
            tutor.setRating("4." + i);
            testData.add(tutor);
            Log.d("HomeFragment", "Added test tutor " + i + " with rating " + "4." + i);
        }

        if (isAdded()) {
            Log.d("HomeFragment", "Setting adapter with " + testData.size() + " test tutors");
            requireActivity().runOnUiThread(() -> {
                VerticalAdapter adapter = new VerticalAdapter(testData, requireContext());
                verticalRecyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                Log.d("HomeFragment", "Adapter set and notified");
            });
        }
    }

    /**
     * 設置過濾器監聽器
     */
    private void setupFilterListeners() {
        // 設置導師過濾器監聽器
        tutorFilterHelper.setFilterListener(new FilterHelper.FilterListener() {
            @Override
            public void onApplyFilter(FilterHelper.FilterCriteria filterCriteria) {
                ArrayList<ApplicationItem> filteredItems = filterCriteria.filter(originalTutorApplications);
                FindingStudentsAdapter adapter = (FindingStudentsAdapter) findingStudentsRecyclerView.getAdapter();
                if (adapter != null) {
                    adapter.updateData(filteredItems);
                }
                
                // 顯示過濾結果提示
                String message = getString(R.string.filter_result_message, filteredItems.size());
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResetFilter() {
                FindingStudentsAdapter adapter = (FindingStudentsAdapter) findingStudentsRecyclerView.getAdapter();
                if (adapter != null) {
                    adapter.updateData(originalTutorApplications);
                }
                Toast.makeText(getContext(), R.string.filter_reset, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 設置學生過濾器監聽器
        studentFilterHelper.setFilterListener(new FilterHelper.FilterListener() {
            @Override
            public void onApplyFilter(FilterHelper.FilterCriteria filterCriteria) {
                ArrayList<ApplicationItem> filteredItems = filterCriteria.filter(originalStudentApplications);
                ApplicationAdapter adapter = (ApplicationAdapter) findingTutorsRecyclerView.getAdapter();
                if (adapter != null) {
                    adapter.updateData(filteredItems);
                }
                
                // 顯示過濾結果提示
                String message = getString(R.string.filter_result_message, filteredItems.size());
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResetFilter() {
                ApplicationAdapter adapter = (ApplicationAdapter) findingTutorsRecyclerView.getAdapter();
                if (adapter != null) {
                    adapter.updateData(originalStudentApplications);
                }
                Toast.makeText(getContext(), R.string.filter_reset, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 初始化過濾器數據
        initializeFilterData();
    }
    
    /**
     * 初始化過濾器數據，包括年級、科目和地區
     */
    private void initializeFilterData() {
        // 從數據庫獲取年級、科目和地區數據
        OkHttpClient client = new OkHttpClient();
        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_filter_data.php";
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "獲取過濾數據失敗: " + e.getMessage());
                // 失敗時使用默認靜態數據
                loadDefaultFilterData();
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        
                        // 處理年級數據
                        JSONArray levelsArray = jsonObject.getJSONArray("levels");
                        List<String> classLevels = new ArrayList<>();
                        for (int i = 0; i < levelsArray.length(); i++) {
                            JSONObject levelObj = levelsArray.getJSONObject(i);
                            classLevels.add(levelObj.getString("class_level_name"));
                        }
                        
                        // 處理科目數據
                        JSONArray subjectsArray = jsonObject.getJSONArray("subjects");
                        List<String> subjects = new ArrayList<>();
                        for (int i = 0; i < subjectsArray.length(); i++) {
                            JSONObject subjectObj = subjectsArray.getJSONObject(i);
                            subjects.add(subjectObj.getString("subject_name"));
                        }
                        
                        // 處理地區數據
                        JSONArray districtsArray = jsonObject.getJSONArray("districts");
                        List<String> districts = new ArrayList<>();
                        for (int i = 0; i < districtsArray.length(); i++) {
                            JSONObject districtObj = districtsArray.getJSONObject(i);
                            districts.add(districtObj.getString("district_name"));
                        }
                        
                        // 在UI線程更新過濾器
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                tutorFilterHelper.setClassLevelItems(classLevels);
                                studentFilterHelper.setClassLevelItems(classLevels);
                                
                                tutorFilterHelper.setSubjectItems(subjects);
                                studentFilterHelper.setSubjectItems(subjects);
                                
                                tutorFilterHelper.setDistrictItems(districts);
                                studentFilterHelper.setDistrictItems(districts);
                                
                                Log.d(TAG, "過濾器數據初始化完成（從數據庫）");
                            });
                        }
                        
                    } catch (JSONException e) {
                        Log.e(TAG, "解析過濾數據JSON失敗: " + e.getMessage());
                        // JSON解析失敗時使用默認靜態數據
                        loadDefaultFilterData();
                    }
                } else {
                    Log.e(TAG, "獲取過濾數據失敗，狀態碼: " + response.code());
                    // 請求失敗時使用默認靜態數據
                    loadDefaultFilterData();
                }
            }
        });
    }
    
    /**
     * 加載默認的靜態過濾器數據（作為後備）
     */
    private void loadDefaultFilterData() {
        if (!isAdded()) return;
        
        requireActivity().runOnUiThread(() -> {
            // 初始化年級數據（靜態數據）
            List<String> classLevels = new ArrayList<>();
            classLevels.add("Kindergarten - K.1");
            classLevels.add("Kindergarten - K.2");
            classLevels.add("Kindergarten - K.3");
            classLevels.add("Primary school - P.1");
            classLevels.add("Primary school - P.2");
            classLevels.add("Primary school - P.3");
            classLevels.add("Primary school - P.4");
            classLevels.add("Primary school - P.5");
            classLevels.add("Primary school - P.6");
            classLevels.add("Secondary School - F.1");
            classLevels.add("Secondary School - F.2");
            classLevels.add("Secondary School - F.3");
            classLevels.add("Secondary School - F.4");
            classLevels.add("Secondary School - F.5");
            classLevels.add("Secondary School - F.6");
            classLevels.add("University - College freshman");
            tutorFilterHelper.setClassLevelItems(classLevels);
            studentFilterHelper.setClassLevelItems(classLevels);
            
            // 初始化科目數據（靜態數據）
            List<String> subjects = new ArrayList<>();
            subjects.add("Chinese Language");
            subjects.add("English Language");
            subjects.add("Mathematics");
            subjects.add("Physics");
            subjects.add("Chemistry");
            subjects.add("Biology");
            subjects.add("History");
            subjects.add("Geography");
            subjects.add("Economics");
            subjects.add("Music");
            subjects.add("Visual Arts");
            subjects.add("Physical Education");
            tutorFilterHelper.setSubjectItems(subjects);
            studentFilterHelper.setSubjectItems(subjects);
            
            // 初始化地區數據（靜態數據）
            List<String> districts = new ArrayList<>();
            districts.add("Central and Western");
            districts.add("Eastern");
            districts.add("Southern");
            districts.add("Wan Chai");
            districts.add("Kowloon City");
            districts.add("Yau Tsim Mong");
            districts.add("Sham Shui Po");
            districts.add("Wong Tai Sin");
            districts.add("Kwun Tong");
            districts.add("Tai Po");
            districts.add("Yuen Long");
            districts.add("Tuen Mun");
            districts.add("North");
            districts.add("Sai Kung");
            districts.add("Sha Tin");
            districts.add("Tsuen Wan");
            districts.add("Kwai Tsing");
            districts.add("Islands");
            tutorFilterHelper.setDistrictItems(districts);
            studentFilterHelper.setDistrictItems(districts);
            
            Log.d(TAG, "過濾器數據初始化完成（使用默認數據）");
        });
    }

    // 添加搜索栏设置方法
    private void setupSearchBar(View view) {
        EditText searchBar = view.findViewById(R.id.searchBar);
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchBar.getText().toString());
                return true;
            }
            return false;
        });

        // 添加文本变化监听器以实现实时搜索
        searchBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 不需要实现
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 不需要实现
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // 实时搜索，延迟300ms执行以避免频繁搜索
                new android.os.Handler().removeCallbacksAndMessages(null);
                new android.os.Handler().postDelayed(() -> performSearch(s.toString()), 300);
            }
        });
    }

    // 执行搜索
    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            // 如果搜索查询为空，隐藏搜索结果区域，显示主内容
            searchResultsContainer.setVisibility(View.GONE);
            mainContentContainer.setVisibility(View.VISIBLE);
            return;
        }
        
        query = query.toLowerCase().trim();
        // 清空之前的搜索结果
        searchResults.clear();
        
        // 搜索所有导师应用
        searchInList(originalTutorApplications, query, searchResults);
        
        // 搜索所有学生应用
        searchInList(originalStudentApplications, query, searchResults);
        
        // 更新搜索结果适配器
        searchResultsAdapter.notifyDataSetChanged();
        
        // 显示搜索结果区域，隐藏主内容
        if (searchResults.isEmpty()) {
            // 如果没有搜索结果，显示提示
            Toast.makeText(requireContext(), "没有找到匹配的结果", Toast.LENGTH_SHORT).show();
            searchResultsContainer.setVisibility(View.GONE);
            mainContentContainer.setVisibility(View.VISIBLE);
        } else {
            // 显示搜索结果区域
            TextView searchResultsTitle = requireView().findViewById(R.id.searchResultsTitle);
            searchResultsTitle.setText("搜索结果 (" + searchResults.size() + ")");
            searchResultsContainer.setVisibility(View.VISIBLE);
            mainContentContainer.setVisibility(View.GONE);
        }
    }
    
    // 在指定列表中搜索关键词
    private void searchInList(ArrayList<ApplicationItem> sourceList, String query, ArrayList<ApplicationItem> resultList) {
        if (sourceList == null || sourceList.isEmpty()) {
            return;
        }
        
        for (ApplicationItem item : sourceList) {
            // 搜索用户名
            if (item.getUsername().toLowerCase().contains(query)) {
                resultList.add(item);
                continue;
            }
            
            // 搜索科目
            boolean foundInSubjects = false;
            for (String subject : item.getSubjects()) {
                if (subject.toLowerCase().contains(query)) {
                    resultList.add(item);
                    foundInSubjects = true;
                    break;
                }
            }
            if (foundInSubjects) continue;
            
            // 搜索地区
            boolean foundInDistricts = false;
            for (String district : item.getDistricts()) {
                if (district.toLowerCase().contains(query)) {
                    resultList.add(item);
                    foundInDistricts = true;
                    break;
                }
            }
            if (foundInDistricts) continue;
            
            // 搜索教育程度
            if (item.getClassLevel().toLowerCase().contains(query)) {
                resultList.add(item);
                continue;
            }
            
            // 搜索教育背景
            if (item.getEducation() != null && item.getEducation().toLowerCase().contains(query)) {
                resultList.add(item);
            }
        }
    }

    /**
     * 多布局搜索结果适配器，可以同时显示导师应用和学生应用
     */
    private class SearchResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_TUTOR = 1;  // 导师应用类型
        private static final int TYPE_STUDENT = 2; // 学生应用类型
        
        private ArrayList<ApplicationItem> data;
        private Context context;
        
        public SearchResultsAdapter(ArrayList<ApplicationItem> data, Context context) {
            this.data = data;
            this.context = context;
        }
        
        @Override
        public int getItemViewType(int position) {
            ApplicationItem item = data.get(position);
            // 根据应用类型返回不同的视图类型
            if ("tutor".equals(item.getApplicationType())) {
                return TYPE_TUTOR;
            } else {
                return TYPE_STUDENT;
            }
        }
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_TUTOR) {
                // 导师应用使用item_finding_tutors布局
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_finding_tutors, parent, false);
                return new TutorViewHolder(view);
            } else {
                // 学生应用使用item_finding_students布局
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_finding_students, parent, false);
                return new StudentViewHolder(view);
            }
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ApplicationItem item = data.get(position);
            
            if (holder instanceof TutorViewHolder) {
                bindTutorViewHolder((TutorViewHolder) holder, item);
            } else if (holder instanceof StudentViewHolder) {
                bindStudentViewHolder((StudentViewHolder) holder, item);
            }
        }
        
        // 绑定导师应用视图数据
        private void bindTutorViewHolder(TutorViewHolder holder, ApplicationItem item) {
            // 加载个人头像
            String profileUrl = item.getProfileIcon();
            if (profileUrl != null && !profileUrl.isEmpty()) {
                String fullProfileUrl = "http://" + IPConfig.getIP() + profileUrl;
                Glide.with(context)
                        .load(fullProfileUrl)
                        .placeholder(R.drawable.circle_background)
                        .into(holder.profileIcon);
            } else {
                holder.profileIcon.setImageResource(R.drawable.circle_background);
            }

            holder.username.setText(item.getUsername());
            holder.classLevelTextView.setText(item.getClassLevel());
            
            // 设置科目
            StringBuilder subjects = new StringBuilder();
            for (String subject : item.getSubjects()) {
                subjects.append(subject).append(", ");
            }
            if (subjects.length() > 2) {
                subjects.setLength(subjects.length() - 2);
            }
            holder.subjectTextView.setText(subjects.toString());

            // 设置地区
            StringBuilder districts = new StringBuilder();
            for (String district : item.getDistricts()) {
                districts.append(district).append(", ");
            }
            if (districts.length() > 2) {
                districts.setLength(districts.length() - 2);
            }
            holder.districtTextView.setText(districts.toString());

            holder.feeTextView.setText("$" + item.getFee());
            
            // 设置点击事件
            holder.layout.setOnClickListener(v -> {
                Intent intent = new Intent(context, PSAppDetail.class);
                intent.putExtra("ps_id", item.getMemberId());
                intent.putStringArrayListExtra("subjects", item.getSubjects());
                intent.putExtra("classLevel", item.getClassLevel());
                intent.putExtra("fee", item.getFee());
                intent.putStringArrayListExtra("districts", item.getDistricts());
                intent.putExtra("psAppId", item.getAppId());
                context.startActivity(intent);
            });
        }
        
        // 绑定学生应用视图数据
        private void bindStudentViewHolder(StudentViewHolder holder, ApplicationItem item) {
            // 加载个人头像
            String profileUrl = item.getProfileIcon();
            if (profileUrl != null && !profileUrl.isEmpty()) {
                String fullProfileUrl = "http://" + IPConfig.getIP() + profileUrl;
                Glide.with(context)
                        .load(fullProfileUrl)
                        .placeholder(R.drawable.circle_background)
                        .into(holder.profileIcon);
            } else {
                holder.profileIcon.setImageResource(R.drawable.circle_background);
            }

            holder.username.setText(item.getUsername());
            holder.classLevelTextView.setText("aims: " + item.getClassLevel());
            
            // 设置科目
            StringBuilder subjects = new StringBuilder();
            for (String subject : item.getSubjects()) {
                subjects.append(subject).append(", ");
            }
            if (subjects.length() > 2) {
                subjects.setLength(subjects.length() - 2);
            }
            holder.subjectTextView.setText(subjects.toString());

            // 设置地区
            StringBuilder districts = new StringBuilder();
            for (String district : item.getDistricts()) {
                districts.append(district).append(", ");
            }
            if (districts.length() > 2) {
                districts.setLength(districts.length() - 2);
            }
            holder.districtTextView.setText(districts.toString());

            holder.feeTextView.setText("$" + item.getFee());
            
            // 设置点击事件
            holder.layout.setOnClickListener(v -> {
                Intent intent = new Intent(context, TutorAppDetail.class);
                intent.putExtra("tutor_id", item.getMemberId());
                intent.putStringArrayListExtra("subjects", item.getSubjects());
                intent.putExtra("classLevel", item.getClassLevel());
                intent.putExtra("fee", item.getFee());
                intent.putStringArrayListExtra("districts", item.getDistricts());
                intent.putExtra("tutotAppId", item.getAppId());
                String education = item.getEducation();
                if (education == null) education = "";
                intent.putExtra("education", education);
                context.startActivity(intent);
            });
        }
        
        @Override
        public int getItemCount() {
            return data.size();
        }
        
        // 导师应用视图持有者
        class TutorViewHolder extends RecyclerView.ViewHolder {
            TextView classLevelTextView, subjectTextView, districtTextView, feeTextView, username;
            ImageView profileIcon;
            LinearLayout layout;
            
            TutorViewHolder(@NonNull View itemView) {
                super(itemView);
                classLevelTextView = itemView.findViewById(R.id.classlevel_tv);
                subjectTextView = itemView.findViewById(R.id.subject_tv);
                districtTextView = itemView.findViewById(R.id.district_tv);
                feeTextView = itemView.findViewById(R.id.fee_tv);
                layout = itemView.findViewById(R.id.item_layout);
                profileIcon = itemView.findViewById(R.id.tutor_icon);
                username = itemView.findViewById(R.id.username);
            }
        }
        
        // 学生应用视图持有者
        class StudentViewHolder extends RecyclerView.ViewHolder {
            TextView classLevelTextView, subjectTextView, districtTextView, feeTextView, username;
            ImageView profileIcon;
            LinearLayout layout;
            
            StudentViewHolder(@NonNull View itemView) {
                super(itemView);
                classLevelTextView = itemView.findViewById(R.id.classlevel_tv);
                subjectTextView = itemView.findViewById(R.id.subject_tv);
                districtTextView = itemView.findViewById(R.id.district_tv);
                feeTextView = itemView.findViewById(R.id.fee_tv);
                layout = itemView.findViewById(R.id.item_layout);
                profileIcon = itemView.findViewById(R.id.tutor_icon);
                username = itemView.findViewById(R.id.username);
            }
        }
    }
}
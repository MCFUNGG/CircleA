    package com.example.circlea.matching.cases.detail.book;

    import android.app.DatePickerDialog;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.content.res.ColorStateList;
    import android.graphics.Color;
    import android.os.Bundle;
    import android.os.Handler;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.EditText;
    import android.widget.NumberPicker;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.annotation.Nullable;
    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.content.ContextCompat;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;
    import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

    import com.example.circlea.IPConfig;
    import com.example.circlea.R;
    import com.example.circlea.matching.cases.detail.TimeSlot;
    import com.example.circlea.matching.cases.detail.TimeSlotAdapter;
    import com.example.circlea.matching.cases.detail.TutorVideoIntroduction;
    import com.google.android.material.appbar.MaterialToolbar;
    import com.google.android.material.button.MaterialButton;
    import com.google.android.material.chip.Chip;
    import com.google.android.material.dialog.MaterialAlertDialogBuilder;
    import com.google.android.material.divider.MaterialDivider;
    import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
    import com.google.android.material.textview.MaterialTextView;

    import org.json.JSONArray;
    import org.json.JSONObject;

    import java.io.IOException;
    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Calendar;
    import java.util.List;
    import java.util.Locale;

    import okhttp3.Call;
    import okhttp3.Callback;
    import okhttp3.FormBody;
    import okhttp3.OkHttpClient;
    import okhttp3.Request;
    import okhttp3.RequestBody;
    import okhttp3.Response;

    import com.example.circlea.data.model.StudentContactResponse;
    import com.google.gson.Gson;

    public class TutorBooking extends AppCompatActivity implements BookingRequestAdapter.OnBookingActionListener  {

        private RecyclerView timeSlotsRecyclerView;
        private TimeSlotAdapter timeSlotAdapter;
        private List<TimeSlot> timeSlots;
        private Calendar selectedDate;
        private String caseId;
        private String tutorId;
        private RecyclerView bookingRequestsRecyclerView;
        private BookingRequestAdapter bookingRequestAdapter;
        private List<BookingRequest> bookingRequests;
        private View bookingDetailCard;
        private MaterialTextView bookingStudentName;
        private MaterialTextView bookingDateTime;
        private Chip bookingStatus;
        private View bookingRequestsCard;
        private TextView availableTimeSlotsTitle;
        private MaterialToolbar topAppBar;
        private SwipeRefreshLayout swipeRefreshLayout;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.booking_tutor);
            Log.d("CurrentJava", "TutorBooking");
            // Get tutor ID from SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("CircleA", Context.MODE_PRIVATE);
            tutorId = sharedPreferences.getString("member_id", "");

            // Get case ID from intent
            caseId = getIntent().getStringExtra("case_id");

            initializeViews();
            setupSwipeRefresh();
            getStudentBookingRequest();
        }

        private void initializeViews() {
            topAppBar = findViewById(R.id.topAppBar);
            setSupportActionBar(topAppBar);
            
            // Enable back button and set its appearance
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            
            // Setup back button click listener
            topAppBar.setNavigationOnClickListener(v -> onBackPressed());

            MaterialButton selectDateButton = findViewById(R.id.select_date_button);
            ExtendedFloatingActionButton saveSlotsButton = findViewById(R.id.save_slots_button);
            timeSlotsRecyclerView = findViewById(R.id.time_slots_recycler_view);
            bookingRequestsRecyclerView = findViewById(R.id.booking_requests_recycler_view);
            bookingRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            bookingRequests = new ArrayList<>();
            bookingRequestAdapter = new BookingRequestAdapter(bookingRequests, this);
            bookingRequestsRecyclerView.setAdapter(bookingRequestAdapter);

            bookingDetailCard = findViewById(R.id.booking_detail_card);
            bookingStudentName = findViewById(R.id.booking_student_name);
            bookingDateTime = findViewById(R.id.booking_date_time);
            bookingStatus = findViewById(R.id.booking_status);
            bookingRequestsCard = findViewById(R.id.booking_requests_card);
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

            // Setup RecyclerView
            timeSlotsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            timeSlots = new ArrayList<>();
            timeSlotAdapter = new TimeSlotAdapter(timeSlots, this, true);
            timeSlotsRecyclerView.setAdapter(timeSlotAdapter);

            availableTimeSlotsTitle = findViewById(R.id.available_time_slots_title);
            selectedDate = Calendar.getInstance();

            // Setup click listeners
            selectDateButton.setOnClickListener(v -> showDatePicker());
            saveSlotsButton.setOnClickListener(v -> saveAvailableSlots());
            MaterialButton updateLessonStatusButton = findViewById(R.id.update_lesson_status_button);
            updateLessonStatusButton.setOnClickListener(v -> showUpdateLessonStatusDialog());

            MaterialButton interviewButton = findViewById(R.id.interview_button);
            interviewButton.setOnClickListener(v -> startInterviewActivity());

            MaterialButton viewStudentContactButton = findViewById(R.id.view_student_contact_button);
            viewStudentContactButton.setOnClickListener(v -> getStudentContact());
        }
        
        private void setupSwipeRefresh() {
            swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark
            );
            
            swipeRefreshLayout.setOnRefreshListener(this::refreshContent);
        }
        
        private void refreshContent() {
            Log.d("TutorBooking", "Refreshing content...");
            
            // 清空現有數據
            timeSlots.clear();
            bookingRequests.clear();
            
            // 重新加載數據
            getStudentBookingRequest();
            loadExistingTimeSlots();
            
            // 延遲一點時間後停止刷新動畫
            new Handler().postDelayed(() -> {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }, 1000);
        }

        private void getStudentBookingRequest() {
            OkHttpClient client = new OkHttpClient();

            Log.d("Request", "Loading booking requests for match_id: " + caseId);

            // Add null checks for caseId and tutorId
            FormBody.Builder formBodyBuilder = new FormBody.Builder();
            
            if (caseId != null && !caseId.isEmpty()) {
                formBodyBuilder.add("match_id", caseId);
            } else {
                Log.e("TutorBooking", "match_id is null or empty");
                Toast.makeText(this, getString(R.string.invalid_case_id), Toast.LENGTH_SHORT).show();
                finish(); // Close activity if caseId is invalid
                return;
            }
            
            if (tutorId != null && !tutorId.isEmpty()) {
                formBodyBuilder.add("tutor_id", tutorId);
            } else {
                Log.e("TutorBooking", "tutor_id is null or empty");
                Toast.makeText(this, getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
                finish(); // Close activity if tutorId is invalid
                return;
            }

            // 首先檢查是否有衝突狀態
            checkForLessonStatusConflict();

            RequestBody formBody = formBodyBuilder.build();

            String url = "http://" + IPConfig.getIP() + "/FYP/php/get_booking_requests.php";
            Log.d("Request", "URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("Network", "Failed to load booking requests: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                            getString(R.string.failed_to_load_booking_requests), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    Log.d("Response", "Booking requests: " + responseData);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray requestsArray = jsonResponse.getJSONArray("requests");
                            processBookingRequests(requestsArray);
                        } else {
                            String message = jsonResponse.getString("message");
                            Log.w("Response", "Server error: " + message);
                            runOnUiThread(() -> {
                                Toast.makeText(TutorBooking.this, message, Toast.LENGTH_SHORT).show();
                                findViewById(R.id.no_requests_text).setVisibility(View.VISIBLE);
                            });
                        }
                    } catch (Exception e) {
                        Log.e("Parse", "Failed to parse booking requests: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                                getString(R.string.error_loading_booking_requests), Toast.LENGTH_SHORT).show());
                    }
                }
            });
        }

        // 添加新方法：檢查課程狀態是否有衝突
        private void checkForLessonStatusConflict() {
            OkHttpClient client = new OkHttpClient();

            Log.d("TutorBooking", "Checking for lesson status conflict for match_id: " + caseId + ", tutor_id: " + tutorId);

            RequestBody formBody = new FormBody.Builder()
                    .add("match_id", caseId)
                    .add("tutor_id", tutorId)
                    .build();

            String url = "http://" + IPConfig.getIP() + "/FYP/php/check_booking_status.php";
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("TutorBooking", "Failed to check lesson status: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    Log.d("TutorBooking", "Lesson status check response: " + responseData);
                    
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        if (jsonResponse.getBoolean("success") && jsonResponse.getBoolean("has_booking")) {
                            JSONObject booking = jsonResponse.getJSONObject("booking");
                            String status = booking.getString("status");
                            
                            if ("conflict".equals(status)) {
                                // 如果有衝突狀態，更新UI顯示衝突信息，隱藏時間槽
                                runOnUiThread(() -> {
                                    // 顯示預約詳情，隱藏時間槽視圖
                                    topAppBar.setTitle(getString(R.string.booking_detail));

                                    // 隱藏"可用時間槽"標題和部分
                                    availableTimeSlotsTitle.setVisibility(View.GONE);
                                    findViewById(R.id.add_slot_card).setVisibility(View.GONE);
                                    timeSlotsRecyclerView.setVisibility(View.GONE);
                                    findViewById(R.id.save_slots_button).setVisibility(View.GONE);

                                    // 顯示衝突信息對話框
                                    new AlertDialog.Builder(TutorBooking.this)
                                            .setTitle(getString(R.string.status_conflict))
                                            .setMessage(getString(R.string.waiting_admin_process))
                                            .setPositiveButton(getString(R.string.ok), null)
                                            .show();
                                });
                                
                                // 獲取並顯示預約詳情
                                fetchConfirmedBooking();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("TutorBooking", "Error checking lesson status: " + e.getMessage());
                    }
                }
            });
        }

        // 添加新方法：直接獲取確認狀態的預約
        private void fetchConfirmedBooking() {
            OkHttpClient client = new OkHttpClient();

            Log.d("TutorBooking", "Getting confirmed booking for match_id: " + caseId + ", tutor_id: " + tutorId);

            // 檢查必要參數
            if (caseId == null || caseId.isEmpty() || tutorId == null || tutorId.isEmpty()) {
                Log.e("TutorBooking", "Invalid case_id or tutor_id");
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.missing_required_information), Toast.LENGTH_SHORT).show());
                return;
            }

            RequestBody formBody = new FormBody.Builder()
                    .add("match_id", caseId)
                    .add("tutor_id", tutorId)
                    .build();

            String url = "http://" + IPConfig.getIP() + "/FYP/php/get_booking_requests.php";
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("TutorBooking", "Failed to get confirmed booking: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    Log.d("TutorBooking", "Confirmed booking response: " + responseData);
                    
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray requests = jsonResponse.getJSONArray("requests");
                            
                            for (int i = 0; i < requests.length(); i++) {
                                JSONObject request = requests.getJSONObject(i);
                                String status = request.getString("status");
                                
                                if ("confirmed".equals(status) || "conflict".equals(status)) {
                                    // 找到確認或衝突狀態的預約，在UI中顯示
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                    
                                    Calendar startTime = Calendar.getInstance();
                                    startTime.setTime(sdf.parse(request.getString("start_time")));
                                    
                                    Calendar endTime = Calendar.getInstance();
                                    endTime.setTime(sdf.parse(request.getString("end_time")));
                                    
                                    final BookingRequest bookingRequest = new BookingRequest(
                                            request.getString("booking_id"),
                                            request.getString("student_id"),
                                            request.getString("student_name"),
                                            startTime,
                                            endTime,
                                            status
                                    );
                                    
                                    runOnUiThread(() -> {
                                        showBookingDetail(bookingRequest);
                                        bookingDetailCard.setVisibility(View.VISIBLE);
                                        findViewById(R.id.view_student_contact_button).setVisibility(View.VISIBLE);
                                        findViewById(R.id.update_lesson_status_button).setVisibility(View.VISIBLE);
                                        findViewById(R.id.interview_button).setVisibility(View.VISIBLE);
                                        
                                        // 隱藏預約請求部分
                                        findViewById(R.id.booking_requests_title).setVisibility(View.GONE);
                                        bookingRequestsCard.setVisibility(View.GONE);
                                        findViewById(R.id.booking_requests_divider).setVisibility(View.GONE);
                                    });
                                    
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("TutorBooking", "Error processing confirmed booking: " + e.getMessage());
                    }
                }
            });
        }

        private void processBookingRequests(JSONArray requestsArray) {
            runOnUiThread(() -> {
                try {
                    bookingRequests.clear();
                    boolean hasConfirmedBooking = false;
                    boolean hasConflictBooking = false;
                    boolean hasCompletedBooking = false;
                    BookingRequest confirmedRequest = null;
                    BookingRequest conflictRequest = null;
                    BookingRequest completedRequest = null;

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                    for (int i = 0; i < requestsArray.length(); i++) {
                        JSONObject requestObject = requestsArray.getJSONObject(i);
                        String status = requestObject.getString("status");
                        
                        Log.d("TutorBooking", "Processing booking: status = " + status);

                        // Parse request data
                        Calendar startTime = Calendar.getInstance();
                        startTime.setTime(sdf.parse(requestObject.getString("start_time")));

                        Calendar endTime = Calendar.getInstance();
                        endTime.setTime(sdf.parse(requestObject.getString("end_time")));

                        BookingRequest request = new BookingRequest(
                                requestObject.getString("booking_id"),
                                requestObject.getString("student_id"),
                                requestObject.getString("student_name"),
                                startTime,
                                endTime,
                                status
                        );
                        
                        if (!hasConfirmedBooking && !hasConflictBooking && !hasCompletedBooking) {
                            loadExistingTimeSlots();
                        }
                        
                        if (status.equals("confirmed")) {
                            hasConfirmedBooking = true;
                            confirmedRequest = request;
                        } else if (status.equals("conflict")) {
                            hasConflictBooking = true;
                            conflictRequest = request;
                        } else if (status.equals("completed")) {
                            hasCompletedBooking = true;
                            completedRequest = request;
                            Log.d("TutorBooking", "Found completed booking: " + request.getRequestId());
                        } else if (status.equals("pending")) {
                            bookingRequests.add(request);
                        }
                    }

                    // 優先處理衝突狀態
                    if (hasConflictBooking && conflictRequest != null) {
                        // 顯示衝突狀態的預約詳情
                        showBookingDetail(conflictRequest);
                        
                        // 設置工具欄標題
                        topAppBar.setTitle(getString(R.string.booking_detail));

                        // 隱藏"可用時間槽"標題和部分
                        availableTimeSlotsTitle.setVisibility(View.GONE);
                        findViewById(R.id.add_slot_card).setVisibility(View.GONE);
                        timeSlotsRecyclerView.setVisibility(View.GONE);
                        findViewById(R.id.save_slots_button).setVisibility(View.GONE);

                        // 隱藏預約請求部分
                        findViewById(R.id.booking_requests_title).setVisibility(View.GONE);
                        bookingRequestsCard.setVisibility(View.GONE);

                        // 顯示預約詳情部分
                        bookingDetailCard.setVisibility(View.VISIBLE);
                        
                        // 衝突狀態下隱藏更新課程狀態按鈕
                        findViewById(R.id.update_lesson_status_button).setVisibility(View.GONE);
                        findViewById(R.id.interview_button).setVisibility(View.GONE);
                        findViewById(R.id.view_student_contact_button).setVisibility(View.VISIBLE);

                        // 隱藏分隔線
                        findViewById(R.id.booking_requests_divider).setVisibility(View.GONE);
                        
                        // 顯示衝突狀態對話框
                        new AlertDialog.Builder(this)
                                .setTitle(getString(R.string.status_conflict))
                                .setMessage(getString(R.string.waiting_admin_process))
                                .setPositiveButton(getString(R.string.ok), null)
                                .show();
                    }
                    // 處理已完成狀態
                    else if (hasCompletedBooking && completedRequest != null) {
                        Log.d("TutorBooking", "Showing completed status UI");
                        
                        // 顯示已完成的預約詳情
                        showBookingDetail(completedRequest);
                        
                        // 設置工具欄標題
                        topAppBar.setTitle(getString(R.string.booking_detail));

                        // 隱藏"可用時間槽"標題和部分
                        availableTimeSlotsTitle.setVisibility(View.GONE);
                        findViewById(R.id.add_slot_card).setVisibility(View.GONE);
                        timeSlotsRecyclerView.setVisibility(View.GONE);
                        findViewById(R.id.save_slots_button).setVisibility(View.GONE);

                        // 隱藏預約請求部分
                        findViewById(R.id.booking_requests_title).setVisibility(View.GONE);
                        bookingRequestsCard.setVisibility(View.GONE);

                        // 顯示預約詳情部分
                        bookingDetailCard.setVisibility(View.VISIBLE);
                        
                        // 完成狀態下隱藏更新課程狀態按鈕
                        findViewById(R.id.update_lesson_status_button).setVisibility(View.GONE);
                        findViewById(R.id.interview_button).setVisibility(View.GONE);
                        findViewById(R.id.view_student_contact_button).setVisibility(View.VISIBLE);

                        // 隱藏分隔線
                        findViewById(R.id.booking_requests_divider).setVisibility(View.GONE);
                        
                        // 顯示完成狀態對話框
                        new AlertDialog.Builder(this)
                                .setTitle(getString(R.string.lesson_completed))
                                .setMessage(getString(R.string.lesson_completed_message))
                                .setPositiveButton(getString(R.string.ok), null)
                                .show();
                    }
                    // 如果沒有衝突或已完成狀態，再處理確認狀態
                    else if (hasConfirmedBooking && confirmedRequest != null) {
                        // 顯示預約詳情並隱藏其他內容
                        showBookingDetail(confirmedRequest);

                        // 設置工具欄標題
                        topAppBar.setTitle(getString(R.string.booking_detail));

                        // 隱藏"可用時間槽"標題和部分
                        availableTimeSlotsTitle.setVisibility(View.GONE);
                        findViewById(R.id.add_slot_card).setVisibility(View.GONE);
                        timeSlotsRecyclerView.setVisibility(View.GONE);

                        // 隱藏預約請求部分
                        findViewById(R.id.booking_requests_title).setVisibility(View.GONE);
                        bookingRequestsCard.setVisibility(View.GONE);

                        // 隱藏保存按鈕
                        findViewById(R.id.save_slots_button).setVisibility(View.GONE);

                        // 顯示預約詳情部分
                        bookingDetailCard.setVisibility(View.VISIBLE);
                        findViewById(R.id.update_lesson_status_button).setVisibility(View.VISIBLE);
                        findViewById(R.id.interview_button).setVisibility(View.VISIBLE);
                        findViewById(R.id.view_student_contact_button).setVisibility(View.VISIBLE);

                        // 隱藏分隔線
                        findViewById(R.id.booking_requests_divider).setVisibility(View.GONE);

                    } else {
                        // 顯示時間槽和請求，隱藏預約詳情
                        topAppBar.setTitle(getString(R.string.set_available_time_slots));
                        findViewById(R.id.update_lesson_status_button).setVisibility(View.GONE);
                        // 顯示時間槽部分
                        availableTimeSlotsTitle.setVisibility(View.VISIBLE);
                        findViewById(R.id.add_slot_card).setVisibility(View.VISIBLE);
                        timeSlotsRecyclerView.setVisibility(View.VISIBLE);
                        findViewById(R.id.save_slots_button).setVisibility(View.VISIBLE);

                        // 隱藏預約詳情部分
                        bookingDetailCard.setVisibility(View.GONE);

                        // 顯示預約請求部分
                        findViewById(R.id.booking_requests_title).setVisibility(View.VISIBLE);
                        bookingRequestsCard.setVisibility(
                                bookingRequests.isEmpty() ? View.GONE : View.VISIBLE
                        );
                        findViewById(R.id.no_requests_text).setVisibility(
                                bookingRequests.isEmpty() ? View.VISIBLE : View.GONE
                        );

                        // 顯示所有分隔線
                        for (int i = 0; i < ((ViewGroup) findViewById(android.R.id.content)).getChildCount(); i++) {
                            View child = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(i);
                            if (child instanceof MaterialDivider) {
                                child.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    bookingRequestAdapter.notifyDataSetChanged();

                } catch (Exception e) {
                    Log.e("BookingRequests", "Error processing booking requests: " + e.getMessage());
                    Toast.makeText(TutorBooking.this,
                            getString(R.string.error_processing_booking_requests), Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        private void showBookingDetail(BookingRequest request) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            String studentLabel = getString(R.string.student_label, request.getStudentName());
            bookingStudentName.setText(studentLabel);

            String dateTimeText = getString(R.string.date_time_format, 
                               dateFormat.format(request.getStartTime().getTime()),
                               timeFormat.format(request.getStartTime().getTime()),
                               timeFormat.format(request.getEndTime().getTime()));
            bookingDateTime.setText(dateTimeText);

            // 設置狀態標籤外觀
            String status = request.getStatus();
            bookingStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase());
            
            int backgroundColor, textColor;
            if ("conflict".equalsIgnoreCase(status)) {
                // 使用錯誤顏色表示衝突
                backgroundColor = ContextCompat.getColor(this, R.color.error_container);
                textColor = ContextCompat.getColor(this, R.color.error);
                
                // 確保顯示付款狀態區塊
                findViewById(R.id.payment_status_layout).setVisibility(View.VISIBLE);
                
                // 衝突狀態下隱藏更新課程狀態按鈕
                findViewById(R.id.update_lesson_status_button).setVisibility(View.GONE);
                findViewById(R.id.interview_button).setVisibility(View.GONE);
                findViewById(R.id.view_student_contact_button).setVisibility(View.VISIBLE);
                Log.d("TutorBooking", "showBookingDetail: 衝突狀態，隱藏按鈕");
            } else if ("completed".equalsIgnoreCase(status)) {
                // 使用成功顏色表示完成狀態
                backgroundColor = ContextCompat.getColor(this, R.color.success_container);
                textColor = ContextCompat.getColor(this, R.color.success);
                
                // 確保顯示付款狀態區塊
                findViewById(R.id.payment_status_layout).setVisibility(View.VISIBLE);
                
                // 完成狀態下隱藏更新課程狀態按鈕
                findViewById(R.id.update_lesson_status_button).setVisibility(View.GONE);
                findViewById(R.id.interview_button).setVisibility(View.GONE);
                findViewById(R.id.view_student_contact_button).setVisibility(View.VISIBLE);
                Log.d("TutorBooking", "showBookingDetail: 已完成狀態，隱藏按鈕");
            } else {
                // 其他狀態（已確認等）使用綠色
                backgroundColor = ContextCompat.getColor(this, R.color.success_container);
                textColor = ContextCompat.getColor(this, R.color.success);
            }
            
            bookingStatus.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
            bookingStatus.setTextColor(textColor);
            
            // 檢查和顯示付款狀態
            checkPaymentStatus(request.getRequestId());

            bookingDetailCard.setVisibility(View.VISIBLE);
            findViewById(R.id.view_student_contact_button).setVisibility(View.VISIBLE);
        }

        // 添加檢查付款狀態的方法
        private void checkPaymentStatus(String bookingId) {
            OkHttpClient client = new OkHttpClient();
            
            RequestBody formBody = new FormBody.Builder()
                    .add("booking_id", bookingId)
                    .build();
                    
            // 使用導師專用的付款狀態檢查PHP
            String url = "http://" + IPConfig.getIP() + "/FYP/php/get_payment_status_for_tutor.php";
            Log.d("Payment", "Checking payment status for booking ID: " + bookingId);
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();
                    
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("Payment", "Failed to check payment status: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(TutorBooking.this, 
                                getString(R.string.failed_to_check_payment_status), 
                                Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    Log.d("Payment", "Payment status response: " + responseData);
                    
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        if (jsonResponse.getBoolean("success")) {
                            String paymentStatus = jsonResponse.getString("status");
                            String bookingStatus = jsonResponse.optString("booking_status", "");
                            
                            // 詳細記錄付款資訊
                            Log.d("Payment", "Parsed payment status: " + paymentStatus);
                            Log.d("Payment", "Booking status from response: " + bookingStatus);
                            if (jsonResponse.has("payment_id")) {
                                Log.d("Payment", "Payment ID: " + jsonResponse.getString("payment_id"));
                            }
                            if (jsonResponse.has("payment_date")) {
                                Log.d("Payment", "Payment date: " + jsonResponse.getString("payment_date"));
                            }
                            if (jsonResponse.has("message")) {
                                Log.d("Payment", "Message: " + jsonResponse.getString("message"));
                            }
                            
                            runOnUiThread(() -> {
                                // 查找付款狀態顯示元素
                                Chip paymentStatusChip = findViewById(R.id.student_payment_status);
                                if (paymentStatusChip != null) {
                                    // 根據付款狀態設置合適的文字和樣式
                                    if ("verified".equalsIgnoreCase(paymentStatus) || 
                                        "confirmed".equalsIgnoreCase(paymentStatus)) {
                                        // 處理已驗證和已確認狀態
                                        Log.d("Payment", "Setting chip to Payment Verified");
                                        paymentStatusChip.setText("Payment Verified");
                                        paymentStatusChip.setChipBackgroundColor(ColorStateList.valueOf(
                                                ContextCompat.getColor(TutorBooking.this, R.color.success_container)));
                                        paymentStatusChip.setTextColor(
                                                ContextCompat.getColor(TutorBooking.this, R.color.success));
                                    } else if ("submitted".equalsIgnoreCase(paymentStatus)) {
                                        Log.d("Payment", "Setting chip to Payment Submitted");
                                        paymentStatusChip.setText("Payment Submitted");
                                        paymentStatusChip.setChipBackgroundColor(ColorStateList.valueOf(
                                                ContextCompat.getColor(TutorBooking.this, R.color.primary_container)));
                                        paymentStatusChip.setTextColor(
                                                ContextCompat.getColor(TutorBooking.this, R.color.primary));
                                    } else {
                                        Log.d("Payment", "Setting chip to Not Submitted");
                                        paymentStatusChip.setText("Not Submitted");
                                        paymentStatusChip.setChipBackgroundColor(ColorStateList.valueOf(
                                                Color.LTGRAY));
                                        paymentStatusChip.setTextColor(
                                                Color.DKGRAY);
                                    }
                                } else {
                                    Log.e("Payment", "Payment status chip not found in layout (id: student_payment_status)");
                                }
                                
                                // 確保付款狀態區域顯示
                                View paymentStatusLayout = findViewById(R.id.payment_status_layout);
                                if (paymentStatusLayout != null) {
                                    paymentStatusLayout.setVisibility(View.VISIBLE);
                                }
                            });
                        } else {
                            Log.e("Payment", "API returned success=false: " + 
                                    jsonResponse.optString("message", "No error message provided"));
                        }
                    } catch (Exception e) {
                        Log.e("Payment", "Error processing payment status: " + e.getMessage(), e);
                    }
                }
            });
        }

        @Override
        public void onAcceptBooking(BookingRequest request) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.accept_booking))
                    .setMessage(getString(R.string.are_you_sure_you_want_to_accept_this_booking_request))
                    .setPositiveButton(getString(R.string.yes), (dialog, which) ->
                            handleBookingResponse(request.getRequestId(), true))
                    .setNegativeButton(getString(R.string.no), null)
                    .show();
        }

        @Override
        public void onRejectBooking(BookingRequest request) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.reject_booking))
                    .setMessage(getString(R.string.are_you_sure_you_want_to_reject_this_booking_request))
                    .setPositiveButton(getString(R.string.reject), (dialog, which) ->
                            handleBookingResponse(request.getRequestId(), false))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }

        private void showDatePicker() {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);

                        // Create new time slot with default times
                        Calendar defaultStart = (Calendar) selectedDate.clone();
                        defaultStart.set(Calendar.HOUR_OF_DAY, 9);
                        defaultStart.set(Calendar.MINUTE, 0);

                        Calendar defaultEnd = (Calendar) defaultStart.clone();
                        defaultEnd.add(Calendar.HOUR, 1);

                        TimeSlot newSlot = new TimeSlot(defaultStart, defaultEnd, false);
                        newSlot.setEditable(true); // Set new slot as editable
                        newSlot.setStatus("available"); // Ensure status is available
                        timeSlots.add(newSlot);
                        showTimePickerDialog(this, newSlot);
                        timeSlotAdapter.notifyDataSetChanged();
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );

            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        }



        public void showTimePickerDialog(Context context, TimeSlot timeSlot) {

            if (!timeSlot.isAvailable() || !timeSlot.isEditable()) {
                Toast.makeText(context, getString(R.string.this_time_slot_cannot_be_edited), Toast.LENGTH_SHORT).show();
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null);
            builder.setView(dialogView);


            NumberPicker startHourPicker = dialogView.findViewById(R.id.start_hour_picker);
            NumberPicker startMinutePicker = dialogView.findViewById(R.id.start_minute_picker);
            NumberPicker endHourPicker = dialogView.findViewById(R.id.end_hour_picker);
            NumberPicker endMinutePicker = dialogView.findViewById(R.id.end_minute_picker);

            setupTimePicker(startHourPicker, startMinutePicker, timeSlot.getStartTime());
            setupTimePicker(endHourPicker, endMinutePicker, timeSlot.getEndTime());

            builder.setPositiveButton(getString(R.string.save), (dialog, which) -> {
                Calendar startTime = (Calendar) timeSlot.getStartTime().clone();
                startTime.set(Calendar.HOUR_OF_DAY, startHourPicker.getValue());
                startTime.set(Calendar.MINUTE, startMinutePicker.getValue());

                Calendar endTime = (Calendar) timeSlot.getEndTime().clone();
                endTime.set(Calendar.HOUR_OF_DAY, endHourPicker.getValue());
                endTime.set(Calendar.MINUTE, endMinutePicker.getValue());

                if (isValidTimeRange(startTime, endTime)) {
                    timeSlot.setStartTime(startTime);
                    timeSlot.setEndTime(endTime);
                    timeSlot.setModified(true);
                    timeSlotAdapter.notifyDataSetChanged();
                }
            });

            builder.setNegativeButton(getString(R.string.cancel), null);
            builder.create().show();
        }

        private void loadExistingTimeSlots() {
            OkHttpClient client = new OkHttpClient();

            Log.d("Request", "Loading time slots for match_id: " + caseId);

            RequestBody formBody = new FormBody.Builder()
                    .add("match_id", caseId)
                    .add("is_tutor", "true")
                    .build();

            String url = "http://" + IPConfig.getIP() + "/FYP/php/get_time_slot.php";
            Log.d("Request", "URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("Network", "Failed to load time slots: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                            getString(R.string.failed_to_load_time_slots), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    Log.d("Response", "Time slots: " + responseData);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray slotsArray = jsonResponse.getJSONArray("slots");
                            processTimeSlots(slotsArray);


                        } else {
                            String message = jsonResponse.getString("message");
                            Log.w("Response", "Server error: " + message);
                            runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                                    message, Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        Log.e("Parse", "Failed to parse time slots: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                                getString(R.string.error_loading_time_slots), Toast.LENGTH_SHORT).show());
                    }
                }
            });
        }

        private void processTimeSlots(JSONArray slotsArray) {
            runOnUiThread(() -> {
                try {
                    timeSlots.clear();
                    for (int i = 0; i < slotsArray.length(); i++) {
                        JSONObject slotObject = slotsArray.getJSONObject(i);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                        Calendar startTime = Calendar.getInstance();
                        startTime.setTime(sdf.parse(slotObject.getString("start_time")));

                        Calendar endTime = Calendar.getInstance();
                        endTime.setTime(sdf.parse(slotObject.getString("end_time")));

                        TimeSlot slot = new TimeSlot(startTime, endTime, false);
                        slot.setSlotId(slotObject.getString("slot_id"));
                        slot.setStatus(slotObject.getString("status"));
                        slot.setStudentId(slotObject.optString("student_id", "")); // 添加学生ID

                        // 根据状态设置是否可编辑
                        boolean isEditable = slot.getStatus().equals("available");
                        slot.setEditable(isEditable);

                        timeSlots.add(slot);
                    }
                    timeSlotAdapter.notifyDataSetChanged();

                } catch (Exception e) {
                    Log.e("TimeSlots", "Error processing time slots: " + e.getMessage());
                    Toast.makeText(TutorBooking.this,
                            getString(R.string.error_processing_time_slots), Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void saveAvailableSlots() {
            if (timeSlots.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_add_at_least_one_time_slot), Toast.LENGTH_SHORT).show();
                return;
            }

            boolean hasChanges = false;
            for (TimeSlot slot : timeSlots) {
                // 只检查可编辑的时间槽的更改
                if (slot.isEditable() && (slot.isModified() || slot.getSlotId() == null)) {
                    hasChanges = true;
                    break;
                }
            }

            if (!hasChanges) {
                Toast.makeText(this, getString(R.string.no_changes_to_save), Toast.LENGTH_SHORT).show();
                return;
            }

            // 只验证和保存可编辑的时间槽
            List<TimeSlot> slotsToSave = new ArrayList<>();
            for (TimeSlot slot : timeSlots) {
                if (slot.isEditable()) {
                    if (!isValidTimeSlot(slot)) {
                        return;
                    }
                    slotsToSave.add(slot);
                }
            }

            sendSlotsToServer(slotsToSave);
        }

        private void sendSlotsToServer(List<TimeSlot> slotsToSave) {
            OkHttpClient client = new OkHttpClient();

            try {
                JSONArray slotsArray = new JSONArray();
                for (TimeSlot slot : slotsToSave) {
                    JSONObject slotObject = new JSONObject();

                    // Use a single format for datetime
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                    // Format complete datetime strings
                    String startDateTime = sdf.format(slot.getStartTime().getTime());
                    String endDateTime = sdf.format(slot.getEndTime().getTime());

                    slotObject.put("start_time", startDateTime);
                    slotObject.put("end_time", endDateTime);
                    slotObject.put("status", "available"); // Set default status

                    if (slot.getSlotId() != null && !slot.getSlotId().isEmpty()) {
                        slotObject.put("slot_id", slot.getSlotId());
                        slotObject.put("action", "update");
                    } else {
                        slotObject.put("action", "create");
                    }

                    slotsArray.put(slotObject);
                }

                // Log the data being sent
                Log.d("TimeSlots", "Sending data: " + slotsArray.toString());

                RequestBody formBody = new FormBody.Builder()
                        .add("match_id", caseId)
                        .add("tutor_id", tutorId)
                        .add("slots", slotsArray.toString())
                        .build();

                Request request = new Request.Builder()
                        .url("http://" + IPConfig.getIP() + "/FYP/php/post_time_slot.php")
                        .post(formBody)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e("TimeSlots", "Network failure: " + e.getMessage(), e);
                        runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                                getString(R.string.failed_to_save_time_slots), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseData = response.body().string();
                        Log.d("TimeSlots", "Server response: " + responseData);

                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            final boolean success = jsonResponse.optBoolean("success", false);
                            final String message = jsonResponse.optString("message", "Unknown error occurred");

                            runOnUiThread(() -> {
                                Toast.makeText(TutorBooking.this, message, Toast.LENGTH_SHORT).show();
                                if (success) {
                                    loadExistingTimeSlots();
                                }
                            });
                        } catch (Exception e) {
                            Log.e("TimeSlots", "Error processing response: " + e.getMessage(), e);
                            runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                                    getString(R.string.error_processing_server_response), Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("TimeSlots", "Error preparing data: " + e.getMessage(), e);
                Toast.makeText(this, getString(R.string.error_preparing_data) + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        private boolean isValidTimeSlot(TimeSlot slot) {
            Calendar now = Calendar.getInstance();

            if (slot.getStartTime().after(slot.getEndTime())) {
                Toast.makeText(this, getString(R.string.start_time_must_be_before_end_time), Toast.LENGTH_SHORT).show();
                return false;
            }

            if (slot.getStartTime().before(now)) {
                Toast.makeText(this, getString(R.string.cannot_create_time_slots_in_the_past), Toast.LENGTH_SHORT).show();
                return false;
            }

            long durationInMinutes = (slot.getEndTime().getTimeInMillis() -
                    slot.getStartTime().getTimeInMillis()) / (60 * 1000);
            if (durationInMinutes < 30) {
                Toast.makeText(this, getString(R.string.time_slot_must_be_at_least_30_minutes), Toast.LENGTH_SHORT).show();
                return false;
            }

            return true;
        }

        private boolean isValidTimeRange(Calendar startTime, Calendar endTime) {
            if (startTime.after(endTime)) {
                Toast.makeText(this, getString(R.string.start_time_must_be_before_end_time), Toast.LENGTH_SHORT).show();
                return false;
            }

            long durationInMinutes = (endTime.getTimeInMillis() - startTime.getTimeInMillis()) / (60 * 1000);
            if (durationInMinutes < 30) {
                Toast.makeText(this, getString(R.string.time_slot_must_be_at_least_30_minutes), Toast.LENGTH_SHORT).show();
                return false;
            }

            return true;
        }

        private void handleBookingResponse(String bookingId, boolean accept) {
            OkHttpClient client = new OkHttpClient();

            Log.d("Request", "Processing booking response - ID: " + bookingId + ", Accept: " + accept);

            RequestBody formBody = new FormBody.Builder()
                    .add("booking_id", bookingId)
                    .add("action", accept ? "accept" : "reject")
                    .build();

            Request request = new Request.Builder()
                    .url("http://" + IPConfig.getIP() + "/FYP/php/post_tutor_booking_response.php")
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("Network", "Failed to process booking response: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                            getString(R.string.failed_to_process_response), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    Log.d("Response", "Booking response: " + responseData);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        final String message = jsonResponse.getString("message");
                        if (!jsonResponse.optBoolean("success", false)) {
                            Log.w("Response", "Server error: " + message);
                        }
                        runOnUiThread(() -> {
                            Toast.makeText(TutorBooking.this, message, Toast.LENGTH_SHORT).show();
                            if (jsonResponse.optBoolean("success", false)) {
                                loadExistingTimeSlots();
                            }
                        });
                    } catch (Exception e) {
                        Log.e("Parse", "Failed to parse booking response: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                                getString(R.string.error_processing_response), Toast.LENGTH_SHORT).show());
                    }
                }
            });
        }

        private void showResponseDialog(TimeSlot slot) {
            if (!slot.getStatus().equals("pending")) {
                Toast.makeText(this, getString(R.string.this_slot_is_not_pending_for_response), Toast.LENGTH_SHORT).show();
                return;
            }

            String message = String.format(getString(R.string.student_request_for_time_slot),
                    slot.getDateString(),
                    new SimpleDateFormat("HH:mm", Locale.getDefault()).format(slot.getStartTime().getTime()),
                    new SimpleDateFormat("HH:mm", Locale.getDefault()).format(slot.getEndTime().getTime()));

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.booking_request))
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.accept), (dialog, which) ->
                            handleBookingResponse(slot.getSlotId(), true))
                    .setNegativeButton(getString(R.string.reject), (dialog, which) ->
                            handleBookingResponse(slot.getSlotId(), false))
                    .setNeutralButton(getString(R.string.cancel), null)
                    .show();
        }
        private void showUpdateLessonStatusDialog() {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lesson_status, null);

            MaterialButton completeButton = dialogView.findViewById(R.id.btn_lesson_complete);
            MaterialButton incompleteButton = dialogView.findViewById(R.id.btn_lesson_incomplete);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.first_lesson_status))
                    .setView(dialogView)
                    .setNegativeButton(getString(R.string.cancel), null)
                    .create();

            completeButton.setOnClickListener(v -> {
                dialog.dismiss();
                showConfirmCompletionDialog();
            });

            incompleteButton.setOnClickListener(v -> {
                dialog.dismiss();
                showIncompleteReasonDialog();
            });

            dialog.show();
        }

        private void showConfirmCompletionDialog() {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.confirm_completion))
                    .setMessage(getString(R.string.by_marking_the_lesson_as_completed_you_ll_proceed_to_the_feedback_process_this_action_cannot_be_undone) + "\n\n" + getString(R.string.do_you_want_to_continue))
                    .setPositiveButton(getString(R.string.continue_btn), (dialogInterface, i) -> {
                        submitLessonStatusWithAction("completed", null, null);
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }

        private void showIncompleteReasonDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_incomplete_reason, null);

            AlertDialog dialog = builder.setTitle(getString(R.string.reason_for_incomplete_lesson))
                    .setView(view)
                    .setNegativeButton(getString(R.string.cancel), null)
                    .create();

            int[] buttonIds = {
                    R.id.btn_reason_no_show,
                    R.id.btn_reason_technical,
                    R.id.btn_reason_emergency,
                    R.id.btn_reason_time,
                    R.id.btn_reason_other
            };
            for (int id : buttonIds) {
                MaterialButton button = view.findViewById(id);
                button.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (id == R.id.btn_reason_other) {
                        showCustomReasonDialog();
                    } else {
                        showRebookOrRefundDialog(button.getText().toString());
                    }
                });
            }

            dialog.show();
        }

        private void showCustomReasonDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_custom_reason, null);
            EditText input = view.findViewById(R.id.edit_text_reason);

            builder.setTitle(getString(R.string.specify_reason))
                    .setView(view)
                    .setPositiveButton(getString(R.string.submit), (dialog, which) -> {
                        String customReason = input.getText().toString().trim();
                        if (!customReason.isEmpty()) {
                            showRebookOrRefundDialog(customReason);
                        } else {
                            Toast.makeText(this, getString(R.string.please_enter_a_reason), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }

        private void showRebookOrRefundDialog(String reason) {
            // Submit directly for tutor with rebook action
            submitLessonStatusWithAction("incomplete", reason, "rebook");

            // Show a simple notification dialog
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.provide_new_time_slot))
                    .setMessage(getString(R.string.please_provide_new_available_time_slots_for_rebooking))
                    .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                        // The page will refresh automatically after status update
                    })
                    .show();
        }

        private void handleLessonStatusResponse(JSONObject jsonResponse) {
            try {
                String status = jsonResponse.getString("status");
                String message = jsonResponse.getString("message");

                switch (status) {
                    case "waiting":
                        showWaitingDialog();
                        break;
                    case "both_incomplete":
                        String reason = jsonResponse.optString("reason", "");
                        showBothIncompleteDialog(reason);
                        break;
                    case "conflict":
                        // Check if tutor marked as completed
                        if (message.contains(getString(R.string.student_marked_lesson_as_incomplete))) {
                            // Tutor marked completed, student marked incomplete
                            showConflictDialog(true);
                        } else {
                            // Tutor marked incomplete, student marked completed
                            showConflictDialog(false);
                        }
                        break;
                    case "completed":
                        showCompletedDialog(message);
                        getStudentBookingRequest(); // Refresh the UI state
                        break;
                    default:
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        break;
                }
            } catch (Exception e) {
                Log.e("LessonStatus", "Error handling response: " + e.getMessage());
                Toast.makeText(this, getString(R.string.error_processing_response), Toast.LENGTH_SHORT).show();
            }
        }

        private void showWaitingDialog() {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.status_submitted))
                    .setMessage(getString(R.string.your_status_has_been_submitted_waiting_for_the_student_s_response))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        }

        private void showBothIncompleteDialog(String reason) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.status_matched))
                    .setMessage(getString(R.string.both_you_and_the_student_have_marked_the_lesson_as_incomplete_the_system_will_process_refund_and_rebooking))
                    .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                        showRebookOrRefundDialog(reason);
                    })
                    .show();
        }


        private void showConflictDialog(boolean tutorMarkedComplete) {
            String message = tutorMarkedComplete ?
                    getString(R.string.you_marked_lesson_completed_but_student_marked_incomplete) :
                    getString(R.string.you_confirmed_lesson_unfinished_but_other_confirmed_completed);

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.status_conflict))
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                        // TODO: Implement evidence submission
                        Toast.makeText(this, getString(R.string.evidence_submission_to_be_implemented),
                                Toast.LENGTH_SHORT).show();
                    })
                    .show();
        }


        private void showCompletedDialog(String message) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.lesson_completed))
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        }



        private void submitLessonStatusWithAction(String status, String reason, String action) {
            OkHttpClient client = new OkHttpClient();

            FormBody.Builder formBuilder = new FormBody.Builder()
                    .add("case_id", caseId)
                    .add("tutor_id", tutorId)
                    .add("status", status);

            if (reason != null) {
                formBuilder.add("reason", reason);
            }

            if (action != null) {
                formBuilder.add("next_action", action);
            }

            Request request = new Request.Builder()
                    .url("http://" + IPConfig.getIP() + "/FYP/php/update_tutor_first_lesson_status.php")
                    .post(formBuilder.build())
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                            getString(R.string.failed_to_update_lesson_status), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    Log.d("Response", "Server response: " + responseData);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        final boolean success = jsonResponse.getBoolean("success");

                        if (success) {
                            runOnUiThread(() -> handleLessonStatusResponse(jsonResponse));
                        } else {
                            final String message = jsonResponse.optString("message", "Failed to update status");
                            runOnUiThread(() -> Toast.makeText(TutorBooking.this, message, Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                                getString(R.string.error_processing_response) + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
            });
        }



        private void setupTimePicker(NumberPicker hourPicker, NumberPicker minutePicker, Calendar time) {
            hourPicker.setMinValue(0);
            hourPicker.setMaxValue(23);
            hourPicker.setValue(time.get(Calendar.HOUR_OF_DAY));
            hourPicker.setFormatter(value -> String.format("%02d", value));

            minutePicker.setMinValue(0);
            minutePicker.setMaxValue(59);
            minutePicker.setValue(time.get(Calendar.MINUTE));
            minutePicker.setFormatter(value -> String.format("%02d", value));
        }

        @Override
        public void onBackPressed() {
            // Check if there are unsaved changes
            boolean hasUnsavedChanges = false;
            for (TimeSlot slot : timeSlots) {
                if (slot.isEditable() && (slot.isModified() || slot.getSlotId() == null)) {
                    hasUnsavedChanges = true;
                    break;
                }
            }

            if (hasUnsavedChanges) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.unsaved_changes))
                        .setMessage(getString(R.string.you_have_unsaved_changes_do_you_want_to_discard_them))
                        .setPositiveButton(getString(R.string.discard), (dialog, which) -> super.onBackPressed())
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            } else {
                super.onBackPressed();
            }
        }

        private void getStudentContact() {
            if (caseId == null || tutorId == null) {
                Toast.makeText(this, getString(R.string.error_loading_contact), Toast.LENGTH_SHORT).show();
                return;
            }
            
            OkHttpClient client = new OkHttpClient();
            
            RequestBody formBody = new FormBody.Builder()
                    .add("match_id", caseId)
                    .add("tutor_id", tutorId)
                    .build();
            
            String url = "http://" + IPConfig.getIP() + "/FYP/php/get_student_contact.php";
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("Network", "Failed to load student contact: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(TutorBooking.this, 
                            getString(R.string.error_loading_contact), Toast.LENGTH_SHORT).show());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    Log.d("Response", "Student contact: " + responseData);
                    
                    try {
                        Gson gson = new Gson();
                        StudentContactResponse contactResponse = gson.fromJson(responseData, StudentContactResponse.class);
                        
                        runOnUiThread(() -> {
                            if (contactResponse.isSuccess()) {
                                // Show student contact information in dialog
                                showStudentContactDialog(contactResponse.getStudent());
                            } else {
                                // Show error message
                                Toast.makeText(TutorBooking.this, contactResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        Log.e("Parse", "Failed to parse student contact: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                                getString(R.string.error_loading_contact), Toast.LENGTH_SHORT).show());
                    }
                }
            });
        }

        private void showStudentContactDialog(StudentContactResponse.StudentContact student) {
            if (student == null) {
                Toast.makeText(this, getString(R.string.no_contact_info), Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Create dialog view
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_contact_info, null);
            TextView nameTextView = dialogView.findViewById(R.id.contact_name);
            TextView phoneTextView = dialogView.findViewById(R.id.contact_phone);
            TextView emailTextView = dialogView.findViewById(R.id.contact_email);
            
            // Set contact information
            nameTextView.setText(getString(R.string.contact_name, student.getName()));
            phoneTextView.setText(getString(R.string.contact_phone, student.getPhone()));
            emailTextView.setText(getString(R.string.contact_email, student.getEmail()));
            
            // Show dialog
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.student_contact_info))
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }

        @Override
        protected void onResume() {
            super.onResume();
            // 在onResume中刷新內容
            refreshContent();
        }

        private void startInterviewActivity() {
            Intent intent = new Intent(this, TutorVideoIntroduction.class);
            intent.putExtra("match_id", caseId);
            intent.putExtra("member_id", tutorId);
            startActivity(intent);
        }
    }



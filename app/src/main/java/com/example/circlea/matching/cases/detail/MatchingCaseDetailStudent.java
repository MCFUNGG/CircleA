package com.example.circlea.matching.cases.detail;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.example.circlea.matching.cases.detail.TutorVideoIntroduction;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MatchingCaseDetailStudent extends AppCompatActivity implements LessonStatusDialogManager.LessonStatusCallback {

    private RecyclerView timeSlotsRecyclerView;
    private TimeSlotAdapter timeSlotAdapter;
    private List<TimeSlot> timeSlots;
    private String caseId;
    private String studentId;
    private String tutorId;
    private Double lessonFeePerHr;
    private MaterialTextView statusText;
    private ExtendedFloatingActionButton sendRequestButton;
    private View paymentCard;
    private MaterialButton paymentButton;
    private MaterialButton finishLessonButton;
    private MaterialButton interviewButton;
    private LessonStatusDialogManager dialogManager;
    private View tutorContactCard;
    private MaterialTextView tutorName;
    private MaterialTextView tutorPhone;
    private MaterialTextView tutorEmail;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentBookingId;
    private View tutorVideoCard;  // 添加导师视频卡片视图


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.matching_case_detail_student);
            Log.d("CurrentJava", "MatchingCaseDetailStudent");
        // Get student ID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", Context.MODE_PRIVATE);
        studentId = sharedPreferences.getString("member_id", "");

        // Get case ID from intent
        caseId = getIntent().getStringExtra("case_id");
        tutorId = getIntent().getStringExtra("tutor_id");
        Log.d("Payment", "tutorId: "+tutorId+"caseId: "+caseId);
        lessonFeePerHr = Double.valueOf(getIntent().getStringExtra("lessonFee"));

        // Initialize dialog manager with isTutor = false for student
        dialogManager = new LessonStatusDialogManager(this, this, false);

        initializeViews();
        
        // 強制隱藏更新課程狀態按鈕和面試按鈕 - 直接在onCreate中設置，最高優先級
        if (finishLessonButton != null) {
            Log.d("ButtonDebug", "Forcing button hide in onCreate");
            finishLessonButton.setVisibility(View.GONE);
            finishLessonButton.setEnabled(false);
            
            // 確保在UI渲染後再次檢查並隱藏按鈕
            new Handler().post(() -> {
                if (finishLessonButton != null) {
                    Log.d("ButtonDebug", "Post handler button hide in onCreate");
                    finishLessonButton.setVisibility(View.GONE);
                    finishLessonButton.setEnabled(false);
                }
            });
        }
        
        if (interviewButton != null) {
            Log.d("ButtonDebug", "Forcing interview button hide in onCreate");
            interviewButton.setVisibility(View.GONE);
            interviewButton.setEnabled(false);
            
            // 確保在UI渲染後再次檢查並隱藏按鈕
            new Handler().post(() -> {
                if (interviewButton != null) {
                    Log.d("ButtonDebug", "Post handler interview button hide in onCreate");
                    interviewButton.setVisibility(View.GONE);
                    interviewButton.setEnabled(false);
                }
            });
        }
        
        // 檢查Intent中是否帶有衝突狀態標記
        boolean hasConflictStatus = getIntent().getBooleanExtra("has_conflict_status", false);
        if (hasConflictStatus) {
            Log.d("ConflictUI", "Conflict status detected from intent");
            // 使用延遲執行確保UI元素已初始化
            new Handler().post(() -> {
                checkAndHideButtonForConflict();
            });
        }
        
        setupSwipeRefresh();
        checkExistingRequest();
        
        // 检查导师是否有视频记录
        checkTutorVideoRecord();
        
        // 在所有初始化完成后，检查当前用户是否为导师，只有导师才能看到Interview按钮
        new Handler().postDelayed(() -> {
            if (finishLessonButton != null && interviewButton != null) {
                // 获取当前用户ID - 直接使用已存在的SharedPreferences实例
                String currentUserId = sharedPreferences.getString("member_id", "");
                
                // 通过比较当前用户ID与tutorId来判断是否为导师
                boolean isCurrentUserTutor = !currentUserId.isEmpty() && currentUserId.equals(tutorId);
                
                // 只有导师且finishLessonButton可见时才显示interviewButton
                if (isCurrentUserTutor && finishLessonButton.getVisibility() == View.VISIBLE) {
                    Log.d("ButtonDebug", "Setting interview button visible for tutor (ID: " + currentUserId + ")");
                    interviewButton.setVisibility(View.VISIBLE);
                    interviewButton.setEnabled(true);
                } else {
                    Log.d("ButtonDebug", "Keeping interview button hidden - user ID: " + currentUserId + ", tutor ID: " + tutorId);
                    interviewButton.setVisibility(View.GONE);
                    interviewButton.setEnabled(false);
                }
            }
        }, 500); // 延迟500毫秒，确保其他UI更新已完成
    }

    private void initializeViews() {
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        statusText = findViewById(R.id.status_text);
        sendRequestButton = findViewById(R.id.send_request_button);
        timeSlotsRecyclerView = findViewById(R.id.time_slots_recycler_view);
        paymentCard = findViewById(R.id.payment_card);
        paymentButton = findViewById(R.id.payment_button);
        finishLessonButton = findViewById(R.id.finish_lesson_button);
        interviewButton = findViewById(R.id.interview_button);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        // Setup RecyclerView
        timeSlotsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        timeSlots = new ArrayList<>();
        timeSlotAdapter = new TimeSlotAdapter(timeSlots, this, false);
        timeSlotsRecyclerView.setAdapter(timeSlotAdapter);

        // Setup click listeners
        sendRequestButton.setOnClickListener(v -> requestSelectedSlot());
        topAppBar.setNavigationOnClickListener(v -> finish());
        if (paymentButton != null) {
            paymentButton.setOnClickListener(v -> handlePayment());
        }
        if (finishLessonButton != null) {
            finishLessonButton.setOnClickListener(v -> handleFinishLesson());
        }
        if (interviewButton != null) {
            interviewButton.setOnClickListener(v -> handleInterview());
        }

        tutorContactCard = findViewById(R.id.tutor_contact_card);
        tutorName = findViewById(R.id.tutor_name);
        tutorPhone = findViewById(R.id.tutor_phone);
        tutorEmail = findViewById(R.id.tutor_email);
        tutorVideoCard = findViewById(R.id.tutor_video_card);
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
        Log.d("MatchingCaseDetailStudent", "Refreshing content...");
        
        // 清空現有數據
        timeSlots.clear();
        
        // 重新加載數據
        checkExistingRequest();
        
        // 如果已有確認的時間槽，檢查支付狀態和課程狀態
        boolean hasConfirmedSlot = getIntent().getBooleanExtra("has_confirmed_slot", false);
        if (hasConfirmedSlot) {
            checkPaymentStatus();
            checkLessonStatus();
        }
        
        // 延遲一點時間後停止刷新動畫
        new Handler().postDelayed(() -> {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 1500);
    }

    private void checkExistingRequest() {
        // 首先，嘗試直接檢查數據庫中的預約狀態
        checkBookingStatus();
    }
    
    private void checkBookingStatus() {
        OkHttpClient client = new OkHttpClient();

        // 創建表單數據
        RequestBody formBody = new FormBody.Builder()
                .add("match_id", caseId)
                .add("student_id", studentId)
                .add("check_all_statuses", "true") // 請求檢查所有狀態
                .add("check_is_new_match", "true") // 添加新標誌，檢查是否為新匹配
                .build();
                
        String url = "http://" + IPConfig.getIP() + "/FYP/php/check_booking_status.php";
        Log.d("BookingStatus", "Sending request to: " + url);
        Log.d("BookingStatus", "With params - match_id: " + caseId + ", student_id: " + studentId);
                
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
                
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("BookingStatus", "Failed to check booking status: " + e.getMessage(), e);
                // 如果無法檢查狀態，回退到原有流程
                checkExistingRequestOriginal();
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("BookingStatus", "Response code: " + response.code());
                Log.d("BookingStatus", "Response body: " + responseData);
                
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    boolean success = jsonResponse.getBoolean("success");
                    
                    // 檢查是否為新匹配
                    boolean isNewMatch = jsonResponse.optBoolean("is_new_match", false);
                    
                    Log.d("BookingStatus", "Response success: " + success + ", isNewMatch: " + isNewMatch);
                    
                    // 如果是新匹配，直接加載所有可用時間段
                    if (success && isNewMatch) {
                        Log.d("BookingStatus", "This is a new match, loading all available time slots");
                        runOnUiThread(() -> {
                            // 清空任何可能存在的pending預約
                            timeSlots.clear();
                            loadTimeSlots();
                        });
                        return;
                    }
                    
                    if (success) {
                        boolean hasBooking = jsonResponse.getBoolean("has_booking");
                        Log.d("BookingStatus", "Has booking: " + hasBooking);
                        
                        if (hasBooking) {
                            // 有預約，根據響應中的數據處理
                            JSONObject bookingData = jsonResponse.getJSONObject("booking");
                            
                            // 直接顯示詳情頁面
                            runOnUiThread(() -> {
                                try {
                                    // 檢查是否有force_detail_view字段
                                    boolean forceDetailView = bookingData.optBoolean("force_detail_view", false);
                                    String status = bookingData.getString("status");
                                    String startTime = bookingData.getString("start_time");
                                    String endTime = bookingData.getString("end_time");
                                    
                                    // 保存booking_id
                                    currentBookingId = bookingData.getString("booking_id");
                                    
                                    // 計算時長和費用
                                    long durationInMinutes = calculateDurationInMinutes(startTime, endTime);
                                    double totalLessonFee = calculateLessonFee(durationInMinutes, lessonFeePerHr);
                                    
                                    // 優先處理conflict狀態
                                    if ("conflict".equalsIgnoreCase(status)) {
                                        Log.d("BookingStatus", "Conflict status detected, showing conflict UI");
                                        statusText.setText("There is a status conflict for your lesson at:\n" +
                                                formatDateTime(startTime) + " - " + formatTime(endTime));
                                        showConflictUI();
                                    } else if (forceDetailView || "completed".equalsIgnoreCase(status)) {
                                        Log.d("BookingStatus", "Forcing detail view based on status or flag");
                                        showCompletedUI(startTime, endTime, totalLessonFee);
                                    } else {
                                        // 將數據傳遞給原有的處理邏輯
                                        showStudentRequestStatus(bookingData);
                                    }
                                } catch (Exception e) {
                                    Log.e("BookingStatus", "Error processing booking data: " + e.getMessage(), e);
                                    checkExistingRequestOriginal();
                                }
                            });
                        } else {
                            // 沒有預約，繼續標準流程
                            Log.d("BookingStatus", "No booking found, proceeding with standard flow");
                            checkExistingRequestOriginal();
                        }
                    } else {
                        // API調用失敗，繼續標準流程
                        String message = jsonResponse.optString("message", "Unknown error");
                        Log.e("BookingStatus", "API call failed: " + message);
                        checkExistingRequestOriginal();
                    }
                } catch (Exception e) {
                    Log.e("BookingStatus", "Error parsing response: " + e.getMessage(), e);
                    checkExistingRequestOriginal(); // 回退到標準流程
                }
            }
        });
    }
    
    // 原來的請求檢查方法，作為備份
    private void checkExistingRequestOriginal() {
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("match_id", caseId)
                .add("student_id", studentId)
                .build();

        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/check_student_request.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MatchingCaseDetailStudent.this,
                            "Failed to check existing requests", Toast.LENGTH_SHORT).show();
                    loadTimeSlots(); // Fallback to loading all slots
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    if (jsonResponse.getBoolean("has_request")) {
                        JSONObject requestData = jsonResponse.getJSONObject("request_data");
                        
                        // 檢查是否為衝突狀態，如果是，直接顯示衝突UI
                        String status = requestData.optString("status", "");
                        if ("conflict".equalsIgnoreCase(status)) {
                            Log.d("BookingStatus", "Conflict status detected in checkExistingRequestOriginal");
                            String startTime = requestData.getString("start_time");
                            String endTime = requestData.getString("end_time");
                            
                            // 保存booking_id
                            currentBookingId = requestData.optString("booking_id", "");
                            
                            runOnUiThread(() -> {
                                statusText.setText("There is a status conflict for your lesson at:\n" +
                                        formatDateTime(startTime) + " - " + formatTime(endTime));
                                showConflictUI();
                            });
                        } else {
                        runOnUiThread(() -> showStudentRequestStatus(requestData));
                        }
                    } else {
                        loadTimeSlots(); // No existing request, load available slots
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(MatchingCaseDetailStudent.this,
                                "Error checking requests", Toast.LENGTH_SHORT).show();
                        loadTimeSlots(); // Fallback to loading all slots
                    });
                }
            }
        });
    }

    private void loadTimeSlots() {
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("match_id", caseId)
                .add("is_tutor", "false")
                .build();

        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/get_time_slot.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MatchingCaseDetailStudent.this,
                            "Failed to load time slots", Toast.LENGTH_SHORT).show();
                    showNoSlotsAvailable();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    if (jsonResponse.getBoolean("success")) {
                        JSONArray slotsArray = jsonResponse.getJSONArray("slots");
                        processTimeSlots(slotsArray);
                    } else {
                        runOnUiThread(() -> {
                            String message = jsonResponse.optString("message", "Error loading time slots");
                            Toast.makeText(MatchingCaseDetailStudent.this,
                                    message, Toast.LENGTH_SHORT).show();
                            showNoSlotsAvailable();
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(MatchingCaseDetailStudent.this,
                                "Error processing response", Toast.LENGTH_SHORT).show();
                        showNoSlotsAvailable();
                    });
                }
            }
        });
    }

    private void processTimeSlots(JSONArray slotsArray) {
        runOnUiThread(() -> {
            try {
                timeSlots.clear();
                boolean hasAvailableSlots = false;

                for (int i = 0; i < slotsArray.length(); i++) {
                    JSONObject slotObject = slotsArray.getJSONObject(i);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                    Calendar startTime = Calendar.getInstance();
                    startTime.setTime(sdf.parse(slotObject.getString("start_time")));

                    Calendar endTime = Calendar.getInstance();
                    endTime.setTime(sdf.parse(slotObject.getString("end_time")));

                    TimeSlot slot = new TimeSlot(startTime, endTime, false);
                    slot.setSlotId(slotObject.getString("slot_id"));
                    slot.setStatus("available");
                    timeSlots.add(slot);
                    hasAvailableSlots = true;
                }

                if (hasAvailableSlots) {
                    showAvailableSlots();
                } else {
                    showNoSlotsAvailable();
                }

                timeSlotAdapter.notifyDataSetChanged();

            } catch (Exception e) {
                Log.e("TimeSlots", "Error processing time slots: " + e.getMessage());
                showNoSlotsAvailable();
            }
        });
    }

    private void showStudentRequestStatus(JSONObject slotObject) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String startTime = slotObject.getString("start_time");
            String endTime = slotObject.getString("end_time");
            String status = slotObject.getString("status");
            
            // 保存booking_id作为成员变量
            currentBookingId = slotObject.optString("booking_id", null);
            Log.d("BookingInfo", "Current booking ID: " + currentBookingId);

            // 檢查force_detail_view標志
            boolean forceDetailView = slotObject.optBoolean("force_detail_view", false);
            if (forceDetailView) {
                Log.d("BookingInfo", "Force detail view flag detected - showing detail view");
                // 如果標志為true，直接顯示已完成的詳情視圖，無論當前狀態是什麼
                long durationInMinutes = calculateDurationInMinutes(startTime, endTime);
                double totalLessonFee = calculateLessonFee(durationInMinutes, lessonFeePerHr);
                showCompletedUI(startTime, endTime, totalLessonFee);
                return;
            }

            // Calculate duration and fee
            long durationInMinutes = calculateDurationInMinutes(startTime, endTime);
            double totalLessonFee = calculateLessonFee(durationInMinutes, lessonFeePerHr);

            String statusMessage;
            switch (status.toLowerCase()) {
                case "pending":
                    statusMessage = "Waiting for tutor to accept your request for:";
                    showPendingUI();
                    break;
                case "confirmed":
                    statusMessage = "Your first lesson has been confirmed for:";
                    showConfirmedUI(startTime, endTime, totalLessonFee);
                    break;
                case "rejected":
                    statusMessage = "Your booking request was rejected for:";
                    showRejectedUI();
                    break;
                case "completed":
                    statusMessage = "Your first lesson was completed at:";
                    showCompletedUI(startTime, endTime, totalLessonFee);  // New method
                    break;
                case "conflict":
                    statusMessage = "There is a status conflict for your lesson at:";
                    showConflictUI();  // Use our new method
                    break;
                default:
                    statusMessage = "Status of your booking request:";
                    showDefaultUI();
                    break;
            }

            statusText.setText(statusMessage + "\n" +
                    formatDateTime(startTime) + " - " + formatTime(endTime));
            statusText.setVisibility(View.VISIBLE);

            // Update status chips
            updateLessonStatusChip(status);
            checkPaymentStatus();
        } catch (Exception e) {
            Log.e("TimeSlots", "Error showing request status: " + e.getMessage());
        }
    }


    private void checkLessonStatus() {
        OkHttpClient client = new OkHttpClient();

        // 构建请求表单
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("match_id", caseId)
                .add("student_id", studentId);

        // 如果存在当前booking_id，添加到请求中
        String currentBookingId = getCurrentBookingId();
        if (currentBookingId != null && !currentBookingId.isEmpty()) {
            formBuilder.add("booking_id", currentBookingId);
            Log.d("LessonStatus", "Checking lesson status with booking_id: " + currentBookingId);
        } else {
            Log.d("LessonStatus", "Checking lesson status without booking_id");
        }

        RequestBody formBody = formBuilder.build();

        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/get_first_lesson_status.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Lesson", "Failed to check lesson status: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("LessonStatus", "Response: " + responseData);
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    if (jsonResponse.getBoolean("success")) {
                        JSONObject lessonData = jsonResponse.getJSONObject("data");
                        String lessonStatus = lessonData.getString("status");
                        runOnUiThread(() -> updateLessonStatusChip(lessonStatus));
                    } else {
                        Log.e("LessonStatus", "Error in response: " + jsonResponse.getString("message"));
                    }
                } catch (Exception e) {
                    Log.e("Lesson", "Error processing lesson status: " + e.getMessage());
                }
            }
        });
    }

    // 获取当前活动的booking_id
    private String getCurrentBookingId() {
        // 首先检查成员变量
        if (currentBookingId != null && !currentBookingId.isEmpty()) {
            return currentBookingId;
        }
        
        // 从Intent获取，如果已传递
        String bookingId = getIntent().getStringExtra("booking_id");
        if (bookingId != null && !bookingId.isEmpty()) {
            currentBookingId = bookingId; // 保存到成员变量
            return bookingId;
        }
        
        return null;
    }

    // Add this method to update the lesson status chip
    private void updateLessonStatusChip(String status) {
        com.google.android.material.chip.Chip statusChip = findViewById(R.id.status_lesson);
        MaterialButton finishLessonButton = findViewById(R.id.finish_lesson_button);

        // 無論如何，首先檢查當前狀態，如果是衝突狀態，強制隱藏按鈕
        if ("conflict".equals(status.toLowerCase()) && finishLessonButton != null) {
            finishLessonButton.setVisibility(View.GONE);
            finishLessonButton.setEnabled(false);
            Log.d("ConflictUI", "Forcing button hide in updateLessonStatusChip");
        }

        if (statusChip != null) {
            statusChip.setVisibility(View.VISIBLE);

            String statusText;
            int backgroundColor;
            int textColor;

            switch (status.toLowerCase()) {
                case "completed":
                    statusText = "Completed";
                    backgroundColor = ContextCompat.getColor(this, R.color.success_container);
                    textColor = ContextCompat.getColor(this, R.color.success);
                    if (finishLessonButton != null) {
                        finishLessonButton.setVisibility(View.GONE);
                    }
                    break;
                case "conflict":
                    statusText = "Conflict";
                    backgroundColor = ContextCompat.getColor(this, R.color.error_container);
                    textColor = ContextCompat.getColor(this, R.color.error);
                    // 再次確保隱藏按鈕（雙重保險）
                    if (finishLessonButton != null) {
                        finishLessonButton.setVisibility(View.GONE);
                        finishLessonButton.setEnabled(false);
                    }
                    break;
                case "incomplete":
                    statusText = "Incomplete";
                    backgroundColor = ContextCompat.getColor(this, R.color.error_container);
                    textColor = ContextCompat.getColor(this, R.color.error);
                    break;
                default:
                    statusText = "Pending";
                    backgroundColor = ContextCompat.getColor(this, R.color.neutral_90);
                    textColor = ContextCompat.getColor(this, R.color.neutral_30);
                    break;
            }

            statusChip.setText(statusText);
            statusChip.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
            statusChip.setTextColor(textColor);
        }
    }

    private long calculateDurationInMinutes(String startTime, String endTime) {
        Log.d("Payment", "calculateDurationInMinutes(String startTime, String endTime)\nstartTime: "+startTime+"\nendTime: "+endTime);
        try {
            // 檢查是否只包含時間部分 (HH:mm:ss)
            if (startTime.matches("\\d{2}:\\d{2}:\\d{2}")) {
                Log.d("Payment", "匹配到時間格式 HH:mm:ss");
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                Date startDate = timeFormat.parse(startTime);
                Date endDate = timeFormat.parse(endTime);
                
                if (startDate == null || endDate == null) {
                    Log.e("Payment", "解析時間失敗，startDate或endDate為null");
                    return 0;
                }
                
                long startMillis = startDate.getTime();
                long endMillis = endDate.getTime();
                
                Log.d("Payment", "startMillis: " + startMillis + ", endMillis: " + endMillis);
                
                // 如果結束時間小於開始時間，表示跨天
                if (endMillis < startMillis) {
                    endMillis += 24 * 60 * 60 * 1000; // 加上一天的毫秒數
                    Log.d("Payment", "時間跨天, 新的endMillis: " + endMillis);
                }
                
                long diffMinutes = (endMillis - startMillis) / (60 * 1000);
                Log.d("Payment", "a: " + diffMinutes); // 正確計算的分鐘數
                return diffMinutes;
            } 
            // 檢查是否為 HH:mm 格式
            else if (startTime.matches("\\d{2}:\\d{2}")) {
                Log.d("Payment", "匹配到時間格式 HH:mm");
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date startDate = timeFormat.parse(startTime);
                Date endDate = timeFormat.parse(endTime);
                
                if (startDate == null || endDate == null) {
                    Log.e("Payment", "解析時間失敗，startDate或endDate為null");
                    return 0;
                }
                
                long startMillis = startDate.getTime();
                long endMillis = endDate.getTime();
                
                Log.d("Payment", "startMillis: " + startMillis + ", endMillis: " + endMillis);
                
                // 如果結束時間小於開始時間，表示跨天
                if (endMillis < startMillis) {
                    endMillis += 24 * 60 * 60 * 1000; // 加上一天的毫秒數
                    Log.d("Payment", "時間跨天, 新的endMillis: " + endMillis);
                }
                
                long diffMinutes = (endMillis - startMillis) / (60 * 1000);
                Log.d("Payment", "a: " + diffMinutes); // 正確計算的分鐘數
                return diffMinutes;
            }
            else {
                Log.d("Payment", "使用完整日期時間格式: yyyy-MM-dd HH:mm");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                Date startDate = sdf.parse(startTime);
                Date endDate = sdf.parse(endTime);
                
                if (startDate == null || endDate == null) {
                    Log.e("Payment", "解析日期時間失敗，startDate或endDate為null");
                    return 0;
                }
                
                long startMillis = startDate.getTime();
                long endMillis = endDate.getTime();
                
                Log.d("Payment", "startMillis: " + startMillis + ", endMillis: " + endMillis);
                
                long diff = endMillis - startMillis;
                long diffMinutes = diff / (60 * 1000);
                Log.d("Payment", "a: "+ diffMinutes); // Convert to minutes
                return diffMinutes;
            }
        } catch (Exception e) {
            Log.e("Payment", "時間計算錯誤: " + e.getMessage(), e);
            Log.d("Payment", "b: 0");
            return 0;
        }
    }

    private double calculateLessonFee(long durationInMinutes, double lessonFeePerHr) {
        // Round up to nearest 30 minutes
        long roundedDuration = ((durationInMinutes + 29) / 30) * 30;
        return (roundedDuration / 60.0) * lessonFeePerHr;
    }


    private void showConfirmedUI(String startTime, String endTime, double totalLessonFee) {
        sendRequestButton.setVisibility(View.GONE);
        timeSlotsRecyclerView.setVisibility(View.GONE);
        paymentCard.setVisibility(View.VISIBLE);
        checkLessonStatus();
        checkPaymentStatus();
        paymentButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, Payment.class);
            intent.putExtra("case_id", caseId);
            intent.putExtra("totalLessonFee", totalLessonFee);
            intent.putExtra("lessonFeePerHr", lessonFeePerHr);
            intent.putExtra("startTime", startTime);
            intent.putExtra("endTime", endTime);
            
            // 添加booking_id到Intent
            if (currentBookingId != null && !currentBookingId.isEmpty()) {
                intent.putExtra("booking_id", currentBookingId);
                Log.d("BookingInfo", "Sending booking_id to Payment: " + currentBookingId);
            }
            
            startActivity(intent);
        });
    }

    private void checkPaymentStatus() {
        OkHttpClient client = new OkHttpClient();

        // 构建请求表单
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("match_id", caseId)
                .add("student_id", studentId);

        // 如果存在当前booking_id，添加到请求中
        String bookingId = getCurrentBookingId();
        if (bookingId != null && !bookingId.isEmpty()) {
            formBuilder.add("booking_id", bookingId);
            Log.d("PaymentStatus", "Checking payment status with booking_id: " + bookingId);
        } else {
            Log.d("PaymentStatus", "Checking payment status without booking_id");
        }

        RequestBody formBody = formBuilder.build();

        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/get_payment_status.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Log.e("Payment", "Failed to check payment status: " + e.getMessage());
                    updatePaymentUI("not_submitted", "Payment Required", null);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("PaymentStatus", "Response: " + responseData);
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    if (jsonResponse.getBoolean("success")) {
                        JSONObject paymentData = jsonResponse.getJSONObject("data");
                        String status = paymentData.optString("status", "not_submitted");
                        String statusText = paymentData.optString("status_text", "Payment Required");
                        String receiptPath = paymentData.optString("receipt_path", null);

                        Log.d("Payment", "Status: " + status); // Add debug logging
                        Log.d("Payment", "Payment proof: " + receiptPath); // Add debug logging
                        runOnUiThread(() -> updatePaymentUI(status, statusText, receiptPath));
                    } else {
                        runOnUiThread(() -> updatePaymentUI("not_submitted", "Payment Required", null));
                    }
                } catch (Exception e) {
                    Log.e("Payment", "Error processing payment status: " + e.getMessage());
                    runOnUiThread(() -> updatePaymentUI("not_submitted", "Payment Required", null));
                }
            }
        });
    }
    private void updatePaymentUI(String status, String statusText, String receiptPath) {
        // 檢查是否存在衝突狀態，並強制隱藏按鈕
        checkAndHideButtonForConflict();
        
        com.google.android.material.chip.Chip statusChip = findViewById(R.id.status_payment);
        MaterialButton paymentButton = findViewById(R.id.payment_button);

        if (statusChip != null) {
            statusChip.setText(statusText);

            int backgroundColor;
            int textColor;

            // 衝突狀態檢查提前 - 最高優先級處理
            if (findViewById(R.id.status_lesson) != null &&
                ((com.google.android.material.chip.Chip)findViewById(R.id.status_lesson)).getText().toString().equals("Conflict")) {
                if (finishLessonButton != null) {
                    Log.d("ConflictUI", "Force hiding button in updatePaymentUI due to conflict status");
                    finishLessonButton.setVisibility(View.GONE);
                    finishLessonButton.setEnabled(false);
                    
                    // 確保在UI刷新後也保持按鈕隱藏
                    new Handler().post(() -> {
                        if (finishLessonButton != null) {
                            finishLessonButton.setVisibility(View.GONE);
                            finishLessonButton.setEnabled(false);
                        }
                    });
                }
            }

            // First check if lesson is completed
            if (findViewById(R.id.status_lesson) != null &&
                    ((com.google.android.material.chip.Chip)findViewById(R.id.status_lesson)).getText().toString().equals("Completed")) {
                // If lesson is completed, always hide the finish lesson button
                if (finishLessonButton != null) {
                    finishLessonButton.setVisibility(View.GONE);
                }
            }
            
            // 如果是衝突狀態，強制隱藏finish button
            if (findViewById(R.id.status_lesson) != null &&
                ((com.google.android.material.chip.Chip)findViewById(R.id.status_lesson)).getText().toString().equals("Conflict")) {
                if (finishLessonButton != null) {
                    finishLessonButton.setVisibility(View.GONE);
                    finishLessonButton.setEnabled(false);
                }
            }

            switch (status.toLowerCase()) {
                case "confirmed":
                    backgroundColor = ContextCompat.getColor(this, R.color.success_container);
                    textColor = ContextCompat.getColor(this, R.color.success);
                    paymentButton.setVisibility(View.GONE);
                    // Only show finish lesson button if lesson is not completed
                    if (!((com.google.android.material.chip.Chip)findViewById(R.id.status_lesson)).getText().toString().equals("Completed")) {
                        finishLessonButton.setVisibility(View.VISIBLE);
                        finishLessonButton.setEnabled(true);
                    }
                    if (tutorId != null && !tutorId.isEmpty()) {
                        getTutorContact(tutorId);
                    }
                    break;

                case "completed":
                    backgroundColor = ContextCompat.getColor(this, R.color.success_container);
                    textColor = ContextCompat.getColor(this, R.color.success);
                    paymentButton.setVisibility(View.GONE);
                    finishLessonButton.setVisibility(View.GONE);
                    // Show tutor contact for completed status as well
                    if (tutorId != null && !tutorId.isEmpty()) {
                        getTutorContact(tutorId);
                    }
                    break;

                case "verified":  // Add this case for conflict situations
                    backgroundColor = ContextCompat.getColor(this, R.color.success_container);
                    textColor = ContextCompat.getColor(this, R.color.success);
                    paymentButton.setVisibility(View.GONE);
                    finishLessonButton.setVisibility(View.GONE);
                    // For conflict status with verified payment, show tutor contact
                    if (findViewById(R.id.status_lesson) != null &&
                        ((com.google.android.material.chip.Chip)findViewById(R.id.status_lesson)).getText().toString().equals("Conflict")) {
                        if (tutorId != null && !tutorId.isEmpty()) {
                            getTutorContact(tutorId);
                        }
                    }
                    break;

                case "pending":
                    backgroundColor = ContextCompat.getColor(this, R.color.warning_container);
                    textColor = ContextCompat.getColor(this, R.color.warning);
                    paymentButton.setVisibility(View.GONE);
                    finishLessonButton.setVisibility(View.GONE);
                    break;

                case "rejected":
                    backgroundColor = ContextCompat.getColor(this, R.color.error_container);
                    textColor = ContextCompat.getColor(this, R.color.error);
                    paymentButton.setVisibility(View.VISIBLE);
                    paymentButton.setEnabled(true);
                    paymentButton.setText("Submit Payment");
                    finishLessonButton.setVisibility(View.GONE);
                    break;

                case "not_submitted":
                default:
                    backgroundColor = ContextCompat.getColor(this, R.color.neutral_90);
                    textColor = ContextCompat.getColor(this, R.color.neutral_30);
                    paymentButton.setVisibility(View.VISIBLE);
                    paymentButton.setEnabled(true);
                    paymentButton.setText("Submit Payment");
                    finishLessonButton.setVisibility(View.GONE);
                    break;
            }

            statusChip.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
            statusChip.setTextColor(textColor);
        }
    }

    private void getTutorContact(String tutorId) {
        // Check if tutorId is null or empty
        if (tutorId == null || tutorId.isEmpty()) {
            Log.e("TutorContact", "Tutor ID is null or empty");
            Toast.makeText(this, "Invalid tutor information", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();

        try {
            RequestBody formBody = new FormBody.Builder()
                    .add("tutor_id", tutorId)
                    .build();

            String url = "http://" + IPConfig.getIP() + "/FYP/php/get_tutor_contact.php";

            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            Log.d("TutorContact", "Requesting tutor contact for ID: " + tutorId);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("TutorContact", "Network failure: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(MatchingCaseDetailStudent.this,
                                "Failed to get tutor contact", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    Log.d("TutorContact", "Response: " + responseData);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject tutorData = jsonResponse.getJSONObject("tutor");
                            runOnUiThread(() -> {
                                try {
                                    tutorName.setText("Name: " + tutorData.optString("name", "N/A"));
                                    tutorPhone.setText("Phone: " + tutorData.optString("phone", "N/A"));
                                    tutorEmail.setText("Email: " + tutorData.optString("email", "N/A"));
                                    tutorContactCard.setVisibility(View.VISIBLE);
                                } catch (Exception e) {
                                    Log.e("TutorContact", "UI update error: " + e.getMessage());
                                    Toast.makeText(MatchingCaseDetailStudent.this,
                                            "Error displaying tutor contact", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            String errorMessage = jsonResponse.optString("message", "Failed to get tutor contact");
                            Log.e("TutorContact", "Server error: " + errorMessage);
                            runOnUiThread(() -> {
                                Toast.makeText(MatchingCaseDetailStudent.this,
                                        errorMessage, Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        Log.e("TutorContact", "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(MatchingCaseDetailStudent.this,
                                    "Error processing response", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e("TutorContact", "Request building error: " + e.getMessage());
            Toast.makeText(this, "Error preparing request", Toast.LENGTH_SHORT).show();
        }
    }

    // Add handleFinishLesson method if not already present
    private void handleFinishLesson() {
        // 檢查是否為衝突狀態
        com.google.android.material.chip.Chip statusChip = findViewById(R.id.status_lesson);
        if (statusChip != null && "Conflict".equals(statusChip.getText().toString())) {
            Log.d("ButtonDebug", "Blocked showing dialog due to conflict status");
            // 如果是衝突狀態，強制隱藏按鈕而不顯示對話框
            if (finishLessonButton != null) {
                finishLessonButton.setVisibility(View.GONE);
                finishLessonButton.setEnabled(false);
            }
            return;
        }
        
        // 只有非衝突狀態才顯示對話框
        dialogManager.showLessonStatusDialog();
    }

    // 实现 LessonStatusCallback 接口方法
    @Override
    public void onLessonComplete() {
        submitLessonStatusWithAction("completed", null, null);
    }

    @Override
    public void onLessonIncomplete(String reason, String action) {
        Log.d("LessonStatus", "onLessonIncomplete called with reason: " + reason + ", action: " + action);
        
        if ("find_other_tutor".equals(action)) {
            // 對於"尋找其他導師"選項，直接調用cancelBookingAndFindOtherTutor方法
            cancelBookingAndFindOtherTutor();
        } else {
            // 對於其他選項（如"rebook"），使用submitLessonStatusWithAction方法
            submitLessonStatusWithAction("incomplete", reason, action);
        }
    }
    
    private void submitLessonStatusWithAction(String status, String reason, String action) {
        OkHttpClient client = new OkHttpClient();
        
        Log.d("LessonStatus", "Preparing to send status: " + status + ", action: " + action);
        
        // 確保在狀態為 incomplete 時提供有效的 reason
        if ("incomplete".equals(status) && (reason == null || reason.trim().isEmpty())) {
            Log.e("LessonStatus", "Reason is required for incomplete status");
            runOnUiThread(() -> {
                Toast.makeText(this, getString(R.string.please_enter_a_reason), Toast.LENGTH_SHORT).show();
            });
            return;
        }

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("case_id", caseId)
                .add("student_id", studentId)
                .add("status", status);

        if (reason != null) {
            formBuilder.add("reason", reason);
        }
        if (action != null) {
            formBuilder.add("next_action", action);
        }
        
        // 添加booking_id參數
        String bookingId = getCurrentBookingId();
        if (bookingId != null && !bookingId.isEmpty()) {
            formBuilder.add("booking_id", bookingId);
        }
        
        String url = "http://" + IPConfig.getIP() + "/FYP/php/update_student_first_lesson_status.php";
        Log.d("LessonStatus", "Request URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .post(formBuilder.build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("LessonStatus", "Network failure: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(MatchingCaseDetailStudent.this,
                        "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("LessonStatus", "Response code: " + response.code());
                Log.d("LessonStatus", "Response data: " + responseData);
                
                // 檢查響應是否為空
                if (responseData == null || responseData.trim().isEmpty()) {
                    Log.e("LessonStatus", "Empty response from server");
                    runOnUiThread(() -> Toast.makeText(MatchingCaseDetailStudent.this,
                            "Server returned empty response", Toast.LENGTH_SHORT).show());
                    return;
                }
                
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    boolean success = jsonResponse.getBoolean("success");
                    
                    // 檢查響應是否包含所需字段
                    if (!jsonResponse.has("status")) {
                        Log.e("LessonStatus", "Response missing 'status' field");
                        runOnUiThread(() -> Toast.makeText(MatchingCaseDetailStudent.this,
                                "Server response missing required fields", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    
                    String responseStatus = jsonResponse.getString("status");
                    String message = jsonResponse.optString("message", "Status updated");
                    
                    runOnUiThread(() -> {
                        if (!success) {
                            Toast.makeText(MatchingCaseDetailStudent.this,
                                    message, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Update UI based on status
                        updateLessonStatusChip(status);

                        // Show appropriate dialog based on server response
                        switch (responseStatus) {
                            case "both_incomplete":
                                if ("rebook".equals(action)) {
                                    // Reset booking status and reload UI
                                    resetBookingStatusAndReload(reason);
                                } else if ("find_other_tutor".equals(action)) {
                                    // 直接處理尋找其他導師的情況
                                    cancelBookingAndFindOtherTutor();
                                } else {
                                    showBothIncompleteDialog(reason);
                                }
                                break;
                            case "waiting":
                                showWaitingDialog();
                                break;
                            case "conflict":
                                showConflictUI();
                                break;
                            case "completed":
                                checkExistingRequest(); // Refresh the entire UI state
                                Toast.makeText(MatchingCaseDetailStudent.this,
                                        message, Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                Toast.makeText(MatchingCaseDetailStudent.this,
                                        message, Toast.LENGTH_SHORT).show();
                                break;
                        }
                    });
                } catch (Exception e) {
                    Log.e("LessonStatus", "Error processing response: " + e.getMessage());
                    Log.e("LessonStatus", "Response was: " + responseData);
                    runOnUiThread(() -> {
                        try {
                            // 嘗試使用更簡單的方式提取錯誤信息
                            String errorMessage = "Error processing response";
                            
                            // 嘗試以不同的方式解析JSON
                            try {
                                JSONObject errorJson = new JSONObject(responseData);
                                if (errorJson.has("message")) {
                                    errorMessage = errorJson.getString("message");
                                }
                            } catch (Exception jsonEx) {
                                // 檢查是否為HTML錯誤頁面
                                if (responseData.contains("<html") || responseData.contains("<!DOCTYPE")) {
                                    errorMessage = "Server returned HTML instead of JSON";
                                }
                            }
                            
                            Toast.makeText(MatchingCaseDetailStudent.this,
                                    errorMessage, Toast.LENGTH_LONG).show();
                        } catch (Exception showEx) {
                            // 確保至少能顯示一些錯誤消息
                            Toast.makeText(MatchingCaseDetailStudent.this,
                                    "Unknown error when processing response", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void resetBookingStatusAndReload(String reason) {
        Log.d("LessonStatus","running resetBookingStatusAndReload()");
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("case_id", caseId)
                .add("student_id", studentId)
                .add("reason", reason)
                .build();

        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/reset_booking_status.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MatchingCaseDetailStudent.this,
                        "Failed to reset booking status", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseData = response.body().string();
                    Log.d("responseData",responseData);
                    JSONObject jsonResponse = new JSONObject(responseData);
                    boolean success = jsonResponse.getBoolean("success");

                    runOnUiThread(() -> {
                        if (success) {
                            recreate(); // Reload the activity
                        } else {
                            Toast.makeText(MatchingCaseDetailStudent.this,
                                    "Failed to reset booking status", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MatchingCaseDetailStudent.this,
                            "Error processing response", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void handleRebooking() {
        new AlertDialog.Builder(this)
                .setTitle("Rebook Lesson")
                .setMessage("You'll now be able to select a new time slot for your lesson.")
                .setPositiveButton("Continue", (dialog, which) -> {
                    loadTimeSlots();
                    showAvailableSlots();
                })
                .setCancelable(false)
                .show();
    }

    private void handleFindOtherTutor() {
        new AlertDialog.Builder(this)
                .setTitle("Find Other Tutor")
                .setMessage("Are you sure you want to cancel this booking and find another tutor?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Cancel current booking and return to tutor search
                    cancelBookingAndFindOtherTutor();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelBookingAndFindOtherTutor() {
        OkHttpClient client = new OkHttpClient();

        // 確保傳遞booking_id，以便伺服器能夠準確定位需要取消的記錄
        String bookingId = getCurrentBookingId();
        
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("case_id", caseId)
                .add("student_id", studentId)
                .add("action", "find_other_tutor");
                
        if (bookingId != null && !bookingId.isEmpty()) {
            formBuilder.add("booking_id", bookingId);
        }
        
        RequestBody formBody = formBuilder.build();
        
        Log.d("FindOtherTutor", "Sending request with case_id: " + caseId + 
                                 ", student_id: " + studentId + 
                                 ", booking_id: " + (bookingId != null ? bookingId : "null"));

        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/cancel_booking_and_find_other_tutor.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FindOtherTutor", "Request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(MatchingCaseDetailStudent.this,
                        "Failed to process your request. Please try again.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseData = response.body().string();
                    Log.d("FindOtherTutor", "Response: " + responseData);
                    
                    JSONObject jsonResponse = new JSONObject(responseData);
                    final boolean success = jsonResponse.getBoolean("success");
                    final String message = jsonResponse.optString("message", "Operation completed");

                    runOnUiThread(() -> {
                        if (success) {
                            new AlertDialog.Builder(MatchingCaseDetailStudent.this)
                                    .setTitle("操作成功")
                                    .setMessage("課程已取消，您現在可以尋找其他導師。")
                                    .setPositiveButton("返回列表", (dialog, which) -> {
                                        setResult(RESULT_OK); // 設置結果代碼，通知前一個頁面刷新
                                        finish(); // 返回前一個頁面
                                    })
                                    .setCancelable(false)
                                    .show();
                        } else {
                            Toast.makeText(MatchingCaseDetailStudent.this,
                                    message, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e("FindOtherTutor", "Error processing response: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(MatchingCaseDetailStudent.this,
                            "Error processing response", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void handleRefundRequest() {
        new AlertDialog.Builder(this)
                .setTitle("Refund Request")
                .setMessage("Your refund request has been submitted. The tutor will be notified, and you'll receive an update soon.")
                .setPositiveButton("OK", null)
                .setCancelable(false)
                .show();
    }


    private void handlePayment() {
        Intent intent = new Intent(this, Payment.class);
        intent.putExtra("case_id", caseId);
        intent.putExtra("lessonFeePerHr", lessonFeePerHr);
        Log.d("CaseMenu", "caseId: " + caseId + " is going to payment, lesson fee per hour: "+ lessonFeePerHr);
        startActivity(intent);
    }
    private void showPendingUI() {
        sendRequestButton.setVisibility(View.GONE);
        timeSlotsRecyclerView.setVisibility(View.GONE);
        paymentCard.setVisibility(View.GONE);
    }

    private void showCompletedUI(String startTime, String endTime, double totalLessonFee) {
        // Show all three cards
        statusText.setVisibility(View.VISIBLE);
        paymentCard.setVisibility(View.VISIBLE);

        // Hide booking-related UI
        sendRequestButton.setVisibility(View.GONE);
        timeSlotsRecyclerView.setVisibility(View.GONE);

        // Update lesson status
        updateLessonStatusChip("completed");

        // Explicitly hide the finish lesson button for completed status
        if (finishLessonButton != null) {
            finishLessonButton.setVisibility(View.GONE);
        }

        // Check payment status (this will show tutor contact if payment is confirmed)
        checkPaymentStatus();
    }

    private void showConflictUI() {
        Log.d("ConflictUI", "Setting up conflict UI");
        
        // Show status text and payment card
        statusText.setVisibility(View.VISIBLE);
        paymentCard.setVisibility(View.VISIBLE);

        // Hide booking-related UI
        sendRequestButton.setVisibility(View.GONE);
        timeSlotsRecyclerView.setVisibility(View.GONE);

        // Update lesson status to conflict
        updateLessonStatusChip("conflict");

        // 強制隱藏更新課程狀態按鈕 - 無條件隱藏
        if (finishLessonButton != null) {
            finishLessonButton.setVisibility(View.GONE);
            finishLessonButton.setEnabled(false);
        }

        // 確保在UI刷新後也保持按鈕隱藏
        runOnUiThread(() -> {
            if (finishLessonButton != null) {
                finishLessonButton.setVisibility(View.GONE);
                finishLessonButton.setEnabled(false);
            }
        });
        
        // 新增：使用Handler延遲確保在UI渲染完成後隱藏按鈕
        new Handler().post(() -> {
            if (finishLessonButton != null) {
                Log.d("ConflictUI", "Post-rendering button hide in showConflictUI");
                finishLessonButton.setVisibility(View.GONE);
                finishLessonButton.setEnabled(false);
            }
        });
        
        // 再次使用延遲執行確保按鈕隱藏（更高保障）
        new Handler().postDelayed(() -> {
            if (finishLessonButton != null) {
                Log.d("ConflictUI", "Delayed post-rendering button hide in showConflictUI");
                finishLessonButton.setVisibility(View.GONE);
                finishLessonButton.setEnabled(false);
            }
        }, 500); // 延遲500毫秒執行

        // 立即檢查支付狀態，這對於顯示支付信息是必要的
        // 注意這裡特意將checkPaymentStatus放在前面，這樣支付UI會先更新
        checkPaymentStatus();
        
        // 顯示導師聯繫方式
        if (tutorId != null && !tutorId.isEmpty()) {
            getTutorContact(tutorId);
        } else {
            Log.w("ConflictUI", "No tutor ID available for contact display");
        }
        
        // 確保支付卡片和其組件可見
        runOnUiThread(() -> {
            if (paymentCard != null) {
                paymentCard.setVisibility(View.VISIBLE);
                
                com.google.android.material.chip.Chip paymentStatusChip = findViewById(R.id.status_payment);
                if (paymentStatusChip != null) {
                    paymentStatusChip.setVisibility(View.VISIBLE);
                }
            }
        });
        
        // 顯示衝突狀態對話框，解釋衝突情況
        showConflictDialog();
    }

    private void showRejectedUI() {
        sendRequestButton.setVisibility(View.VISIBLE);
        timeSlotsRecyclerView.setVisibility(View.VISIBLE);
        paymentCard.setVisibility(View.GONE);
    }

    private void showDefaultUI() {
        sendRequestButton.setVisibility(View.GONE);
        timeSlotsRecyclerView.setVisibility(View.GONE);
        paymentCard.setVisibility(View.GONE);
    }

    private void showAvailableSlots() {
        statusText.setVisibility(View.GONE);
        sendRequestButton.setVisibility(View.VISIBLE);
        timeSlotsRecyclerView.setVisibility(View.VISIBLE);
        paymentCard.setVisibility(View.GONE);
    }

    private void showNoSlotsAvailable() {
        statusText.setText("No time slots available yet.\nPlease check back later.");
        statusText.setVisibility(View.VISIBLE);
        sendRequestButton.setVisibility(View.GONE);
        timeSlotsRecyclerView.setVisibility(View.GONE);
        paymentCard.setVisibility(View.GONE);
    }

    private void requestSelectedSlot() {
        TimeSlot selectedSlot = timeSlotAdapter.getSelectedSlot();
        if (selectedSlot == null) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Booking Confirm")
                .setMessage("Do you want to book this time slot?\n" +
                        selectedSlot.getDateString() + "\n" +
                        selectedSlot.getTimeString())
                .setPositiveButton("Confirm", (dialog, which) -> sendSlotRequest(selectedSlot))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendSlotRequest(TimeSlot slot) {
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("match_id", caseId)
                .add("slot_id", slot.getSlotId())
                .add("student_id", studentId)
                .add("action", "book")
                .build();

        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/post_booking_request.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MatchingCaseDetailStudent.this,
                        "Failed to send booking request", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    final String message = jsonResponse.getString("message");
                    final boolean success = jsonResponse.optBoolean("success", false);

                    runOnUiThread(() -> {
                        Toast.makeText(MatchingCaseDetailStudent.this,
                                message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            checkExistingRequest(); // Reload to show updated status
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MatchingCaseDetailStudent.this,
                            "Error processing response", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private String formatDateTime(String dateTime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(dateTime));
        } catch (Exception e) {
            return dateTime;
        }
    }

    private String formatTime(String dateTime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(dateTime));
        } catch (Exception e) {
            return dateTime;
        }
    }

    @Override
    public void onSubmitIncompleteStatus(String reason) {
        // 確保 reason 不為空
        if (reason == null || reason.trim().isEmpty()) {
            runOnUiThread(() -> {
                Toast.makeText(this, getString(R.string.please_enter_a_reason), Toast.LENGTH_SHORT).show();
            });
            return;
        }
        
        // 使用非空的 reason 調用 submitLessonStatusWithAction
        submitLessonStatusWithAction("incomplete", reason, null);
    }

    private void showWaitingDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.status_submitted_student))
                .setMessage(getString(R.string.waiting_for_other_party))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    private void showBothIncompleteDialog(String reason) {
        dialogManager.showBothIncompleteDialog(reason, new LessonStatusDialogManager.OnLessonStatusDialogCallback() {
            @Override
            public void onLessonComplete() {
                // Not used here
            }

            @Override
            public void onLessonIncomplete(String action) {
                // Handle the chosen action
                submitLessonStatusWithAction("incomplete", reason, action);
            }
        });
    }

    private void showConflictDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.status_conflict_student))
                .setMessage(getString(R.string.student_conflict_message))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    // 對話框關閉後無需執行特殊操作
                    Log.d("ConflictStatus", "User acknowledged conflict status");
                })
                .setCancelable(false)  // 強制用戶確認信息
                .show();
    }

    public String getCaseId() {
        return caseId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getTutorId() {
        return tutorId;
    }

    private void checkAndHideButtonForConflict() {
        Log.d("ConflictUI", "Checking and hiding buttons for conflict status");
        
        // 從SharedPreferences獲取用戶類型
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", Context.MODE_PRIVATE);
        boolean isTutor = sharedPreferences.getBoolean("is_tutor", false);
        Log.d("ConflictUI", "User is tutor: " + isTutor);
        
        // 獲取當前Intent中的衝突狀態
        boolean hasConflictStatus = getIntent().getBooleanExtra("has_conflict_status", false);
        
        // 只有當用戶是導師且沒有衝突狀態時才顯示按鈕
        if (isTutor && !hasConflictStatus) {
            // 導師可以看到finish_lesson_button
            if (finishLessonButton != null) {
                Log.d("ButtonDebug", "Showing finish lesson button for tutor");
                finishLessonButton.setVisibility(View.VISIBLE);
                finishLessonButton.setEnabled(true);
            }
            
            // 導師可以看到interview_button
            if (interviewButton != null) {
                Log.d("ButtonDebug", "Showing interview button for tutor");
                interviewButton.setVisibility(View.VISIBLE);
                interviewButton.setEnabled(true);
            }
        } else {
            // 學生或有衝突狀態時隱藏按鈕
            if (finishLessonButton != null) {
                Log.d("ButtonDebug", "Hiding finish lesson button");
                finishLessonButton.setVisibility(View.GONE);
                finishLessonButton.setEnabled(false);
            }
            
            if (interviewButton != null) {
                Log.d("ButtonDebug", "Hiding interview button");
                interviewButton.setVisibility(View.GONE);
                interviewButton.setEnabled(false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 檢查狀態晶片，如果是衝突狀態，強制隱藏按鈕
        com.google.android.material.chip.Chip statusChip = findViewById(R.id.status_lesson);
        if (statusChip != null && "Conflict".equals(statusChip.getText().toString())) {
            Log.d("ButtonDebug", "Hiding button in onResume due to conflict status");
            if (finishLessonButton != null) {
                finishLessonButton.setVisibility(View.GONE);
                finishLessonButton.setEnabled(false);
            }
        }
        
        // 檢查當前頁面是否處於衝突UI
        if (statusChip != null && "Conflict".equals(statusChip.getText().toString())) {
            Log.d("ButtonDebug", "Conflict status detected in onResume, showing conflict UI");
            showConflictUI();
        }
    }

    private void handleInterview() {
        Intent intent = new Intent(this, TutorVideoIntroduction.class);
        intent.putExtra("match_id", caseId);
        intent.putExtra("member_id", tutorId);
        startActivity(intent);
    }

    private void checkTutorVideoRecord() {
        if (caseId == null || caseId.isEmpty()) {
            Log.e("TutorVideo", "Case ID is null or empty");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        
        RequestBody formBody = new FormBody.Builder()
                .add("match_id", caseId) // 使用当前案例ID
                .build();
        
        String url = "http://" + IPConfig.getIP() + "/FYP/php/check_tutor_video.php";
        Log.d("TutorVideo", "Checking tutor video for match_id: " + caseId);
        
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("TutorVideo", "Failed to check tutor video: " + e.getMessage());
                runOnUiThread(() -> {
                    // 隐藏相关UI元素
                    if (tutorVideoCard != null) {
                        tutorVideoCard.setVisibility(View.GONE);
                    }
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("TutorVideo", "Response: " + responseData);
                
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    try {
                        boolean success = jsonResponse.getBoolean("success");
                        
                        if (success) {
                            try {
                                boolean hasVideo = jsonResponse.getBoolean("has_video");
                                
                                runOnUiThread(() -> {
                                    if (tutorVideoCard == null) {
                                        Log.e("TutorVideo", "Tutor video card view is null");
                                        return;
                                    }
                                    
                                    if (hasVideo) {
                                        try {
                                            // 有视频记录，显示视频信息
                                            JSONObject videoData = jsonResponse.getJSONObject("video_data");
                                            
                                            // 设置视频信息到UI元素
                                            TextView videoMarkText = findViewById(R.id.video_mark);
                                            TextView videoDateText = findViewById(R.id.video_datetime);
                                            TextView videoSummaryText = findViewById(R.id.video_summary);
                                            TextView videoAnalysisText = findViewById(R.id.video_analysis);
                                            
                                            if (videoMarkText != null && videoDateText != null && 
                                                videoSummaryText != null && videoAnalysisText != null) {
                                                
                                                videoMarkText.setText(String.valueOf(videoData.optDouble("video_mark", 0)));
                                                videoDateText.setText(videoData.optString("video_datetime", ""));
                                                videoSummaryText.setText(videoData.optString("video_summary", ""));
                                                videoAnalysisText.setText(videoData.optString("video_analysis", ""));
                                                
                                                // 显示视频卡片
                                                tutorVideoCard.setVisibility(View.VISIBLE);
                                                Log.d("TutorVideo", "Showing tutor video card");
                                            } else {
                                                Log.e("TutorVideo", "One or more video info TextViews are null");
                                            }
                                        } catch (JSONException e) {
                                            Log.e("TutorVideo", "Error getting video data JSON object: " + e.getMessage());
                                            tutorVideoCard.setVisibility(View.GONE);
                                        }
                                    } else {
                                        // 没有视频记录，隐藏视频卡片
                                        tutorVideoCard.setVisibility(View.GONE);
                                        Log.d("TutorVideo", "No video data found, hiding tutor video card");
                                    }
                                });
                            } catch (JSONException e) {
                                Log.e("TutorVideo", "Error getting video data JSON object: " + e.getMessage());
                                tutorVideoCard.setVisibility(View.GONE);
                            }
                        } else {
                            // API返回失败
                            String errorMsg = jsonResponse.optString("message", "Unknown error");
                            Log.e("TutorVideo", "API error: " + errorMsg);
                            runOnUiThread(() -> {
                                if (tutorVideoCard != null) {
                                    tutorVideoCard.setVisibility(View.GONE);
                                }
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("TutorVideo", "Error parsing success status: " + e.getMessage());
                        runOnUiThread(() -> {
                            if (tutorVideoCard != null) {
                                tutorVideoCard.setVisibility(View.GONE);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("TutorVideo", "Error parsing response: " + e.getMessage());
                    runOnUiThread(() -> {
                        if (tutorVideoCard != null) {
                            tutorVideoCard.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }
}
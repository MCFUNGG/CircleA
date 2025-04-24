package com.example.circlea.matching.cases.detail.book;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.example.circlea.data.model.TutorContactResponse;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.circlea.matching.cases.detail.TimeSlot;
import com.example.circlea.matching.cases.detail.TimeSlotAdapter;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.example.circlea.matching.cases.detail.FirstLessonFeedback;

public class StudentBooking extends AppCompatActivity {
    private MaterialToolbar topAppBar;
    private RecyclerView timeSlotsRecyclerView;
    private List<TimeSlot> timeSlots;
    private TimeSlotAdapter timeSlotAdapter;
    private boolean hasUnsavedChanges = false;
    private MaterialTextView bookingTutorName;
    private MaterialTextView bookingDateTime;
    private Chip statusChip;
    private View bookingStatusCard;
    private MaterialButton viewTutorContactButton;
    private MaterialButton submitFeedbackButton;
    private String caseId; // 匹配ID
    private String studentId; // 学生ID
    private String tutorId; // 导师ID
    private String bookingId; // 预约ID
    private static final String TAG = "StudentBooking";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.student_booking);

        // 获取匹配ID
        caseId = getIntent().getStringExtra("case_id");

        // 从SharedPreferences获取学生ID
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", Context.MODE_PRIVATE);
        studentId = sharedPreferences.getString("member_id", "");

        initializeViews();
        setupToolbar();
        setupRecyclerView();

        // 先加载预约状态，然后在回调中检查第一堂课状态
        loadBookingStatus();
    }

    private void initializeViews() {
        topAppBar = findViewById(R.id.topAppBar);
        timeSlotsRecyclerView = findViewById(R.id.time_slots_recycler_view);
        bookingTutorName = findViewById(R.id.booking_tutor_name);
        bookingDateTime = findViewById(R.id.booking_date_time);
        statusChip = findViewById(R.id.status_chip);
        bookingStatusCard = findViewById(R.id.booking_status_card);
        viewTutorContactButton = findViewById(R.id.view_tutor_contact_button);
        submitFeedbackButton = findViewById(R.id.submit_feedback_button);

        timeSlots = new ArrayList<>();

        // 设置查看导师联系方式按钮点击事件
        viewTutorContactButton.setOnClickListener(v -> getTutorContact());

        // 设置提交反馈按钮点击事件
        submitFeedbackButton.setOnClickListener(v -> openFeedbackPage());

        // 初始时隐藏导师联系方式按钮和反馈按钮
        viewTutorContactButton.setVisibility(View.GONE);
        submitFeedbackButton.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        setSupportActionBar(topAppBar);

        // Enable back button and set its appearance
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Setup back button click listener
        topAppBar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        timeSlotAdapter = new TimeSlotAdapter(timeSlots, this, false);
        timeSlotsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        timeSlotsRecyclerView.setAdapter(timeSlotAdapter);
    }

    // 加载预约状态
    private void loadBookingStatus() {
        if (caseId == null || studentId == null) {
            Toast.makeText(this, "无效的预约信息", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();

        // 创建请求体
        RequestBody formBody = new FormBody.Builder()
                .add("match_id", caseId)
                .add("student_id", studentId)
                .build();

        // 构建请求
        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_booking_status.php";
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Network", "Failed to load booking status: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(StudentBooking.this,
                        "无法加载预约状态", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("Response", "Booking status: " + responseData);

                try {
                    JSONObject jsonResponse = new JSONObject(responseData);

                    if (jsonResponse.getBoolean("success")) {
                        JSONObject bookingData = jsonResponse.getJSONObject("booking");
                        String status = bookingData.getString("status");
                        tutorId = bookingData.getString("tutor_id");
                        bookingId = bookingData.getString("booking_id");
                        String tutorName = bookingData.getString("tutor_name");
                        String dateTime = bookingData.getString("formatted_date_time");

                        runOnUiThread(() -> updateBookingUI(status, tutorName, dateTime));

                        // 加载完预约状态后，再检查第一堂课状态
                        checkFirstLessonStatus();
                    } else {
                        runOnUiThread(() -> {
                            bookingStatusCard.setVisibility(View.GONE);
                            try {
                                Toast.makeText(StudentBooking.this,
                                        jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("Parse", "Failed to parse booking status: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(StudentBooking.this,
                            "解析预约状态时出错", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // 更新UI显示预约状态
    private void updateBookingUI(String status, String tutorName, String dateTime) {
        bookingStatusCard.setVisibility(View.VISIBLE);

        // 设置导师名称和预约时间
        bookingTutorName.setText("Tutor: " + tutorName);
        bookingDateTime.setText("Schedule: " + dateTime);

        // 调试日志
        Log.d(TAG, "更新UI状态: status = " + status);

        if ("confirmed".equals(status.toLowerCase())) {
            statusChip.setText(status);
            statusChip.setChipBackgroundColorResource(com.google.android.material.R.color.design_default_color_on_secondary);
            // 如果状态是确认的，显示查看导师联系方式按钮
            viewTutorContactButton.setVisibility(View.VISIBLE);
        } else if ("pending".equals(status.toLowerCase())) {
            statusChip.setText(status);
            statusChip.setChipBackgroundColorResource(com.google.android.material.R.color.design_default_color_on_primary);
            viewTutorContactButton.setVisibility(View.GONE);
        } else if ("completed".equals(status.toLowerCase())) {
            // 如果状态是completed，直接设置为课程已完成
            Log.d(TAG, "状态为completed，设置为课程已完成");

            try {
                // 显示完成状态
                statusChip.setText(R.string.lesson_completed);
                // 使用一个明显的颜色来显示完成状态
                statusChip.setChipBackgroundColorResource(com.google.android.material.R.color.design_default_color_on_secondary);
                statusChip.setTextColor(getResources().getColor(android.R.color.white, null));

                // 修改按钮文本，明确表示这是给反馈的按钮
                submitFeedbackButton.setText(R.string.rate_tutor_experience);
                // 显示反馈按钮
                submitFeedbackButton.setVisibility(View.VISIBLE);

                Log.d(TAG, "课程完成状态设置完毕: 文本=" + getResources().getString(R.string.lesson_completed));
            } catch (Exception e) {
                Log.e(TAG, "更新状态芯片时出错: " + e.getMessage(), e);
            }
        } else {
            statusChip.setText(status);
            statusChip.setChipBackgroundColorResource(com.google.android.material.R.color.design_default_color_error);
            viewTutorContactButton.setVisibility(View.GONE);
        }
    }

    // 检查第一堂课的状态
    private void checkFirstLessonStatus() {
        if (caseId == null || studentId == null || bookingId == null) {
            Log.e(TAG, "Cannot check first lesson status: missing required parameters");
            Log.e(TAG, "caseId: " + caseId + ", studentId: " + studentId + ", bookingId: " + bookingId);
            return;
        }

        OkHttpClient client = new OkHttpClient();

        // 创建请求体
        RequestBody formBody = new FormBody.Builder()
                .add("match_id", caseId)
                .add("student_id", studentId)
                .add("booking_id", bookingId)
                .build();

        // 构建请求
        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_first_lesson_status.php";
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        Log.d(TAG, "Checking first lesson status for booking_id: " + bookingId);

        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to check first lesson status: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "First lesson status response: " + responseData);

                try {
                    JSONObject jsonResponse = new JSONObject(responseData);

                    if (jsonResponse.getBoolean("success")) {
                        JSONObject statusData = jsonResponse.getJSONObject("data");
                        String student_status = statusData.optString("student_status", "");
                        String tutor_status = statusData.optString("tutor_status", "");
                        String bothCompleted = statusData.optString("both_completed", "false");
                        String feedbackSubmitted = statusData.optString("feedback_submitted", "false");

                        Log.d(TAG, "详细课程状态: student_status = " + student_status);
                        Log.d(TAG, "详细课程状态: tutor_status = " + tutor_status);
                        Log.d(TAG, "详细课程状态: both_completed = " + bothCompleted);
                        Log.d(TAG, "详细课程状态: feedback_submitted = " + feedbackSubmitted);

                        runOnUiThread(() -> {
                            // 如果双方都已完成并且学生还没有提交反馈
                            if ("true".equals(bothCompleted) && "false".equals(feedbackSubmitted)) {
                                Log.d(TAG, "设置UI为课程已完成状态");
                                try {
                                    // 显示完成状态
                                    statusChip.setText(R.string.lesson_completed);
                                    // 使用一个更明显的颜色（绿色）来显示完成状态
                                    statusChip.setChipBackgroundColorResource(com.google.android.material.R.color.design_default_color_on_secondary);
                                    statusChip.setTextColor(getResources().getColor(android.R.color.white, null));

                                    // 修改按钮文本，明确表示这是给反馈的按钮
                                    submitFeedbackButton.setText(R.string.rate_tutor_experience);
                                    // 显示反馈按钮
                                    submitFeedbackButton.setVisibility(View.VISIBLE);

                                    Log.d(TAG, "课程完成状态设置完毕: 文本=" + getResources().getString(R.string.lesson_completed));
                                } catch (Exception e) {
                                    Log.e(TAG, "更新状态芯片时出错: " + e.getMessage(), e);
                                }
                            } else {
                                Log.d(TAG, "不设置UI为课程已完成状态，条件不满足");
                                Log.d(TAG, "bothCompleted条件: " + "true".equals(bothCompleted));
                                Log.d(TAG, "feedbackSubmitted条件: " + "false".equals(feedbackSubmitted));
                            }
                        });
                    } else {
                        Log.e(TAG, "API返回success=false: " + jsonResponse.optString("message", "No message"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析课程状态响应时出错: " + e.getMessage(), e);
                }
            }
        });
    }

    // 获取导师联系方式
    private void getTutorContact() {
        if (tutorId == null) {
            Toast.makeText(this, getString(R.string.error_loading_tutor_contact), Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();

        // 创建请求体
        RequestBody formBody = new FormBody.Builder()
                .add("tutor_id", tutorId)
                .build();

        // 构建请求
        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_tutor_contact.php";
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Network", "Failed to load tutor contact: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(StudentBooking.this,
                        getString(R.string.error_loading_tutor_contact), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("Response", "Tutor contact: " + responseData);

                try {
                    Gson gson = new Gson();
                    TutorContactResponse contactResponse = gson.fromJson(responseData, TutorContactResponse.class);

                    runOnUiThread(() -> {
                        if (contactResponse.isSuccess()) {
                            // 显示导师联系方式对话框
                            showTutorContactDialog(contactResponse.getTutor());
                        } else {
                            // 显示错误信息
                            Toast.makeText(StudentBooking.this, contactResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e("Parse", "Failed to parse tutor contact: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(StudentBooking.this,
                            getString(R.string.error_loading_tutor_contact), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // 显示导师联系方式对话框
    private void showTutorContactDialog(TutorContactResponse.TutorContact tutor) {
        if (tutor == null) {
            Toast.makeText(this, getString(R.string.tutor_contact_not_available), Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建对话框视图
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_contact_info, null);
        TextView nameTextView = dialogView.findViewById(R.id.contact_name);
        TextView phoneTextView = dialogView.findViewById(R.id.contact_phone);
        TextView emailTextView = dialogView.findViewById(R.id.contact_email);

        // 设置联系信息
        nameTextView.setText(getString(R.string.contact_name, tutor.getName()));
        phoneTextView.setText(getString(R.string.contact_phone, tutor.getPhone()));
        emailTextView.setText(getString(R.string.contact_email, tutor.getEmail()));

        // 显示对话框
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.tutor_contact_info))
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    // 打开反馈页面
    private void openFeedbackPage() {
        if (caseId == null || studentId == null || tutorId == null) {
            Toast.makeText(this, R.string.feedback_error, Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建Intent跳转到FirstLessonFeedback页面
        Intent intent = new Intent(this, FirstLessonFeedback.class);
        // 传递必要参数
        intent.putExtra("case_id", caseId);
        intent.putExtra("student_id", studentId);
        intent.putExtra("tutor_id", tutorId);
        // 启动页面
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
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
} 
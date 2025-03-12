    package com.example.circlea.matching.cases.detail;

    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.content.res.ColorStateList;
    import android.graphics.Color;
    import android.os.Bundle;
    import android.text.InputType;
    import android.util.Log;
    import android.view.View;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.LinearLayout;
    import android.widget.RadioButton;
    import android.widget.RadioGroup;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.annotation.Nullable;
    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.content.ContextCompat;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.example.circlea.IPConfig;
    import com.example.circlea.R;
    import com.google.android.material.appbar.MaterialToolbar;
    import com.google.android.material.button.MaterialButton;
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

    public class MatchingCaseDetailStudent extends AppCompatActivity {

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

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.matching_case_detail_student);

            // Get student ID from SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("CircleA", Context.MODE_PRIVATE);
            studentId = sharedPreferences.getString("member_id", "");

            // Get case ID from intent
            caseId = getIntent().getStringExtra("case_id");
            tutorId = getIntent().getStringExtra("tutor_id");
            lessonFeePerHr = Double.valueOf(getIntent().getStringExtra("lessonFee"));

            initializeViews();
            checkExistingRequest();
        }

        private void initializeViews() {
            MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
            statusText = findViewById(R.id.status_text);
            sendRequestButton = findViewById(R.id.send_request_button);
            timeSlotsRecyclerView = findViewById(R.id.time_slots_recycler_view);
            paymentCard = findViewById(R.id.payment_card);
            paymentButton = findViewById(R.id.payment_button);
            finishLessonButton = findViewById(R.id.finish_lesson_button);

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
        }

        private void checkExistingRequest() {
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
                            runOnUiThread(() -> showStudentRequestStatus(requestData));
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

                // Calculate duration and fee
                long durationInMinutes = calculateDurationInMinutes(startTime, endTime);
                double totalLessonFee = calculateLessonFee(durationInMinutes, lessonFeePerHr);

                String statusMessage;
                switch (status) {
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
                    default:
                        statusMessage = "Status of your booking request:";
                        showDefaultUI();
                        break;
                }

                statusText.setText(statusMessage + "\n" +
                        formatDateTime(startTime) + " - " + formatTime(endTime));
                statusText.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e("TimeSlots", "Error showing request status: " + e.getMessage());
            }
        }

        private void checkLessonStatus() {
            OkHttpClient client = new OkHttpClient();

            RequestBody formBody = new FormBody.Builder()
                    .add("match_id", caseId)
                    .add("student_id", studentId)
                    .build();

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
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject lessonData = jsonResponse.getJSONObject("data");
                            String lessonStatus = lessonData.getString("status");
                            runOnUiThread(() -> updateLessonStatusChip(lessonStatus));
                        }
                    } catch (Exception e) {
                        Log.e("Lesson", "Error processing lesson status: " + e.getMessage());
                    }
                }
            });
        }

        // Add this method to update the lesson status chip
        private void updateLessonStatusChip(String status) {
            com.google.android.material.chip.Chip statusChip = findViewById(R.id.status_lesson);
            MaterialButton finishLessonButton = findViewById(R.id.finish_lesson_button);

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
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                long diff = sdf.parse(endTime).getTime() - sdf.parse(startTime).getTime();
                return diff / (60 * 1000); // Convert to minutes
            } catch (Exception e) {
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
                startActivity(intent);
            });
        }

        private void checkPaymentStatus() {
            OkHttpClient client = new OkHttpClient();

            RequestBody formBody = new FormBody.Builder()
                    .add("match_id", caseId)
                    .add("student_id", studentId)
                    .build();

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
            com.google.android.material.chip.Chip statusChip = findViewById(R.id.status_payment);
            MaterialButton paymentButton = findViewById(R.id.payment_button);

            if (statusChip != null) {
                statusChip.setText(statusText);

                int backgroundColor;
                int textColor;

                switch (status.toLowerCase()) {
                    case "confirmed":
                        backgroundColor = ContextCompat.getColor(this, R.color.success_container);
                        textColor = ContextCompat.getColor(this, R.color.success);
                        paymentButton.setVisibility(View.GONE);
                        finishLessonButton.setVisibility(View.VISIBLE);
                        finishLessonButton.setEnabled(true);
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

        // Add handleFinishLesson method if not already present
        private void handleFinishLesson() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_lesson_status, null);

            MaterialButton completeButton = view.findViewById(R.id.btn_lesson_complete);
            MaterialButton incompleteButton = view.findViewById(R.id.btn_lesson_incomplete);

            AlertDialog dialog = builder.setTitle("First Lesson Status")
                    .setView(view)
                    .setNegativeButton("Cancel", null)
                    .create();

            completeButton.setOnClickListener(v -> {
                dialog.dismiss();
                new AlertDialog.Builder(this)
                        .setTitle("Confirm Completion")
                        .setMessage("By marking the lesson as completed, you'll proceed to the feedback process. This action cannot be undone.\n\nDo you want to continue?")
                        .setPositiveButton("Continue", (dialogInterface, i) -> {
                            submitLessonStatus("completed", null);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            incompleteButton.setOnClickListener(v -> {
                dialog.dismiss();
                showIncompleteReasonDialog();
            });

            dialog.show();
        }

        private void showIncompleteReasonDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_incomplete_reason, null);

            AlertDialog dialog = builder.setTitle("Reason for Incomplete Lesson")
                    .setView(view)
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.setOnShowListener(dialogInterface -> {
                TextView titleView = dialog.findViewById(android.R.id.title);
                if (titleView != null) {
                    titleView.setTextAppearance(R.style.DialogTitleStyle);
                }
            });
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
                        submitLessonStatus("incomplete", button.getText().toString());
                    }
                });
            }

            dialog.show();
        }

        private void showCustomReasonDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_custom_reason, null);
            EditText input = view.findViewById(R.id.edit_text_reason);

            builder.setTitle("Specify Reason")
                    .setView(view)
                    .setPositiveButton("Submit", (dialog, which) -> {
                        String customReason = input.getText().toString().trim();
                        if (!customReason.isEmpty()) {
                            submitLessonStatus("incomplete", customReason);
                        } else {
                            Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void submitLessonStatus(String status, String reason) {
            OkHttpClient client = new OkHttpClient();

            FormBody.Builder formBuilder = new FormBody.Builder()
                    .add("case_id", caseId)
                    .add("student_id", studentId)
                    .add("status", status);

            if (reason != null) {
                formBuilder.add("reason", reason);
            }

            RequestBody formBody = formBuilder.build();

            Request request = new Request.Builder()
                    .url("http://" + IPConfig.getIP() + "/FYP/php/update_first_lesson_status.php")
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(MatchingCaseDetailStudent.this,
                                "Failed to update lesson status", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        final boolean success = jsonResponse.getBoolean("success");
                        final String message = jsonResponse.optString("message", "Status updated successfully");

                        runOnUiThread(() -> {
                            Toast.makeText(MatchingCaseDetailStudent.this, message, Toast.LENGTH_SHORT).show();
                            if (success) {
                                // Disable the finish lesson button after successful submission
                                finishLessonButton.setEnabled(false);
                                finishLessonButton.setText("Lesson Status Updated");

                                if (status.equals("completed")) {
                                    Intent intent = new Intent(MatchingCaseDetailStudent.this,
                                            FirstLessonFeedback.class);
                                    intent.putExtra("case_id", caseId);
                                    intent.putExtra("student_id", studentId);
                                    intent.putExtra("tutor_id", tutorId);
                                    startActivity(intent);
                                    finish(); // Close current activity
                                }
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(MatchingCaseDetailStudent.this,
                                    "Error processing response", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
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
                    .setTitle("Confirm Request")
                    .setMessage("Do you want to request this time slot?\n" +
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
    }
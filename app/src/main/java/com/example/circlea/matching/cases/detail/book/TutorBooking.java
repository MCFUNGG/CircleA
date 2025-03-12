package com.example.circlea.matching.cases.detail.book;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.example.circlea.matching.cases.detail.TimeSlot;
import com.example.circlea.matching.cases.detail.TimeSlotAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.booking_tutor);

        // Get tutor ID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", Context.MODE_PRIVATE);
        tutorId = sharedPreferences.getString("member_id", "");

        // Get case ID from intent
        caseId = getIntent().getStringExtra("case_id");

        initializeViews();
        loadExistingTimeSlots();
        getStudentBookingRequest();
    }

    private void initializeViews() {
        MaterialButton selectDateButton = findViewById(R.id.select_date_button);
        ExtendedFloatingActionButton saveSlotsButton = findViewById(R.id.save_slots_button);
        timeSlotsRecyclerView = findViewById(R.id.time_slots_recycler_view);
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        bookingRequestsRecyclerView = findViewById(R.id.booking_requests_recycler_view);
        bookingRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookingRequests = new ArrayList<>();
        bookingRequestAdapter = new BookingRequestAdapter(bookingRequests, this);
        bookingRequestsRecyclerView.setAdapter(bookingRequestAdapter);

        // Setup RecyclerView
        timeSlotsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        timeSlots = new ArrayList<>();
        timeSlotAdapter = new TimeSlotAdapter(timeSlots, this, true);
        timeSlotsRecyclerView.setAdapter(timeSlotAdapter);

        selectedDate = Calendar.getInstance();

        // Setup click listeners
        selectDateButton.setOnClickListener(v -> showDatePicker());
        saveSlotsButton.setOnClickListener(v -> saveAvailableSlots());

        // Setup toolbar
        topAppBar.setNavigationOnClickListener(v -> finish());
    }


    private void getStudentBookingRequest() {
        OkHttpClient client = new OkHttpClient();

        Log.d("BookingRequests", "Loading booking requests for match_id: " + caseId);

        RequestBody formBody = new FormBody.Builder()
                .add("match_id", caseId)
                .add("tutor_id", tutorId)
                .build();

        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_booking_requests.php";
        Log.d("BookingRequests", "Request URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("BookingRequests", "Network failure: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                        "Failed to load booking requests", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("BookingRequests", "Raw response: " + responseData);

                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    if (jsonResponse.getBoolean("success")) {
                        JSONArray requestsArray = jsonResponse.getJSONArray("requests");
                        processBookingRequests(requestsArray);
                    } else {
                        String message = jsonResponse.getString("message");
                        runOnUiThread(() -> {
                            Toast.makeText(TutorBooking.this, message, Toast.LENGTH_SHORT).show();
                            // Show "no requests" message
                            findViewById(R.id.no_requests_text).setVisibility(View.VISIBLE);
                        });
                    }
                } catch (Exception e) {
                    Log.e("BookingRequests", "Error processing response: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                            "Error loading booking requests", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void processBookingRequests(JSONArray requestsArray) {
        runOnUiThread(() -> {
            try {
                bookingRequests.clear();

                // Hide/show no requests message
                findViewById(R.id.no_requests_text).setVisibility(
                        requestsArray.length() == 0 ? View.VISIBLE : View.GONE
                );

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                for (int i = 0; i < requestsArray.length(); i++) {
                    JSONObject requestObject = requestsArray.getJSONObject(i);

                    // Parse start and end times
                    Calendar startTime = Calendar.getInstance();
                    startTime.setTime(sdf.parse(requestObject.getString("start_time")));

                    Calendar endTime = Calendar.getInstance();
                    endTime.setTime(sdf.parse(requestObject.getString("end_time")));

                    BookingRequest request = new BookingRequest(
                            requestObject.getString("slot_id"),  // Using slot_id as request_id
                            requestObject.getString("student_id"),
                            requestObject.getString("student_name"),
                            startTime,
                            endTime,
                            requestObject.getString("status")
                    );

                    bookingRequests.add(request);
                }

                // Update the adapter
                bookingRequestAdapter.notifyDataSetChanged();

                // Show/hide the booking requests section
                View bookingRequestsCard = findViewById(R.id.booking_requests_card);
                bookingRequestsCard.setVisibility(bookingRequests.isEmpty() ? View.GONE : View.VISIBLE);

            } catch (Exception e) {
                Log.e("BookingRequests", "Error processing booking requests: " + e.getMessage());
                Toast.makeText(TutorBooking.this,
                        "Error processing booking requests", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onAcceptBooking(BookingRequest request) {
        new AlertDialog.Builder(this)
                .setTitle("Accept Booking")
                .setMessage("Are you sure you want to accept this booking request?")
                .setPositiveButton("Accept", (dialog, which) ->
                        handleBookingResponse(request.getRequestId(), true))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRejectBooking(BookingRequest request) {
        new AlertDialog.Builder(this)
                .setTitle("Reject Booking")
                .setMessage("Are you sure you want to reject this booking request?")
                .setPositiveButton("Reject", (dialog, which) ->
                        handleBookingResponse(request.getRequestId(), false))
                .setNegativeButton("Cancel", null)
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

        if (!timeSlot.isEditable()) {
            Toast.makeText(context, "This time slot cannot be edited", Toast.LENGTH_SHORT).show();
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

        builder.setPositiveButton("Save", (dialog, which) -> {
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

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void loadExistingTimeSlots() {
        OkHttpClient client = new OkHttpClient();

        Log.d("TimeSlots", "Loading slots with match_id: " + caseId);

        RequestBody formBody = new FormBody.Builder()
                .add("match_id", caseId)
                .add("is_tutor", "true")
                .build();

        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_time_slot.php";
        Log.d("TimeSlots", "Request URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("TimeSlots", "Network failure: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                        "Failed to load time slots", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("TimeSlots", "Raw response: " + responseData);

                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    if (jsonResponse.getBoolean("success")) {
                        JSONArray slotsArray = jsonResponse.getJSONArray("slots");
                        processTimeSlots(slotsArray);

                        // Process pending requests if they exist
                        if (jsonResponse.has("requests")) {
                            JSONArray requestsArray = jsonResponse.getJSONArray("requests");
                            processBookingRequests(requestsArray);
                        }
                    } else {
                        String message = jsonResponse.getString("message");
                        runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                                message, Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    Log.e("TimeSlots", "Error processing response: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                            "Error loading time slots", Toast.LENGTH_SHORT).show());
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
                        "Error processing time slots", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAvailableSlots() {
        if (timeSlots.isEmpty()) {
            Toast.makeText(this, "Please add at least one time slot", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
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

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

                String date = dateFormat.format(slot.getStartTime().getTime());
                String startTime = timeFormat.format(slot.getStartTime().getTime());
                String endTime = timeFormat.format(slot.getEndTime().getTime());

                slotObject.put("date", date);
                slotObject.put("start_time", startTime);
                slotObject.put("end_time", endTime);

                if (slot.getSlotId() != null && !slot.getSlotId().isEmpty()) {
                    slotObject.put("slot_id", slot.getSlotId());
                    slotObject.put("action", "update");
                } else {
                    slotObject.put("action", "create");
                }

                slotsArray.put(slotObject);
            }

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
                    runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                            "Failed to save time slots", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        final boolean success = jsonResponse.optBoolean("success", false);
                        final String message = jsonResponse.getString("message");

                        runOnUiThread(() -> {
                            Toast.makeText(TutorBooking.this,
                                    message, Toast.LENGTH_SHORT).show();
                            if (success) {
                                loadExistingTimeSlots();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                                "Error processing server response", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error preparing data", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidTimeSlot(TimeSlot slot) {
        Calendar now = Calendar.getInstance();

        if (slot.getStartTime().after(slot.getEndTime())) {
            Toast.makeText(this, "Start time must be before end time", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (slot.getStartTime().before(now)) {
            Toast.makeText(this, "Cannot create time slots in the past", Toast.LENGTH_SHORT).show();
            return false;
        }

        long durationInMinutes = (slot.getEndTime().getTimeInMillis() -
                slot.getStartTime().getTimeInMillis()) / (60 * 1000);
        if (durationInMinutes < 30) {
            Toast.makeText(this, "Time slot must be at least 30 minutes", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isValidTimeRange(Calendar startTime, Calendar endTime) {
        if (startTime.after(endTime)) {
            Toast.makeText(this, "Start time must be before end time", Toast.LENGTH_SHORT).show();
            return false;
        }

        long durationInMinutes = (endTime.getTimeInMillis() - startTime.getTimeInMillis()) / (60 * 1000);
        if (durationInMinutes < 30) {
            Toast.makeText(this, "Time slot must be at least 30 minutes", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void handleBookingResponse(String bookingId, boolean accept) {
        OkHttpClient client = new OkHttpClient();

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
                runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                        "Failed to process response", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    final String message = jsonResponse.getString("message");
                    runOnUiThread(() -> {
                        Toast.makeText(TutorBooking.this, message, Toast.LENGTH_SHORT).show();
                        if (jsonResponse.optBoolean("success", false)) {
                            loadExistingTimeSlots(); // 重新加载时间槽
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(TutorBooking.this,
                            "Error processing response", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showResponseDialog(TimeSlot slot) {
        if (!slot.getStatus().equals("pending")) {
            Toast.makeText(this, "This slot is not pending for response", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = String.format("Student request for time slot:\n%s\n%s - %s",
                slot.getDateString(),
                new SimpleDateFormat("HH:mm", Locale.getDefault()).format(slot.getStartTime().getTime()),
                new SimpleDateFormat("HH:mm", Locale.getDefault()).format(slot.getEndTime().getTime()));

        new AlertDialog.Builder(this)
                .setTitle("Booking Request")
                .setMessage(message)
                .setPositiveButton("Accept", (dialog, which) ->
                        handleBookingResponse(slot.getSlotId(), true))
                .setNegativeButton("Reject", (dialog, which) ->
                        handleBookingResponse(slot.getSlotId(), false))
                .setNeutralButton("Cancel", null)
                .show();
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
}


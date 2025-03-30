    package com.example.circlea.matching.cases.detail.book;

    import android.app.DatePickerDialog;
    import android.content.Context;
    import android.content.SharedPreferences;
    import android.content.res.ColorStateList;
    import android.os.Bundle;
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

    import com.example.circlea.IPConfig;
    import com.example.circlea.R;
    import com.example.circlea.matching.cases.detail.TimeSlot;
    import com.example.circlea.matching.cases.detail.TimeSlotAdapter;
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
            getStudentBookingRequest();
        }

        private void initializeViews() {
            topAppBar = findViewById(R.id.topAppBar);
            setSupportActionBar(topAppBar);

            MaterialButton selectDateButton = findViewById(R.id.select_date_button);
            ExtendedFloatingActionButton saveSlotsButton = findViewById(R.id.save_slots_button);
            timeSlotsRecyclerView = findViewById(R.id.time_slots_recycler_view);
            MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
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

            // Setup RecyclerView
            timeSlotsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            timeSlots = new ArrayList<>();
            timeSlotAdapter = new TimeSlotAdapter(timeSlots, this, true);
            timeSlotsRecyclerView.setAdapter(timeSlotAdapter);

            availableTimeSlotsTitle = findViewById(R.id.available_time_slots_title);
            selectedDate = Calendar.getInstance();

            topAppBar = findViewById(R.id.topAppBar);  // Initialize toolbar reference
            bookingRequestsRecyclerView = findViewById(R.id.booking_requests_recycler_view);

            // Setup click listeners
            selectDateButton.setOnClickListener(v -> showDatePicker());
            saveSlotsButton.setOnClickListener(v -> saveAvailableSlots());
            MaterialButton updateLessonStatusButton = findViewById(R.id.update_lesson_status_button);
            updateLessonStatusButton.setOnClickListener(v -> showUpdateLessonStatusDialog());
            // Setup toolbar
            topAppBar.setNavigationOnClickListener(v -> finish());
        }

        private void getStudentBookingRequest() {
            OkHttpClient client = new OkHttpClient();

            Log.d("Request", "Loading booking requests for match_id: " + caseId);

            RequestBody formBody = new FormBody.Builder()
                    .add("match_id", caseId)
                    .add("tutor_id", tutorId)
                    .build();

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
                            "Failed to load booking requests", Toast.LENGTH_SHORT).show());
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
                                "Error loading booking requests", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        }

        private void processBookingRequests(JSONArray requestsArray) {
            runOnUiThread(() -> {
                try {
                    bookingRequests.clear();
                    boolean hasConfirmedBooking = false;
                    BookingRequest confirmedRequest = null;

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                    for (int i = 0; i < requestsArray.length(); i++) {
                        JSONObject requestObject = requestsArray.getJSONObject(i);
                        String status = requestObject.getString("status");

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
                        if (!hasConfirmedBooking) {
                            loadExistingTimeSlots();
                        }
                        if (status.equals("confirmed")) {
                            hasConfirmedBooking = true;
                            confirmedRequest = request;
                        } else if (status.equals("pending")) {
                            bookingRequests.add(request);
                        }
                    }

                    // Update UI based on booking status
                    if (hasConfirmedBooking && confirmedRequest != null) {
                        // Show booking detail and hide everything else
                        showBookingDetail(confirmedRequest);

                        // Set toolbar title
                        topAppBar.setTitle("Booking Detail");

                        // Hide "Available Time Slots" title and section
                        availableTimeSlotsTitle.setVisibility(View.GONE);
                        findViewById(R.id.add_slot_card).setVisibility(View.GONE);
                        timeSlotsRecyclerView.setVisibility(View.GONE);

                        // Hide booking requests section
                        findViewById(R.id.booking_requests_title).setVisibility(View.GONE);
                        bookingRequestsCard.setVisibility(View.GONE);

                        // Hide save button
                        findViewById(R.id.save_slots_button).setVisibility(View.GONE);

                        // Show booking detail section
                        bookingDetailCard.setVisibility(View.VISIBLE);
                        findViewById(R.id.update_lesson_status_button).setVisibility(View.VISIBLE);

                        // Hide dividers
                        findViewById(R.id.booking_requests_divider).setVisibility(View.GONE);

                    } else {
                        // Show time slots and requests, hide booking detail
                        topAppBar.setTitle("Set Available Time Slots");
                        findViewById(R.id.update_lesson_status_button).setVisibility(View.GONE);
                        // Show time slots section
                        availableTimeSlotsTitle.setVisibility(View.VISIBLE);
                        findViewById(R.id.add_slot_card).setVisibility(View.VISIBLE);
                        timeSlotsRecyclerView.setVisibility(View.VISIBLE);
                        findViewById(R.id.save_slots_button).setVisibility(View.VISIBLE);

                        // Hide booking detail section
                        bookingDetailCard.setVisibility(View.GONE);

                        // Show booking requests section
                        findViewById(R.id.booking_requests_title).setVisibility(View.VISIBLE);
                        bookingRequestsCard.setVisibility(
                                bookingRequests.isEmpty() ? View.GONE : View.VISIBLE
                        );
                        findViewById(R.id.no_requests_text).setVisibility(
                                bookingRequests.isEmpty() ? View.VISIBLE : View.GONE
                        );

                        // Show all dividers
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
                            "Error processing booking requests", Toast.LENGTH_SHORT).show();
                }
            });
        }
        private void showBookingDetail(BookingRequest request) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            bookingStudentName.setText("Student: " + request.getStudentName());
            bookingDateTime.setText(String.format("Date: %s\nTime: %s - %s",
                    dateFormat.format(request.getStartTime().getTime()),
                    timeFormat.format(request.getStartTime().getTime()),
                    timeFormat.format(request.getEndTime().getTime())
            ));

            // Set status chip appearance
            bookingStatus.setText(request.getStatus());
            int backgroundColor = ContextCompat.getColor(this, R.color.success_container);
            int textColor = ContextCompat.getColor(this, R.color.success);
            bookingStatus.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
            bookingStatus.setTextColor(textColor);

            bookingDetailCard.setVisibility(View.VISIBLE);
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
                            "Failed to load time slots", Toast.LENGTH_SHORT).show());
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
                                "Failed to save time slots", Toast.LENGTH_SHORT).show());
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
                                    "Error processing server response", Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("TimeSlots", "Error preparing data: " + e.getMessage(), e);
                Toast.makeText(this, "Error preparing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            "Failed to process response", Toast.LENGTH_SHORT).show());
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
        private void showUpdateLessonStatusDialog() {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lesson_status, null);

            MaterialButton completeButton = dialogView.findViewById(R.id.btn_lesson_complete);
            MaterialButton incompleteButton = dialogView.findViewById(R.id.btn_lesson_incomplete);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("First Lesson Status")
                    .setView(dialogView)
                    .setNegativeButton("Cancel", null)
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
                    .setTitle("Confirm Completion")
                    .setMessage("By marking the lesson as completed, you'll proceed to the feedback process. This action cannot be undone.\n\nDo you want to continue?")
                    .setPositiveButton("Continue", (dialogInterface, i) -> {
                        submitLessonStatusWithAction("completed", null, null);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void showIncompleteReasonDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_incomplete_reason, null);

            AlertDialog dialog = builder.setTitle("Reason for Incomplete Lesson")
                    .setView(view)
                    .setNegativeButton("Cancel", null)
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

            builder.setTitle("Specify Reason")
                    .setView(view)
                    .setPositiveButton("Submit", (dialog, which) -> {
                        String customReason = input.getText().toString().trim();
                        if (!customReason.isEmpty()) {
                            showRebookOrRefundDialog(customReason);
                        } else {
                            Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void showRebookOrRefundDialog(String reason) {
            // Submit directly for tutor with rebook action
            submitLessonStatusWithAction("incomplete", reason, "rebook");

            // Show a simple notification dialog
            new AlertDialog.Builder(this)
                    .setTitle("Provide New Time Slot")
                    .setMessage("Please provide new available time slots for rebooking.")
                    .setPositiveButton("OK", (dialog, which) -> {
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
                        if (message.contains("Student marked lesson as incomplete")) {
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
                Toast.makeText(this, "Error processing response", Toast.LENGTH_SHORT).show();
            }
        }

        private void showWaitingDialog() {
            new AlertDialog.Builder(this)
                    .setTitle("Status Submitted")
                    .setMessage("Your status has been submitted. Waiting for the student's response.")
                    .setPositiveButton("OK", null)
                    .show();
        }

        private void showBothIncompleteDialog(String reason) {
            new AlertDialog.Builder(this)
                    .setTitle("Status Matched")
                    .setMessage("Both you and the student have marked the lesson as incomplete. The system will process refund and rebooking.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        showRebookOrRefundDialog(reason);
                    })
                    .show();
        }


        private void showConflictDialog(boolean tutorMarkedComplete) {
            String message = tutorMarkedComplete ?
                    "您標記課程已完成，但學生標記未完成。管理員將於 1-3 個工作日內審核,透過whatsapp與你聯繫,請提供詳細說明及證明文件。" :
                    "您確認課程未完成，但對方確認已完成。管理員將於 1-3 個工作日內審核,透過whatsapp與你聯繫,請提供詳細說明及證明文件。";

            new AlertDialog.Builder(this)
                    .setTitle("Status Conflict")
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> {
                        // TODO: 实现证据提交
                        Toast.makeText(this, "Evidence submission to be implemented",
                                Toast.LENGTH_SHORT).show();
                    })
                    .show();
        }


        private void showCompletedDialog(String message) {
            new AlertDialog.Builder(this)
                    .setTitle("Lesson Completed")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
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
                            "Failed to update lesson status", Toast.LENGTH_SHORT).show());
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
                                "Error processing response: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
    }


package com.example.circlea.application;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class ParentApplicationFillDetail extends AppCompatActivity {

    private Spinner studentLevelSpinner;
    private Button submitButton;
    private EditText feePerHr;
    private RadioGroup radioGroup;
    private EditText descriptionInput;
    private LinearLayout districtContainer;
    private LinearLayout subjectContainer, dateContainer;
    private ArrayList<String> selectedDates; // To hold selected dates and their times

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_application_step1);

        // Initialize views
        studentLevelSpinner = findViewById(R.id.student_level_spinner);
        submitButton = findViewById(R.id.submit_button);
        feePerHr = findViewById(R.id.fee_input);
        radioGroup = findViewById(R.id.radio_group);
        descriptionInput = findViewById(R.id.description_input);
        districtContainer = findViewById(R.id.district_container);
        subjectContainer = findViewById(R.id.subject_container);
        dateContainer = findViewById(R.id.date_container);

        selectedDates = new ArrayList<>(); // Initialize the list for selected dates

        // Load data and setup spinners
        loadStudentLevelsAndSubjects();
        setupDateCheckBoxes();

        // Set up button listeners
        submitButton.setOnClickListener(v -> onSubmitButtonClick());

        Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> finish());
    }

    private void loadStudentLevelsAndSubjects() {
        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2/FYP/php/get_studentLevels&Subject&District.php";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("LoadLevelsRequest", "Request failed: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(ParentApplicationFillDetail.this, "Error fetching data", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String serverResponse = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(serverResponse);
                        JSONArray levelsArray = jsonResponse.getJSONArray("levels");
                        JSONArray subjectsArray = jsonResponse.getJSONArray("subjects");
                        JSONArray districtsArray = jsonResponse.getJSONArray("districts");

                        ArrayList<String> levels = new ArrayList<>();
                        ArrayList<String> subjects = new ArrayList<>();
                        ArrayList<String> districts = new ArrayList<>();
                        ArrayList<String> levelIds = new ArrayList<>();
                        ArrayList<String> subjectIds = new ArrayList<>();
                        ArrayList<String> districtIds = new ArrayList<>();

                        for (int i = 0; i < levelsArray.length(); i++) {
                            JSONObject levelObj = levelsArray.getJSONObject(i);
                            levels.add(levelObj.getString("class_level_name"));
                            levelIds.add(levelObj.getString("class_level_id"));
                        }

                        for (int i = 0; i < subjectsArray.length(); i++) {
                            JSONObject subjectObj = subjectsArray.getJSONObject(i);
                            subjects.add(subjectObj.getString("subject_name"));
                            subjectIds.add(subjectObj.getString("subject_id"));
                        }

                        for (int i = 0; i < districtsArray.length(); i++) {
                            JSONObject districtObj = districtsArray.getJSONObject(i);
                            districts.add(districtObj.getString("district_name"));
                            districtIds.add(districtObj.getString("district_id"));
                        }

                        runOnUiThread(() -> {
                            studentLevelSpinner.setAdapter(new ArrayAdapter<>(ParentApplicationFillDetail.this, android.R.layout.simple_spinner_item, levels));
                            setupSubjectCheckBoxes(subjects, subjectIds);
                            setupDistrictCheckBoxes(districts, districtIds);

                            studentLevelSpinner.setTag(levelIds);
                        });
                    } catch (JSONException e) {
                        Log.e("LoadLevelsRequest", "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() ->
                                Toast.makeText(ParentApplicationFillDetail.this, "Error processing response", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("LoadLevelsRequest", "Request failed with response code: " + response.code());
                    runOnUiThread(() ->
                            Toast.makeText(ParentApplicationFillDetail.this, "Failed to fetch levels and subjects", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void setupDistrictCheckBoxes(ArrayList<String> districts, ArrayList<String> districtIds) {
        districtContainer.removeAllViews();

        for (int i = 0; i < districts.size(); i++) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(districts.get(i));
            checkBox.setTag(districtIds.get(i));
            districtContainer.addView(checkBox);
        }
    }

    private void setupSubjectCheckBoxes(ArrayList<String> subjects, ArrayList<String> subjectIds) {
        subjectContainer.removeAllViews();

        for (int i = 0; i < subjects.size(); i++) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(subjects.get(i));
            checkBox.setTag(subjectIds.get(i));
            subjectContainer.addView(checkBox);
        }
    }

    private void setupDateCheckBoxes() {
        dateContainer.removeAllViews();

        String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (String day : daysOfWeek) {
            LinearLayout dayLayout = new LinearLayout(this);
            dayLayout.setOrientation(LinearLayout.HORIZONTAL);
            dayLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(day);

            EditText timeInput = new EditText(this);
            timeInput.setHint("1400-1630");
            timeInput.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            ));
            timeInput.setEnabled(false);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                timeInput.setEnabled(isChecked);
                if (!isChecked) {
                    timeInput.setText("");
                    selectedDates.removeIf(date -> date.startsWith(day));
                } else {
                    // When checked, ensure the date is added only if there's time
                    timeInput.setOnFocusChangeListener((v, hasFocus) -> {
                        if (!hasFocus) {
                            String time = timeInput.getText().toString();
                            if (!time.isEmpty()) {
                                selectedDates.add(day + ": " + time);
                            }
                        }
                    });
                }
            });

            dayLayout.addView(checkBox);
            dayLayout.addView(timeInput);

            dateContainer.addView(dayLayout);
        }
    }

    private ArrayList<String> getSelectedSubjectIds() {
        ArrayList<String> selectedIds = new ArrayList<>();

        for (int i = 0; i < subjectContainer.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) subjectContainer.getChildAt(i);
            if (checkBox.isChecked()) {
                selectedIds.add((String) checkBox.getTag());
            }
        }
        return selectedIds;
    }

    private ArrayList<String> getSelectedDistrictIds() {
        ArrayList<String> selectedIds = new ArrayList<>();
        for (int i = 0; i < districtContainer.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) districtContainer.getChildAt(i);
            if (checkBox.isChecked()) {
                selectedIds.add((String) checkBox.getTag());
            }
        }
        return selectedIds;
    }

    private void onSubmitButtonClick() {
        String memberId = getMemberIdFromLocalDatabase();
        String studentLevel = studentLevelSpinner.getSelectedItem() != null ? studentLevelSpinner.getSelectedItem().toString() : "";
        String description = descriptionInput.getText().toString().trim();

        ArrayList<String> selectedSubjectIds = getSelectedSubjectIds();

        if (selectedSubjectIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one subject", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> levelIds = (ArrayList<String>) studentLevelSpinner.getTag();
        ArrayList<String> selectedDistrictIds = getSelectedDistrictIds();

        if (selectedDistrictIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one district", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedLevelId = levelIds.get(studentLevelSpinner.getSelectedItemPosition());

        String appCreator = "";
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radio_parent) {
            appCreator = "PS";
        } else if (selectedId == R.id.radio_tutor) {
            appCreator = "T";
        }

        if (studentLevel.isEmpty() || description.isEmpty() || selectedDates.isEmpty()) {
            Toast.makeText(this, "Please fill in all required information", Toast.LENGTH_SHORT).show();
            return;
        } else {
            // Convert selectedDates to JSON format for submission
            String selectedDatesJson = new JSONArray(selectedDates).toString();
            submitData(memberId, appCreator, selectedSubjectIds, selectedLevelId, selectedDistrictIds, description, selectedDatesJson);
        }
    }

    private void submitData(String memberId, String appCreator, ArrayList<String> subjectIds, String classLevelId, ArrayList<String> districtIds, String description, String selectedDatesJson) {
        OkHttpClient client = new OkHttpClient();
        Log.d("SubmitData", "Member ID: " + memberId);
        Log.d("SubmitData", "App Creator: " + appCreator);
        Log.d("SubmitData", "Selected Subject IDs: " + subjectIds.toString());
        Log.d("SubmitData", "Selected Dates: " + selectedDatesJson);

        RequestBody formBody = new FormBody.Builder()
                .add("member_id", memberId)
                .add("app_creator", appCreator)
                .add("subject_ids", new JSONArray(subjectIds).toString())
                .add("class_level_id", classLevelId)
                .add("district_ids", new JSONArray(districtIds).toString())
                .add("description", description)
                .add("fee_per_hr", feePerHr.getText().toString())
                .add("selected_dates", selectedDatesJson) // Add selected dates
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.2.2/FYP/php/post_application.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ParentApplicationFillDetail.this, "Request failed, please try again.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            if (jsonResponse.getBoolean("success")) {
                                Toast.makeText(ParentApplicationFillDetail.this, "Application submitted successfully!", Toast.LENGTH_SHORT).show();
                                finish(); // Close the activity
                            } else {
                                Toast.makeText(ParentApplicationFillDetail.this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(ParentApplicationFillDetail.this, "Error processing response", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(ParentApplicationFillDetail.this, "Server error, please try again later.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private String getMemberIdFromLocalDatabase() {
        // Replace with actual implementation to retrieve member ID from shared preferences or local database
        SharedPreferences sharedPreferences = getSharedPreferences("YourPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("member_id", "");
    }
}
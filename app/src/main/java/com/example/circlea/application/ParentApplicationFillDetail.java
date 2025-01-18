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
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ParentApplicationFillDetail extends AppCompatActivity {

    private ViewFlipper viewFlipper;
    private ProgressBar progressBar;
    private Button nextButton;
    private Button exitButton;

    private Spinner studentLevelSpinner;
    private EditText feePerHr;
    private RadioGroup radioGroup;
    private EditText descriptionInput;
    private LinearLayout districtContainer, subjectContainer;
    private LinearLayout dateContainer;

    private int currentStep = 0;
    private final ArrayList<String> selectedDates = new ArrayList<>();
    private final ArrayList<String> selectedSubjects = new ArrayList<>();
    private final ArrayList<String> selectedSubjectIds = new ArrayList<>();
    private final ArrayList<String> selectedDistricts = new ArrayList<>();
    private final ArrayList<String> subjectIds = new ArrayList<>();
    private final HashMap<String, String> classLevelMap = new HashMap<>(); // Map for class level name to ID
    private final ArrayList<String> subjects = new ArrayList<>(); // Declare the subjects ArrayList
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_application_step1); // Update your layout file

        // Initialize views
        viewFlipper = findViewById(R.id.view_flipper);
        progressBar = findViewById(R.id.progress_bar);
        nextButton = findViewById(R.id.next_button);
        exitButton = findViewById(R.id.exit_button);
        Button backButton = findViewById(R.id.back_button);

        studentLevelSpinner = findViewById(R.id.student_level_spinner);
        feePerHr = findViewById(R.id.fee_input);
        radioGroup = findViewById(R.id.radio_group);
        descriptionInput = findViewById(R.id.description_input);
        districtContainer = findViewById(R.id.district_container);
        dateContainer = findViewById(R.id.date_container);

        // Load data
        loadStudentLevelsAndSubjects();

        nextButton.setOnClickListener(v -> {
            if (currentStep < 2) {
                currentStep++;
                viewFlipper.showNext();
                progressBar.setProgress(currentStep + 1);
                updateNextButtonText(); // Update button text based on the current step
                if (currentStep == 1) {
                    setupDateCheckBoxes();
                }
            } else {
                submitData();
            }
        });

        backButton.setOnClickListener(v -> {
            if (currentStep > 0) {
                currentStep--;
                viewFlipper.showPrevious();
                progressBar.setProgress(currentStep + 1);
                updateNextButtonText(); // Update button text based on the current step
            }
        });

        exitButton.setOnClickListener(v -> finish());

        updateNextButtonText(); // Initial setup for button text
    }

    private void updateNextButtonText() {
        if (currentStep == 2) {
            nextButton.setText("Submit");
        } else {
            nextButton.setText("Next");
        }
    }

    private void loadStudentLevelsAndSubjects() {
        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2/FYP/php/get_studentLevels&Subject&District.php"; // Your PHP URL

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
                        subjectIds.clear();
                        ArrayList<String> districts = new ArrayList<>();

                        for (int i = 0; i < levelsArray.length(); i++) {
                            JSONObject levelObj = levelsArray.getJSONObject(i);
                            String levelId = levelObj.getString("class_level_id");
                            String levelName = levelObj.getString("class_level_name");
                            levels.add(levelName);
                            classLevelMap.put(levelName, levelId); // Store mapping of name to ID
                        }

                        for (int i = 0; i < subjectsArray.length(); i++) {
                            JSONObject subjectObj = subjectsArray.getJSONObject(i);
                            subjects.add(subjectObj.getString("subject_name"));
                            subjectIds.add(subjectObj.getString("subject_id"));
                        }

                        for (int i = 0; i < districtsArray.length(); i++) {
                            JSONObject districtObj = districtsArray.getJSONObject(i);
                            String districtName = districtObj.getString("district_name");
                            String districtId = districtObj.getString("district_id");
                            districts.add(districtId + ":" + districtName);
                        }

                        runOnUiThread(() -> {
                            studentLevelSpinner.setAdapter(new ArrayAdapter<>(ParentApplicationFillDetail.this, android.R.layout.simple_spinner_item, levels));
                            setupSubjectCheckBoxes(subjects);
                            setupDistrictCheckBoxes(districts);
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

    private void setupSubjectCheckBoxes(ArrayList<String> subjects) {
        LinearLayout subjectContainer = findViewById(R.id.subject_container);
        subjectContainer.removeAllViews();

        for (int i = 0; i < subjects.size(); i++) {
            String subject = subjects.get(i);
            String subjectId = subjectIds.get(i);

            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(subject);
            checkBox.setTag(subjectId);
            checkBox.setChecked(selectedSubjectIds.contains(subjectId)); // Restore selection state

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedSubjectIds.add(subjectId);
                } else {
                    selectedSubjectIds.remove(subjectId);
                }
            });

            subjectContainer.addView(checkBox);
        }
    }

    private void setupDistrictCheckBoxes(ArrayList<String> districts) {
        districtContainer.removeAllViews();

        for (String district : districts) {
            String[] parts = district.split(":");
            String districtId = parts[0];
            String districtName = parts[1];

            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(districtName);
            checkBox.setTag(districtId);
            checkBox.setChecked(selectedDistricts.contains(districtId)); // Restore selection state

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedDistricts.add(districtId);
                } else {
                    selectedDistricts.remove(districtId);
                }
            });

            districtContainer.addView(checkBox);
        }
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
            checkBox.setChecked(selectedDates.stream().anyMatch(date -> date.startsWith(day))); // Restore selection state

            EditText timeInput = new EditText(this);
            timeInput.setHint("1400-1630");
            timeInput.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            timeInput.setEnabled(checkBox.isChecked());

            selectedDates.stream()
                    .filter(date -> date.startsWith(day))
                    .findFirst()
                    .ifPresent(date -> timeInput.setText(date.split(": ")[1])); // Restore time value

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                timeInput.setEnabled(isChecked);
                if (!isChecked) {
                    timeInput.setText("");
                    selectedDates.removeIf(date -> date.startsWith(day));
                } else {
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

    private void submitData() {
        String memberId = getMemberIdFromLocalDatabase();
        int selectedId = radioGroup.getCheckedRadioButtonId();
        String appCreator;

        if (selectedId == R.id.radio_tutor) {
            appCreator = "T";
        } else if (selectedId == R.id.radio_parent) {
            appCreator = "PS";
        } else {
            Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show();
            return;
        }

        String classLevelName = (String) studentLevelSpinner.getSelectedItem();
        String classLevelId = classLevelMap.get(classLevelName); // Get the ID from the map
        String description = descriptionInput.getText().toString().trim();
        String fee = feePerHr.getText().toString().trim();

        ArrayList<String> selectedDistrictIds = getSelectedDistrictIds();

        if (classLevelId == null || selectedDistrictIds.isEmpty() || fee.isEmpty() || selectedDates.isEmpty() || selectedSubjectIds.isEmpty()) {
            Toast.makeText(this, "Please fill in all required information", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("member_id", memberId)
                .add("app_creator", appCreator)
                .add("subject_ids", new JSONArray(selectedSubjectIds).toString())
                .add("class_level_id", classLevelId)
                .add("district_ids", new JSONArray(selectedDistrictIds).toString())
                .add("description", description)
                .add("fee_per_hr", fee)
                .add("selected_dates", new JSONArray(selectedDates).toString())
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
                                finish();
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("selectedSubjects", selectedSubjectIds);
        outState.putStringArrayList("selectedDistricts", selectedDistricts);
        outState.putStringArrayList("selectedDates", selectedDates);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selectedSubjectIds.clear();
        selectedDistricts.clear();
        selectedDates.clear();

        selectedSubjectIds.addAll(savedInstanceState.getStringArrayList("selectedSubjects"));
        selectedDistricts.addAll(savedInstanceState.getStringArrayList("selectedDistricts"));
        selectedDates.addAll(savedInstanceState.getStringArrayList("selectedDates"));

        // Re-populate checkboxes after restoring state
        loadStudentLevelsAndSubjects(); // Reloads the data
    }

    private String getMemberIdFromLocalDatabase() {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        return sharedPreferences.getString("member_id", "");
    }
}
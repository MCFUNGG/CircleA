package com.example.circlea.application;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.IPConfig;
import com.example.circlea.LanguageManager;
import com.example.circlea.R;
import com.example.circlea.utils.ContentFilter;
import com.example.circlea.utils.TranslationHelper;

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
    private TextView studentLevelLabel;
    private boolean isParent = true;
    private TextView lessonsPerWeekText;
    private int selectedDaysCount = 0;
    private LanguageManager languageManager;

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

        // Initialize LanguageManager
        languageManager = new LanguageManager(this);

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
        studentLevelLabel = findViewById(R.id.student_level_label);

        lessonsPerWeekText = findViewById(R.id.lessons_per_week_text);
        updateLessonsPerWeekText(0);



        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isParent = (checkedId == R.id.radio_parent);
            if (isParent) {
                studentLevelLabel.setText("Student Level");
                dateContainer.setVisibility(View.VISIBLE);
                lessonsPerWeekText.setVisibility(View.VISIBLE);
            } else {
                studentLevelLabel.setText("Target Student Level");
                dateContainer.setVisibility(View.VISIBLE);
                lessonsPerWeekText.setVisibility(View.GONE);
            }
        });

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
            nextButton.setText(getString(R.string.submit));
        } else {
            nextButton.setText(getString(R.string.next));
        }
    }

    private void loadStudentLevelsAndSubjects() {
        OkHttpClient client = new OkHttpClient();
        String url = "http://"+ IPConfig.getIP()+"/FYP/php/get_studentLevels&Subject&District.php"; // Your PHP URL

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("LoadLevelsRequest", "Request failed: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(ParentApplicationFillDetail.this, getString(R.string.error_fetching_data), Toast.LENGTH_SHORT).show());
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
                                Toast.makeText(ParentApplicationFillDetail.this, getString(R.string.error_processing_response), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("LoadLevelsRequest", "Request failed with response code: " + response.code());
                    runOnUiThread(() ->
                            Toast.makeText(ParentApplicationFillDetail.this, getString(R.string.failed_to_fetch_levels_and_subjects), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void setupSubjectCheckBoxes(ArrayList<String> subjects) {
        LinearLayout subjectContainer = findViewById(R.id.subject_container);
        subjectContainer.removeAllViews();

        // Use GridLayout for consistent button sizes
        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(2); // Two columns
        gridLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        for (int i = 0; i < subjects.size(); i++) {
            String subject = subjects.get(i);
            String subjectId = subjectIds.get(i);

            // 翻译科目名称
            String translatedSubject = languageManager.translateDatabaseField(subject, "subject");

            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(translatedSubject);
            checkBox.setTag(subjectId);
            checkBox.setChecked(selectedSubjectIds.contains(subjectId)); // Restore selection state

            // Apply button-like styling
            checkBox.setButtonDrawable(null); // Remove default checkbox
            checkBox.setBackgroundResource(R.drawable.radio_selector); // Use the same selector
            checkBox.setTextColor(getResources().getColorStateList(R.drawable.radio_text_selector));
            checkBox.setGravity(Gravity.CENTER);
            checkBox.setPadding(12, 12, 12, 12);

            // Set layout parameters for equal width and height
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // Ensure equal height
            params.setMargins(4, 4, 4, 4);
            checkBox.setLayoutParams(params);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedSubjectIds.add(subjectId);
                } else {
                    selectedSubjectIds.remove(subjectId);
                }
            });

            gridLayout.addView(checkBox);
        }

        subjectContainer.addView(gridLayout);
    }

    private void setupDistrictCheckBoxes(ArrayList<String> districts) {
        districtContainer.removeAllViews();

        for (String district : districts) {
            String[] parts = district.split(":");
            String districtId = parts[0];
            String districtName = parts[1];
            
            // 翻译地区名称
            String translatedDistrictName = languageManager.translateDatabaseField(districtName, "district");

            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(translatedDistrictName);
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
            checkBox.setChecked(selectedDates.stream().anyMatch(date -> date.startsWith(day)));

            EditText timeInput = new EditText(this);
            timeInput.setHint("1400-1630");
            timeInput.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            timeInput.setEnabled(checkBox.isChecked());

            // Restore previously entered time if any
            selectedDates.stream()
                    .filter(date -> date.startsWith(day))
                    .findFirst()
                    .ifPresent(date -> timeInput.setText(date.split(": ")[1]));

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                timeInput.setEnabled(isChecked);
                if (!isChecked) {
                    timeInput.setText("");
                    selectedDates.removeIf(date -> date.startsWith(day));
                    selectedDaysCount--;
                } else {
                    selectedDaysCount++;
                }
                updateLessonsPerWeekText(selectedDaysCount);

                if (isChecked) {
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

            // Update initial count for checked boxes
            if (checkBox.isChecked()) {
                selectedDaysCount++;
                updateLessonsPerWeekText(selectedDaysCount);
            }
        }
    }
    private void updateLessonsPerWeekText(int count) {
        lessonsPerWeekText.setText(count + " day" + (count != 1 ? "s" : "") + " per week");
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
            Toast.makeText(this, getString(R.string.please_select_an_option), Toast.LENGTH_SHORT).show();
            return;
        }

        String classLevelName = (String) studentLevelSpinner.getSelectedItem();
        String classLevelId = classLevelMap.get(classLevelName); // Get the ID from the map
        String description = descriptionInput.getText().toString().trim();
        String fee = feePerHr.getText().toString().trim();

        ArrayList<String> selectedDistrictIds = getSelectedDistrictIds();

        if (classLevelId == null || selectedDistrictIds.isEmpty() || fee.isEmpty() || selectedDates.isEmpty() || selectedSubjectIds.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_fill_in_all_required_information), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 檢查描述內容是否包含敏感詞或個人信息
        if (!description.isEmpty()) {
            ContentFilter.ContentCheckResult checkResult = ContentFilter.checkContent(description);
            
            if (!checkResult.isClean()) {
                String warningMessage = ContentFilter.getWarningMessage(checkResult);
                Toast.makeText(this, warningMessage, Toast.LENGTH_LONG).show();
                return;
            }
        }

        OkHttpClient client = new OkHttpClient();
        FormBody.Builder formBodyBuilder = new FormBody.Builder()
                .add("member_id", memberId)
                .add("app_creator", isParent ? "PS" : "T")
                .add("subject_ids", new JSONArray(selectedSubjectIds).toString())
                .add("class_level_id", classLevelId)
                .add("district_ids", new JSONArray(selectedDistrictIds).toString())
                .add("description", description)
                .add("fee_per_hr", fee)
                .add("selected_dates", new JSONArray(selectedDates).toString());

        if (isParent) {
            formBodyBuilder.add("lessons_per_week", String.valueOf(selectedDaysCount));
        }

        RequestBody formBody = formBodyBuilder.build();

        Request request = new Request.Builder()
                .url("http://"+ IPConfig.getIP()+"/FYP/php/post_application.php")
                .post(formBody)
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ParentApplicationFillDetail.this, getString(R.string.request_failed_please_try_again), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            if (jsonResponse.getBoolean("success")) {
                                Toast.makeText(ParentApplicationFillDetail.this, getString(R.string.application_submitted_successfully), Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(ParentApplicationFillDetail.this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(ParentApplicationFillDetail.this, getString(R.string.error_processing_response), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(ParentApplicationFillDetail.this, getString(R.string.server_error_please_try_again_later), Toast.LENGTH_SHORT).show());
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
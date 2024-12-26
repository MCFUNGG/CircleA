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

    private Spinner studentLevelSpinner, lessonPerWeekSpinner, districtSpinner;
    private Button submitButton;
    private EditText feePerHr;
    private RadioGroup radioGroup;
    private EditText descriptionInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_application_step1);

        studentLevelSpinner = findViewById(R.id.student_level_spinner);
        lessonPerWeekSpinner = findViewById(R.id.lesson_per_week_spinner);
        districtSpinner = findViewById(R.id.district_spinner);
        submitButton = findViewById(R.id.submit_button);
        feePerHr = findViewById(R.id.fee_input);
        radioGroup = findViewById(R.id.radio_group);
        descriptionInput = findViewById(R.id.description_input);

        loadStudentLevelsAndSubjects();
        setupLessonPerWeekSpinner(lessonPerWeekSpinner);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitButtonClick();
            }
        });
    }

    private void loadStudentLevelsAndSubjects() {
        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2/FYP/php/get_studentLevelsAndSubject.php";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("LoadLevelsRequest", "请求失败: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(ParentApplicationFillDetail.this, "获取数据时出错", Toast.LENGTH_SHORT).show());
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
                            districtSpinner.setAdapter(new ArrayAdapter<>(ParentApplicationFillDetail.this, android.R.layout.simple_spinner_item, districts));

                            studentLevelSpinner.setTag(levelIds);
                            districtSpinner.setTag(districtIds);
                        });
                    } catch (JSONException e) {
                        Log.e("LoadLevelsRequest", "JSON解析错误: " + e.getMessage());
                        runOnUiThread(() ->
                                Toast.makeText(ParentApplicationFillDetail.this, "处理响应时出错", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("LoadLevelsRequest", "请求失败，响应代码: " + response.code());
                    runOnUiThread(() ->
                            Toast.makeText(ParentApplicationFillDetail.this, "获取级别和科目失败", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void setupSubjectCheckBoxes(ArrayList<String> subjects, ArrayList<String> subjectIds) {
        LinearLayout subjectContainer = findViewById(R.id.subject_container);
        subjectContainer.removeAllViews();

        for (int i = 0; i < subjects.size(); i++) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(subjects.get(i));
            checkBox.setTag(subjectIds.get(i));
            subjectContainer.addView(checkBox);
        }
    }

    private void setupLessonPerWeekSpinner(Spinner lessonPerWeekSpinner) {
        ArrayList<String> lessonOptions = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            lessonOptions.add(String.valueOf(i));
        }

        ArrayAdapter<String> lessonAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, lessonOptions);
        lessonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lessonPerWeekSpinner.setAdapter(lessonAdapter);
    }

    public void onSubmitButtonClick() {
        String memberId = getMemberIdFromLocalDatabase();
        String studentLevel = studentLevelSpinner.getSelectedItem().toString();
        String lessonsPerWeek = lessonPerWeekSpinner.getSelectedItem().toString();
        String district = districtSpinner.getSelectedItem().toString();
        String description = descriptionInput.getText().toString();

        // 获取选中的科目ID
        String selectedSubjectId = getSelectedSubjectId();

        if (selectedSubjectId == null) {
            Toast.makeText(this, "请至少选择一个科目", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> levelIds = (ArrayList<String>) studentLevelSpinner.getTag();
        ArrayList<String> districtIds = (ArrayList<String>) districtSpinner.getTag();

        String selectedLevelId = levelIds.get(studentLevelSpinner.getSelectedItemPosition());
        String selectedDistrictId = districtIds.get(districtSpinner.getSelectedItemPosition());

        String appCreator = "";
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radio_parent) {
            appCreator = "PS";
        } else if (selectedId == R.id.radio_tutor) {
            appCreator = "T";
        }

        if (studentLevel.isEmpty() || lessonsPerWeek.isEmpty() || district.isEmpty()) {
            Toast.makeText(this, "请填写所有必填信息", Toast.LENGTH_SHORT).show();
            return;
        } else {
            submitData(memberId, appCreator, selectedSubjectId, selectedLevelId, selectedDistrictId, description);
        }
    }

    private String getSelectedSubjectId() {
        LinearLayout subjectContainer = findViewById(R.id.subject_container);
        for (int i = 0; i < subjectContainer.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) subjectContainer.getChildAt(i);
            if (checkBox.isChecked()) {
                return (String) checkBox.getTag(); // 返回选中的科目ID
            }
        }
        return null; // 如果没有选中任何科目，返回null
    }

    private void submitData(String memberId, String appCreator, String subjectId, String classLevelId, String districtId, String description) {
        OkHttpClient client = new OkHttpClient();
        Log.d("SubmitData", "Member ID: " + memberId);
        Log.d("SubmitData", "App Creator: " + appCreator);
        Log.d("SubmitData", "Subject ID: " + subjectId); // 修正日志输出
        Log.d("SubmitData", "Class Level ID: " + classLevelId);
        Log.d("SubmitData", "District ID: " + districtId);
        Log.d("SubmitData", "Description: " + description);
        Log.d("SubmitData", "Fee per Hour: " + feePerHr.getText().toString());

        RequestBody formBody = new FormBody.Builder()
                .add("member_id", memberId)
                .add("app_creator", appCreator)
                .add("subject_id", subjectId) // 只发送一个科目ID
                .add("class_level_id", classLevelId)
                .add("district_id", districtId)
                .add("description", description)
                .add("fee_per_hr", feePerHr.getText().toString())
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.2.2/FYP/php/post_application.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ParentApplicationFillDetail.this, "请求失败，请重试。", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String serverResponse = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(serverResponse);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.getString("message");
                        runOnUiThread(() -> Toast.makeText(ParentApplicationFillDetail.this, message, Toast.LENGTH_SHORT).show());
                        Log.d("ServerResponse", "Message: " + message);
                    } catch (JSONException e) {
                        runOnUiThread(() -> Toast.makeText(ParentApplicationFillDetail.this, "处理响应时出错", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(ParentApplicationFillDetail.this, "提交申请失败", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private String getMemberIdFromLocalDatabase() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("member_id", ""); // 如果未找到，默认返回空字符串
    }
}
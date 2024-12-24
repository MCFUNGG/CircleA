package com.example.circlea;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ParentApplicationFillDetail extends AppCompatActivity {

    private Spinner studentLevelSpinner, subjectInputSpinner, lessonPerWeekSpinner,districtSpinner;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_application_step1);

        studentLevelSpinner = findViewById(R.id.student_level_spinner);
        subjectInputSpinner = findViewById(R.id.subject_input_spinner);
        lessonPerWeekSpinner = findViewById(R.id.lesson_per_week_spinner);
        districtSpinner = findViewById(R.id.district_spinner);


        submitButton = findViewById(R.id.submit_button);
        // 获取学生级别选项
        loadStudentLevelsAndSubjects();
        setupLessonPerWeekSpinner(lessonPerWeekSpinner);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 处理提交逻辑
            }
        });
    }

    private void loadStudentLevelsAndSubjects() {
        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2/FYP/php/get_studentLevelsAndSubject.php";

        // Create request
        Request request = new Request.Builder()
                .url(url)
                .build();

        // Send request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("LoadLevelsRequest", "Request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ParentApplicationFillDetail.this, "Error fetching data", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String serverResponse = response.body().string();
                    Log.d("LoadLevelsRequest", "Server response: " + serverResponse);

                    // Parse JSON response
                    try {
                        JSONObject jsonResponse = new JSONObject(serverResponse);
                        JSONArray levelsArray = jsonResponse.getJSONArray("levels");
                        JSONArray subjectsArray = jsonResponse.getJSONArray("subjects");
                        JSONArray districtsArray = jsonResponse.getJSONArray("districts"); // Add this line

                        ArrayList<String> levels = new ArrayList<>();
                        ArrayList<String> subjects = new ArrayList<>();
                        ArrayList<String> districts = new ArrayList<>(); // Add this line

                        for (int i = 0; i < levelsArray.length(); i++) {
                            levels.add(levelsArray.getString(i));
                        }

                        for (int i = 0; i < subjectsArray.length(); i++) {
                            subjects.add(subjectsArray.getString(i));
                        }

                        // Parse districts
                        for (int i = 0; i < districtsArray.length(); i++) {
                            districts.add(districtsArray.getString(i)); // Add this line
                        }

                        // Update UI thread
                        runOnUiThread(() -> {
                            // Set student levels to Spinner
                            ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(ParentApplicationFillDetail.this,
                                    android.R.layout.simple_spinner_item, levels);
                            levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            studentLevelSpinner.setAdapter(levelAdapter);

                            // Set subjects to Spinner
                            ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(ParentApplicationFillDetail.this,
                                    android.R.layout.simple_spinner_item, subjects);
                            subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            subjectInputSpinner.setAdapter(subjectAdapter);

                            // Set districts to Spinner
                            ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(ParentApplicationFillDetail.this,
                                    android.R.layout.simple_spinner_item, districts); // Update this line
                            districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            districtSpinner.setAdapter(districtAdapter); // Update this line
                        });
                    } catch (JSONException e) {
                        Log.e("LoadLevelsRequest", "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(ParentApplicationFillDetail.this, "Error processing response", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("LoadLevelsRequest", "Request failed, response code: " + response.code());
                    runOnUiThread(() -> Toast.makeText(ParentApplicationFillDetail.this, "Failed to fetch levels and subjects", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
    private void setupLessonPerWeekSpinner(Spinner lessonPerWeekSpinner) {
        ArrayList<String> lessonOptions = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            lessonOptions.add(String.valueOf(i)); // 添加 1 到 7 的选项
        }

        ArrayAdapter<String> lessonAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, lessonOptions);
        lessonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lessonPerWeekSpinner.setAdapter(lessonAdapter); // 设置适配器
    }

}
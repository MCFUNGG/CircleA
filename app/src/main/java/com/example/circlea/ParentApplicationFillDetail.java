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

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ParentApplicationFillDetail extends AppCompatActivity {

    private Spinner studentLevelSpinner, subjectInputSpinner;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_application_step1);

        studentLevelSpinner = findViewById(R.id.student_level_spinner);
        subjectInputSpinner = findViewById(R.id.subject_input_spinner);
        submitButton = findViewById(R.id.submit_button);

        // 获取学生级别选项
        loadStudentLevels();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 处理提交逻辑
            }
        });
    }

    private void loadStudentLevels() {
        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2/FYP/php/get_student_levels.php";

        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .build();

        // 发送请求
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

                    // 解析 JSON 响应
                    try {
                        JSONArray jsonResponse = new JSONArray(serverResponse);
                        ArrayList<String> levels = new ArrayList<>();

                        for (int i = 0; i < jsonResponse.length(); i++) {
                            levels.add(jsonResponse.getString(i));
                        }

                        // 更新 UI 线程
                        runOnUiThread(() -> {
                            // 将数据设置到 Spinner 中
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(ParentApplicationFillDetail.this,
                                    android.R.layout.simple_spinner_item, levels);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            studentLevelSpinner.setAdapter(adapter);
                        });
                    } catch (JSONException e) {
                        Log.e("LoadLevelsRequest", "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(ParentApplicationFillDetail.this, "Error processing response", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("LoadLevelsRequest", "Request failed, response code: " + response.code());
                    runOnUiThread(() -> Toast.makeText(ParentApplicationFillDetail.this, "Failed to fetch levels", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }}
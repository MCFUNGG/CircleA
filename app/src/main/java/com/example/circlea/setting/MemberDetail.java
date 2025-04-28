package com.example.circlea.setting;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.IPConfig;
import com.example.circlea.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MemberDetail extends AppCompatActivity {

    private Spinner districtSpinner;
    private EditText addressEditText;
    private EditText dobEditText;
    private EditText descriptionEditText;
    private RadioGroup genderRadioGroup;
    private Button submitButton;
    
    // 地区数据
    private List<String> districtNames;
    private Map<String, Integer> districtMap;  // 名称到ID的映射

    // Variables to hold initial values
    private String initialDistrictId;
    private String initialAddress;
    private String initialDob;
    private String initialProfile; // 保留这个字段，只是不在UI中显示
    private String initialDescription;
    private String initialGender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.member_detail);

        // 初始化地区数据
        initializeDistrictData();
        
        // Initialize views
        districtSpinner = findViewById(R.id.address_district_id);
        addressEditText = findViewById(R.id.address);
        dobEditText = findViewById(R.id.dob);
        descriptionEditText = findViewById(R.id.description);
        genderRadioGroup = findViewById(R.id.gender);
        submitButton = findViewById(R.id.submit_button);

        // 设置地区下拉列表
        setupDistrictSpinner();
        
        // Set up DatePicker for dobEditText
        dobEditText.setOnClickListener(v -> showDatePickerDialog());

        // Fetch past member details to populate the UI
        getPastMemberDetails();

        // Set button click listener
        submitButton.setOnClickListener(v -> saveMemberDetails());

        Button exitButton = findViewById(R.id.exit_button);
        exitButton.setOnClickListener(v -> finish());
    }
    
    private void initializeDistrictData() {
        districtNames = new ArrayList<>();
        districtMap = new HashMap<>();
        
        // 添加地区数据（来自SQL文件）
        districtNames.add("Select a district");  // 默认选项
        
        districtMap.put("Central and Western", 1);
        districtNames.add("Central and Western");
        
        districtMap.put("Eastern", 2);
        districtNames.add("Eastern");
        
        districtMap.put("Southern", 3);
        districtNames.add("Southern");
        
        districtMap.put("Wan Chai", 4);
        districtNames.add("Wan Chai");
        
        districtMap.put("Kowloon City", 5);
        districtNames.add("Kowloon City");
        
        districtMap.put("Yau Tsim Mong", 6);
        districtNames.add("Yau Tsim Mong");
        
        districtMap.put("Sham Shui Po", 7);
        districtNames.add("Sham Shui Po");
        
        districtMap.put("Wong Tai Sin", 8);
        districtNames.add("Wong Tai Sin");
        
        districtMap.put("Kwun Tong", 9);
        districtNames.add("Kwun Tong");
        
        districtMap.put("Tai Po", 10);
        districtNames.add("Tai Po");
        
        districtMap.put("Yuen Long", 11);
        districtNames.add("Yuen Long");
        
        districtMap.put("Tuen Mun", 12);
        districtNames.add("Tuen Mun");
        
        districtMap.put("North", 13);
        districtNames.add("North");
        
        districtMap.put("Sai Kung", 14);
        districtNames.add("Sai Kung");
        
        districtMap.put("Sha Tin", 15);
        districtNames.add("Sha Tin");
        
        districtMap.put("Tsuen Wan", 16);
        districtNames.add("Tsuen Wan");
        
        districtMap.put("Kwai Tsing", 17);
        districtNames.add("Kwai Tsing");
        
        districtMap.put("Islands", 18);
        districtNames.add("Islands");
    }
    
    private void setupDistrictSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, districtNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        districtSpinner.setAdapter(adapter);
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            // Format the selected date to yyyy-MM-dd
            String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
            dobEditText.setText(formattedDate);
        }, year, month, day);
        datePickerDialog.show();
    }

    private void getPastMemberDetails() {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);
        Toast.makeText(MemberDetail.this, "member_id:"+memberId, Toast.LENGTH_SHORT).show();

        if (memberId == null) {
            return;
        }

        String url = "http://"+ IPConfig.getIP()+"/FYP/php/get_member_detail.php"; 

        // Create the request body
        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", memberId)
                .build();

        // Build the request
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // Send the request
        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchSettingData", "Request failed: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(MemberDetail.this, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    Log.d("FetchSettingData", "Server response: " + jsonResponse);

                    try {
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        if (jsonObject.getBoolean("success")) {
                            JSONArray dataArray = jsonObject.getJSONArray("data");

                            // Check if dataArray has at least one entry
                            if (dataArray.length() > 0) {
                                JSONObject data = dataArray.getJSONObject(0);
                                populateFields(data);
                            } else {
                                Log.d("SettingData", "No data found for member");
                            }
                        } else {
                            String message = jsonObject.optString("message", "Unknown error");
                            runOnUiThread(() ->
                                    Toast.makeText(MemberDetail.this, message, Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (JSONException e) {
                        Log.e("FetchSettingData", "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() ->
                                Toast.makeText(MemberDetail.this, "Error processing data", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    Log.e("FetchSettingData", "Request failed, response code: " + response.code());
                    runOnUiThread(() ->
                            Toast.makeText(MemberDetail.this, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void populateFields(JSONObject data) throws JSONException {
        runOnUiThread(() -> {
            initialDistrictId = data.optString("Address_District_id", "");
            initialAddress = data.optString("Address", "");
            initialDob = data.optString("DOB", "");
            initialProfile = data.optString("profile", ""); // 保留这个字段，用于提交表单
            initialDescription = data.optString("description", "");
            initialGender = data.optString("Gender", "");

            // 根据district_id设置Spinner位置
            if (!initialDistrictId.isEmpty()) {
                try {
                    int districtId = Integer.parseInt(initialDistrictId);
                    // 查找对应的district名称
                    for (int i = 0; i < districtNames.size(); i++) {
                        String name = districtNames.get(i);
                        if (name.equals("Select a district")) continue;
                        
                        Integer id = districtMap.get(name);
                        if (id != null && id == districtId) {
                            districtSpinner.setSelection(i);
                            break;
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.e("MemberDetail", "Error parsing district ID: " + initialDistrictId);
                }
            }
            
            addressEditText.setText(initialAddress);
            dobEditText.setText(initialDob);
            descriptionEditText.setText(initialDescription);

            // Set selected gender in the RadioGroup
            if ("M".equalsIgnoreCase(initialGender)) {
                genderRadioGroup.check(R.id.gender_male);
            } else if ("F".equalsIgnoreCase(initialGender)) {
                genderRadioGroup.check(R.id.gender_female);
            }
        });
    }

    private void saveMemberDetails() {
        // Get input data
        String districtId = getSelectedDistrictId();
        String address = addressEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String gender = "";

        // Get selected gender
        int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();
        if (selectedGenderId != -1) {
            RadioButton selectedGender = findViewById(selectedGenderId);
            String genderText = selectedGender.getText().toString();
            gender = "Male".equals(genderText) ? "M" : "F";
        } else {
            Toast.makeText(this, "請選擇性別", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 验证地区选择
        if (districtId.isEmpty()) {
            Toast.makeText(this, "Please select a district", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate inputs (basic validation)
        if (TextUtils.isEmpty(address) || TextUtils.isEmpty(dob) ||
                TextUtils.isEmpty(description)) {
            Toast.makeText(this, "請填寫所有字段", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the date is formatted correctly (yyyy-MM-dd)
        if (!dob.matches("\\d{4}-\\d{2}-\\d{2}")) {
            Toast.makeText(this, "日期格式無效。請使用 YYYY-MM-DD 格式。", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for changes before submitting
        boolean hasChanges = !districtId.equals(initialDistrictId) ||
                !address.equals(initialAddress) ||
                !dob.equals(initialDob) ||
                !description.equals(initialDescription) ||
                !gender.equals(initialGender);
                
        if (!hasChanges) {
            Toast.makeText(this, "沒有檢測到更改。提交已取消。", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取用户ID
        String memberId = getMemberIdFromLocalDatabase();
        
        // 直接提交表单，服务器端会获取最新的profile数据
        sendMemberDetailToServer(memberId, gender, address, districtId, dob, initialProfile, description);
    }

    private String getSelectedDistrictId() {
        int position = districtSpinner.getSelectedItemPosition();
        if (position <= 0) {
            return "";
        }
        
        String selectedName = districtNames.get(position);
        Integer districtId = districtMap.get(selectedName);
        return districtId != null ? String.valueOf(districtId) : "";
    }

    /**
     * 发送会员详细信息到服务器
     */
    private void sendMemberDetailToServer(String memberId, String gender, String address, 
                                         String addressDistrictId, String dob, 
                                         String profile, String description) {
        OkHttpClient client = new OkHttpClient();

        // Build the request body
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("member_id", memberId)
                .add("gender", gender)
                .add("address", address)
                .add("address_district_id", addressDistrictId)
                .add("dob", dob)
                .add("description", description);
        
        // 不需要传递profile字段，服务器会自动获取最新的profile
        
        RequestBody formBody = formBuilder.build();

        // Create the request
        Request request = new Request.Builder()
                .url("http://"+IPConfig.getIP()+"/FYP/php/post_member_detail.php") // Ensure this is the correct server address
                .post(formBody)
                .build();

        // Send the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MemberDetail.this, "Request failed, please try again.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String serverResponse = response.body().string();

                    // Parse JSON response
                    try {
                        JSONObject jsonResponse = new JSONObject(serverResponse);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.getString("message");

                        runOnUiThread(() -> {
                            Toast.makeText(MemberDetail.this, message, Toast.LENGTH_SHORT).show();
                            if (success) {
                                // Optionally navigate to another activity or perform further actions
                            }
                        });
                    } catch (JSONException e) {
                        runOnUiThread(() -> Toast.makeText(MemberDetail.this, "Error processing response", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MemberDetail.this, "Failed to submit, please try again.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private String getMemberIdFromLocalDatabase() {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        return sharedPreferences.getString("member_id", ""); // Return empty string if not found
    }
}
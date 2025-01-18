package com.example.circlea.setting;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MemberDetail extends AppCompatActivity {

    private EditText addressDistrictIdEditText;
    private EditText addressEditText;
    private EditText dobEditText;
    private EditText profileEditText;
    private EditText descriptionEditText;
    private RadioGroup genderRadioGroup;
    private Button submitButton;

    // Variables to hold initial values
    private String initialAddressDistrictId;
    private String initialAddress;
    private String initialDob;
    private String initialProfile;
    private String initialDescription;
    private String initialGender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.member_detail);

        // Initialize views
        addressDistrictIdEditText = findViewById(R.id.address_district_id);
        addressEditText = findViewById(R.id.address);
        dobEditText = findViewById(R.id.dob);
        profileEditText = findViewById(R.id.profile);
        descriptionEditText = findViewById(R.id.description);
        genderRadioGroup = findViewById(R.id.gender);
        submitButton = findViewById(R.id.submit_button);

        // Set up DatePicker for dobEditText
        dobEditText.setOnClickListener(v -> showDatePickerDialog());

        // Fetch past member details to populate the UI
        getPastMemberDetails();

        // Set button click listener
        submitButton.setOnClickListener(v -> saveMemberDetails());

        Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> finish());
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

        if (memberId == null) {
            return; // If no memberId, return
        }

        String url = "http://10.0.2.2/FYP/php/get_member_detail.php"; // Update with your URL

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
            initialAddressDistrictId = data.optString("Address_District_id", "");
            initialAddress = data.optString("Address", "");
            initialDob = data.optString("DOB", "");
            initialProfile = data.optString("profile", "");
            initialDescription = data.optString("description", "");
            initialGender = data.optString("Gender", "");

            addressDistrictIdEditText.setText(initialAddressDistrictId);
            addressEditText.setText(initialAddress);
            dobEditText.setText(initialDob);
            profileEditText.setText(initialProfile);
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
        String addressDistrictId = addressDistrictIdEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim(); // This should now be in yyyy-MM-dd format
        String profile = profileEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String gender = "";

        // Get selected gender
        int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();
        if (selectedGenderId != -1) {
            RadioButton selectedGender = findViewById(selectedGenderId);
            gender = selectedGender.getText().toString(); // "Male" or "Female"
        } else {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate inputs (basic validation)
        if (TextUtils.isEmpty(addressDistrictId) || TextUtils.isEmpty(address) || TextUtils.isEmpty(dob) ||
                TextUtils.isEmpty(profile) || TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the date is formatted correctly (yyyy-MM-dd)
        if (!dob.matches("\\d{4}-\\d{2}-\\d{2}")) {
            Toast.makeText(this, "Invalid date format. Please use YYYY-MM-DD.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for changes before submitting
        if (addressDistrictId.equals(initialAddressDistrictId) &&
                address.equals(initialAddress) &&
                dob.equals(initialDob) &&
                profile.equals(initialProfile) &&
                description.equals(initialDescription) &&
                gender.equals(initialGender)) {
            Toast.makeText(this, "No changes detected. Submission canceled.", Toast.LENGTH_SHORT).show();
            return;
        }

        String memberId = getMemberIdFromLocalDatabase();
        sendMemberDetailToServer(memberId, gender, address, addressDistrictId, dob, profile, description);
    }

    private void sendMemberDetailToServer(String memberId, String gender, String address, String addressDistrictId, String dob, String profile, String description) {
        OkHttpClient client = new OkHttpClient();

        // Build the request body
        RequestBody formBody = new FormBody.Builder()
                .add("member_id", memberId)
                .add("gender", gender)
                .add("address", address)
                .add("address_district_id", addressDistrictId)
                .add("dob", dob)
                .add("profile", profile)
                .add("description", description)
                .build();

        // Create the request
        Request request = new Request.Builder()
                .url("http://10.0.2.2/FYP/php/post_member_detail.php") // Ensure this is the correct server address
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
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("member_id", ""); // Return empty string if not found
    }
}
package com.example.circlea.setting;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.member_detail); // Ensure your XML file is named correctly

        // Initialize views
        addressDistrictIdEditText = findViewById(R.id.address_district_id);
        addressEditText = findViewById(R.id.address);
        dobEditText = findViewById(R.id.dob);
        profileEditText = findViewById(R.id.profile);
        descriptionEditText = findViewById(R.id.description);
        genderRadioGroup = findViewById(R.id.gender);
        submitButton = findViewById(R.id.submit_button);

        // Set button click listener
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMemberDetails();
            }
        });

        Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });
    }

    private void saveMemberDetails() {
        // Get input data
        String addressDistrictId = addressDistrictIdEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim();
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

        // Format the date of birth
        String formattedDob = formatDateString(dob);
        if (formattedDob == null) {
            Toast.makeText(this, "Invalid date format. Please use MM/DD/YYYY.", Toast.LENGTH_SHORT).show();
            return;
        }

        String memberId = getMemberIdFromLocalDatabase();
        sendMemberDetailToServer(memberId, gender, address, addressDistrictId, formattedDob, profile, description);
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

    private String formatDateString(String dateString) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null; // Return null if parsing fails
        }
    }

    private String getMemberIdFromLocalDatabase() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("member_id", ""); // Return empty string if not found
    }
}
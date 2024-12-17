package com.example.circlea.registration;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.R;

import java.io.IOException;

public class Registration extends AppCompatActivity {

    private EditText etEmail, etPhone, etPassword, etConfirmPassword;
    private CheckBox checkboxAgreement;
    private Button btnRegister;
    private EditText etUserPhone, etVerificationCode;
// Removed unused TextView references

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);

        // Initialize views
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        checkboxAgreement = findViewById(R.id.checkbox_agreement);
        btnRegister = findViewById(R.id.btn_register);
        etUserPhone = findViewById(R.id.et_user_phone); // Added for phone verification
        etVerificationCode = findViewById(R.id.et_verification_code); // Added for verification

        // Register button click listener
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Send code button click listener
        findViewById(R.id.btn_send_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendVerificationCode();
            }
        });

        // Submit code button click listener
        findViewById(R.id.btn_submit_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyCode();
            }
        });
    }
    private void registerUser() {
        String email = etEmail.getText().toString();
        String phone = etPhone.getText().toString();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Check for empty fields
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check password match
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check agreement checkbox
        if (!checkboxAgreement.isChecked()) {
            Toast.makeText(this, "You must agree to the terms", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send data to server
        sendDataToServer();
    }

    private void sendDataToServer() {
        OkHttpClient client = new OkHttpClient();

        // Build the request body
        RequestBody formBody = new FormBody.Builder()
                .add("email", etEmail.getText().toString())
                .add("phoneNumber", etPhone.getText().toString())
                .add("password", etPassword.getText().toString())
                .build();

        // Create the request
        Request request = new Request.Builder()
                .url("http://10.0.2.2/FYP/php/create_account.php") // Use 10.0.2.2 for emulator
                .post(formBody)
                .build();

        // Send the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SendToDatabase", "Request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(Registration.this, "Request failed, please try again.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Read and log the server's response data
                    String serverResponse = response.body().string();
                    Log.d("SendToDatabase", "Server response: " + serverResponse);
                    runOnUiThread(() -> {
                        Toast.makeText(Registration.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        // Optionally, navigate to the next activity here
                    });
                } else {
                    Log.e("SendToDatabase", "Request failed, response code: " + response.code());
                    runOnUiThread(() -> Toast.makeText(Registration.this, "Registration failed, please try again.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void sendVerificationCode() {
        String phone = etUserPhone.getText().toString();
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simulate sending verification code (e.g., via SMS)
        Toast.makeText(this, "Verification code sent to " + phone, Toast.LENGTH_SHORT).show();
    }

    private void verifyCode() {
        String code = etVerificationCode.getText().toString();
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simulate verification process
        Toast.makeText(this, "Code verified successfully!", Toast.LENGTH_SHORT).show();
    }
}
package com.example.circlea.registration;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.Login;
import com.example.circlea.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class EmailVerification extends AppCompatActivity {

    private Button sendVerificationEmailButton;
    private Button continueButton;
    private TextView emailAddressTextView;
    private FirebaseAuthHelper authHelper;
    private String email;
    private String password;
    private String phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.email_vaild);

        authHelper = new FirebaseAuthHelper();
        sendVerificationEmailButton = findViewById(R.id.btn_send_verification_email);
        continueButton = findViewById(R.id.btn_continue);
        emailAddressTextView = findViewById(R.id.tv_email_address);

        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");
        phone = getIntent().getStringExtra("password");
        if (email != null && !email.isEmpty()) {
            emailAddressTextView.setText("Email: " + email);
        } else {
            emailAddressTextView.setText("Email not provided");
            Toast.makeText(this, "Email not provided", Toast.LENGTH_SHORT).show();
        }

        Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });

        sendVerificationEmailButton.setOnClickListener(v -> {
            authHelper.registerUser(email, password, this, new FirebaseAuthHelper.RegistrationCallback() {
                @Override
                public void onRegistrationSuccess() {
                    Toast.makeText(EmailVerification.this, "Verification email sent!", Toast.LENGTH_SHORT).show();
                    // 登錄用戶
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // 將用戶資料發送到伺服器
                                    sendLoginDataToServer(email, password);
                                } else {
                                    Toast.makeText(EmailVerification.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }

                @Override
                public void onRegistrationFailed(Exception e) {
                    Toast.makeText(EmailVerification.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        continueButton.setOnClickListener(v -> checkEmailVerification());
    }

    private void checkEmailVerification() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (user.isEmailVerified()) {
                        Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(EmailVerification.this, PhoneNumVerification.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Email not verified. Please check your inbox.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(this, "No user is currently logged in.", Toast.LENGTH_SHORT).show();
        }
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 將用戶資料發送到伺服器
                        sendLoginDataToServer(email, password);
                    } else {
                        Toast.makeText(EmailVerification.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendLoginDataToServer(String email, String password) {
        OkHttpClient client = new OkHttpClient();

        // Build the request body
        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .add("phone",phone)
                .build();

        // Create the request
        Request request = new Request.Builder()
                .url("http://10.0.2.2/FYP/php/create_account.php") // 確保這裡是正確的伺服器地址
                .post(formBody)
                .build();

        // Send the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EmailVerification.this, "Login request failed, please try again.", Toast.LENGTH_SHORT).show());
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
                            Toast.makeText(EmailVerification.this, message, Toast.LENGTH_SHORT).show();
                            if (success) {
                                // Navigate to the main activity or further actions

                            }
                        });
                    } catch (JSONException e) {
                        runOnUiThread(() -> Toast.makeText(EmailVerification.this, "Error processing response", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(EmailVerification.this, "Invalid email or password", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
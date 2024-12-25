package com.example.circlea;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.registration.Registration;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin,btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Initialize views
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);

        // Login button click listener
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, Registration.class);
                startActivity(intent);
            }
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        // Check for empty fields
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Log input values for debugging
        Log.d("LoginRequest", "Email: " + email);
        Log.d("LoginRequest", "Password: " + password);

        // Send data to server
        sendLoginDataToServer(email, password);
    }

    private void sendLoginDataToServer(String email, String password) {
        OkHttpClient client = new OkHttpClient();

        // Build the request body
        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .build();

        // Create the request
        Request request = new Request.Builder()
                    .url("http://10.0.2.2/FYP/php/login.php")
                .post(formBody)
                .build();

        // Send the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("LoginRequest", "Request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(Login.this, "Login request failed, please try again.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String serverResponse = response.body().string();
                    Log.d("LoginRequest", "Server response: " + serverResponse);

                    // Parse JSON response
                    try {
                        JSONObject jsonResponse = new JSONObject(serverResponse);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.getString("message");

                        // 獲取用戶 ID
                        String memberID = jsonResponse.optString("member_id", null); // 使用 optString 避免 JSONException

                        runOnUiThread(() -> {
                            Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show();
                            if (success) {
                                // 儲存用戶 ID 到 SharedPreferences
                                runOnUiThread(() -> Toast.makeText(Login.this, "member_id: "+memberID, Toast.LENGTH_SHORT).show());
                                SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("member_id", memberID);
                                editor.apply(); // 确保使用 apply() 方法

                                // 确认 ID 是否保存成功
                                String savedMemberId = sharedPreferences.getString("member_id", null);
                                Log.d("LoginRequest", "Saved member ID: " + savedMemberId);


                                // Navigate to the main activity
                                Intent intent = new Intent(Login.this, Home.class);
                                startActivity(intent);
                            }
                        });
                    } catch (JSONException e) {
                        Log.e("LoginRequest", "JSON parsing error: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(Login.this, "Error processing response", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("LoginRequest", "Request failed, response code: " + response.code());
                    runOnUiThread(() -> Toast.makeText(Login.this, "Invalid email or password", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
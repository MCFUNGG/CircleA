package com.example.circlea.registration;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.R;

public class Registration extends AppCompatActivity {

    private EditText etEmail, etPhone, etPassword, etConfirmPassword, etUsername;
    private CheckBox checkboxAgreement;
    private Button btnRegister;
    private FirebaseAuthHelper firebaseAuthHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);

        firebaseAuthHelper = new FirebaseAuthHelper();
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etUsername = findViewById(R.id.et_username);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        checkboxAgreement = findViewById(R.id.checkbox_agreement);
        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> registerUser());

        Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });
    }

    private void registerUser() {
        String email = etEmail.getText().toString();
        String phone = etPhone.getText().toString();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String username = etUsername.getText().toString();
        // Check for empty fields
        if (TextUtils.isEmpty(username) ||TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) ||
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

        // Proceed to EmailVerification activity
        Intent intent = new Intent(Registration.this, EmailVerification.class);
        intent.putExtra("username",username);
        intent.putExtra("email", email);
        intent.putExtra("password", password);
        intent.putExtra("phone", phone);
        startActivity(intent);
    }
}
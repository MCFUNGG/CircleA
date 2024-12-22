package com.example.circlea.registration;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.ActionCodeSettings;

public class VaildEmail extends AppCompatActivity {
    private EditText verificationCodeEditText;
    private Button getCodeButton;
    private Button submitCodeButton;
    private TextView verificationMessage;
    private TextView emailAddressTextView;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.email_vaild); // Set the layout

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        verificationCodeEditText = findViewById(R.id.et_verification_code);
        getCodeButton = findViewById(R.id.btn_send_code);
        submitCodeButton = findViewById(R.id.btn_submit_code);
        verificationMessage = findViewById(R.id.tv_email_verification_message);
        emailAddressTextView = findViewById(R.id.tv_email_address);

        // Get email from Intent (make sure to pass this from the previous activity)
        String email = getIntent().getStringExtra("email");
        emailAddressTextView.setText(email);

        // Set up the button click listeners
        getCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendVerificationEmail(email);
            }
        });

        submitCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyCode();
            }
        });
    }

    private void sendVerificationEmail(String email) {
        ActionCodeSettings actionCodeSettings =
                ActionCodeSettings.newBuilder()
                        .setUrl("https://www.example.com/finishSignUp?cartId=1234")
                        .setHandleCodeInApp(true)
                        .setIOSBundleId("com.example.ios")
                        .setAndroidPackageName("com.example.circlea", true, "12")
                        .build();

        mAuth.sendSignInLinkToEmail(email, actionCodeSettings)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        verificationMessage.setText("Verification email sent!");
                    } else {
                        Toast.makeText(VaildEmail.this, "Error sending email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void verifyCode() {
        String email = emailAddressTextView.getText().toString();
        String code = verificationCodeEditText.getText().toString().trim();

        if (TextUtils.isEmpty(code)) {
            verificationCodeEditText.setError("Verification code is required");
            return;
        }

        // Use the verification code to sign in
        mAuth.signInWithEmailLink(email, code)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        Toast.makeText(VaildEmail.this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                        // Proceed to next activity or main app flow
                    } else {
                        Toast.makeText(VaildEmail.this, "Error verifying code: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
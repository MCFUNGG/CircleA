package com.example.circlea.registration;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.Login;
import com.example.circlea.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException;

import java.util.concurrent.TimeUnit;

public class PhoneNumVerification extends AppCompatActivity {

    private static final String TAG = "PhoneNumVerification";

    private EditText phoneNumberEditText;
    private EditText verificationCodeEditText;
    private Button sendVerificationButton;
    private Button submitCodeButton;
    private Button exitButton;

    private String verificationId;
    private FirebaseAuth auth;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_vaild); // Ensure this matches your layout file name

        auth = FirebaseAuth.getInstance();
        phoneNumberEditText = findViewById(R.id.et_user_phone);
        verificationCodeEditText = findViewById(R.id.et_verification_code);
        sendVerificationButton = findViewById(R.id.btn_send_code);
        submitCodeButton = findViewById(R.id.btn_submit_code);
        exitButton = findViewById(R.id.exitButton);

        // Set up exit button to finish activity
        exitButton.setOnClickListener(v -> finish());

        // Send verification code
        sendVerificationButton.setOnClickListener(v -> {
            String phoneNumber = phoneNumberEditText.getText().toString().trim();
            if (!phoneNumber.isEmpty()) {
                sendVerificationCode(phoneNumber);
                //sendVerificationCode("+852 " + phoneNumber); // Prepend country code
            } else {
                Toast.makeText(PhoneNumVerification.this, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show();
            }
        });

        // Submit verification code
        submitCodeButton.setOnClickListener(v -> {
            String code = verificationCodeEditText.getText().toString().trim();
            if (!code.isEmpty() && verificationId != null) {
                verifyCode(code);
            } else {
                Toast.makeText(PhoneNumVerification.this, "Please enter the verification code.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendVerificationCode(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity for callback binding
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                Log.d(TAG, "onVerificationCompleted:" + credential);
                                signInWithPhoneAuthCredential(credential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Log.w(TAG, "onVerificationFailed", e);
                                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                    // Invalid request
                                    Toast.makeText(PhoneNumVerification.this, "Invalid phone number format.", Toast.LENGTH_SHORT).show();
                                } else if (e instanceof FirebaseAuthMissingActivityForRecaptchaException) {
                                    // reCAPTCHA verification attempted with null Activity
                                    Toast.makeText(PhoneNumVerification.this, "reCAPTCHA verification failed.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId,
                                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                Log.d(TAG, "onCodeSent:" + verificationId);
                                PhoneNumVerification.this.verificationId = verificationId;
                                resendToken = token;  // Save token for possible resending
                                Toast.makeText(PhoneNumVerification.this, "Code sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
                            // Navigate to the Login activity
                            Intent intent = new Intent(PhoneNumVerification.this, Login.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Toast.makeText(PhoneNumVerification.this, "Invalid verification code.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }
}
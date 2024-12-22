package com.example.circlea.registration;

import android.content.Context;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthHelper {
    private FirebaseAuth mAuth;

    public FirebaseAuthHelper() {
        mAuth = FirebaseAuth.getInstance();
    }

    public void registerUser(String email, String password, Context context, RegistrationCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            sendEmailVerification(user, context, callback);
                        }
                    } else {
                        Toast.makeText(context, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        callback.onRegistrationFailed(task.getException());
                    }
                });
    }

    private void sendEmailVerification(FirebaseUser user, Context context, RegistrationCallback callback) {
        user.sendEmailVerification()
                .addOnCompleteListener(emailTask -> {
                    if (emailTask.isSuccessful()) {
                        Toast.makeText(context, "Verification email sent.", Toast.LENGTH_SHORT).show();
                        callback.onRegistrationSuccess();
                    } else {
                        Toast.makeText(context, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                        callback.onRegistrationFailed(emailTask.getException());
                    }
                });
    }


    public interface RegistrationCallback {
        void onRegistrationSuccess();
        void onRegistrationFailed(Exception e);
    }
}
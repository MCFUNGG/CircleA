package com.example.circlea.registration;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.R;



public class VaildEmail extends AppCompatActivity {
    private EditText verificationCodeEditText;
    private Button getCodeButton;
    private Button submitCodeButton;
    private TextView verificationMessage;
    private TextView emailAddressTextView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.email_vaild); // Set the layout

        // Initialize views
        verificationCodeEditText = findViewById(R.id.et_verification_code);
        getCodeButton = findViewById(R.id.btn_get_code);
        submitCodeButton = findViewById(R.id.btn_submit_code);
        verificationMessage = findViewById(R.id.tv_email_verification_message);
        emailAddressTextView = findViewById(R.id.tv_email_address);

        // Set up the button click listeners
        getCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        submitCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }


}
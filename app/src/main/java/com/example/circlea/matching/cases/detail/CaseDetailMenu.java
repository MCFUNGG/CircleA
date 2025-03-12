package com.example.circlea.matching.cases.detail;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.R;
import com.example.circlea.matching.cases.detail.book.TutorBooking;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class CaseDetailMenu extends AppCompatActivity {
    private String caseId;
    private double lessonFee;
    private boolean isTutor;
    private MaterialCardView BookingCard;
    private MaterialCardView requestManagementCard;
    private TextView timeManagementDesc;
    private TextView requestManagementDesc;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.case_detail_menu);

        // Get data from intent
        isTutor = getIntent().getBooleanExtra("is_tutor", false);
        caseId = getIntent().getStringExtra("case_id");
        lessonFee = Double.parseDouble(getIntent().getStringExtra("lessonFee"));
    Log.d("case lesson fee(using Double.parseDouble ):", String.valueOf(lessonFee));

        initializeViews();
        setupDescriptions();
        setupClickListeners();
    }

    private void initializeViews() {
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        BookingCard = findViewById(R.id.timeManagementCard);
        requestManagementCard = findViewById(R.id.requestManagementCard);
        timeManagementDesc = findViewById(R.id.timeManagementDesc);
        requestManagementDesc = findViewById(R.id.requestManagementDesc);

        topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void setupDescriptions() {
        if (isTutor) {
            timeManagementDesc.setText("Set/Modify Available Time Slots");
            requestManagementDesc.setText("Review/Reject Appointment Requests");
        } else {
            timeManagementDesc.setText("View/Book Time Slots");
            requestManagementDesc.setText("View Request ");
        }
    }

    private void setupClickListeners() {
        BookingCard.setOnClickListener(v -> {
            Intent intent;

            intent = new Intent(this, TutorBooking.class);
            intent.putExtra("case_id", caseId);
            intent.putExtra("is_tutor", isTutor);
            intent.putExtra("lessonFee",lessonFee);

            Log.d("CaseMenu", "caseId: " + caseId + " ,isTutor: " + isTutor);
            startActivity(intent);
        });

    }
}
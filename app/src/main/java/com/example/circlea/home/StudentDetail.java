package com.example.circlea.home;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.R;

public class StudentDetail extends AppCompatActivity {

    private TextView appIdTextView, memberIdTextView, subjectTextView, classLevelTextView, feeTextView, districtTextView, matchingScoreTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.student_detail);

        appIdTextView = findViewById(R.id.appIdTextView); // TextView to show appId
        memberIdTextView = findViewById(R.id.memberIdTextView); // TextView to show member id
        subjectTextView = findViewById(R.id.subjectTextView); // TextView to show subject
        classLevelTextView = findViewById(R.id.classLevelTextView); // TextView to show class level
        feeTextView = findViewById(R.id.feeTextView); // TextView to show fee
        districtTextView = findViewById(R.id.districtTextView); // TextView to show district
        //districtTextView = findViewById(R.id.matchingScoreTextView); // TextView to show matching

        // Get the appId passed from the previous activity
        String appId = getIntent().getStringExtra("appId");
        String subject = getIntent().getStringExtra("subject");
        String classLevel = getIntent().getStringExtra("classLevel");
        String fee = getIntent().getStringExtra("fee");
        String district = getIntent().getStringExtra("district");
        String memberId = getIntent().getStringExtra("member_id");

        // Display the appId or handle other logic
        if (appId != null) {
            appIdTextView.setText("Application ID: " + appId);
            subjectTextView.setText("Subject: " + subject);
            classLevelTextView.setText("Class level: " + classLevel);
            feeTextView.setText("Fee: $" + fee + " /hr");
            districtTextView.setText("District: " + district);
            memberIdTextView.setText("Member ID: " + memberId);
        }

    }
}

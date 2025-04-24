package com.example.circlea.matching.cases.detail;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.case_detail_menu);
        Log.d("CurrentJava", "CaseDetailMenu");
        // Get data from intent
        isTutor = getIntent().getBooleanExtra("is_tutor", false);
        caseId = getIntent().getStringExtra("case_id");
        lessonFee = Double.parseDouble(getIntent().getStringExtra("lessonFee"));
        Log.d("case lesson fee(using Double.parseDouble ):", String.valueOf(lessonFee));

        initializeViews();
        setupDescriptions();
        setupClickListeners();
        setupSwipeRefresh();
    }

    private void initializeViews() {
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        BookingCard = findViewById(R.id.timeManagementCard);
        requestManagementCard = findViewById(R.id.requestManagementCard);
        timeManagementDesc = findViewById(R.id.timeManagementDesc);
        requestManagementDesc = findViewById(R.id.requestManagementDesc);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void setupDescriptions() {
        if (isTutor) {
            timeManagementDesc.setText(getString(R.string.tutor_time_management_desc));
            requestManagementDesc.setText(getString(R.string.tutor_request_management_desc));
        } else {
            timeManagementDesc.setText(getString(R.string.student_time_management_desc));
            requestManagementDesc.setText(getString(R.string.student_request_management_desc));
        }
    }

    private void setupClickListeners() {
        BookingCard.setOnClickListener(v -> {
            Intent intent;

            intent = new Intent(this, TutorBooking.class);
            intent.putExtra("case_id", caseId);
            intent.putExtra("is_tutor", isTutor);
            intent.putExtra("lessonFee", String.valueOf(lessonFee));

            Log.d("CaseMenu", "caseId: " + caseId + " ,isTutor: " + isTutor);
            startActivity(intent);
        });
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            android.R.color.holo_green_dark,
            android.R.color.holo_orange_dark,
            android.R.color.holo_blue_dark
        );
        
        swipeRefreshLayout.setOnRefreshListener(this::refreshContent);
    }
    
    private void refreshContent() {
        // 在這裡重新載入資料
        Log.d("CaseDetailMenu", "Refreshing content...");
        
        // 模擬網路請求延遲
        new Handler().postDelayed(() -> {
            // 這裡可以添加重新載入卡片資料的代碼
            
            // 完成後停止刷新動畫
            swipeRefreshLayout.setRefreshing(false);
        }, 1000);
    }
}
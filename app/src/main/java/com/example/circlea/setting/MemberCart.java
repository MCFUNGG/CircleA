package com.example.circlea.setting;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.circlea.DatabaseHelper;
import com.example.circlea.R;
import com.example.circlea.home.ApplicationItem;

import java.util.ArrayList;

public class MemberCart extends AppCompatActivity {
    private LinearLayout applicationsContainer;
    private DatabaseHelper dbHelper;
    private Button tutorAppsButton, studentAppsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.member_cart);

        // Initialize views
        applicationsContainer = findViewById(R.id.history_application_container);
        tutorAppsButton = findViewById(R.id.tutorAppsButton);
        studentAppsButton = findViewById(R.id.studentAppsButton);
        dbHelper = new DatabaseHelper(this);

        // Set up button listeners
        tutorAppsButton.setOnClickListener(v -> {
            updateButtonStates(tutorAppsButton);
            loadApplications("tutor");
        });

        studentAppsButton.setOnClickListener(v -> {
            updateButtonStates(studentAppsButton);
            loadApplications("student");
        });

        ImageButton exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> finish());

        // Load tutor applications by default
        updateButtonStates(tutorAppsButton);
        loadApplications("tutor");
    }

    private void updateButtonStates(Button selectedButton) {
        tutorAppsButton.setSelected(false);
        studentAppsButton.setSelected(false);
        selectedButton.setSelected(true);

        // Update visual states
        int selectedColor = ContextCompat.getColor(this, R.color.purple_500);
        int defaultColor = ContextCompat.getColor(this, R.color.gray);

        tutorAppsButton.setTextColor(tutorAppsButton.isSelected() ? selectedColor : defaultColor);
        studentAppsButton.setTextColor(studentAppsButton.isSelected() ? selectedColor : defaultColor);
    }

    private void loadApplications(String type) {
        ArrayList<ApplicationItem> savedApps = dbHelper.getSavedApplicationsByType(type);

        if (savedApps.isEmpty()) {
            showEmptyMessage(type);
            return;
        }

        displayApplications(savedApps);
    }

    private void displayApplications(ArrayList<ApplicationItem> applications) {
        applicationsContainer.removeAllViews();

        for (ApplicationItem app : applications) {
            View itemView = LayoutInflater.from(this)
                    .inflate(R.layout.item_saved_application, applicationsContainer, false);

            // Initialize views
            TextView usernameView = itemView.findViewById(R.id.username);
            TextView classLevelView = itemView.findViewById(R.id.classlevel_tv);
            TextView subjectsView = itemView.findViewById(R.id.subject_tv);
            TextView districtsView = itemView.findViewById(R.id.district_tv);
            TextView feeView = itemView.findViewById(R.id.fee_tv);
            ImageButton removeButton = itemView.findViewById(R.id.remove_button);

            // Set data
            usernameView.setText(app.getUsername());
            classLevelView.setText("Class Level: " + app.getClassLevel());
            subjectsView.setText("Subjects: " + TextUtils.join(", ", app.getSubjects()));
            districtsView.setText("Districts: " + TextUtils.join(", ", app.getDistricts()));
            feeView.setText("Fee: $" + app.getFee());

            // Set up remove button
            removeButton.setOnClickListener(v -> {
                dbHelper.removeApplication(app.getAppId());
                applicationsContainer.removeView(itemView);

                if (applicationsContainer.getChildCount() == 0) {
                    showEmptyMessage(app.getApplicationType());
                }

                Toast.makeText(this, "Application removed", Toast.LENGTH_SHORT).show();
            });

            applicationsContainer.addView(itemView);
        }
    }

    private void showEmptyMessage(String type) {
        applicationsContainer.removeAllViews();

        TextView emptyMessage = new TextView(this);
        emptyMessage.setText("No saved " + type + " applications");
        emptyMessage.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        emptyMessage.setPadding(0, 50, 0, 0);
        emptyMessage.setTextSize(16);
        emptyMessage.setTextColor(ContextCompat.getColor(this, R.color.black));

        applicationsContainer.addView(emptyMessage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload current application type
        String currentType = tutorAppsButton.isSelected() ? "tutor" : "student";
        loadApplications(currentType);
    }
}
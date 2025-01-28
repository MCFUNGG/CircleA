package com.example.circlea;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.circlea.matching.Matching;
import com.example.circlea.application.ApplicationFragment;
import com.example.circlea.application.ApplicationHistory;
import com.example.circlea.application.ParentApplicationFillDetail;
import com.example.circlea.application.ScanCV;
import com.example.circlea.home.HomeFragment;
import com.example.circlea.setting.SettingFragment;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;

public class Home extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        //Local Database print
        CheckSharedPreferences checkSharedPreferences = new CheckSharedPreferences(this);
        checkSharedPreferences.printSharedPreferences();
        //Local Database


        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Inflate custom item layouts
        View customViewApplication = LayoutInflater.from(this).inflate(R.layout.menu_item_custom_application, null);
        TextView customTextViewApplication = customViewApplication.findViewById(R.id.custom_text_application);
        customTextViewApplication.setText("Application - (Parent/Tutor)");

        View customViewTutor = LayoutInflater.from(this).inflate(R.layout.menu_item_custom_tutor, null);
        TextView customTextViewTutor = customViewTutor.findViewById(R.id.custom_text_tutor);
        customTextViewTutor.setText("I am Tutor");

        View customView = LayoutInflater.from(this).inflate(R.layout.menu_item_custom_view, null);

        // Set the custom view as the action view for the placeholder item
        navigationView.getMenu().findItem(R.id.nav_custom_item_application).setActionView(customViewApplication);
        navigationView.getMenu().findItem(R.id.nav_custom_item_tutor).setActionView(customViewTutor);
        navigationView.getMenu().findItem(R.id.nav_custom_item_view).setActionView(customView);
        // Load default Fragment
        loadFragment(new HomeFragment());

        // Set OnClickListener for the menu header
        View headerView = navigationView.getHeaderView(0);
        headerView.setOnClickListener(v -> {
            loadFragment(new SettingFragment()); // Load the SettingFragment
            drawerLayout.closeDrawer(GravityCompat.START); // Close the drawer
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            // Use if-else statements to select the fragment or start an activity
            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_post_application) {
                Intent intent = new Intent(this, ParentApplicationFillDetail.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (item.getItemId() == R.id.nav_history_application) {
                Intent intent = new Intent(this, ApplicationHistory.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (item.getItemId() == R.id.nav_create_cv) {
                Intent intent = new Intent(this, ScanCV.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (item.getItemId() == R.id.nav_matching) {
                selectedFragment = new Matching();
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (item.getItemId() == R.id.nav_application) {
                selectedFragment = new ApplicationFragment();
            } else if (item.getItemId() == R.id.nav_setting) {
                selectedFragment = new SettingFragment();
            } else {
                return false; // Handle unknown menu item
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, fragment);
        transaction.commit();
    }

    public void openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START); // Method to open the drawer
    }
}
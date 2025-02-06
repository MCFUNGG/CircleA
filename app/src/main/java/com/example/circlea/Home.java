package com.example.circlea;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.circlea.matching.Matching;
import com.example.circlea.application.ApplicationFragment;
import com.example.circlea.application.ApplicationHistory;
import com.example.circlea.application.ParentApplicationFillDetail;
import com.example.circlea.setting.ScanCV;
import com.example.circlea.home.HomeFragment;
import com.example.circlea.setting.SettingFragment;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Home extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView headerUsername,headerEmail;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        client = new OkHttpClient();

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
        headerUsername = headerView.findViewById(R.id.username);
        headerEmail = headerView.findViewById(R.id.user_email);
        loadUserInfo();

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

    private void loadUserInfo() {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);
        Toast.makeText(this, "Member ID : "+memberId, Toast.LENGTH_SHORT).show();

        if (memberId == null) {
            Toast.makeText(this, "Member ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = "http://"+IPConfig.getIP()+"/FYP/php/get_member_own_profile.php"; // 更新为您的 URL

        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", memberId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchSettingData", "Request failed: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(Home.this, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    Log.d("FetchSettingData", "Server response: " + jsonResponse);

                    try {
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        if (jsonObject.getBoolean("success")) {
                            JSONArray dataArray = jsonObject.getJSONArray("data");

                            if (dataArray.length() > 0) {
                                JSONObject data = dataArray.getJSONObject(0);
                                String email = data.optString("email", "N/A");
                                String username = data.optString("username", "N/A");
                                //String profileUrl = data.optString("profile", "");

                                //Log.d("ProfileImageURL", "Loading image from URL: " + profileUrl); // Debug log

                                runOnUiThread(() -> {
                                    headerEmail.setText(email);
                                    headerUsername.setText(username);

                                    /*
                                    if (!profileUrl.isEmpty()) {
                                        String fullProfileUrl = "http://10.0.2.2"+profileUrl;
                                        Glide.with(getActivity())
                                                .load(fullProfileUrl)
                                                .into(userIcon);
                                        Toast.makeText(getActivity(), fullProfileUrl, Toast.LENGTH_SHORT).show();
                                    } else {
                                        userIcon.setImageResource(R.drawable.ic_launcher_foreground); // 设置默认头像
                                    }
                                    */
                                });
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("FetchSettingData", "JSON parsing error: " + e.getMessage());
                    }
                }
            }
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
package com.example.circlea;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
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
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Home extends AppCompatActivity {
    // Add these constants for fragment tracking
    private static final String CURRENT_FRAGMENT_KEY = "current_fragment";
    public static final String FRAGMENT_APPLICATION = "application";
    public static final String FRAGMENT_HOME = "home";
    public static final String FRAGMENT_SETTING = "setting";
    public static final String FRAGMENT_MATCHING = "matching";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView headerUsername, headerEmail;
    private OkHttpClient client;

    private Map<Integer, View> notificationDots = new HashMap<>();
    private static final int[] MENU_ITEMS_WITH_BADGE = {
            R.id.nav_home,
            R.id.nav_message,
            R.id.nav_post_application,
            R.id.nav_history_application,
            R.id.nav_create_cv,
            R.id.nav_history_cv
    };

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

        // MODIFIED: Force HomeFragment on first launch
        if (savedInstanceState == null) {
            // Check if this is first app launch
            SharedPreferences prefs = getSharedPreferences("FragmentState", MODE_PRIVATE);
            boolean isFirstLaunch = prefs.getBoolean("FIRST_LAUNCH", true);

            if (isFirstLaunch) {
                // First time launching the app - use HomeFragment and mark as not first launch anymore
                loadFragment(new HomeFragment());
                setCurrentFragment(FRAGMENT_HOME);
                prefs.edit().putBoolean("FIRST_LAUNCH", false).commit();
                Log.d("Home", "First launch - loading HomeFragment");
            } else {
                // Not first launch, check if we're coming back from a language change
                String currentFragment = prefs.getString(CURRENT_FRAGMENT_KEY, FRAGMENT_HOME);
                Log.d("Home", "Not first launch - loading saved fragment: " + currentFragment);

                switch (currentFragment) {
                    case FRAGMENT_APPLICATION:
                        loadFragment(new ApplicationFragment());
                        break;
                    case FRAGMENT_SETTING:
                        loadFragment(new SettingFragment());
                        break;
                    case FRAGMENT_MATCHING:
                        loadFragment(new Matching());
                        break;
                    case FRAGMENT_HOME:
                    default:
                        loadFragment(new HomeFragment());
                        break;
                }
            }
        }

        // Set OnClickListener for the menu header
        View headerView = navigationView.getHeaderView(0);
        headerUsername = headerView.findViewById(R.id.username);
        headerEmail = headerView.findViewById(R.id.user_email);
        loadUserInfo();

        headerView.setOnClickListener(v -> {
            loadFragment(new SettingFragment()); // Load the SettingFragment
            setCurrentFragment(FRAGMENT_SETTING); // Add this line to remember fragment
            drawerLayout.closeDrawer(GravityCompat.START); // Close the drawer
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // First hide the badge (if exists)
            showBadge(itemId, false);

            // Use if-else statements to select the fragment or start an activity
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
                setCurrentFragment(FRAGMENT_HOME);
            } else if (itemId == R.id.nav_post_application) {
                Intent intent = new Intent(this, ParentApplicationFillDetail.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (itemId == R.id.nav_history_application) {
                Intent intent = new Intent(this, ApplicationHistory.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (itemId == R.id.nav_create_cv) {
                Intent intent = new Intent(this, ScanCV.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (itemId == R.id.nav_matching) {
                selectedFragment = new Matching();
                setCurrentFragment(FRAGMENT_MATCHING);
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (itemId == R.id.nav_application) {
                selectedFragment = new ApplicationFragment();
                setCurrentFragment(FRAGMENT_APPLICATION);
            } else if (itemId == R.id.nav_setting) {
                selectedFragment = new SettingFragment();
                setCurrentFragment(FRAGMENT_SETTING);
            } else {
                return false; // Handle unknown menu item
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // setup
        setupAllMenuBadges();
    }

    // Add this method to remember the current fragment
    public void setCurrentFragment(String fragmentKey) {
        Log.d("Home", "Setting current fragment to: " + fragmentKey);
        SharedPreferences prefs = getSharedPreferences("FragmentState", MODE_PRIVATE);
        prefs.edit().putString(CURRENT_FRAGMENT_KEY, fragmentKey).commit(); // Using commit instead of apply for immediate save
    }

    private void setupAllMenuBadges() {
        for (int itemId : MENU_ITEMS_WITH_BADGE) {
            setupMenuItemBadge(itemId);
        }
    }

    private void setupMenuItemBadge(int itemId) {
        MenuItem menuItem = navigationView.getMenu().findItem(itemId);
        if (menuItem != null) {
            FrameLayout customLayout = (FrameLayout) getLayoutInflater().inflate(
                    R.layout.menu_item_with_badge, null);

            View notificationDot = customLayout.findViewById(R.id.notification_dot);
            notificationDots.put(itemId, notificationDot);

            // Modified click event handling
            customLayout.setOnClickListener(v -> {
                showBadge(itemId, false);  // Hide the dot first
                navigationView.setCheckedItem(itemId);
                // Trigger the original menu item click event
                navigationView.getMenu().performIdentifierAction(itemId, 0);
            });

            menuItem.setActionView(customLayout);
        }
    }

    private boolean onNavigationItemSelected(MenuItem item) {
        Fragment selectedFragment = null;

        // Use if-else statements to select the fragment or start an activity
        if (item.getItemId() == R.id.nav_home) {
            selectedFragment = new HomeFragment();
            setCurrentFragment(FRAGMENT_HOME);
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
            setCurrentFragment(FRAGMENT_MATCHING);
        } else if (item.getItemId() == R.id.nav_application) {
            selectedFragment = new ApplicationFragment();
            setCurrentFragment(FRAGMENT_APPLICATION);
        } else if (item.getItemId() == R.id.nav_setting) {
            selectedFragment = new SettingFragment();
            setCurrentFragment(FRAGMENT_SETTING);
        }

        if (selectedFragment != null) {
            loadFragment(selectedFragment);
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void showBadge(int itemId, boolean show) {
        View notificationDot = notificationDots.get(itemId);
        if (notificationDot != null) {
            if (show) {
                notificationDot.setVisibility(View.VISIBLE);
                // Add display animation
                notificationDot.setScaleX(0f);
                notificationDot.setScaleY(0f);
                notificationDot.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .setInterpolator(new OvershootInterpolator())
                        .start();
            } else {
                notificationDot.setVisibility(View.GONE);
            }
        }
    }

    // Clear all badges
    public void clearAllBadges() {
        for (int itemId : MENU_ITEMS_WITH_BADGE) {
            showBadge(itemId, false);
        }
    }

    private void loadUserInfo() {
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);
        Toast.makeText(this, "Member ID : "+memberId, Toast.LENGTH_SHORT).show();

        if (memberId == null) {
            Toast.makeText(this, "Member ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = "http://"+IPConfig.getIP()+"/FYP/php/get_member_own_profile.php";

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

                                runOnUiThread(() -> {
                                    headerEmail.setText(email);
                                    headerUsername.setText(username);
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

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Rebuild the navigation drawer with the new language
        refreshNavigationMenu();
    }
    
    public void refreshNavigationMenu() {
        // Reinflate custom item layouts with updated strings
        View customViewApplication = LayoutInflater.from(this).inflate(R.layout.menu_item_custom_application, null);
        TextView customTextViewApplication = customViewApplication.findViewById(R.id.custom_text_application);
        customTextViewApplication.setText(getString(R.string.nav_post_application));

        View customViewTutor = LayoutInflater.from(this).inflate(R.layout.menu_item_custom_tutor, null);
        TextView customTextViewTutor = customViewTutor.findViewById(R.id.custom_text_tutor);
        customTextViewTutor.setText(getString(R.string.tutor_section_title));

        View customView = LayoutInflater.from(this).inflate(R.layout.menu_item_custom_view, null);

        // Clear the menu and reinflate it with the updated resources
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.menu_main);

        // Set the custom view as the action view for the placeholder item
        navigationView.getMenu().findItem(R.id.nav_custom_item_application).setActionView(customViewApplication);
        navigationView.getMenu().findItem(R.id.nav_custom_item_tutor).setActionView(customViewTutor);
        navigationView.getMenu().findItem(R.id.nav_custom_item_view).setActionView(customView);

        // Setup all menu badges again
        setupAllMenuBadges();
    }
}
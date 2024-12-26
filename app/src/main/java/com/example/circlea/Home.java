package com.example.circlea;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.circlea.application.ApplicationFragment;
import com.example.circlea.home.HomeFragment;
import com.example.circlea.setting.SettingFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home); // 确保这个布局文件存在

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav_menu);

        // 设置默认选择
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // 加载默认的 Fragment
        loadFragment(new HomeFragment());

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                Fragment selectedFragment = null;
                int id = menuItem.getItemId();
                Log.d("HomeActivity", "Selected item ID: " + id); // 记录选中的 ID

                if (id == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (id == R.id.nav_application) {
                    Log.d("HomeActivity", "Navigating to Application");
                    selectedFragment = new ApplicationFragment(); // 创建 ApplicationFragment
                } else if (id == R.id.nav_setting) {
                    Log.d("HomeActivity", "Navigating to Settings");
                    selectedFragment = new SettingFragment();
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                }
                return true;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, fragment); // 确保这个 ID 与您的布局匹配
        transaction.commit();
    }
}
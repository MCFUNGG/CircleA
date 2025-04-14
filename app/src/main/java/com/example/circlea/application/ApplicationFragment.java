package com.example.circlea.application;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.util.Log;
import com.example.circlea.Home;
import com.example.circlea.LanguageManager;
import com.example.circlea.R;
import com.example.circlea.setting.ScanCV;
import android.widget.Toast;
import android.app.AlertDialog;
import com.example.circlea.setting.MyCVActivity;

public class ApplicationFragment extends Fragment {
    private Button btnpost, btnhistory, createCvButton, myCvButton;
    private LanguageManager languageManager;
    private static final String TAG = "ApplicationFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        // Set fragment type as early as possible
        if (getActivity() instanceof Home) {
            ((Home) getActivity()).setCurrentFragment(Home.FRAGMENT_APPLICATION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_application, container, false);

        // CRITICAL: Set current fragment BEFORE doing anything else
        if (getActivity() instanceof Home) {
            Log.d(TAG, "Setting current fragment to APPLICATION");
            ((Home) getActivity()).setCurrentFragment(Home.FRAGMENT_APPLICATION);
        }

        // Initialize LanguageManager
        languageManager = new LanguageManager(requireContext());
        languageManager.applyLanguage();

        // Add language button
        Button languageButton = view.findViewById(R.id.languageButton);
        languageButton.setOnClickListener(v -> {
            Log.d(TAG, "Language button clicked");
            if (getActivity() != null) {
                // IMPORTANT - Save our state AGAIN right before language change
                if (getActivity() instanceof Home) {
                    Log.d(TAG, "Saving state before language change");
                    ((Home) getActivity()).setCurrentFragment(Home.FRAGMENT_APPLICATION);
                }

                // Now change language (this will recreate the activity)
                languageManager.switchLanguage(getActivity());
            }
        });

        // Rest of your code remains the same
        btnpost = view.findViewById(R.id.post_button);
        btnhistory = view.findViewById(R.id.my_application_button);
        createCvButton = view.findViewById(R.id.create_cv_button);
        myCvButton = view.findViewById(R.id.my_cv_button);
        ImageButton menuButton = view.findViewById(R.id.menuButton);

        menuButton.setOnClickListener(v -> {
            if (getActivity() instanceof Home) {
                ((Home) getActivity()).openDrawer();
            }
        });

        btnpost.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ParentApplicationFillDetail.class);
            startActivity(intent);
        });

        createCvButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ScanCV.class);
            startActivity(intent);
        });

        btnhistory.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ApplicationHistory.class);
            startActivity(intent);
        });

        myCvButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyCVActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        // Double-check fragment is set when resuming
        if (getActivity() instanceof Home) {
            ((Home) getActivity()).setCurrentFragment(Home.FRAGMENT_APPLICATION);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }
}
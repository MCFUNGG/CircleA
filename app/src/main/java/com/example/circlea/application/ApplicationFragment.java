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

import com.example.circlea.Home;
import com.example.circlea.R;
import com.example.circlea.setting.ScanCV;

public class ApplicationFragment extends Fragment {
    private Button btnpost, btnhistory, createCvButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_application, container, false);

        btnpost = view.findViewById(R.id.post_button);
        btnhistory = view.findViewById(R.id.my_application_button);
        createCvButton = view.findViewById(R.id.create_cv_button);
        ImageButton menuButton = view.findViewById(R.id.menuButton);

        menuButton.setOnClickListener(v -> {
            ((Home) getActivity()).openDrawer(); // Call method from Home activity
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

        return view;
    }
}
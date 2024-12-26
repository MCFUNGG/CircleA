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

import com.example.circlea.R;

public class ApplicationFragment extends Fragment {
    private Button btnpost, btnhistory, createCvButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_application, container, false);

        // Initialize the button
        btnpost = view.findViewById(R.id.post_button); // Replace with your actual button ID
        btnhistory = view.findViewById(R.id.my_application_button);
        createCvButton = view.findViewById(R.id.create_cv_button);
        // Set the onClick listener for the button
        btnpost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Use getActivity() or requireActivity() to get the context
                Intent intent = new Intent(getActivity(), ParentApplicationFillDetail.class);
                startActivity(intent);
            }
        });



        // Set the onClick listener for the button
        createCvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ScanCV.class);
                startActivity(intent);
            }
        });

        btnhistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Use getActivity() or requireActivity() to get the context
                Intent intent = new Intent(getActivity(), ApplicationHistory.class);
                startActivity(intent);
            }
        });

        return view;
    }
}
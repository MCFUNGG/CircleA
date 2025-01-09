package com.example.circlea.setting;

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
import com.example.circlea.application.ParentApplicationFillDetail;

public class SettingFragment extends Fragment {

    private Button userOwnDetailButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        // Initialize the button
        userOwnDetailButton = view.findViewById(R.id.user_own_detail_button);
        // Set the onClick listener for the button
        userOwnDetailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Use getActivity() or requireActivity() to get the context
                Intent intent = new Intent(getActivity(), MemberDetail.class);
                startActivity(intent);
            }
        });

        return view;
    }
}
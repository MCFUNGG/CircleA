package com.example.circlea.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.circlea.R;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private RecyclerView horizontalRecyclerView, verticalRecyclerView;
    private ImageButton menuButton;
    private EditText searchBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 初始化视图
        horizontalRecyclerView = view.findViewById(R.id.horizontalRecyclerView);
        verticalRecyclerView = view.findViewById(R.id.verticalRecyclerView);

        // 设置水平 RecyclerView
        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        horizontalRecyclerView.setLayoutManager(horizontalLayoutManager);
        //custom
        ArrayList<String> horizontalData = new ArrayList<>();
        horizontalData.add("1");
        horizontalData.add("2");
        horizontalData.add("3");
        HorizontalAdapter horizontalAdapter = new HorizontalAdapter(horizontalData);
        horizontalRecyclerView.setAdapter(horizontalAdapter);


        LinearLayoutManager verticalLayoutManager = new LinearLayoutManager(getContext());
        verticalRecyclerView.setLayoutManager(verticalLayoutManager);

        //custom
        ArrayList<String> verticalData = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            verticalData.add("Tutor " + i);
        }
        VerticalAdapter verticalAdapter = new VerticalAdapter(verticalData);
        verticalRecyclerView.setAdapter(verticalAdapter);

        return view;
    }
}
package com.example.circlea.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.circlea.R;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FindingStudentsAdapter extends RecyclerView.Adapter<FindingStudentsAdapter.ViewHolder> {

    private ArrayList<ApplicationItem> data;
    private Context context;

    public FindingStudentsAdapter(ArrayList<ApplicationItem> data, Context context) {
        this.data = data;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_finding_students, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (data != null && position < data.size()) {
            ApplicationItem application = data.get(position);
            holder.classLevelTextView.setText("Class level: " + application.getClassLevel());
            holder.subjectTextView.setText("Subject: " + application.getSubject());
            holder.districtTextView.setText("District: " + application.getDistrict());
            holder.feeTextView.setText("Fee: $" + application.getFee() + " /hr");

            // Set up star button click listener
            holder.starButton.setOnClickListener(v -> saveAppIdToPreferences(application.getAppId()));
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    private void saveAppIdToPreferences(String appId) {
        Log.d("FindingStudentsAdapter", "Attempting to save appId: " + appId);
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Retrieve existing IDs
        Set<String> appIds = sharedPreferences.getStringSet("selected_app_ids", new HashSet<>());
        appIds.add(appId); // Add the new ID

        editor.putStringSet("selected_app_ids", appIds); // Save back to preferences
        editor.apply();
        Log.d("Preference", "Saved app IDs: " + appIds);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView classLevelTextView;
        TextView subjectTextView;
        TextView districtTextView;
        TextView feeTextView;
        Button starButton; // Add the star button reference

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            classLevelTextView = itemView.findViewById(R.id.classlevel_tv);
            subjectTextView = itemView.findViewById(R.id.subject_tv);
            districtTextView = itemView.findViewById(R.id.district_tv);
            feeTextView = itemView.findViewById(R.id.fee_tv);
            starButton = itemView.findViewById(R.id.star_button); // Initialize the star button
        }
    }
}
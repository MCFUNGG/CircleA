package com.example.circlea.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.circlea.R;

import java.util.ArrayList;

public class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ViewHolder> {

    private ArrayList<ApplicationItem> data;

    public ApplicationAdapter(ArrayList<ApplicationItem> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_finding_tutors, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (data != null && position < data.size()) {
            ApplicationItem application = data.get(position);
            holder.classLevelTextView.setText("Class level: " + application.getClassLevel());

            // Concatenate subjects
            StringBuilder subjects = new StringBuilder("Subjects: ");
            for (String subject : application.getSubjects()) {
                subjects.append(subject).append(", ");
            }
            // Remove trailing comma and space
            if (subjects.length() > 2) {
                subjects.setLength(subjects.length() - 2);
            }
            holder.subjectTextView.setText(subjects.toString());

            // Concatenate districts
            StringBuilder districts = new StringBuilder("Districts: ");
            for (String district : application.getDistricts()) {
                districts.append(district).append(", ");
            }
            // Remove trailing comma and space
            if (districts.length() > 2) {
                districts.setLength(districts.length() - 2);
            }
            holder.districtTextView.setText(districts.toString());

            holder.feeTextView.setText("Fee: $" + application.getFee() + " /hr");
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView classLevelTextView;
        TextView subjectTextView;
        TextView districtTextView;
        TextView feeTextView,username;
        Button starButton;
        LinearLayout layout;
        ImageView profileIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            classLevelTextView = itemView.findViewById(R.id.classlevel_tv);
            subjectTextView = itemView.findViewById(R.id.subject_tv);
            districtTextView = itemView.findViewById(R.id.district_tv);
            feeTextView = itemView.findViewById(R.id.fee_tv);
            starButton = itemView.findViewById(R.id.star_button);
            layout = itemView.findViewById(R.id.item_layout);
            profileIcon = itemView.findViewById(R.id.tutor_icon);
            username = itemView.findViewById(R.id.username);
        }
    }
}
package com.example.circlea.matching;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.circlea.R;
import java.util.List;

public class MatchingCaseAdapter extends RecyclerView.Adapter<MatchingCaseAdapter.ViewHolder> {
    private List<MatchingCase> cases;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MatchingCase matchingCase, int position);
    }

    public MatchingCaseAdapter(Context context, List<MatchingCase> cases) {
        this.context = context;
        this.cases = cases;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_matching_case, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MatchingCase matchingCase = cases.get(position);

        // Set fee
        holder.feeTv.setText("HK$" + matchingCase.getFee() + "/hr");

        // Set status and its background color
        String status = matchingCase.getStatus();
        if (status.equalsIgnoreCase("P")) {
            holder.statusTv.setText("Pending");
            holder.statusTv.setBackground(ContextCompat.getDrawable(context, R.drawable.status_pending_pill));
        } else if (status.equalsIgnoreCase("A")) {
            holder.statusTv.setText("Approved");
            holder.statusTv.setBackground(ContextCompat.getDrawable(context, R.drawable.status_approved_pill));
        }
        // Set username
        holder.username.setText(matchingCase.getUsername());

        // Set other details
        holder.classLevelTv.setText(matchingCase.getClassLevel());
        holder.subjectTv.setText(matchingCase.getSubjects());
        holder.districtTv.setText(matchingCase.getDistricts());

        // Load profile image
        String profileUrl = matchingCase.getProfileIcon();
        if (profileUrl != null && !profileUrl.isEmpty()) {
            String fullProfileUrl = "http://10.0.2.2" + profileUrl;
            Glide.with(context)
                    .load(fullProfileUrl)
                    .placeholder(R.drawable.circle_background)
                    .error(R.drawable.circle_background)
                    .into(holder.profileIcon);
        } else {
            holder.profileIcon.setImageResource(R.drawable.circle_background);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(matchingCase, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cases != null ? cases.size() : 0;
    }

    public void updateData(List<MatchingCase> newCases) {
        this.cases = newCases;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView feeTv, statusTv, username, classLevelTv, subjectTv, districtTv;
        ImageView profileIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            feeTv = itemView.findViewById(R.id.fee_tv);
            statusTv = itemView.findViewById(R.id.status_tv);
            username = itemView.findViewById(R.id.username);
            classLevelTv = itemView.findViewById(R.id.classlevel_tv);
            subjectTv = itemView.findViewById(R.id.subject_tv);
            districtTv = itemView.findViewById(R.id.district_tv);
            profileIcon = itemView.findViewById(R.id.profileIcon);
        }
    }
}
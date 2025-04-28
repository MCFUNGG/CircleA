package com.example.circlea.matching.request;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.circlea.IPConfig;
import com.example.circlea.R;
import java.util.List;

public class MatchingRequestReceivedAdapter extends RecyclerView.Adapter<MatchingRequestReceivedAdapter.ViewHolder> {
    private List<MatchingRequest> requests;
    private Context context;
    private OnItemClickListener listener;
    private String requestType;
    private String currentUsername;

    public interface OnItemClickListener {
        void onItemClick(MatchingRequest request, int position);
    }

    public MatchingRequestReceivedAdapter(Context context, List<MatchingRequest> requests, String requestType) {
        this.context = context;
        this.requests = requests;
        this.requestType = requestType;
        SharedPreferences sharedPreferences = context.getSharedPreferences("CircleA", Context.MODE_PRIVATE);
        this.currentUsername = sharedPreferences.getString("username", "");
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_matching_request_received, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MatchingRequest request = requests.get(position);

        // Set username based on match creator
        String displayUsername = request.getDisplayName(true, currentUsername);
        holder.username.setText(displayUsername);

        // Set other details
        holder.fee.setText("HK$" + request.getFee());
        holder.classLevel.setText(request.getClassLevel());
        holder.subject.setText(request.getSubjects());
        holder.district.setText(request.getDistricts());

        // Handle profile icon
        String profileUrl = request.getProfileIcon();
        if (profileUrl != null && !profileUrl.isEmpty()) {
            // Store the path portion for passing to the detail page
            String profilePath = profileUrl;
            
            // Use the full URL for loading the image in the current view
            String fullProfileUrl = "http://" + IPConfig.getIP() + profileUrl.trim();
            Log.d("MatchingRequestAdapter", "Loading profile: " + fullProfileUrl);
            Glide.with(context)
                    .load(fullProfileUrl)
                    .placeholder(R.drawable.circle_background)
                    .error(R.drawable.circle_background)
                    .into(holder.profileIcon);
        } else {
            holder.profileIcon.setImageResource(R.drawable.circle_background);
        }

        holder.itemView.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = context.getSharedPreferences("CircleA", Context.MODE_PRIVATE);
            String userMemberId = sharedPreferences.getString("member_id", null);

            if (userMemberId != null) {
                Intent intent = new Intent(context, RequestReceivedDetail.class);

                // Common data
                intent.putExtra("match_id", request.getMatchId());
                intent.putExtra("fee", request.getFee());
                intent.putExtra("class_level", request.getClassLevel());
                intent.putExtra("subjects", request.getSubjects());
                intent.putExtra("districts", request.getDistricts());
                intent.putExtra("match_mark", request.getMatchMark());
                
                // 传递两个版本的URL，确保其中一个能正常工作
                intent.putExtra("profile_icon", profileUrl);
                intent.putExtra("target_profileUrl", profileUrl);
                
                intent.putExtra("match_creator", request.getMatchCreator());
                // Add role-specific data
                if (request.getMatchCreator().equals("PS")) {
                    intent.putExtra("ps_app_id", request.getPsAppId());
                    intent.putExtra("psUsername", request.getPsUsername());
                } else {
                    intent.putExtra("tutor_app_id", request.getTutorAppId());
                    intent.putExtra("tutorUsername", request.getTutorUsername());
                }

                context.startActivity(intent);
            } else {
                Log.e("MatchingRequestAdapter", "No member_id found in SharedPreferences.");
                Toast.makeText(context, "Error: User information not found", Toast.LENGTH_SHORT).show();
            }

            if (listener != null) {
                listener.onItemClick(request, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView username, fee, classLevel, subject, district;
        ImageView profileIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            fee = itemView.findViewById(R.id.fee_tv);
            classLevel = itemView.findViewById(R.id.classlevel_tv);
            subject = itemView.findViewById(R.id.subject_tv);
            district = itemView.findViewById(R.id.district_tv);
            profileIcon = itemView.findViewById(R.id.profileIcon);
        }
    }

    public void updateData(List<MatchingRequest> newRequests) {
        this.requests = newRequests;
        notifyDataSetChanged();
    }
}
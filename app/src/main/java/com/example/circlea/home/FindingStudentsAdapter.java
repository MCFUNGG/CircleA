package com.example.circlea.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.circlea.IPConfig;
import com.example.circlea.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

            // Load the profile icon using Glide
            String profileUrl = application.getProfileIcon(); // Assuming this method exists in ApplicationItem
            if (profileUrl != null && !profileUrl.isEmpty()) {
                String fullProfileUrl = "http://"+ IPConfig.getIP() + profileUrl;
                Glide.with(context)
                        .load(fullProfileUrl)
                        .placeholder(R.drawable.circle_background) // This is your default image
                        .into(holder.profileIcon);
            } else {
                // Load the default image
                holder.profileIcon.setImageResource(R.drawable.circle_background); // Use the default drawable
            }

            holder.username.setText(application.getUsername());
            holder.classLevelTextView.setText("aims: "+application.getClassLevel());

            // Concatenate subjects
            StringBuilder subjects = new StringBuilder("");
            for (String subject : application.getSubjects()) {
                subjects.append(subject).append(", ");
            }
            if (subjects.length() > 2) {
                subjects.setLength(subjects.length() - 2);
            }
            holder.subjectTextView.setText(subjects.toString());

            // Concatenate districts
            StringBuilder districts = new StringBuilder("");
            for (String district : application.getDistricts()) {
                districts.append(district).append(", ");
            }
            if (districts.length() > 2) {
                districts.setLength(districts.length() - 2);
            }
            holder.districtTextView.setText(districts.toString());

            holder.feeTextView.setText("$" + application.getFee());

            holder.layout.setOnClickListener(v -> {
                SharedPreferences sharedPreferences = context.getSharedPreferences("CircleA", Context.MODE_PRIVATE);
                String userMemberId = sharedPreferences.getString("member_id", null);
                String tutorsMemberId = application.getMemberId();
                String AppId = application.getAppId();

                if (userMemberId != null && tutorsMemberId != null && AppId != null) {
                    Log.d("RetrieveMemberID", "UserMemberID: " + userMemberId);
                    Log.d("RetrieveMemberID", "TutorsMemberID: " + tutorsMemberId);
                    sendMemberIdsToServer(userMemberId, tutorsMemberId,AppId);
                } else {
                    Log.d("RetrieveMemberID", "No member_id found in SharedPreferences.");
                    Toast.makeText(context, "holder.layout.setOnClickListener:error", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Send the entire list of subjects and districts
                Intent intent = new Intent(context, TutorAppDetail.class);
                intent.putExtra("tutor_id", application.getMemberId());
                intent.putStringArrayListExtra("subjects", application.getSubjects()); // Send all subjects
                intent.putExtra("classLevel", application.getClassLevel());
                intent.putExtra("fee", application.getFee());
                intent.putStringArrayListExtra("districts", application.getDistricts()); // Send all districts
                intent.putExtra("tutotAppId", application.getAppId());
                context.startActivity(intent);
            });

            holder.starButton.setOnClickListener(v -> saveAppIdToPreferences(application.getAppId()));
        }
    }

    private void sendMemberIdsToServer(String userMemberId, String tutorsMemberId, String AppId) {
        OkHttpClient client = new OkHttpClient();
        String url = "http://"+ IPConfig.getIP()+"/Matching/get_MemberID.php";

        RequestBody formBody = new FormBody.Builder()
                .add("PSMemberID", userMemberId)
                .add("TutorsMemberID", tutorsMemberId)
                .add("AppId",AppId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SendMemberID", "Request failed: " + e.getMessage());
                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Failed to send member IDs. Please try again.", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String serverResponse = response.body().string();
                    Log.d("SendMemberID", "Server response: " + serverResponse);
                    ((Activity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Data sent successfully.", Toast.LENGTH_SHORT).show()
                    );
                } else {
                    Log.e("SendMemberID", "Server returned error: " + response.code());
                    ((Activity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Server error. Please try again.", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    private void saveAppIdToPreferences(String appId) {
        Log.d("FindingStudentsAdapter", "Attempting to save appId: " + appId);
        SharedPreferences sharedPreferences = context.getSharedPreferences("CircleA", Context.MODE_PRIVATE);
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
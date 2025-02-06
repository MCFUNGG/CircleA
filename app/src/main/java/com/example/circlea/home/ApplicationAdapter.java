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

public class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ViewHolder> {

    private ArrayList<ApplicationItem> data;
    private Context context;

    public ApplicationAdapter(ArrayList<ApplicationItem> data, Context context) {
        this.data = data;
        this.context = context;
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

            // Load profile icon using Glide
            String profileUrl = application.getProfileIcon();
            if (profileUrl != null && !profileUrl.isEmpty()) {
                String fullProfileUrl = "http://"+ IPConfig.getIP() + profileUrl;
                Glide.with(context)
                        .load(fullProfileUrl)
                        .placeholder(R.drawable.circle_background)
                        .into(holder.profileIcon);
            } else {
                holder.profileIcon.setImageResource(R.drawable.circle_background);
            }

            holder.username.setText(application.getUsername());
            holder.classLevelTextView.setText("aims: " + application.getClassLevel());

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
                String tutorId = sharedPreferences.getString("member_id", null);
                String psId = application.getMemberId();
                String psAppId = application.getAppId();

                if (tutorId != null && psId != null && psAppId != null) {
                    Log.d("RetrieveMemberID", "TutorID: " + tutorId);
                    Log.d("RetrieveMemberID", "PSID: " + psId);
                    sendMemberIdsToServer(tutorId, psId, psAppId);
                } else {
                    Log.d("RetrieveMemberID", "Missing required IDs");
                    Toast.makeText(context, "Error: Missing required information", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Navigate to PSAppDetail
                Intent intent = new Intent(context, PSAppDetail.class);
                intent.putExtra("ps_id", application.getMemberId());
                intent.putStringArrayListExtra("subjects", application.getSubjects());
                intent.putExtra("classLevel", application.getClassLevel());
                intent.putExtra("fee", application.getFee());
                intent.putStringArrayListExtra("districts", application.getDistricts());
                intent.putExtra("psAppId", application.getAppId());
                context.startActivity(intent);
            });

            holder.starButton.setOnClickListener(v -> saveAppIdToPreferences(application.getAppId()));
        }
    }

    private void sendMemberIdsToServer(String tutorId, String psId, String psAppId) {
        OkHttpClient client = new OkHttpClient();
        String url = "http://"+ IPConfig.getIP()+"/Matching/get_MemberID.php";

        RequestBody formBody = new FormBody.Builder()
                .add("TutorID", tutorId)
                .add("PSID", psId)
                .add("PSAppId", psAppId)
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
                        Toast.makeText(context, "Failed to send member IDs", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String serverResponse = response.body().string();
                    Log.d("SendMemberID", "Server response: " + serverResponse);
                    ((Activity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Data sent successfully", Toast.LENGTH_SHORT).show());
                } else {
                    Log.e("SendMemberID", "Server error: " + response.code());
                    ((Activity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Server error", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void saveAppIdToPreferences(String appId) {
        Log.d("PSApplicationAdapter", "Saving appId: " + appId);
        SharedPreferences sharedPreferences = context.getSharedPreferences("CircleA", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> appIds = sharedPreferences.getStringSet("selected_ps_app_ids", new HashSet<>());
        appIds.add(appId);

        editor.putStringSet("selected_ps_app_ids", appIds);
        editor.apply();
        Log.d("Preference", "Saved PS app IDs: " + appIds);
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView classLevelTextView;
        TextView subjectTextView;
        TextView districtTextView;
        TextView feeTextView, username;
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
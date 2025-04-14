package com.example.circlea.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
// Uncomment if using an ImageView for profile icons
// import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.circlea.DatabaseHelper;
import com.example.circlea.IPConfig;
import com.example.circlea.R;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VerticalAdapter extends RecyclerView.Adapter<VerticalAdapter.ViewHolder> {
    private ArrayList<ApplicationItem> data;
    private Context context;
    private DatabaseHelper dbHelper;
    private OnItemClickListener mListener;
    
    // 定義項目點擊監聽器接口
    public interface OnItemClickListener {
        void onItemClick(int position, ApplicationItem item);
    }
    
    // 設置點擊監聽器的方法
    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public VerticalAdapter(ArrayList<ApplicationItem> data, Context context) {
        this.data = data;
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_vertical, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (data != null && position < data.size()) {
            ApplicationItem application = data.get(position);

            holder.username.setText(application.getUsername());
            holder.classLevelTextView.setText(application.getClassLevel());


            Log.d("ApplicationAdapter", "onBindViewHolder: " + application.getSubjects());


            // Concatenate subjects
            StringBuilder subjects = new StringBuilder();
            for (String subject : application.getSubjects()) {
                subjects.append(subject).append(", ");
            }
            if (subjects.length() > 2) {
                subjects.setLength(subjects.length() - 2);
            }
            holder.subjectTextView.setText(subjects.toString());

            // Concatenate districts
            StringBuilder districts = new StringBuilder();
            for (String district : application.getDistricts()) {
                districts.append(district).append(", ");
            }
            if (districts.length() > 2) {
                districts.setLength(districts.length() - 2);
            }
            holder.districtTextView.setText(districts.toString());

            holder.feeTextView.setText("$" + application.getFee());

            holder.ratingTextView.setText(application.getRating());

            // Load the profile icon using Glide (same as in FindingStudentsAdapter)
            String profileUrl = application.getProfileIcon();
            if (profileUrl != null && !profileUrl.isEmpty()) {
                String fullProfileUrl = "http://" + IPConfig.getIP() + profileUrl;
                Glide.with(context)
                        .load(fullProfileUrl)
                        .placeholder(R.drawable.circle_background)
                        .into(holder.profileIcon);
            } else {
                holder.profileIcon.setImageResource(R.drawable.circle_background);
            }


            holder.layout.setOnClickListener(v -> {
                // 如果使用了自定義點擊監聽器，優先調用它
                if (mListener != null) {
                    mListener.onItemClick(position, application);
                    return;
                }
                
                // 默認的點擊行為 - 打開TutorAppDetail頁面
                SharedPreferences sharedPreferences = context.getSharedPreferences("CircleA", Context.MODE_PRIVATE);
                String userMemberId = sharedPreferences.getString("member_id", null);
                String tutorsMemberId = application.getMemberId();
                String AppId = application.getAppId();

                if (userMemberId != null && tutorsMemberId != null && AppId != null) {
                    sendMemberIdsToServer(userMemberId, tutorsMemberId, AppId);
                } else {
                    Log.d("RetrieveMemberID", "No member_id found in SharedPreferences.");
                    Toast.makeText(context, "Error: Missing information", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(context, TutorAppDetail.class);
                intent.putExtra("tutor_id", application.getMemberId());
                intent.putExtra("tutorName", application.getUsername());
                intent.putStringArrayListExtra("subjects", application.getSubjects());
                intent.putExtra("classLevel", application.getClassLevel());
                intent.putExtra("fee", application.getFee());
                intent.putStringArrayListExtra("districts", application.getDistricts());
                intent.putExtra("tutotAppId", application.getAppId());
                context.startActivity(intent);
            });
        }
    }

    private void sendMemberIdsToServer(String tutorId, String psId, String psAppId) {
        OkHttpClient client = new OkHttpClient();
        String url = "http://" + IPConfig.getIP() + "/Matching/get_MemberID.php";

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


    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView username;
        public TextView ratingTextView;
        public TextView feeTextView;
        public TextView classLevelTextView;
        public TextView subjectTextView;
        public TextView districtTextView;
        public ImageView profileIcon; // Uncommented to display the user icon
        LinearLayout layout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            ratingTextView = itemView.findViewById(R.id.rating_tv);
            feeTextView = itemView.findViewById(R.id.fee_tv);
            classLevelTextView = itemView.findViewById(R.id.classlevel_tv);
            subjectTextView = itemView.findViewById(R.id.subject_tv);
            districtTextView = itemView.findViewById(R.id.district_tv);
            profileIcon = itemView.findViewById(R.id.tutor_icon);
            layout = itemView.findViewById(R.id.item_layout);
        }
    }
}
package com.example.circlea.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.circlea.R;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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
    private static final String SERVER_ADDRESS = "localhost";  // Change to your server address
    private static final int SERVER_PORT = 8080;  // Change to your server port


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

            // Set up the LinearLayout click listener to navigate to the detail activity
            holder.layout.setOnClickListener(v -> {
                // Retrieve the member_id from SharedPreferences
                SharedPreferences sharedPreferences = context.getSharedPreferences("CircleA", Context.MODE_PRIVATE);
                String TutorsMemberID = sharedPreferences.getString("member_id", null);
                //get application id
                String UserMemberId = application.getMemberId();

                if (UserMemberId != null) {
                    // Use the member_id (e.g., for logging or additional operations)
                    Log.d("RetrieveMemberID", "Retrieved member_id: " + UserMemberId);
                    Log.d("RetrieveMemberID", "retreived tutor member id: " + TutorsMemberID);
                    // Send the member_id to the PHP server
                    sendMemberIdToServerPS(UserMemberId);
                    sendMemberIdToServerT(TutorsMemberID);
                } else {
                    // Handle case when member_id is not found
                    Log.d("RetrieveMemberID", "No member_id found in SharedPreferences.");
                    Toast.makeText(context, "User not logged in. Please log in first.", Toast.LENGTH_SHORT).show();
                    return; // Exit if member_id is required and not found
                }

                // Continue with navigating to the detail activity
                // Continue with navigating to the detail activity
                Intent intent = new Intent(context, StudentDetail.class);
                intent.putExtra("member_id", application.getMemberId());
                intent.putExtra("subject", application.getSubject());
                intent.putExtra("classLevel", application.getClassLevel());
                intent.putExtra("fee", application.getFee());
                intent.putExtra("district", application.getDistrict());
                intent.putExtra("appId", application.getAppId());
                context.startActivity(intent);
            });


            // Set up star button click listener to save appId and navigate to the detail activity
            holder.starButton.setOnClickListener(v -> saveAppIdToPreferences(application.getMemberId()));
        }
    }

    // 使用 OkHttp 发送 member_id 到 matching_T.php
    private void sendMemberIdToServerT(String TutorsMemberID) {
        OkHttpClient client = new OkHttpClient();

        String url = "http://10.0.2.2/Matching/get_MemberID.php";

        // Create the POST request body with the number
        RequestBody formBody = new FormBody.Builder()
                .add("TutorsMemberID", String.valueOf(TutorsMemberID))
                .build();

        // Create the POST request
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        Log.d("getNumber", "Number: " + TutorsMemberID);

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SendMemberID", "Request failed: " + e.getMessage());
                // Notify user of failure
                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Failed to send member_id. Please try again.", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String serverResponse = response.body().string();
                    Log.d("SendMemberID", "Server response: " + serverResponse);

                    // Notify user of success
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

    private void sendMemberIdToServerPS(String UserMemberId) {
        OkHttpClient client = new OkHttpClient();

        // Build the URL with the member_id as a GET parameter
        String url = "http://10.0.2.2/Matching/get_MemberID.php";

        // Create the POST request body with the number
        RequestBody formBody = new FormBody.Builder()
                .add("PSMemberID", String.valueOf(UserMemberId))
                .build();

        // Create the POST request
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        Log.d("getNumber", "Number: " + UserMemberId);

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SendMemberID", "Request failed: " + e.getMessage());
                // Notify user of failure
                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Failed to send member_id. Please try again.", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String serverResponse = response.body().string();
                    Log.d("SendMemberID", "Server response: " + serverResponse);

                    // Notify user of success
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
        TextView feeTextView;
        Button starButton;
        LinearLayout layout; // LinearLayout reference

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            classLevelTextView = itemView.findViewById(R.id.classlevel_tv);
            subjectTextView = itemView.findViewById(R.id.subject_tv);
            districtTextView = itemView.findViewById(R.id.district_tv);
            feeTextView = itemView.findViewById(R.id.fee_tv);
            starButton = itemView.findViewById(R.id.star_button);
            layout = itemView.findViewById(R.id.item_layout); // Initialize LinearLayout reference
        }
    }
}
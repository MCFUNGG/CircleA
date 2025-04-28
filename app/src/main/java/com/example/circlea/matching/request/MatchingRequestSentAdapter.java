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
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MatchingRequestSentAdapter extends RecyclerView.Adapter<MatchingRequestSentAdapter.ViewHolder> {
    private static final String TAG = "MatchingRequestSentAdapter";
    
    private List<MatchingRequest> requests;
    private Context context;
    private OnItemClickListener listener;
    private String currentUsername;
    private String memberId;

    public interface OnItemClickListener {
        void onItemClick(MatchingRequest request, int position);
    }
    
    // Add interface for refreshing parent after cancellation
    public interface OnRequestCancelledListener {
        void onRequestCancelled();
    }
    
    private OnRequestCancelledListener cancelListener;
    
    public void setOnRequestCancelledListener(OnRequestCancelledListener listener) {
        this.cancelListener = listener;
    }

    public MatchingRequestSentAdapter(Context context, List<MatchingRequest> requests) {
        this.context = context;
        this.requests = requests;
        SharedPreferences sharedPreferences = context.getSharedPreferences("CircleA", Context.MODE_PRIVATE);
        this.currentUsername = sharedPreferences.getString("username", "");
        this.memberId = sharedPreferences.getString("member_id", "");
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_matching_request_sent, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MatchingRequest request = requests.get(position);

        // 始终显示接收者的用户名
        String displayUsername = request.getRecipientUsername();
        if (displayUsername == null || displayUsername.isEmpty()) {
            // 如果未设置接收者用户名，则通过比较判断
            if (currentUsername.equals(request.getPsUsername())) {
                // 如果当前用户是PS，则显示家教的用户名
                displayUsername = request.getTutorUsername();
            } else {
                // 如果当前用户是家教，则显示PS的用户名
                displayUsername = request.getPsUsername();
            }
        }
        holder.username.setText(displayUsername);

        // Determine if the request was sent as PS
        boolean isSentAsPS = request.getPsUsername().equals(currentUsername);

        // Set other details
        holder.fee.setText("HK$" + request.getFee() + "/hr");
        holder.classLevel.setText(request.getClassLevel());
        holder.subject.setText(request.getSubjects());
        holder.district.setText(request.getDistricts());

        // Handle profile icon
        String profileUrl = request.getProfileIcon();
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

        // Set cancel button click listener
        holder.cancelButton.setOnClickListener(v -> {
            showCancelConfirmationDialog(request);
        });

        holder.itemView.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = context.getSharedPreferences("CircleA", Context.MODE_PRIVATE);
            String userMemberId = sharedPreferences.getString("member_id", null);

            if (userMemberId != null) {
                Intent intent = new Intent(context, RequestSentDetail.class);

                // Common data
                intent.putExtra("match_id", request.getMatchId());
                intent.putExtra("fee", request.getFee());
                intent.putExtra("class_level", request.getClassLevel());
                intent.putExtra("subjects", request.getSubjects());
                intent.putExtra("districts", request.getDistricts());
                intent.putExtra("match_mark", request.getMatchMark());
                intent.putExtra("profile_icon", request.getProfileIcon());
                intent.putExtra("match_creator", request.getMatchCreator());

                // Add sender role information and role-specific data
                intent.putExtra("sent_as_ps", isSentAsPS);
                if (isSentAsPS) {
                    intent.putExtra("ps_app_id", request.getPsAppId());
                    intent.putExtra("recipient_username", request.getTutorUsername());
                } else {
                    intent.putExtra("tutor_app_id", request.getTutorAppId());
                    intent.putExtra("recipient_username", request.getPsUsername());
                }

                context.startActivity(intent);

                Log.d("MatchingRequestSentAdapter", "Opening sent request detail: " + request.getMatchId());
            } else {
                Log.e("MatchingRequestSentAdapter", "No member_id found in SharedPreferences.");
                Toast.makeText(context, "Error: User information not found", Toast.LENGTH_SHORT).show();
            }

            if (listener != null) {
                listener.onItemClick(request, position);
            }
        });
    }
    
    private void showCancelConfirmationDialog(MatchingRequest request) {
        new AlertDialog.Builder(context)
                .setTitle("Cancel Request")
                .setMessage("Are you sure you want to cancel this matching request?")
                .setPositiveButton("Yes", (dialog, which) -> cancelMatchRequest(request.getMatchId()))
                .setNegativeButton("No", null)
                .show();
    }
    
    private void cancelMatchRequest(String matchId) {
        if (matchId == null || memberId == null) {
            Toast.makeText(context, "Error: Missing request information", Toast.LENGTH_SHORT).show();
            return;
        }
        
        OkHttpClient client = new OkHttpClient();
        
        RequestBody formBody = new FormBody.Builder()
                .add("match_id", matchId)
                .add("member_id", memberId)
                .build();
        
        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/cancel_match_request.php")
                .post(formBody)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to cancel request: " + e.getMessage());
                if (context != null) {
                    ((android.app.Activity) context).runOnUiThread(() -> 
                        Toast.makeText(context, "Failed to cancel request. Please try again.", Toast.LENGTH_SHORT).show());
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "Cancel response: " + responseData);
                
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    final boolean success = jsonResponse.getBoolean("success");
                    final String message = jsonResponse.getString("message");
                    
                    if (context != null) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            if (success && cancelListener != null) {
                                // Notify parent to refresh data
                                cancelListener.onRequestCancelled();
                            }
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing response: " + e.getMessage());
                    if (context != null) {
                        ((android.app.Activity) context).runOnUiThread(() -> 
                            Toast.makeText(context, "Error processing server response", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView username, fee, classLevel, subject, district, statusText;
        ImageView profileIcon;
        MaterialButton cancelButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            fee = itemView.findViewById(R.id.fee_tv);
            classLevel = itemView.findViewById(R.id.classlevel_tv);
            subject = itemView.findViewById(R.id.subject_tv);
            district = itemView.findViewById(R.id.district_tv);
            profileIcon = itemView.findViewById(R.id.profileIcon);
            statusText = itemView.findViewById(R.id.status_text);
            cancelButton = itemView.findViewById(R.id.dialogCancelButton);
        }
    }

    public void updateData(List<MatchingRequest> newRequests) {
        this.requests = newRequests;
        notifyDataSetChanged();
    }
}
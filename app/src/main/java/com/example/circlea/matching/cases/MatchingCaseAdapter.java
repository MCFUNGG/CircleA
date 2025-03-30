package com.example.circlea.matching.cases;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.example.circlea.home.PSAppDetail;
import com.example.circlea.matching.cases.detail.CaseDetailMenu;
import com.example.circlea.matching.cases.detail.MatchingCaseDetailStudent;

import org.json.JSONArray;
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
        SharedPreferences sharedPreferences = context.getSharedPreferences("CircleA", Context.MODE_PRIVATE);
        String currentMemberId = sharedPreferences.getString("member_id", "");

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
        String profileUrl;
        if (currentMemberId.equals(matchingCase.getPsId())) {
            // If current user is student, show tutor's profile
            profileUrl = matchingCase.getTutorProfileIcon();
            holder.username.setText(matchingCase.getTutorUsername());
        } else if (currentMemberId.equals(matchingCase.getTutorId())) {
            // If current user is tutor, show student's profile
            profileUrl = matchingCase.getPsProfileIcon();
            holder.username.setText(matchingCase.getPsUsername());
        } else {
            profileUrl = "";
        }

        if (profileUrl != null && !profileUrl.isEmpty() && !profileUrl.equals("N/A")) {
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

                if (matchingCase.getStatus().equalsIgnoreCase("A")) {
                    // Check user role
                    if (currentMemberId.equals(matchingCase.getTutorId())) {
                        // User is tutor - can set available slots
                        Intent intent = new Intent(context, CaseDetailMenu.class);
                        intent.putExtra("case_id", matchingCase.getMatchId());
                        intent.putExtra("is_tutor", true);
                        intent.putExtra("lessonFee", matchingCase.getFee());
                        context.startActivity(intent);
                    } else if (currentMemberId.equals(matchingCase.getPsId())) {
                        // User is student - show available slots or waiting message
                        // TODO: Check if tutor has created time slots

                        showStudentOptionsDialog(matchingCase);
                    }
                }
            }
        });
    }



    private void showStudentOptionsDialog(MatchingCase matchingCase) {
        // First, check if there are any available slots
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("match_id", matchingCase.getMatchId())
                .add("is_tutor", "false")  // Checking as student
                .build();

        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_time_slot.php";

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                ((Activity) context).runOnUiThread(() -> {
                    showNoSlotsDialog("Failed to check available slots");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    JSONArray slotsArray = jsonResponse.optJSONArray("slots");

                    ((Activity) context).runOnUiThread(() -> {
                        if (jsonResponse.optBoolean("success", false) && slotsArray != null && slotsArray.length() > 0) {
                            // There are available slots, show the selection screen
                            Intent intent = new Intent(context, MatchingCaseDetailStudent.class);
                            intent.putExtra("case_id", matchingCase.getMatchId());
                            intent.putExtra("is_tutor", false);
                            intent.putExtra("tutor_id",matchingCase.getTutorId());
                            intent.putExtra("lessonFee",matchingCase.getFee());
                                Log.d("MatchingCaseAdapter",
                                    " case_id: "+matchingCase.getMatchId()+
                                    " lesson Fee: $"+matchingCase.getFee()
                            );

                            context.startActivity(intent);
                        } else {
                            // No slots available
                            showNoSlotsDialog("Waiting for tutor to create available time slots");
                        }
                    });
                } catch (Exception e) {
                    ((Activity) context).runOnUiThread(() -> {
                        showNoSlotsDialog("Error checking available slots");
                    });
                }
            }
        });
    }

    private void showNoSlotsDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Time Slot Options")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
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
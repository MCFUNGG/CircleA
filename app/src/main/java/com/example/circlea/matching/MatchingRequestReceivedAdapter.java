    package com.example.circlea.matching;

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
    import com.example.circlea.R;
    import java.util.List;

    public class MatchingRequestReceivedAdapter extends RecyclerView.Adapter<MatchingRequestReceivedAdapter.ViewHolder> {
        private List<MatchingRequest> requests;
        private Context context;
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(MatchingRequest request, int position);
        }

        public MatchingRequestReceivedAdapter(Context context, List<MatchingRequest> requests) {
            this.context = context;
            this.requests = requests;
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

            Log.d("MatchingRequestAdapter", "Setting username: " + request.getPsUsername());
            holder.username.setText(request.getPsUsername());
            holder.fee.setText("HK$" + request.getFee());
            holder.classLevel.setText(request.getClassLevel());
            holder.subject.setText(request.getSubjects());
            holder.district.setText(request.getDistricts());

            String profileUrl = request.getProfileIcon(); // Assuming this method exists in ApplicationItem
            if (profileUrl != null && !profileUrl.isEmpty()) {
                String fullProfileUrl = "http://10.0.2.2" + profileUrl;
                Glide.with(context)
                        .load(fullProfileUrl)
                        .placeholder(R.drawable.circle_background) // This is your default image
                        .into(holder.profileIcon);
            } else {
                // Load the default image
                holder.profileIcon.setImageResource(R.drawable.circle_background); // Use the default drawable
            }

            Log.d("MatchingRequestAdapter", "holder: " + holder.username.getText());


            holder.itemView.setOnClickListener(v -> {
                // Get member_id from SharedPreferences
                SharedPreferences sharedPreferences = context.getSharedPreferences("CircleA", Context.MODE_PRIVATE);
                String userMemberId = sharedPreferences.getString("member_id", null);

                if (userMemberId != null) {
                    Log.d("MatchingRequestAdapter", "UserMemberID: " + userMemberId);
                    Log.d("MatchingRequestAdapter", "PS App ID: " + request.getPsAppId());
                    Log.d("MatchingRequestAdapter", "Match ID: " + request.getMatchId());

                    // Create intent to start RequestDetail activity
                    Intent intent = new Intent(context, RequestReceivedDetail.class);

                    // Pass all necessary data
                    intent.putExtra("match_id", request.getMatchId());
                    intent.putExtra("ps_app_id", request.getPsAppId());
                    intent.putExtra("psUsername", request.getPsUsername());
                    intent.putExtra("fee", request.getFee());
                    intent.putExtra("class_level", request.getClassLevel());
                    intent.putExtra("subjects", request.getSubjects());
                    intent.putExtra("districts", request.getDistricts());
                    intent.putExtra("match_mark", request.getMatchMark());
                    intent.putExtra("profile_icon", request.getProfileIcon());

                    // Start the activity
                    context.startActivity(intent);
                } else {
                    Log.d("MatchingRequestAdapter", "No member_id found in SharedPreferences.");
                    Toast.makeText(context, "Error: User information not found", Toast.LENGTH_SHORT).show();
                }

                // Call the click listener if set
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
    }
package com.example.circlea.home;

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
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.circlea.R;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
            holder.classLevelTextView.setText("Class level: " + application.getClassLevel());
            holder.subjectTextView.setText("Subject: " + application.getSubject());
            holder.districtTextView.setText("District: " + application.getDistrict());
            holder.feeTextView.setText("Fee: $" + application.getFee() + " /hr");

            // Set up the LinearLayout click listener to navigate to the detail activity
            holder.layout.setOnClickListener(v -> {
                // Send the appId to PHP server first
                new SendDataToServerTask(application.getAppId()).execute();

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

    // AsyncTask to send data to PHP
    private static class SendDataToServerTask extends AsyncTask<Void, Void, Void> {
        private String member_Id;

        public SendDataToServerTask(String member_Id) {
            this.member_Id = member_Id;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // Set up the connection
                URL url = new URL("http://10.0.2.2/FYP/php/matching_T.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                // Prepare the data to send (send appId as form data)
                String data = "member_id=" + URLEncoder.encode(member_Id, "UTF-8");

                // Send the data
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(data);
                writer.flush();
                writer.close();

                // Get the response
                int responseCode = connection.getResponseCode();
                Log.d("SendDataToServerTask", "Response Code: " + responseCode);

                // Optionally read the response from the server if needed
                // InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                // BufferedReader in = new BufferedReader(reader);
                // String inputLine;
                // StringBuffer response = new StringBuffer();
                // while ((inputLine = in.readLine()) != null) {
                //     response.append(inputLine);
                // }
                // in.close();

            } catch (Exception e) {
                Log.e("SendDataToServerTask", "Error sending data to server", e);
            }
            return null;
        }
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
            layout = itemView.findViewById(R.id.item_layout);

        }
    }
}

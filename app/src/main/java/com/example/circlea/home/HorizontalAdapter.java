package com.example.circlea.home;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
// import com.example.circlea.Advertisement;
import com.example.circlea.R;
import java.util.ArrayList;
import java.util.List;

public class HorizontalAdapter extends RecyclerView.Adapter<HorizontalAdapter.ViewHolder> {
    private List<Advertisement> advertisements;

    public HorizontalAdapter(ArrayList<String> data) {
        this.advertisements = new ArrayList<>();
    }

    public void setAdvertisements(List<Advertisement> advertisements) {
        this.advertisements = advertisements;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (advertisements != null && position < advertisements.size()) {
            Advertisement ad = advertisements.get(position);
            if (holder.adImageView != null && ad.getImageUrl() != null) {
                Glide.with(holder.itemView.getContext())
                        .load(ad.getImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(holder.adImageView);
                
                // 设置点击事件
                holder.adImageView.setOnClickListener(v -> {
                    String linkUrl = ad.getLinkUrl();
                    Log.d("AdClick", "ad ID: " + ad.getId() + ", URL: " + linkUrl);
                    
                    if (linkUrl != null && !linkUrl.isEmpty() && !linkUrl.equalsIgnoreCase("null")) {
                        try {
                            Log.d("AdClick", "opening: " + linkUrl);
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl));
                            holder.itemView.getContext().startActivity(browserIntent);
                        } catch (Exception e) {
                            Log.e("AdClick", "open failed: " + e.getMessage());
                        }
                    } else {
                        Log.d("AdClick", "no link");
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return advertisements != null ? advertisements.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView adImageView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            adImageView = itemView.findViewById(R.id.adImageView);
        }
    }
}
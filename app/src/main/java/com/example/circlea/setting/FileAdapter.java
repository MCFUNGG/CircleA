package com.example.circlea.setting;

import android.content.Context;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.circlea.R;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private Context context;
    private List<FileItem> fileItems;

    public FileAdapter(Context context, List<FileItem> fileItems) {
        this.context = context;
        this.fileItems = fileItems;
    }

    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 使用 parent 來自動套用正確的 LayoutParams
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem currentItem = fileItems.get(position);
        Uri fileUri = currentItem.getFileUri();
        // Use Glide to load image if file is an image, otherwise load default icon
        String mimeType = context.getContentResolver().getType(fileUri);
        if (mimeType != null && mimeType.startsWith("image/")) {
            Glide.with(context)
                    .load(fileUri)
                    .into(holder.imageView);
        } else {
            // Set a default icon for non-image files
            holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        // Set the description text and update model on text change
        holder.descriptionEditText.setText(currentItem.getDescription());
        holder.descriptionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action required
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentItem.setDescription(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {
                // No action required
            }
        });

        // Set remove button click event to remove this file item from list
        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                fileItems.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, fileItems.size());
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileItems.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        EditText descriptionEditText;
        ImageButton btnRemove;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.itemImageView);
            descriptionEditText = itemView.findViewById(R.id.itemDescriptionEditText);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
} 
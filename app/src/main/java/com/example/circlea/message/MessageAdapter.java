package com.example.circlea.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.circlea.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private final List<Message> messages;
    private final OnMessageClickListener listener;
    private final SimpleDateFormat dateFormat;

    public interface OnMessageClickListener {
        void onMessageClick(Message message);
    }

    public MessageAdapter(List<Message> messages, OnMessageClickListener listener) {
        this.messages = messages;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.titleText.setText(message.getTitle());
        holder.contentText.setText(message.getContent());
        holder.timeText.setText(dateFormat.format(message.getCreatedAt()));
        
        // 根據消息的已讀狀態顯示或隱藏紅點
        holder.unreadDot.setVisibility(message.isRead() ? View.GONE : View.VISIBLE);
        
        holder.itemView.setOnClickListener(v -> {
            listener.onMessageClick(message);
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView contentText;
        TextView timeText;
        View unreadDot;

        MessageViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.titleText);
            contentText = itemView.findViewById(R.id.contentText);
            timeText = itemView.findViewById(R.id.timeText);
            unreadDot = itemView.findViewById(R.id.unreadDot);
        }
    }
} 
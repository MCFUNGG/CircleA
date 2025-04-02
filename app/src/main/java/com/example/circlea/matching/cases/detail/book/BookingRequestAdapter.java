package com.example.circlea.matching.cases.detail.book;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.circlea.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BookingRequestAdapter extends RecyclerView.Adapter<BookingRequestAdapter.ViewHolder> {

    private final List<BookingRequest> bookingRequests;
    private final Context context;
    private final OnBookingActionListener actionListener;

    public interface OnBookingActionListener {
        void onAcceptBooking(BookingRequest request);
        void onRejectBooking(BookingRequest request);
    }

    public BookingRequestAdapter(List<BookingRequest> bookingRequests, Context context) {
        this.bookingRequests = bookingRequests;
        this.context = context;
        this.actionListener = (OnBookingActionListener) context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingRequest request = bookingRequests.get(position);

        // Set student information
        holder.studentName.setText(request.getStudentName());
        holder.studentId.setText(context.getString(R.string.id_format, request.getStudentId()));

        // Format and set date/time
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        holder.dateText.setText(context.getString(R.string.date_format, dateFormat.format(request.getDateTime().getTime())));
        holder.timeText.setText(context.getString(R.string.time_format, timeFormat.format(request.getDateTime().getTime())));

        // Set status chip
        holder.statusChip.setText(request.getStatus());

        // Configure status chip color based on status
        switch (request.getStatus().toLowerCase()) {
            case "pending":
                holder.statusChip.setChipBackgroundColorResource(R.color.status_pending);
                holder.actionButtons.setVisibility(View.VISIBLE);
                break;
            case "confirmed":
                holder.statusChip.setChipBackgroundColorResource(R.color.status_approved);
                holder.actionButtons.setVisibility(View.GONE);
                break;
            case "rejected":
                holder.statusChip.setChipBackgroundColorResource(R.color.error_red);
                holder.actionButtons.setVisibility(View.GONE);
                break;
        }

        // Set button click listeners
        holder.acceptButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onAcceptBooking(request);
            }
        });

        holder.rejectButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onRejectBooking(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookingRequests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialTextView studentName;
        final MaterialTextView studentId;
        final MaterialTextView dateText;
        final MaterialTextView timeText;
        final Chip statusChip;
        final View actionButtons;
        final MaterialButton acceptButton;
        final MaterialButton rejectButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            studentName = itemView.findViewById(R.id.student_name);
            studentId = itemView.findViewById(R.id.student_id);
            dateText = itemView.findViewById(R.id.date_text);
            timeText = itemView.findViewById(R.id.time_text);
            statusChip = itemView.findViewById(R.id.status_chip);
            actionButtons = itemView.findViewById(R.id.action_buttons);
            acceptButton = itemView.findViewById(R.id.accept_button);
            rejectButton = itemView.findViewById(R.id.reject_button);
        }
    }

    public void updateBookingRequests(List<BookingRequest> newRequests) {
        bookingRequests.clear();
        bookingRequests.addAll(newRequests);
        notifyDataSetChanged();
    }
}
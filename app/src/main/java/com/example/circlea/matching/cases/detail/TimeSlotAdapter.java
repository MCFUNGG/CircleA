package com.example.circlea.matching.cases.detail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.circlea.matching.cases.detail.book.TutorBooking;
import com.google.android.material.button.MaterialButton;
import com.example.circlea.R;

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {
    private List<TimeSlot> timeSlots;
    private final TutorBooking tutorActivity;
    private boolean isTutor;
    private TimeSlot selectedSlot = null;
    private Context context;

    public TimeSlotAdapter(List<TimeSlot> timeSlots, Context context, boolean isTutor) {
        this.timeSlots = timeSlots;
        this.context = context;
        this.isTutor = isTutor;
        this.tutorActivity = isTutor ? (TutorBooking) context : null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimeSlot timeSlot = timeSlots.get(position);
        holder.dateTextView.setText(timeSlot.getDateString());
        holder.timeTextView.setText(timeSlot.getTimeString());


        if (isTutor) {
            // Tutor view - can edit time slots
            holder.editButton.setVisibility(View.VISIBLE);
            holder.editButton.setOnClickListener(v -> {
                // 修改这里，直接调用 tutorActivity 的方法
                if (tutorActivity != null) {
                    tutorActivity.showTimePickerDialog(context, timeSlot);
                }
            });
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        } else {
            // Student view - can select time slots
            holder.editButton.setVisibility(View.GONE);

            // Update the background based on selection state
            holder.itemView.setBackgroundResource(
                    timeSlot.isSelected() ? R.color.selected_slot_background : android.R.color.transparent
            );

            // Handle click for selection
            holder.itemView.setOnClickListener(v -> {
                // Deselect previously selected slot if any
                if (selectedSlot != null && selectedSlot != timeSlot) {
                    selectedSlot.setSelected(false);
                    notifyItemChanged(timeSlots.indexOf(selectedSlot));
                }

                // Toggle selection of current slot
                timeSlot.setSelected(!timeSlot.isSelected());
                selectedSlot = timeSlot.isSelected() ? timeSlot : null;
                notifyItemChanged(position);
            });
        }
    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
    }
    public void updateTimeSlots(List<TimeSlot> newSlots) {
        this.timeSlots = newSlots;
        notifyDataSetChanged();
    }
    public TimeSlot getSelectedSlot() {
        return selectedSlot;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView;
        TextView timeTextView;
        MaterialButton editButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.date_text);
            timeTextView = itemView.findViewById(R.id.time_text);
            editButton = itemView.findViewById(R.id.edit_button);
        }
    }
}
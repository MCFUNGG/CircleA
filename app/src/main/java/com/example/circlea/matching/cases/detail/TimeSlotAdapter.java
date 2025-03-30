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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

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

        // Set date and time
        holder.dateTextView.setText(timeSlot.getDateString());
        holder.timeTextView.setText(timeSlot.getTimeString());

        MaterialCardView cardView = (MaterialCardView) holder.itemView;

        if (isTutor) {
            // Tutor view logic
            holder.editButton.setVisibility(View.VISIBLE);

            // Enable edit button only for available and editable slots
            boolean canEdit = timeSlot.isAvailable() && timeSlot.isEditable();
            holder.editButton.setEnabled(canEdit);
            holder.editButton.setAlpha(canEdit ? 1.0f : 0.5f);

            // Set edit button click listener
            holder.editButton.setOnClickListener(v -> {
                if (tutorActivity != null && canEdit) {
                    tutorActivity.showTimePickerDialog(context, timeSlot);
                }
            });

            // Update card appearance based on status
            updateCardAppearance(cardView, timeSlot);

            // Add status to time text if not available
            if (!timeSlot.isAvailable()) {
                holder.timeTextView.setText(String.format("%s (%s)",
                        timeSlot.getTimeString(),
                        timeSlot.getStatus().toUpperCase()));
            }
        } else {
            // Student view logic
            holder.editButton.setVisibility(View.GONE);

            // Set card selection state
            cardView.setChecked(timeSlot.isSelected());
            cardView.setCheckable(true);
            cardView.setClickable(true);

            // Update card appearance based on selection
            if (timeSlot.isSelected()) {
                cardView.setStrokeColor(context.getColor(R.color.selected_slot_background));
                cardView.setStrokeWidth(4);
            } else {
                cardView.setStrokeColor(context.getColor(com.google.android.material.R.color.material_on_surface_stroke));
                cardView.setStrokeWidth(1);
            }

            // Handle click for selection
            cardView.setOnClickListener(v -> {
                if (timeSlot.isAvailable()) {
                    // Deselect previous slot if exists
                    if (selectedSlot != null && selectedSlot != timeSlot) {
                        selectedSlot.setSelected(false);
                        notifyItemChanged(timeSlots.indexOf(selectedSlot));
                    }

                    // Select new slot
                    timeSlot.setSelected(true);
                    selectedSlot = timeSlot;
                    notifyItemChanged(position);
                }
            });
        }

    }

    private void updateCardAppearance(MaterialCardView cardView, TimeSlot timeSlot) {
        Context context = cardView.getContext();
        int strokeColor;
        float strokeWidth;

        switch (timeSlot.getStatus().toLowerCase()) {
            case "pending":
                strokeColor = context.getColor(R.color.status_pending);
                strokeWidth = 2;
                break;
            case "confirmed":
                strokeColor = context.getColor(R.color.status_approved);
                strokeWidth = 2;
                break;
            case "expired":
                strokeColor = context.getColor(R.color.error_red);
                strokeWidth = 1;
                cardView.setAlpha(0.7f);
                break;
            default: // available
                strokeColor = context.getColor(com.google.android.material.R.color.material_on_surface_stroke);
                strokeWidth = 1;
                cardView.setAlpha(1.0f);
                break;
        }

        cardView.setStrokeColor(strokeColor);
        cardView.setStrokeWidth((int) (strokeWidth * context.getResources().getDisplayMetrics().density));
    }

    private void handleSlotSelection(TimeSlot timeSlot, int position) {
        // Deselect previous slot
        if (selectedSlot != null && selectedSlot != timeSlot) {
            selectedSlot.setSelected(false);
            notifyItemChanged(timeSlots.indexOf(selectedSlot));
        }

        // Toggle current slot
        timeSlot.setSelected(!timeSlot.isSelected());
        selectedSlot = timeSlot.isSelected() ? timeSlot : null;
        notifyItemChanged(position);
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
        MaterialTextView dateTextView;
        MaterialTextView timeTextView;
        MaterialButton editButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.date_text);
            timeTextView = itemView.findViewById(R.id.time_text);
            editButton = itemView.findViewById(R.id.edit_button);
        }
    }
}
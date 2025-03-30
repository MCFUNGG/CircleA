package com.example.circlea.matching.cases.detail;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LessonStatusDialogManager {
    private final Context context;
    private final LessonStatusCallback callback;
    private final boolean isTutor;

    public interface LessonStatusCallback {
        void onLessonComplete();
        void onLessonIncomplete(String reason, String action);
        void onSubmitIncompleteStatus(String reason);
    }

    public LessonStatusDialogManager(Context context, LessonStatusCallback callback, boolean isTutor) {
        this.context = context;
        this.callback = callback;
        this.isTutor = isTutor;
    }

    public void showLessonStatusDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_lesson_status, null);

        MaterialButton completeButton = view.findViewById(R.id.btn_lesson_complete);
        MaterialButton incompleteButton = view.findViewById(R.id.btn_lesson_incomplete);

        AlertDialog dialog = builder.setTitle("First Lesson Status")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .create();

        completeButton.setOnClickListener(v -> {
            dialog.dismiss();
            showConfirmCompletionDialog();
        });

        incompleteButton.setOnClickListener(v -> {
            dialog.dismiss();
            showIncompleteReasonDialog();
        });

        dialog.show();
    }

    private void showConfirmCompletionDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Confirm Completion")
                .setMessage("By marking the lesson as completed, you'll proceed to the feedback process. This action cannot be undone.\n\nDo you want to continue?")
                .setPositiveButton("Continue", (dialogInterface, i) -> {
                    callback.onLessonComplete();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showIncompleteReasonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_incomplete_reason, null);

        AlertDialog dialog = builder.setTitle("Reason for Incomplete Lesson")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .create();

        int[] buttonIds = {
                R.id.btn_reason_no_show,
                R.id.btn_reason_technical,
                R.id.btn_reason_emergency,
                R.id.btn_reason_time,
                R.id.btn_reason_other
        };

        for (int id : buttonIds) {
            MaterialButton button = view.findViewById(id);
            button.setOnClickListener(v -> {
                dialog.dismiss();
                if (id == R.id.btn_reason_other) {
                    showCustomReasonDialog();
                } else {
                    if (isTutor) {
                        showNextStepsDialog(button.getText().toString());
                    } else {
                        callback.onSubmitIncompleteStatus(button.getText().toString());
                    }
                }
            });
        }

        dialog.show();
    }

    private void showCustomReasonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_custom_reason, null);
        EditText input = view.findViewById(R.id.edit_text_reason);

        builder.setTitle("Specify Reason")
                .setView(view)
                .setPositiveButton("Submit", (dialog, which) -> {
                    String customReason = input.getText().toString().trim();
                    if (!customReason.isEmpty()) {
                        if (isTutor) {
                            showNextStepsDialog(customReason);
                        } else {
                            callback.onSubmitIncompleteStatus(customReason);
                        }
                    } else {
                        Toast.makeText(context, "Please enter a reason", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void showNextStepsDialog(String reason) {
        if (isTutor) {
            // For tutors: Show only "Provide New Time Slot" option
            View view = LayoutInflater.from(context).inflate(R.layout.dialog_next_steps, null);
            MaterialButton rebookButton = view.findViewById(R.id.btn_rebook);
            MaterialButton findOtherTutorButton = view.findViewById(R.id.btn_find_other_tutor);

            rebookButton.setText("Provide New Time Slot");
            findOtherTutorButton.setVisibility(View.GONE);

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("Provide New Time Slot")
                    .setMessage("Please provide new available time slots for rebooking.")
                    .setView(view)
                    .setCancelable(true)
                    .create();

            rebookButton.setOnClickListener(v -> {
                dialog.dismiss();
                callback.onLessonIncomplete(reason, "rebook");
            });

            dialog.show();
        } else {
            // For students: Show both "Rebook" and "Find Other Tutor" options
            View view = LayoutInflater.from(context).inflate(R.layout.dialog_next_steps, null);
            MaterialButton rebookButton = view.findViewById(R.id.btn_rebook);
            MaterialButton findOtherTutorButton = view.findViewById(R.id.btn_find_other_tutor);

            rebookButton.setText("Rebook Lesson");
            findOtherTutorButton.setText("Find Other Tutor");
            findOtherTutorButton.setVisibility(View.VISIBLE);

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("Next Steps")
                    .setMessage("What would you like to do next?")
                    .setView(view)
                    .setCancelable(true)
                    .create();

            rebookButton.setOnClickListener(v -> {
                dialog.dismiss();
                callback.onLessonIncomplete(reason, "rebook");
            });

            findOtherTutorButton.setOnClickListener(v -> {
                dialog.dismiss();
                callback.onLessonIncomplete(reason, "find_other_tutor");
            });

            dialog.show();
        }
    }
}
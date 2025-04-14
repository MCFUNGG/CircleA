package com.example.circlea.matching.cases.detail;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.example.circlea.matching.cases.detail.FirstLessonFeedback;
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

        AlertDialog dialog = builder.setTitle(context.getString(R.string.first_lesson_status))
                .setView(view)
                .setNegativeButton(context.getString(R.string.cancel), null)
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
                .setTitle(context.getString(R.string.confirm_completion))
                .setMessage(context.getString(R.string.by_marking_the_lesson_as_completed_you_ll_proceed_to_the_feedback_process_this_action_cannot_be_undone) + "\n\n" + context.getString(R.string.do_you_want_to_continue))
                .setPositiveButton(context.getString(R.string.continue_btn), (dialogInterface, i) -> {
                    callback.onLessonComplete();
                    
                    // 添加延迟，等待课程状态更新完成后再打开反馈界面
                    if (context instanceof Activity) {
                        new android.os.Handler().postDelayed(() -> {
                            // 判断是否是学生界面，通过检查Activity类型
                            if (context.getClass().getSimpleName().contains("Student")) {
                                // 获取当前Activity的实例
                                Activity activity = (Activity) context;
                                
                                // 创建打开反馈界面的Intent
                                Intent feedbackIntent = new Intent(context, FirstLessonFeedback.class);
                                
                                // 如果当前Activity是MatchingCaseDetailStudent，从中获取必要参数
                                if (context instanceof MatchingCaseDetailStudent) {
                                    MatchingCaseDetailStudent studentActivity = (MatchingCaseDetailStudent) context;
                                    feedbackIntent.putExtra("case_id", studentActivity.getCaseId());
                                    feedbackIntent.putExtra("student_id", studentActivity.getStudentId());
                                    feedbackIntent.putExtra("tutor_id", studentActivity.getTutorId());
                                    activity.startActivity(feedbackIntent);
                                } else {
                                    // 如果无法直接获取参数，给用户提示
                                    Toast.makeText(context, "请在状态更新后手动打开反馈界面", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }, 1000); // 延迟1秒
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }

    private void showIncompleteReasonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_incomplete_reason, null);

        AlertDialog dialog = builder.setTitle(context.getString(R.string.reason_for_incomplete_lesson))
                .setView(view)
                .setNegativeButton(context.getString(R.string.cancel), null)
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

        builder.setTitle(context.getString(R.string.specify_reason))
                .setView(view)
                .setPositiveButton(context.getString(R.string.submit), (dialog, which) -> {
                    String customReason = input.getText().toString().trim();
                    if (!customReason.isEmpty()) {
                        if (isTutor) {
                            showNextStepsDialog(customReason);
                        } else {
                            callback.onSubmitIncompleteStatus(customReason);
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.please_enter_a_reason), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }

    public void showNextStepsDialog(String reason) {
        if (isTutor) {
            // For tutors: Show only "Provide New Time Slot" option
            View view = LayoutInflater.from(context).inflate(R.layout.dialog_next_steps, null);
            MaterialButton rebookButton = view.findViewById(R.id.btn_rebook);
            MaterialButton findOtherTutorButton = view.findViewById(R.id.btn_find_other_tutor);

            rebookButton.setText(context.getString(R.string.provide_new_time_slot));
            findOtherTutorButton.setVisibility(View.GONE);

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.provide_new_time_slot))
                    .setMessage(context.getString(R.string.please_provide_new_available_time_slots_for_rebooking))
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

            rebookButton.setText(context.getString(R.string.rebook_lesson));
            findOtherTutorButton.setText(context.getString(R.string.find_other_tutor));
            findOtherTutorButton.setVisibility(View.VISIBLE);

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.next_steps))
                    .setMessage(context.getString(R.string.what_would_you_like_to_do_next))
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
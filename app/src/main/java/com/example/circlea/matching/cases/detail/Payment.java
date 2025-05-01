package com.example.circlea.matching.cases.detail;

import static android.text.format.DateUtils.formatDateTime;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.google.android.material.button.MaterialButton;
import com.example.circlea.LanguageManager;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Payment extends AppCompatActivity {
    private static final String TAG = "Payment";
    final private double platformFee = 100;
    private double lessonFeePerHr;
    private double totalLessonFee;
    private double totalAmount;

    private TextView lessonFeeText;
    private TextView platformFeeText;
    private TextView totalAmountText;
    private TextView lessonTimeText;
    private TextView lessonFeeDetailText;
    private ImageView receiptImage;
    private MaterialButton uploadButton;
    private MaterialButton submitButton;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 应用语言设置
        LanguageManager languageManager = new LanguageManager(this);
        languageManager.applyLanguage();
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment);
        Log.d("CurrentJava", "Payment");
        initializeViews();
        setupToolbar();
        processIntentData();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 确保活动恢复时使用正确的语言
        LanguageManager languageManager = new LanguageManager(this);
        languageManager.applyLanguage();
    }

    private void initializeViews() {
        lessonFeeText = findViewById(R.id.lesson_fee_text);
        platformFeeText = findViewById(R.id.platform_fee_text);
        totalAmountText = findViewById(R.id.total_amount_text);
        lessonTimeText = findViewById(R.id.lesson_time_text);
        lessonFeeDetailText = findViewById(R.id.lesson_fee_detail_text);
        receiptImage = findViewById(R.id.receipt_image);
        uploadButton = findViewById(R.id.upload_button);
        submitButton = findViewById(R.id.submit_button);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void processIntentData() {
        String startTime = getIntent().getStringExtra("startTime");
        String endTime = getIntent().getStringExtra("endTime");

        if (startTime != null && endTime != null) {
            String timeDisplay = formatDateTime(startTime) + " - " + formatTime(endTime);
            lessonTimeText.setText(timeDisplay);

            long durationInMinutes = calculateDurationInMinutes(startTime, endTime);
            double hours = Math.ceil(durationInMinutes / 30.0) * 0.5;
            String durationText = String.format(Locale.getDefault(),
                    "Duration: %.1f hours", hours);

            TextView durationTextView = findViewById(R.id.duration_text);
            if (durationTextView != null) {
                durationTextView.setText(durationText);
            }
        }

        setPaymentAmounts();
    }

    private void setupClickListeners() {
        uploadButton.setOnClickListener(v -> openImagePicker());
        submitButton.setOnClickListener(v -> submitVerification());
    }

    private void setPaymentAmounts() {
        totalLessonFee = getIntent().getDoubleExtra("totalLessonFee", 0.0);
        lessonFeePerHr = getIntent().getDoubleExtra("lessonFeePerHr", 0.0);
        String startTime = getIntent().getStringExtra("startTime");
        String endTime = getIntent().getStringExtra("endTime");
        totalAmount = totalLessonFee + platformFee;

        long durationInMinutes = calculateDurationInMinutes(startTime, endTime);
        double hours = Math.ceil(durationInMinutes / 30.0) * 0.5;

        String calculationDetail = String.format(Locale.getDefault(),
                "$%.2f × %.1f hours\n($%.2f per hour)",
                lessonFeePerHr,
                hours,
                lessonFeePerHr
        );

        lessonFeeDetailText.setText(calculationDetail);

        String currencyFormat = "$%.2f";
        lessonFeeText.setText(String.format(currencyFormat, totalLessonFee));
        platformFeeText.setText(String.format(currencyFormat, platformFee));
        totalAmountText.setText(String.format(currencyFormat, totalAmount));

        Log.d(TAG, "Total Amount: " + String.format(currencyFormat, totalAmount));
    }

    private long calculateDurationInMinutes(String startTime, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            
            // 檢查是否只包含時間部分 (HH:mm:ss)
            if (startTime.matches("\\d{2}:\\d{2}:\\d{2}")) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                long startMillis = timeFormat.parse(startTime).getTime();
                long endMillis = timeFormat.parse(endTime).getTime();
                
                // 如果結束時間小於開始時間，表示跨天
                if (endMillis < startMillis) {
                    endMillis += 24 * 60 * 60 * 1000; // 加上一天的毫秒數
                }
                
                return (endMillis - startMillis) / (60 * 1000);
            } else {
                long diff = sdf.parse(endTime).getTime() - sdf.parse(startTime).getTime();
                return diff / (60 * 1000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating duration: " + e.getMessage());
            return 0;
        }
    }

    private String formatDateTime(String dateTime) {
        try {
            // 檢查是否只包含時間部分 (HH:mm:ss)
            if (dateTime.matches("\\d{2}:\\d{2}:\\d{2}")) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return outputFormat.format(timeFormat.parse(dateTime));
            } else {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault());
                return outputFormat.format(inputFormat.parse(dateTime));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting datetime: " + e.getMessage());
            return dateTime;
        }
    }

    private String formatTime(String dateTime) {
        try {
            // 檢查是否只包含時間部分 (HH:mm:ss)
            if (dateTime.matches("\\d{2}:\\d{2}:\\d{2}")) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return outputFormat.format(timeFormat.parse(dateTime));
            } else {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return outputFormat.format(inputFormat.parse(dateTime));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting time: " + e.getMessage());
            return dateTime;
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Receipt Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                receiptImage.setImageBitmap(bitmap);
                receiptImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Log.d(TAG, "Image selected successfully");
            } catch (IOException e) {
                Log.e(TAG, "Error loading image: " + e.getMessage());
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitVerification() {
        if (selectedImageUri == null) {
            Log.w(TAG, "No receipt image selected");
            Toast.makeText(this, "Please upload a receipt image", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting payment verification...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Log.d(TAG, "Starting payment submission process");
        submitCompletePayment(progressDialog);
    }

    private void submitCompletePayment(ProgressDialog progressDialog) {
        try {
            Log.d(TAG, "Preparing payment data");

            // 準備所有數據
            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            // 添加付款相關資訊
            String matchId = getIntent().getStringExtra("case_id");
            String studentId = getSharedPreferences("CircleA", Context.MODE_PRIVATE).getString("member_id", "");

            multipartBuilder.addFormDataPart("match_id", matchId)
                    .addFormDataPart("student_id", studentId)
                    .addFormDataPart("amount", String.valueOf(totalAmount));

            Log.d(TAG, String.format("Payment details - Match ID: %s, Student ID: %s, Amount: %.2f",
                    matchId, studentId, totalAmount));

            // 處理收據圖片
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            byte[] fileBytes = baos.toByteArray();

            // 生成文件名並添加圖片
            String fileName = "RECEIPT_" + System.currentTimeMillis() + ".jpeg";
            Log.d(TAG, "Generated filename: " + fileName);

            multipartBuilder.addFormDataPart("receipt", fileName,
                    RequestBody.create(MediaType.parse("image/jpeg"), fileBytes));

            // 創建請求
            Request request = new Request.Builder()
                    .url("http://" + IPConfig.getIP() + "/FYP/php/process_payment.php")
                    .post(multipartBuilder.build())
                    .build();

            // 配置 OkHttpClient
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Log.d(TAG, "Sending payment request to server");

            // 執行請求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network request failed: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(Payment.this,
                                "Payment submission failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    Log.d(TAG, "Server response: " + responseData);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        if (jsonResponse.optBoolean("success")) {
                            Log.d(TAG, "Payment submitted successfully");
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                showSuccessDialog();
                            });
                        } else {
                            // 獲取詳細錯誤信息
                            String message = jsonResponse.getString("message");
                            JSONObject errorDetails = jsonResponse.optJSONObject("error_details");

                            String logMessage = "Payment Error: " + message;
                            if (errorDetails != null) {
                                logMessage += "\nError Code: " + errorDetails.optString("error_code")
                                        + "\nStep: " + errorDetails.optString("step")
                                        + "\nDetails: " + errorDetails.optString("technical_details");
                            }

                            Log.e(TAG, logMessage);

                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(Payment.this, message, Toast.LENGTH_LONG).show();
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage(), e);
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(Payment.this,
                                    "Error processing server response",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });

        } catch (IOException e) {
            Log.e(TAG, "Error preparing payment data: " + e.getMessage(), e);
            progressDialog.dismiss();
            Toast.makeText(this,
                    "Error preparing payment data: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showSuccessDialog() {
        Log.d(TAG, "Showing success dialog");
        new AlertDialog.Builder(this)
                .setTitle("Verification Submitted")
                .setMessage("Your payment verification has been submitted successfully. " +
                        "The tutor contract will be unlocked once verified.")
                .setPositiveButton("OK", (dialog, which) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
package com.example.circlea.setting;

import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.circlea.IPConfig;
import com.example.circlea.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class uploadCert extends AppCompatActivity {

    // List of selected file items with individual description input
    private List<FileItem> selectedFileItems = new ArrayList<>();
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private RecyclerView recyclerViewFiles;
    private FileAdapter fileAdapter;
    private String memberId, contact, skills, education, language, other;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_cert);

        // Retrieve data passed from previous activity
        Intent intent = getIntent();
        memberId = intent.getStringExtra("member_id");
        contact = intent.getStringExtra("contact");
        skills = intent.getStringExtra("skills");
        education = intent.getStringExtra("education");
        language = intent.getStringExtra("language");
        other = intent.getStringExtra("other");

        Button btnSelectFiles = findViewById(R.id.btnSelectFiles);
        Button btnUpload = findViewById(R.id.btnUpload);

        // Initialize RecyclerView for file previews with individual description input
        recyclerViewFiles = findViewById(R.id.recyclerViewFiles);
        recyclerViewFiles.setLayoutManager(new GridLayoutManager(this, 3)); // 3 columns
        fileAdapter = new FileAdapter(this, selectedFileItems);
        recyclerViewFiles.setAdapter(fileAdapter);

        // Initialize file picker using ACTION_OPEN_DOCUMENT (supports multiple selection)
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                        if (result.getData().getClipData() != null) {
                            ClipData clipData = result.getData().getClipData();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                selectedFileItems.add(new FileItem(clipData.getItemAt(i).getUri()));
                            }
                            Toast.makeText(this, "Selected " + clipData.getItemCount() + " files", Toast.LENGTH_SHORT).show();
                        } else if (result.getData().getData() != null) {
                            selectedFileItems.add(new FileItem(result.getData().getData()));
                            Toast.makeText(this, "Selected 1 file", Toast.LENGTH_SHORT).show();
                        }
                        // Refresh RecyclerView to show newly added files
                        fileAdapter.notifyDataSetChanged();
                    }
                });

        // 點選「選取檔案」按鈕時，不清除原有選擇，直接追加新檔案
        btnSelectFiles.setOnClickListener(v -> openFilePicker());

        btnUpload.setOnClickListener(v -> {
            if (!selectedFileItems.isEmpty()) {
                uploadAllFiles();
            } else {
                Toast.makeText(this, "Please select files first!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFilePicker() {
        // Use ACTION_OPEN_DOCUMENT to support multiple file selection on compatible devices
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf",
                "image/jpeg",
                "image/png",
                "image/jpg"
        });
        filePickerLauncher.launch(Intent.createChooser(intent, "Select Files"));
    }

    private void uploadAllFiles() {
        final int totalFiles = selectedFileItems.size();
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failCount = new AtomicInteger(0);

        for (FileItem fileItem : selectedFileItems) {
            uploadFileToServer(fileItem, new UploadCallback() {
                @Override
                public void onSuccess() {
                    int completed = successCount.incrementAndGet();
                    checkUploadCompletion(completed, failCount.get(), totalFiles);
                }
                @Override
                public void onFailure() {
                    int failed = failCount.incrementAndGet();
                    checkUploadCompletion(successCount.get(), failed, totalFiles);
                }
            });
        }
    }

    private void checkUploadCompletion(int successCount, int failCount, int totalFiles) {
        if (successCount + failCount == totalFiles) {
            runOnUiThread(() -> {
                String message = String.format("Upload completed. Success: %d, Failed: %d",
                        successCount, failCount);
                Toast.makeText(uploadCert.this, message, Toast.LENGTH_LONG).show();
                if (successCount == totalFiles) {
                    finish();
                }
            });
        }
    }

    interface UploadCallback {
        void onSuccess();
        void onFailure();
    }

    private void uploadFileToServer(FileItem fileItem, UploadCallback callback) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileItem.getFileUri());
            if (inputStream == null) {
                Toast.makeText(this, "Failed to open file", Toast.LENGTH_SHORT).show();
                callback.onFailure();
                return;
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            byte[] fileBytes = byteArrayOutputStream.toByteArray();
            inputStream.close();

            String fileName = getFileName(fileItem.getFileUri());
            String description = fileItem.getDescription();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("memberId", memberId)
                    .addFormDataPart("file", fileName,
                            RequestBody.create(MediaType.parse("application/octet-stream"), fileBytes))
                    .addFormDataPart("description", description)
                    .addFormDataPart("contact", contact)
                    .addFormDataPart("skills", skills)
                    .addFormDataPart("education", education)
                    .addFormDataPart("language", language)
                    .addFormDataPart("other", other)
                    .build();

            Request request = new Request.Builder()
                    .url("http://" + IPConfig.getIP() + "/FYP/php/uploadCV.php")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(uploadCert.this,
                            "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    callback.onFailure();
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure();
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
            callback.onFailure();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
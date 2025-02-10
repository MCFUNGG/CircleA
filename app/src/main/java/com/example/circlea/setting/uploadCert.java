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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
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
        recyclerViewFiles.setLayoutManager(new GridLayoutManager(this, 1));
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
                                Uri selectedImageUri = clipData.getItemAt(i).getUri();
                                String selectedMimeType = getContentResolver().getType(selectedImageUri);
                                if (selectedMimeType != null &&
                                        (selectedMimeType.equals("application/pdf") ||
                                                selectedMimeType.equals("image/jpeg") ||
                                                selectedMimeType.equals("image/png") ||
                                                selectedMimeType.equals("image/jpg"))) {
                                    selectedFileItems.add(new FileItem(selectedImageUri));
                                } else {
                                    Toast.makeText(this, "Selected file type is not supported", Toast.LENGTH_SHORT).show();
                                }
                            }
                            Toast.makeText(this, "Selected " + clipData.getItemCount() + " files", Toast.LENGTH_SHORT).show();
                        } else if (result.getData().getData() != null) {
                            Uri selectedImageUri = result.getData().getData();
                            String selectedMimeType = getContentResolver().getType(selectedImageUri);
                            if (selectedMimeType != null &&
                                    (selectedMimeType.equals("application/pdf") ||
                                            selectedMimeType.equals("image/jpeg") ||
                                            selectedMimeType.equals("image/png") ||
                                            selectedMimeType.equals("image/jpg"))) {
                                selectedFileItems.add(new FileItem(selectedImageUri));
                            } else {
                                Toast.makeText(this, "Selected file type is not supported", Toast.LENGTH_SHORT).show();
                            }
                            Toast.makeText(this, "Selected 1 file", Toast.LENGTH_SHORT).show();
                        }
                        // Refresh RecyclerView to show newly added files
                        fileAdapter.notifyDataSetChanged();
                    }
                });

        // 點選「選取檔案」按鈕時，不清除原有選擇，直接追加新檔案
        btnSelectFiles.setOnClickListener(v -> openFilePicker());

        // 當上傳按鈕被點擊時，先呼叫 deleteOldCertificates()，待刪除成功後再上傳新檔案
        btnUpload.setOnClickListener(v -> deleteOldCertificates(new DeleteCallback() {
            @Override
            public void onDeleteSuccess() {
                // 刪除成功後上傳新檔案
                uploadNewFiles();
            }

            @Override
            public void onDeleteFailure(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(uploadCert.this,
                        "Delete old certificates failed: " + errorMessage, Toast.LENGTH_SHORT).show());
            }
        }));
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

    private void deleteOldCertificates(DeleteCallback callback) {
        FormBody formBody = new FormBody.Builder()
                .add("memberId", memberId)
                .build();

        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/delete_cert.php")
                .post(formBody)
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onDeleteFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseStr = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseStr);
                        if (json.getString("status").equals("success")) {
                            callback.onDeleteSuccess();
                        } else {
                            callback.onDeleteFailure(json.getString("message"));
                        }
                    } catch (JSONException e) {
                        callback.onDeleteFailure(e.getMessage());
                    }
                } else {
                    callback.onDeleteFailure("Response code: " + response.code());
                }
            }
        });
    }

    private void uploadNewFiles() {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        // 加入表單參數
        multipartBuilder.addFormDataPart("memberId", memberId);
        multipartBuilder.addFormDataPart("description", "");
        multipartBuilder.addFormDataPart("contact", contact);
        multipartBuilder.addFormDataPart("skills", skills);
        multipartBuilder.addFormDataPart("education", education);
        multipartBuilder.addFormDataPart("language", language);
        multipartBuilder.addFormDataPart("other", other);

        for (FileItem fileItem : selectedFileItems) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(fileItem.getFileUri());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                byte[] fileBytes = baos.toByteArray();

                // 產生唯一的檔案名稱
                String fileName = "CERT_" + memberId + "_" + System.currentTimeMillis() + ".jpeg";
                multipartBuilder.addFormDataPart("file[]", fileName,
                        RequestBody.create(MediaType.parse("application/octet-stream"), fileBytes));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        RequestBody requestBody = multipartBuilder.build();

        Request request = new Request.Builder()
                .url("http://" + IPConfig.getIP() + "/FYP/php/upload_cv.php")
                .post(requestBody)
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(uploadCert.this,
                        "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(uploadCert.this, "Upload successful!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(uploadCert.this,
                            "Upload failed: " + response.message(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // DeleteCallback interface to handle deleteCert.php response
    private interface DeleteCallback {
        void onDeleteSuccess();
        void onDeleteFailure(String errorMessage);
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
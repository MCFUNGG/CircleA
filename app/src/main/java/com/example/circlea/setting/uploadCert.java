package com.example.circlea.setting;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.circlea.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class uploadCert extends AppCompatActivity {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_FILE_EXTENSIONS = {"pdf", "jpg", "jpeg", "png"};

    private String memberId = "2";
    private Uri selectedFileUri;
    private ImageView imageView;

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_cert);

        //memberId = getIntent().getStringExtra("MEMBER_ID");

        imageView = findViewById(R.id.imageView);
        Button btnSelectFile = findViewById(R.id.btnSelectFile);
        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);
        Button btnUpload = findViewById(R.id.btnUpload);

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        selectedFileUri = result.getData().getData();
                        if (validateFile(selectedFileUri)) {
                            showFilePreview(selectedFileUri);
                        } else {
                            Toast.makeText(this, "Invalid file format or size exceeds 10MB!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                        imageView.setImageBitmap(photo);
                        savePhoto(photo);
                    }
                }
        );

        btnSelectFile.setOnClickListener(v -> openFilePicker());
        btnTakePhoto.setOnClickListener(v -> takePhoto());
        btnUpload.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                uploadFileToServer(selectedFileUri);
            } else {
                Toast.makeText(this, "Please select a file or take a photo first!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf", "image/jpeg", "image/png", "image/jpg"});
        filePickerLauncher.launch(intent);
    }

    private void takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 2);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(cameraIntent);
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePhoto(Bitmap photo) {
        File photoFile = new File(getExternalFilesDir(null), "captured_photo.jpg");
        try (FileOutputStream out = new FileOutputStream(photoFile)) {
            photo.compress(Bitmap.CompressFormat.JPEG, 100, out);
            selectedFileUri = Uri.fromFile(photoFile);
            Log.d("PhotoSave", "Photo saved at: " + selectedFileUri.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving photo", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateFile(Uri fileUri) {
        return isValidFileFormat(fileUri) && isValidFileSize(fileUri);
    }

    private boolean isValidFileFormat(Uri fileUri) {
        String fileExtension = getFileExtension(fileUri);
        Log.d("FileExtension", "Selected file extension: " + fileExtension);
        if (fileExtension == null) {
            return false;
        }
        for (String allowedExtension : ALLOWED_FILE_EXTENSIONS) {
            if (fileExtension.equalsIgnoreCase(allowedExtension)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidFileSize(Uri fileUri) {
        try {
            if (fileUri.getScheme().equals("content")) {
                Cursor cursor = getContentResolver().query(fileUri, null, null, null, null);
                if (cursor != null) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (cursor.moveToFirst()) {
                        long size = cursor.getLong(sizeIndex);
                        cursor.close();
                        return size <= MAX_FILE_SIZE;
                    }
                    cursor.close();
                }
            } else {
                String filePath = getRealPathFromURI(fileUri);
                File file = new File(filePath);
                return file.exists() && file.length() <= MAX_FILE_SIZE;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getFileExtension(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } else {
            fileName = uri.getLastPathSegment();
        }

        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1);
        }
        return null;
    }

    private String getRealPathFromURI(Uri uri) {
        String filePath = null;
        String[] projection = {MediaStore.Images.Media.DATA};

        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            filePath = cursor.getString(column_index);
            cursor.close();
        }

        if (filePath == null) {
            filePath = uri.getPath();
        }

        return filePath;
    }

    private void showFilePreview(Uri fileUri) {
        String mimeType = getContentResolver().getType(fileUri);

        if (mimeType != null && mimeType.startsWith("image/")) {
            imageView.setImageURI(fileUri);
        } else if (mimeType != null && mimeType.equals("application/pdf")) {
            imageView.setImageResource(R.drawable.ic_pdf_icon);
        } else {
            imageView.setImageDrawable(null);
        }
    }

    private void uploadFileToServer(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                Toast.makeText(this, "Failed to open InputStream", Toast.LENGTH_SHORT).show();
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

            String newFileName = generateNewFileName(fileUri);

            Log.d("UploadFile", "memberId: " + memberId);
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("memberId", memberId) // Add memberId to the request
                    .addFormDataPart("file", newFileName,
                            RequestBody.create(fileBytes, MediaType.parse("application/octet-stream")))
                    .build();

            Request request = new Request.Builder()
                    .url("http://10.0.2.2/FYP/php/uploadCert.php") // Change to your server URL
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("UploadFile", "Upload failed: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(uploadCert.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> Toast.makeText(uploadCert.this, "File uploaded successfully!", Toast.LENGTH_SHORT).show());
                    } else {
                        Log.e("UploadFile", "Upload failed with code: " + response.code());
                        runOnUiThread(() -> Toast.makeText(uploadCert.this, "Upload failed with code: " + response.code(), Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateNewFileName(Uri fileUri) {
        String fileExtension = getFileExtension(fileUri);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        String currentTime = sdf.format(new Date());
        String uniqueId = UUID.randomUUID().toString();
        return memberId + "_" + currentTime + (fileExtension != null ? "." + fileExtension : "");
    }
}
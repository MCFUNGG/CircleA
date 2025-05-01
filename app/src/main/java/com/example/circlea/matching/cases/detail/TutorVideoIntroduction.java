package com.example.circlea.matching.cases.detail;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.circlea.IPConfig;
import com.example.circlea.R;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TutorVideoIntroduction extends AppCompatActivity {
    private String videoFilePath;
    private Button recordButton;
    private static final String TAG = "TutorVideoIntro";
    private int matchId;
    private int memberId;

    private ActivityResultLauncher<Intent> videoCaptureLauncher;
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutor_video_introduction);

        // 假設 matchId 和 memberId 從某處獲取，暫時硬編碼
        matchId = 114;
        //memberId = 1;

        if (matchId == -1 || memberId == -1) {
            Toast.makeText(this, "無效的 match 或 member 資料", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        recordButton = findViewById(R.id.record_button);
        recordButton.setText(getString(R.string.record_video_intro));

        // 檢查相機權限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        // 設置影片錄製結果處理
        videoCaptureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri videoUri = result.getData().getData();
                        if (videoUri != null) {
                            Log.i(TAG, "影片錄製成功，URI: " + videoUri);
                            videoFilePath = getRealPathFromURI(this, videoUri);
                            if (videoFilePath != null && !videoFilePath.isEmpty()) {
                                Log.i(TAG, "影片檔案路徑: " + videoFilePath);
                                uploadVideoToServer();
                            } else {
                                Log.e(TAG, "無法從 URI 獲取檔案路徑: " + videoUri);
                                Toast.makeText(this, "錯誤：無法存取錄製的影片檔案", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e(TAG, "影片 URI 為空");
                            Toast.makeText(this, "錯誤：無法獲取影片資料", Toast.LENGTH_SHORT).show();
                        }
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        Log.i(TAG, "使用者取消錄製");
                        Toast.makeText(this, "錄製已取消", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "錄製失敗，結果碼: " + result.getResultCode());
                        Toast.makeText(this, "影片錄製失敗", Toast.LENGTH_SHORT).show();
                    }
                });

        // 設置錄製按鈕點擊事件
        recordButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
                videoCaptureLauncher.launch(takeVideoIntent);
            } else {
                Log.e(TAG, "未找到可處理錄影的相機應用");
                Toast.makeText(this, "此設備未找到相機應用", Toast.LENGTH_LONG).show();
            }
        });

        recordButton.setEnabled(true);
    }

    // 從 URI 獲取真實檔案路徑
    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Video.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor == null) {
                Log.e(TAG, "ContentResolver 查詢返回空 cursor，URI: " + contentUri);
                if ("file".equalsIgnoreCase(contentUri.getScheme())) {
                    return contentUri.getPath();
                }
                return null;
            }
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            Log.e(TAG, "從 URI 獲取真實路徑失敗: " + contentUri, e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // 上傳影片到後端
    private void uploadVideoToServer() {
        if (videoFilePath == null || videoFilePath.isEmpty()) {
            Log.e(TAG, "無法上傳，影片路徑為空");
            Toast.makeText(this, "錯誤 Madsen：影片檔案路徑缺失", Toast.LENGTH_LONG).show();
            return;
        }
        File videoFile = new File(videoFilePath);
        if (!videoFile.exists() || videoFile.length() == 0) {
            Log.e(TAG, "影片檔案無效或為空: " + videoFilePath);
            Toast.makeText(this, "錯誤：錄製的影片檔案無效或為空", Toast.LENGTH_LONG).show();
            return;
        }

        Log.i(TAG, "開始上傳影片: " + videoFilePath);
        Toast.makeText(this, "正在上傳並分析...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("video", "intro_" + matchId + ".mp4",
                                RequestBody.create(videoFile, MediaType.parse("video/mp4")))
                        .addFormDataPart("match_id", String.valueOf(matchId))
                        .build();

                Request request = new Request.Builder()
                        .url("http://"+ IPConfig.getIP()+":5000/FYP/upload_video") // 請確保此 URL 正確
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : null;

                if (response.isSuccessful() && responseBody != null) {
                    Log.i(TAG, "上傳成功，回應: " + responseBody);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (jsonResponse.has("parsed_result")) {
                            JSONObject parsedResult = jsonResponse.getJSONObject("parsed_result");
                            int videoMark = parsedResult.getInt("video_mark");
                            String videoSummary = parsedResult.getString("video_summary");
                            String videoAnalysis = parsedResult.getString("video_analysis");

                            runOnUiThread(() -> Toast.makeText(this,
                                    "AI分析完成\n分數: " + videoMark +
                                            "\n總結: " + videoSummary +
                                            "\n分析: " + videoAnalysis,
                                    Toast.LENGTH_LONG).show());
                        } else if (jsonResponse.has("error")) {
                            String errorMsg = jsonResponse.getString("error");
                            runOnUiThread(() -> Toast.makeText(this,
                                    "上傳錯誤: " + errorMsg, Toast.LENGTH_LONG).show());
                        } else {
                            runOnUiThread(() -> Toast.makeText(this,
                                    "伺服器回應格式異常", Toast.LENGTH_LONG).show());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析 JSON 回應失敗: " + responseBody, e);
                        runOnUiThread(() -> Toast.makeText(this,
                                "錯誤：無法解析伺服器回應", Toast.LENGTH_LONG).show());
                    }
                } else {
                    String errorDetail = responseBody != null ? responseBody : response.message();
                    Log.e(TAG, "上傳失敗: 狀態碼=" + response.code() + ", 訊息=" + errorDetail);
                    runOnUiThread(() -> Toast.makeText(this,
                            "上傳失敗 (" + response.code() + ")", Toast.LENGTH_LONG).show());
                }
            } catch (IOException e) {
                Log.e(TAG, "上傳失敗，發生 IOException", e);
                runOnUiThread(() -> Toast.makeText(this,
                        "上傳錯誤: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recordButton.setEnabled(true);
            } else {
                recordButton.setEnabled(false);
                Toast.makeText(this, "相機權限被拒絕，無法錄影", Toast.LENGTH_LONG).show();
            }
        }
    }
}
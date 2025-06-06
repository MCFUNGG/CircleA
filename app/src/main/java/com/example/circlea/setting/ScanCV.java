package com.example.circlea.setting;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.IPConfig;
import com.example.circlea.R;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.Text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

public class ScanCV extends AppCompatActivity {

    private static final String BASE_URL = "http://"+ IPConfig.getIP()+"/FYP/php/save_cv_data.php";
    private static final int PICK_IMAGE = 100;
    private ImageView imageView;
    private EditText contactEditText, skillsEditText, educationEditText,
            languageEditText, otherEditText;
    private TextRecognizer textRecognizer;
    private Uri savedImageUri;
    private Button saveButton;
    private TextView titleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_cv);

        // Initialize ML Kit text recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Initialize views
        titleTextView = findViewById(R.id.titleTextView);
        imageView = findViewById(R.id.imageView);
        contactEditText = findViewById(R.id.contactEditText);
        skillsEditText = findViewById(R.id.skillsEditText);
        educationEditText = findViewById(R.id.educationEditText);
        languageEditText = findViewById(R.id.languageEditText);
        otherEditText = findViewById(R.id.otherEditText);
        saveButton = findViewById(R.id.saveButton);

        // 檢查是否有傳入的CV數據（編輯模式）
        Intent intent = getIntent();
        if (intent.hasExtra("cv_id")) {
            // 編輯模式 - 預填充表單
            contactEditText.setText(intent.getStringExtra("contact"));
            skillsEditText.setText(intent.getStringExtra("skills"));
            educationEditText.setText(intent.getStringExtra("education"));
            languageEditText.setText(intent.getStringExtra("language"));
            otherEditText.setText(intent.getStringExtra("other"));

            // 設置標題為編輯模式
            titleTextView.setText(getString(R.string.edit_cv));
        } else {
            // 創建模式
            titleTextView.setText(getString(R.string.create_cv));
        }

        // Set up buttons
        findViewById(R.id.selectImageBtn).setOnClickListener(v -> openGallery());
        saveButton.setOnClickListener(v -> {
            if (validateFormData()) {
                saveCV();
                Toast.makeText(this, "儲存中...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            Uri imageUri = data.getData();
            savedImageUri = imageUri;
            imageView.setImageURI(imageUri);
            processImage(imageUri);
        }
    }

    private void processImage(Uri imageUri) {
        try {
            // Convert URI to Bitmap
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT < 28) {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            } else {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageUri);
                bitmap = ImageDecoder.decodeBitmap(source);
            }

            // Create InputImage object
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            // Process the image
            textRecognizer.process(image)
                    .addOnSuccessListener(this::processRecognizedText)
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "辨識錯誤: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "載入圖片失敗", Toast.LENGTH_SHORT).show();
        }
    }

    private void processRecognizedText(Text visionText) {
        String fullText = visionText.getText();

        // Split text into sections based on keywords
        Map<String, String> sections = categorizeText(fullText);

        // Update UI with categorized text
        runOnUiThread(() -> {
            contactEditText.setText(sections.get("contact"));
            skillsEditText.setText(sections.get("skills"));
            educationEditText.setText(sections.get("education"));
            languageEditText.setText(sections.get("language"));
            otherEditText.setText(sections.get("other"));
        });
    }

    private Map<String, String> categorizeText(String fullText) {
        Map<String, String> sections = new HashMap<>();

        // Define keywords for each section
        String[] contactKeywords = { "contact", "phone", "email", "address" };
        String[] skillsKeywords = { "skills", "expertise", "competencies" };
        String[] educationKeywords = { "education", "qualification", "degree", "higher diploma", "school",
                "university" };
        String[] languageKeywords = { "language", "languages", "linguistic" };
        String[] otherKeywords = { "projects", "school projects", "hackathon", "school hackathon" };

        // Split text into lines
        String[] lines = fullText.split("\n");

        StringBuilder currentSection = new StringBuilder();
        String currentCategory = "other";
        boolean isRightSection = false;

        for (String line : lines) {
            String lowerLine = line.toLowerCase();

            // Check if we're entering a right-hand section (projects or hackathon)
            if (containsKeywords(lowerLine, otherKeywords)) {
                if (currentSection.length() > 0) {
                    sections.put(currentCategory, currentSection.toString().trim());
                }
                isRightSection = true;
                currentCategory = "other";
                currentSection = new StringBuilder();
                currentSection.append(line).append("\n");
                continue;
            }

            // Process left-hand sections only if not in right section
            if (!isRightSection) {
                if (containsKeywords(lowerLine, contactKeywords)) {
                    if (currentSection.length() > 0) {
                        sections.put(currentCategory, currentSection.toString().trim());
                    }
                    currentCategory = "contact";
                    currentSection = new StringBuilder();
                } else if (containsKeywords(lowerLine, skillsKeywords)) {
                    if (currentSection.length() > 0) {
                        sections.put(currentCategory, currentSection.toString().trim());
                    }
                    currentCategory = "skills";
                    currentSection = new StringBuilder();
                } else if (containsKeywords(lowerLine, educationKeywords)) {
                    if (currentSection.length() > 0) {
                        sections.put(currentCategory, currentSection.toString().trim());
                    }
                    currentCategory = "education";
                    currentSection = new StringBuilder();
                } else if (containsKeywords(lowerLine, languageKeywords)) {
                    if (currentSection.length() > 0) {
                        sections.put(currentCategory, currentSection.toString().trim());
                    }
                    currentCategory = "language";
                    currentSection = new StringBuilder();
                }
            }

            // Append the current line to the appropriate section
            currentSection.append(line).append("\n");
        }

        // Add the last section
        if (currentSection.length() > 0) {
            sections.put(currentCategory, currentSection.toString().trim());
        }

        // Ensure all categories exist
        String[] categories = { "contact", "skills", "education", "language", "other" };
        for (String category : categories) {
            if (!sections.containsKey(category)) {
                sections.put(category, "");
            }
        }

        return sections;
    }

    private boolean containsKeywords(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void saveCV() {
        // 檢查表單資料
        if (!validateFormData()) {
            return;
        }

        // 創建要發送到Flask服務器的數據
        JSONObject jsonBody = new JSONObject();
        AtomicInteger SCORE = new AtomicInteger(0); // 用於存儲分數

        try {
            Intent intent = new Intent(this, uploadCert.class);
            jsonBody.put("contact", contactEditText.getText().toString().trim());
            jsonBody.put("skills", skillsEditText.getText().toString().trim());
            jsonBody.put("education", educationEditText.getText().toString().trim());
            jsonBody.put("language", languageEditText.getText().toString().trim());
            jsonBody.put("other", otherEditText.getText().toString().trim());

            // 獲取member_id
            SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
            String memberId = sharedPreferences.getString("member_id", "");
            jsonBody.put("member_id", memberId);

            // 添加職位名稱（可以從其他地方獲取或使用默認值）
            jsonBody.put("job_title", "general position");

            // 創建網絡請求
            String url = "http://"+IPConfig.getIP()+":5030/evaluate_cv"; // 本地測試用10.0.2.2，實際部署時改為服務器IP
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        try {
                            if (response.getBoolean("success")) {
                                int score = response.getInt("score");
                                Toast.makeText(this, "CV評分: " + score + "/100", Toast.LENGTH_LONG).show();
                                int finalScore = score;
                                SCORE.set(finalScore); // 更新分數
                                Log.d("ScanCV", "Score: " + SCORE);

                                finish();

                            } else {
                                Toast.makeText(this, "評分失敗，請稍後重試", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "解析響應失敗", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(this, "網絡請求失敗: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });

            // 添加請求到隊列
            Volley.newRequestQueue(this).add(request);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "創建請求失敗", Toast.LENGTH_SHORT).show();
        }

        new android.os.Handler().postDelayed(() -> {
            // 在這裡執行延遲後的操作，例如跳轉到下一個活動
            // 這裡可以使用Intent來跳轉到uploadCert活動
            Intent intent = new Intent(ScanCV.this, uploadCert.class);
            intent.putExtra("contact", contactEditText.getText().toString().trim());
            intent.putExtra("skills", skillsEditText.getText().toString().trim());
            intent.putExtra("education", educationEditText.getText().toString().trim());
            intent.putExtra("language", languageEditText.getText().toString().trim());
            intent.putExtra("other", otherEditText.getText().toString().trim());
            intent.putExtra("score", SCORE.get());

            // 獲取member_id並傳遞
            SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
            String memberId = sharedPreferences.getString("member_id", "");
            intent.putExtra("member_id", memberId);

            // 檢查是否為編輯模式，如果是則傳遞CV ID
            if (getIntent().hasExtra("cv_id")) {
                intent.putExtra("cv_id", getIntent().getIntExtra("cv_id", 0));
                intent.putExtra("is_edit", true);
            }

            startActivity(intent);
        }, 5000); // 延遲5秒 (5000毫秒)
    }

    private String convertImageToBase64(Uri uri) {
        if (uri == null) {
            return null; // 如果沒有圖片，返回null
        }
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (IOException | SecurityException e) {
            Toast.makeText(this, "圖片轉換失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null; // 返回null以表示轉換失敗
        }
    }

    // 表單資料驗證
    private boolean validateFormData() {
        if (contactEditText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.please_fill_contact_info), Toast.LENGTH_SHORT).show();
            contactEditText.requestFocus();
            return false;
        }
        return true;
    }
}

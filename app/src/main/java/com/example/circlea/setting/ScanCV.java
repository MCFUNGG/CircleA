package com.example.circlea.setting;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

import android.util.Base64;

public class ScanCV extends AppCompatActivity {

    private static final String BASE_URL = "http://"+ IPConfig.getIP()+"/FYP/php/save_cv_data.php";
    private static final int PICK_IMAGE = 100;
    private ImageView imageView;
    private EditText contactEditText, skillsEditText, educationEditText,
            languageEditText, otherEditText;
    private TextRecognizer textRecognizer;
    private Uri savedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_cv);

        // Initialize ML Kit text recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Initialize views
        imageView = findViewById(R.id.imageView);
        contactEditText = findViewById(R.id.contactEditText);
        skillsEditText = findViewById(R.id.skillsEditText);
        educationEditText = findViewById(R.id.educationEditText);
        languageEditText = findViewById(R.id.languageEditText);
        otherEditText = findViewById(R.id.otherEditText);

        // Set up buttons
        Button selectImageBtn = findViewById(R.id.selectImageBtn);
        Button saveButton = findViewById(R.id.saveButton);

        selectImageBtn.setOnClickListener(v -> openGallery());
        saveButton.setOnClickListener(v -> saveCV());
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
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
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

        // 嘗試轉換圖片，允許圖片為空
        String base64Image = convertImageToBase64(savedImageUri);

        // 創建Intent並傳遞所有資料到uploadCert
        Intent intent = new Intent(this, uploadCert.class);
        intent.putExtra("contact", contactEditText.getText().toString().trim());
        intent.putExtra("skills", skillsEditText.getText().toString().trim());
        intent.putExtra("education", educationEditText.getText().toString().trim());
        intent.putExtra("language", languageEditText.getText().toString().trim());
        intent.putExtra("other", otherEditText.getText().toString().trim());

        // 獲取member_id並傳遞
        SharedPreferences sharedPreferences = getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", "");
        intent.putExtra("member_id", memberId);

        startActivity(intent);
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
            showToast("圖片轉換失敗: " + e.getMessage());
            return null; // 返回null以表示轉換失敗
        }
    }

    // 表單資料驗證
    private boolean validateFormData() {
        if (contactEditText.getText().toString().trim().isEmpty()) {
            showToast(getString(R.string.please_fill_contact_info));
            return false;
        }
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

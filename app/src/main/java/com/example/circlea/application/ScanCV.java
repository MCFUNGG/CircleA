package com.example.circlea.application;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.circlea.R;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import com.android.volley.DefaultRetryPolicy;

public class ScanCV extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2/FYP/php/save_cv_data.php";
    private static final int PICK_IMAGE = 100;
    private ImageView imageView;
    private EditText contactEditText, skillsEditText, educationEditText,
            languageEditText, otherEditText;
    private TextRecognizer textRecognizer;

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
        String[] educationKeywords = { "education", "qualification", "degree" };
        String[] languageKeywords = { "language", "languages", "linguistic" };

        // Split text into lines
        String[] lines = fullText.split("\n");

        StringBuilder currentSection = new StringBuilder();
        String currentCategory = "other";

        for (String line : lines) {
            String lowerLine = line.toLowerCase();

            // Determine section based on keywords
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
        String url = BASE_URL;

        // Get member_id from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", "");

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.getString("message");

                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                        if (success) {
                            finish(); // Close activity on success
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error processing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("member_id", memberId);
                params.put("contact", contactEditText.getText().toString().trim());
                params.put("skills", skillsEditText.getText().toString().trim());
                params.put("education", educationEditText.getText().toString().trim());
                params.put("language", languageEditText.getText().toString().trim());
                params.put("other", otherEditText.getText().toString().trim());
                return params;
            }
        };

        // Add retry policy to handle timeouts
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);
    }
}

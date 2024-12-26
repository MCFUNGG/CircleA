package com.example.circlea.application;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.circlea.R;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class ScanCV extends AppCompatActivity {

    private static final int SELECT_IMAGE_REQUEST = 1;
    private ImageView imageView;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_cv);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        Button selectImageBtn = findViewById(R.id.selectImageBtn);

        selectImageBtn.setOnClickListener(v -> {
            // 打开图片选择器
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, SELECT_IMAGE_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    Bitmap bitmap;
                    // 获取图片
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), imageUri));
                    } else {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    }

                    // 显示图片
                    imageView.setImageBitmap(bitmap);

                    // 处理图片进行文字识别
                    processImage(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processImage(Bitmap bitmap) {
        try {
            // 将 Bitmap 转换为 InputImage
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            // 初始化 ML Kit 的文本识别器
            TextRecognizer recognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());

            // 处理图片
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String rawText = visionText.getText();  // 获取识别的文本
                        textView.setText(rawText);  // 显示文本结果
                    })
                    .addOnFailureListener(e -> textView.setText("识别失败，请重试。\n错误信息: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            textView.setText("处理图片时出错：" + e.getMessage());
        }
    }
}

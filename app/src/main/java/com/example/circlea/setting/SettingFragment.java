package com.example.circlea.setting;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.circlea.CheckSharedPreferences;
import com.example.circlea.Home;
import com.example.circlea.IPConfig;
import com.example.circlea.Login;
import com.example.circlea.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SettingFragment extends Fragment {

    private OkHttpClient client;
    private TextView userEmailTextView;
    private TextView userPhoneTextView, usernameTextView, logOutTextView;
    private Button userOwnDetailbtn, userOwnCartbtn;
    private ImageView userIcon; // 用户头像的 ImageView

    // 声明 ActivityResultLauncher
    private ActivityResultLauncher<Intent> getImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        // 显示选择的图片
                        userIcon.setImageURI(selectedImageUri);
                        // 上传图片
                        uploadImage(selectedImageUri);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        client = new OkHttpClient();

        CheckSharedPreferences checkPrefs = new CheckSharedPreferences(requireContext());
        checkPrefs.printSharedPreferences();

        // 初始化 TextViews
        usernameTextView = view.findViewById(R.id.username);
        userEmailTextView = view.findViewById(R.id.user_email);
        userPhoneTextView = view.findViewById(R.id.user_phone);
        userOwnDetailbtn = view.findViewById(R.id.user_own_detail_button);
        userOwnCartbtn = view.findViewById(R.id.cart_button);
        logOutTextView = view.findViewById(R.id.log_out_tv);
        userIcon = view.findViewById(R.id.user_icon); // 初始化用户头像
        ImageButton menuButton = view.findViewById(R.id.menuButton);

        menuButton.setOnClickListener(v -> {
            ((Home) getActivity()).openDrawer(); // Call method from Home activity
        });

        // 设置头像点击事件
        userIcon.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            getImageLauncher.launch(intent); // 使用 ActivityResultLauncher
        });

        // 设置按钮点击监听
        userOwnDetailbtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MemberDetail.class);
            startActivity(intent);
        });

        userOwnCartbtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MemberCart.class);
            startActivity(intent);
        });

        logOutTextView.setOnClickListener(v -> {
            // 清除 SharedPreferences 中的所有数据
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("CircleA", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // 启动登录活动
            Intent intent = new Intent(getActivity(), Login.class);
            startActivity(intent);
            getActivity().finish();
        });

        // 获取设置数据
        fetchSettingData();

        return view;
    }

    private void fetchSettingData() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);
        Toast.makeText(getActivity(), "Member ID : "+memberId, Toast.LENGTH_SHORT).show();

        if (memberId == null) {
            Toast.makeText(getActivity(), "Member ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "http://"+ IPConfig.getIP()+"/FYP/php/get_member_own_profile.php"; // 更新为您的 URL
        String ip = "";
        RequestBody requestBody = new FormBody.Builder()
                .add("member_id", memberId)
                .add("ip",ip)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchSettingData", "Request failed: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Failed to fetch data", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    Log.d("FetchSettingData", "Server response: " + jsonResponse);

                    try {
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        if (jsonObject.getBoolean("success")) {
                            JSONArray dataArray = jsonObject.getJSONArray("data");

                            if (dataArray.length() > 0) {
                                JSONObject data = dataArray.getJSONObject(0);
                                String email = data.optString("email", "N/A");
                                String phone = data.optString("phone", "N/A");
                                String username = data.optString("username", "N/A");
                                String profileUrl = data.optString("profile", ""); // 获取头像 URL

                                Log.d("ProfileImageURL", "Loading image from URL: " + profileUrl); // Debug log

                                requireActivity().runOnUiThread(() -> {
                                    userEmailTextView.setText(email);
                                    userPhoneTextView.setText(phone);
                                    usernameTextView.setText(username);

                                    if (!profileUrl.isEmpty()) {
                                        String fullProfileUrl = "http://" +IPConfig.getIP()+profileUrl;
                                        Glide.with(getActivity())
                                                .load(fullProfileUrl)
                                                .into(userIcon);
                                        Toast.makeText(getActivity(), fullProfileUrl, Toast.LENGTH_SHORT).show();
                                    } else {
                                        userIcon.setImageResource(R.drawable.ic_launcher_foreground); // 设置默认头像
                                    }
                                });
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("FetchSettingData", "JSON parsing error: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void uploadImage(Uri imageUri) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("CircleA", MODE_PRIVATE);
        String memberId = sharedPreferences.getString("member_id", null);

        if (memberId == null) {
            Toast.makeText(getActivity(), "Member ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "http://"+IPConfig.getIP()+"/FYP/php/update_member_icon.php";

        // 创建 MultipartBody
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("member_id", memberId);

        // 获取文件的 MIME 类型
        String mimeType = requireActivity().getContentResolver().getType(imageUri);
        String fileName = "image"; // 默认文件名

        // 将图片文件转换为 File 进行上传
        try {
            // 获取文件的路径（如果是从内容 URI 获取的）
            InputStream inputStream = requireActivity().getContentResolver().openInputStream(imageUri);
            byte[] byteArray = getBytesFromInputStream(inputStream);

            // 获取文件扩展名以便命名
            String extension = mimeType != null ? MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) : "jpg";
            fileName += "." + extension;

            // 上传文件
            builder.addFormDataPart("file", fileName, RequestBody.create(MediaType.parse(mimeType), byteArray));
        } catch (IOException e) {
            Log.e("UploadImage", "Error getting image: " + e.getMessage());
            return;
        }

        // 构建请求体
        RequestBody requestBody = builder.build();

        // 构建请求
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("UploadImage", "Request failed: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    Log.d("UploadImage", "Server response: " + jsonResponse);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getActivity(), "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Log.e("UploadImage", "Request failed, response code: " + response.code());
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    // 将 InputStream 转换为字节数组的辅助方法
    private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            byteBuffer.write(data, 0, nRead);
        }
        byteBuffer.flush();
        return byteBuffer.toByteArray();
    }
}
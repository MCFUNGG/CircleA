package com.example.circlea.message;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.circlea.R;
import com.example.circlea.Config;
import com.example.circlea.IPConfig;
import com.example.circlea.Home;
import com.example.circlea.matching.Matching;
import com.example.circlea.application.ApplicationHistory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MessageFragment extends Fragment {
    private static final String TAG = "MessageFragment";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private OkHttpClient client;
    private String memberId;
    private TextView emptyView;
    private ImageButton menuButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating MessageFragment view");
        View view = inflater.inflate(R.layout.fragment_message, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        emptyView = view.findViewById(R.id.emptyView);
        menuButton = view.findViewById(R.id.menuButton);

        // Setup menu button
        menuButton.setOnClickListener(v -> {
            if (getActivity() instanceof Home) {
                ((Home) getActivity()).openDrawer();
            }
        });

        // Setup RecyclerView
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, message -> onMessageClick(message));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(messageAdapter);
        
        // Initialize OkHttpClient
        client = new OkHttpClient();
        
        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> loadMessages());
        
        Log.d(TAG, "onCreateView: Starting initial message load");
        // Load messages when fragment is created
        loadMessages();
        
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.message_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            loadMessages();
            return true;
        } else if (id == R.id.action_mark_all_read) {
            markAllMessagesAsRead();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void markAllMessagesAsRead() {
        // TODO: 實現標記所有消息為已讀的功能
        Toast.makeText(getContext(), "此功能即將推出", Toast.LENGTH_SHORT).show();
    }

    private void updateEmptyView() {
        if (messageList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void loadMessages() {
        // Get member_id from SharedPreferences
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("CircleA", MODE_PRIVATE);
        memberId = sharedPreferences.getString("member_id", null);
        Log.d(TAG, "loadMessages: Member ID = " + memberId);

        String url = "http://" + IPConfig.getIP() + "/FYP/php/get_messages.php";
        Log.d(TAG, "loadMessages: Request URL = " + url);

        RequestBody formBody = new FormBody.Builder()
                .add("member_id", memberId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        Log.d(TAG, "loadMessages: Sending request to server");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "onFailure: Request failed", e);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), "加載消息失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateEmptyView();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (getActivity() == null) return;
                
                try {
                    String responseData = response.body().string();
                    Log.d(TAG, "onResponse: Server response = " + responseData);
                    
                    JSONObject jsonResponse = new JSONObject(responseData);
                    Log.d(TAG, "onResponse: Parsed JSON response");
                    
                    if (jsonResponse.getBoolean("success")) {
                        JSONArray messagesArray = jsonResponse.getJSONArray("data");
                        Log.d(TAG, "onResponse: Found " + messagesArray.length() + " messages");
                        List<Message> newMessages = new ArrayList<>();
                        
                        for (int i = 0; i < messagesArray.length(); i++) {
                            JSONObject messageObj = messagesArray.getJSONObject(i);
                            Message message = new Message(
                                messageObj.getString("id"),
                                messageObj.getString("sender_id"),
                                messageObj.getString("receiver_id"),
                                messageObj.getString("title"),
                                messageObj.getString("content"),
                                messageObj.getString("type"),
                                messageObj.getInt("is_read") == 1,
                                parseDate(messageObj.getString("created_at"))
                            );
                            newMessages.add(message);
                            Log.d(TAG, "onResponse: Added message: " + message.getTitle());
                        }
                        
                        getActivity().runOnUiThread(() -> {
                            messageList.clear();
                            messageList.addAll(newMessages);
                            messageAdapter.notifyDataSetChanged();
                            swipeRefreshLayout.setRefreshing(false);
                            updateEmptyView();
                            Log.d(TAG, "onResponse: Updated UI with " + newMessages.size() + " messages");
                        });
                    } else {
                        String errorMessage = jsonResponse.optString("message");
                        Log.e(TAG, "onResponse: Server returned error: " + errorMessage);
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "加載消息失敗: " + errorMessage, Toast.LENGTH_SHORT).show();
                            swipeRefreshLayout.setRefreshing(false);
                            updateEmptyView();
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse: JSON parsing error", e);
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "解析數據失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                        updateEmptyView();
                    });
                }
            }
        });
    }

    private void onMessageClick(Message message) {
        if (!(getActivity() instanceof Home)) {
            return;
        }
        
        Home homeActivity = (Home) getActivity();
        
        // 標記消息為已讀
        if (!message.isRead()) {
            message.setRead(true);
            messageAdapter.notifyDataSetChanged();
            ((Home) getActivity()).showBadge(R.id.nav_message, false);
            markMessageAsRead(message);
        }
        
        // 根據消息類型處理
        switch (message.getType()) {
            case "new_request":
                // 切換到 Matching Fragment
                Fragment matchingFragment = new Matching();
                homeActivity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, matchingFragment)
                    .addToBackStack(null)
                    .commit();
                homeActivity.setCurrentFragment(Home.FRAGMENT_MATCHING);
                break;
                
            case "application_approved":
                // 啟動 ApplicationHistory Activity
                Intent intent = new Intent(getActivity(), ApplicationHistory.class);
                startActivity(intent);
                break;
                
            default:
                Toast.makeText(getContext(), "暫不支持此類型消息的跳轉", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void markMessageAsRead(Message message) {
        String url = "http://" + IPConfig.getIP() + "/FYP/php/mark_message_read.php";
        Log.d(TAG, "markMessageAsRead: Request URL = " + url);
        Log.d(TAG, "markMessageAsRead: Marking message " + message.getId() + " as read");

        RequestBody formBody = new FormBody.Builder()
                .add("message_id", message.getId())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "markMessageAsRead: Request failed", e);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "標記消息已讀失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (getActivity() == null) return;
                
                try {
                    String responseData = response.body().string();
                    Log.d(TAG, "markMessageAsRead: Server response = " + responseData);
                    
                    JSONObject jsonResponse = new JSONObject(responseData);
                    
                    if (jsonResponse.getBoolean("success")) {
                        Log.d(TAG, "markMessageAsRead: Successfully marked message as read");
                        getActivity().runOnUiThread(() -> {
                            message.setRead(true);
                            messageAdapter.notifyDataSetChanged();
                            // 檢查是否需要更新未讀消息數量
                            if (getActivity() instanceof Home) {
                                ((Home) getActivity()).showBadge(R.id.nav_message, false);
                            }
                        });
                    } else {
                        String errorMessage = jsonResponse.optString("message", "未知錯誤");
                        Log.e(TAG, "markMessageAsRead: Server returned error: " + errorMessage);
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "標記消息已讀失敗: " + errorMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "markMessageAsRead: JSON parsing error", e);
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "解析數據失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private Date parseDate(String dateStr) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return format.parse(dateStr);
        } catch (ParseException e) {
            Log.e(TAG, "parseDate: Error parsing date: " + dateStr, e);
            return new Date();
        }
    }
} 
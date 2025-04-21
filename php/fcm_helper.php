<?php
// FCM 輔助函數文件

/**
 * 發送 FCM 消息
 * @param string $token 接收者的 FCM token
 * @param string $title 通知標題
 * @param string $message 通知內容
 * @param string $type 通知類型
 * @param array $additional_data 額外數據
 * @return array 包含發送結果的數組
 */
function sendFCMMessage($token, $title, $message, $type, $additional_data = []) {
    $fcm_debug = [];
    error_log("開始發送 FCM 消息給 token: " . $token);
    error_log("標題: " . $title);
    error_log("消息: " . $message);
    error_log("類型: " . $type);
    error_log("額外數據: " . json_encode($additional_data));
    
    // 讀取服務帳戶憑證
    $serviceAccount = json_decode(file_get_contents('D:/xampp/htdocs/firebase-service-account.json'), true);
    if (!$serviceAccount) {
        error_log("錯誤：無法讀取服務帳戶憑證文件");
        return [
            "success" => false,
            "error" => "無法讀取服務帳戶憑證"
        ];
    }
    error_log("成功讀取服務帳戶憑證");

    try {
        // 生成 JWT token
        $jwt = generateJWT($serviceAccount);
        error_log("JWT token 生成成功");
        
        $accessToken = getAccessToken($jwt);
        error_log("Access token 獲取成功");

        // 準備基本數據
        $data = [
            "type" => $type,
            "title" => $title,
            "body" => $message,
            "channel_id" => "CircleA_Channel",
            "click_action" => "FLUTTER_NOTIFICATION_CLICK"
        ];

        // 合併額外數據
        $data = array_merge($data, $additional_data);
        error_log("完整的消息數據: " . json_encode($data));

        // 準備 FCM 消息
        $fcm_message = [
            "message" => [
                "token" => $token,
                "data" => $data,
                "android" => [
                    "priority" => "high"
                ]
            ]
        ];

        // 發送 FCM 消息
        $ch = curl_init();
        $url = 'https://fcm.googleapis.com/v1/projects/' . $serviceAccount['project_id'] . '/messages:send';
        error_log("FCM URL: " . $url);
        
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Authorization: Bearer ' . $accessToken,
            'Content-Type: application/json'
        ]);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fcm_message));

        error_log("發送的 FCM 消息內容: " . json_encode($fcm_message));
        $fcm_result = curl_exec($ch);
        $fcm_debug["fcm_request"] = $fcm_message;
        
        if ($fcm_result === FALSE) {
            $error = curl_error($ch);
            error_log("FCM 發送失敗: " . $error);
            $fcm_debug["fcm_error"] = $error;
            curl_close($ch);
            return [
                "success" => false,
                "error" => $error,
                "debug" => $fcm_debug
            ];
        }

        $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        error_log("FCM 響應狀態碼: " . $http_code);
        
        curl_close($ch);
        $response_data = json_decode($fcm_result, true);
        $fcm_debug["fcm_response"] = $response_data;
        error_log("FCM 響應內容: " . $fcm_result);

        if ($http_code !== 200) {
            error_log("FCM 請求失敗，狀態碼: " . $http_code);
            return [
                "success" => false,
                "error" => "FCM request failed with status " . $http_code,
                "response" => $response_data,
                "debug" => $fcm_debug
            ];
        }

        error_log("FCM 消息發送成功");
        return [
            "success" => true,
            "response" => $response_data,
            "debug" => $fcm_debug
        ];

    } catch (Exception $e) {
        error_log("FCM 發送異常: " . $e->getMessage());
        return [
            "success" => false,
            "error" => $e->getMessage(),
            "debug" => $fcm_debug
        ];
    }
}

/**
 * 生成 JWT token
 */
function generateJWT($serviceAccount) {
    $header = [
        'typ' => 'JWT',
        'alg' => 'RS256'
    ];

    $time = time();
    $payload = [
        'iss' => $serviceAccount['client_email'],
        'sub' => $serviceAccount['client_email'],
        'aud' => 'https://oauth2.googleapis.com/token',
        'iat' => $time,
        'exp' => $time + 3600,
        'scope' => 'https://www.googleapis.com/auth/firebase.messaging'
    ];

    $base64Header = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode(json_encode($header)));
    $base64Payload = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode(json_encode($payload)));

    $privateKey = openssl_pkey_get_private($serviceAccount['private_key']);
    openssl_sign(
        $base64Header . '.' . $base64Payload,
        $signature,
        $privateKey,
        'SHA256'
    );
    $base64Signature = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($signature));

    return $base64Header . '.' . $base64Payload . '.' . $base64Signature;
}

/**
 * 獲取 access token
 */
function getAccessToken($jwt) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, 'https://oauth2.googleapis.com/token');
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query([
        'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion' => $jwt
    ]));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    
    $response = curl_exec($ch);
    curl_close($ch);
    
    $data = json_decode($response, true);
    return $data['access_token'];
} 
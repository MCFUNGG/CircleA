<?php
// 關閉所有PHP錯誤顯示
error_reporting(E_ALL);
ini_set('display_errors', 1);

header("Content-Type: application/json"); // Set response type
header("Access-Control-Allow-Origin: *");

// 獲取 POST 數據
$json = file_get_contents('php://input');
$data = json_decode($json, true);

if (!$data) {
    echo json_encode([
        "success" => false,
        "message" => "無效的請求數據",
        "debug" => "JSON 解析失敗"
    ]);
    exit;
}

// 檢查必要參數
if (empty($data['token']) || empty($data['title']) || empty($data['message'])) {
    echo json_encode([
        "success" => false,
        "message" => "缺少必要參數",
        "debug" => $data
    ]);
    exit;
}

// 讀取服務帳戶憑證
$serviceAccount = json_decode(file_get_contents('D:/xampp/htdocs/firebase-service-account.json'), true);
if (!$serviceAccount) {
    echo json_encode([
        "success" => false,
        "message" => "無法讀取服務帳戶憑證",
        "debug" => [
            "error" => json_last_error_msg(),
            "path" => 'D:/xampp/htdocs/firebase-service-account.json'
        ]
    ]);
    exit;
}

// 準備 FCM 消息
$fcm_message = [
    "message" => [
        "token" => $data['token'],
        "notification" => [
            "title" => $data['title'],
            "body" => $data['message']
        ],
        "android" => [
            "notification" => [
                "channel_id" => "CircleA_Channel",
                "click_action" => "FLUTTER_NOTIFICATION_CLICK"
            ]
        ],
        "data" => [
            "type" => $data['type'] ?? "test",
            "click_action" => "FLUTTER_NOTIFICATION_CLICK"
        ]
    ]
];

try {
    // 獲取 access token
    $jwt = generateJWT($serviceAccount);
    $accessToken = getAccessToken($jwt);

    // 發送 FCM 消息
    $ch = curl_init();
    $url = 'https://fcm.googleapis.com/v1/projects/' . $serviceAccount['project_id'] . '/messages:send';
    
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: Bearer ' . $accessToken,
        'Content-Type: application/json'
    ]);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fcm_message));

    // 添加調試信息
    curl_setopt($ch, CURLOPT_VERBOSE, true);
    $verbose = fopen('php://temp', 'w+');
    curl_setopt($ch, CURLOPT_STDERR, $verbose);

    $response = curl_exec($ch);
    $http_status = curl_getinfo($ch, CURLINFO_HTTP_CODE);

    // 獲取詳細的 curl 信息
    rewind($verbose);
    $verbose_log = stream_get_contents($verbose);

    if ($response === FALSE) {
        throw new Exception(curl_error($ch));
    }

    $fcm_response = json_decode($response, true);
    echo json_encode([
        "success" => ($http_status == 200),
        "message" => ($http_status == 200) 
            ? "測試消息發送成功" 
            : "FCM 響應錯誤: " . $response,
        "debug" => [
            "fcm_response" => $fcm_response,
            "status" => $http_status,
            "request" => $fcm_message,
            "curl_info" => curl_getinfo($ch),
            "verbose_log" => $verbose_log
        ]
    ]);

} catch (Exception $e) {
    echo json_encode([
        "success" => false,
        "message" => "發送失敗",
        "debug" => [
            "error" => $e->getMessage(),
            "request" => $fcm_message
        ]
    ]);
}

curl_close($ch);

// 生成 JWT token
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

// 獲取 access token
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
?> 
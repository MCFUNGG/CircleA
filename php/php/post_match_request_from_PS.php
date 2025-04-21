<?php
// 關閉所有PHP錯誤顯示
error_reporting(E_ALL);
ini_set('display_errors', 1);

header("Content-Type: application/json"); // Set response type
header("Access-Control-Allow-Origin: *");

require_once 'db_config.php';
// 不再需要 fcm_helper.php
// require_once 'fcm_helper.php';  

// 创建数据库连接
$connect = getDbConnection();
if (!$connect) {
    echo json_encode([
        "success" => false, 
        "message" => "Unable to connect to the database",
        "error" => mysqli_connect_error()
    ]);
    exit;
}

// Get POST data with validation
$tutorAppId = isset($_POST["tutor_app_id"]) ? trim($_POST["tutor_app_id"]) : '';
$psAppId = isset($_POST["ps_app_id"]) ? trim($_POST["ps_app_id"]) : '';
$tutorId = isset($_POST["tutor_id"]) ? trim($_POST["tutor_id"]) : '';
$psId = isset($_POST["ps_id"]) ? trim($_POST["ps_id"]) : '';
$matchMark = isset($_POST["match_mark"]) ? trim($_POST["match_mark"]) : '';

// 檢查必要參數
if(empty($tutorAppId) || empty($psAppId) || empty($tutorId) || empty($psId) || empty($matchMark)) {
    echo json_encode([
        "success" => false, 
        "message" => "Missing required parameters",
        "debug" => [
            "tutor_app_id" => $tutorAppId,
            "ps_app_id" => $psAppId,
            "tutor_id" => $tutorId,
            "ps_id" => $psId,
            "match_mark" => $matchMark
        ]
    ]);
    exit;
}

// 獲取學生名稱，用於通知內容
$ps_name = "";
$query_ps_name = "SELECT username FROM member WHERE member_id = '$psId'";
$result_ps_name = mysqli_query($connect, $query_ps_name);
if ($result_ps_name && mysqli_num_rows($result_ps_name) > 0) {
    $row = mysqli_fetch_assoc($result_ps_name);
    $ps_name = $row['username'];
} else {
    echo json_encode([
        "success" => false,
        "message" => "Failed to get student name",
        "error" => mysqli_error($connect),
        "query" => $query_ps_name
    ]);
    exit;
}

$query = "INSERT INTO `match` (match_creator, tutor_id, tutor_app_id, ps_id, ps_app_id, match_mark, status) 
          VALUES ('PS', '$tutorId', '$tutorAppId', '$psId', '$psAppId', '$matchMark', 'WT')";
$result = mysqli_query($connect, $query);

if ($result) {
    // 獲取新插入的 match_id
    $match_id = mysqli_insert_id($connect);
    
    // 檢查 FCM 令牌是否存在
    $token_query = "SELECT token FROM fcm_tokens WHERE member_id = '$tutorId'";
    $token_result = mysqli_query($connect, $token_query);
    
    $fcm_debug = [];
    
    if ($token_result && mysqli_num_rows($token_result) > 0) {
        $row = mysqli_fetch_assoc($token_result);
        $token = $row['token'];
        
        $fcm_debug["token_found"] = true;
        $fcm_debug["token"] = $token;
        
        // 保存通知到數據庫
        $title = "新補習請求";
        $message = $ps_name . " 向您發送了補習請求";
        $type = "new_request";
        
        $notification_query = "INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) 
                              VALUES ('$tutorId', '$title', '$message', '$type', '$match_id', 0, NOW())";
        $notification_result = mysqli_query($connect, $notification_query);
        
        $fcm_debug["notification_saved"] = $notification_result ? true : false;
        if (!$notification_result) {
            $fcm_debug["notification_error"] = mysqli_error($connect);
        }

        // 使用直接 FCM API 發送消息而不是 helper 函數
        // 讀取服務帳戶憑證
        $serviceAccount = json_decode(file_get_contents('D:/xampp/htdocs/firebase-service-account.json'), true);
        if (!$serviceAccount) {
            $fcm_debug["error"] = "無法讀取服務帳戶憑證";
        } else {
            try {
                // 生成 JWT token
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

                $jwt = $base64Header . '.' . $base64Payload . '.' . $base64Signature;
                
                // 獲取 access token
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
                $accessToken = $data['access_token'];

                // 準備 FCM 消息
                $fcm_message = [
                    "message" => [
                        "token" => $token,
                        "notification" => [
                            "title" => $title,
                            "body" => $message
                        ],
                        "android" => [
                            "notification" => [
                                "channel_id" => "CircleA_Channel",
                                "click_action" => "FLUTTER_NOTIFICATION_CLICK"
                            ]
                        ],
                        "data" => [
                            "type" => $type,
                            "match_id" => (string)$match_id,
                            "student_name" => $ps_name,
                            "click_action" => "FLUTTER_NOTIFICATION_CLICK"
                        ]
                    ]
                ];

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

                $fcm_result = curl_exec($ch);
                $http_status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
                
                $fcm_debug["fcm_request"] = $fcm_message;
                $fcm_debug["fcm_response"] = json_decode($fcm_result, true);
                $fcm_debug["fcm_status"] = $http_status;
                
                curl_close($ch);
                
                if ($http_status !== 200) {
                    $fcm_debug["error"] = "FCM request failed with status " . $http_status;
                }
            } catch (Exception $e) {
                $fcm_debug["error"] = $e->getMessage();
            }
        }
        
    } else {
        $fcm_debug["token_found"] = false;
        $fcm_debug["error"] = "FCM token not found for tutor ID: $tutorId";
    }
    
    echo json_encode([
        "success" => true, 
        "message" => "Application submitted successfully!",
        "match_id" => $match_id,
        "fcm_debug" => $fcm_debug
    ]);
} else {
    echo json_encode([
        "success" => false, 
        "message" => "Failed to submit application",
        "error" => mysqli_error($connect),
        "query" => $query
    ]);
}

// Close database connection
mysqli_close($connect);
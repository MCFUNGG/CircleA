<?php
header("Content-Type: application/json");
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', 'php_error.log');

// 記錄接收到的請求
error_log("Received payment data: " . print_r($_POST, true));
error_log("Received files: " . print_r($_FILES, true));

try {
    // 引入數據庫配置文件
    require_once 'db_config.php';

    // 創建數據庫連接
    $connect = getDbConnection();
    if (!$connect) {
        throw new Exception("Database connection failed: " . mysqli_connect_error());
    }

    // 獲取POST數據並驗證必填字段
    if (!isset($_POST['match_id']) || !isset($_POST['student_id']) || !isset($_POST['amount'])) {
        throw new Exception("Missing required parameters");
    }

    $matchId = mysqli_real_escape_string($connect, $_POST['match_id']);
    $studentId = mysqli_real_escape_string($connect, $_POST['student_id']);
    $amount = floatval($_POST['amount']);

    // 驗證是否上傳了收據圖片
    if (!isset($_FILES['receipt']) || $_FILES['receipt']['error'] !== UPLOAD_ERR_OK) {
        throw new Exception("Receipt image is required");
    }

    // 處理收據圖片上傳
    $uploadsDir = 'uploads/receipts/';
    if (!file_exists($uploadsDir)) {
        if (!mkdir($uploadsDir, 0777, true)) {
            throw new Exception("Failed to create uploads directory");
        }
    }

    $fileName = uniqid('RECEIPT_') . '_' . basename($_FILES['receipt']['name']);
    $fileDestination = $uploadsDir . $fileName;
    $relativeFilePath = $fileDestination; // 相對路徑，存儲到數據庫

    if (!move_uploaded_file($_FILES['receipt']['tmp_name'], $fileDestination)) {
        throw new Exception("Failed to upload receipt image");
    }

    // 獲取match詳細信息，包括導師ID和名稱
    $matchQuery = "SELECT m.tutor_id, 
                   b.booking_id, b.date, b.start_time, b.end_time,
                   COALESCE(t.username, 'Unknown Tutor') as tutor_name,
                   COALESCE(s.username, 'Unknown Student') as student_name
                   FROM `match` m
                   JOIN booking b ON b.match_id = m.match_id AND b.status = 'confirmed'
                   LEFT JOIN member t ON t.member_id = m.tutor_id
                   LEFT JOIN member s ON s.member_id = ?
                   WHERE m.match_id = ?
                   LIMIT 1";

    $stmt = mysqli_prepare($connect, $matchQuery);
    mysqli_stmt_bind_param($stmt, "ss", $studentId, $matchId);
    
    if (!mysqli_stmt_execute($stmt)) {
        throw new Exception("Error retrieving match details: " . mysqli_error($connect));
    }
    
    $matchResult = mysqli_stmt_get_result($stmt);
    
    if (mysqli_num_rows($matchResult) === 0) {
        throw new Exception("Match or confirmed booking not found");
    }
    
    $matchDetails = mysqli_fetch_assoc($matchResult);
    $tutorId = $matchDetails['tutor_id'];
    $tutorName = $matchDetails['tutor_name'];
    $studentName = $matchDetails['student_name'];
    $bookingId = $matchDetails['booking_id'];
    
    // 格式化日期和時間
    $date = $matchDetails['date'];
    $startTime = date('H:i', strtotime($matchDetails['start_time']));
    $endTime = date('H:i', strtotime($matchDetails['end_time']));
    $timeSlotFormatted = "$date $startTime - $endTime";

    // 開始事務處理
    mysqli_begin_transaction($connect);

    try {
        // 插入付款記錄
        $paymentStatus = 'pending'; // 初始狀態為待審核
        $paymentQuery = "INSERT INTO payment (match_id, student_id, amount, receipt_path, status, submitted_at) 
                         VALUES (?, ?, ?, ?, ?, NOW())";
        
        $stmt = mysqli_prepare($connect, $paymentQuery);
        mysqli_stmt_bind_param($stmt, "ssdss", $matchId, $studentId, $amount, $relativeFilePath, $paymentStatus);
        
        if (!mysqli_stmt_execute($stmt)) {
            throw new Exception("Failed to insert payment record: " . mysqli_error($connect));
        }
        
        $paymentId = mysqli_insert_id($connect);
        
        // 準備通知
        $notificationTitle = "學生已付款";
        $notificationMessage = "{$studentName} 已為 {$timeSlotFormatted} 的課程付款 $" . number_format($amount, 2) . "，確認後將解鎖聯絡方式。";
        $notificationType = "payment_submitted";
        
        // 在數據庫插入通知記錄
        $insertNotificationQuery = "INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) 
                                   VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)";
        
        $stmt = mysqli_prepare($connect, $insertNotificationQuery);
        mysqli_stmt_bind_param($stmt, "ssssi", 
            $tutorId,
            $notificationTitle,
            $notificationMessage,
            $notificationType,
            $matchId
        );
        
        if (!mysqli_stmt_execute($stmt)) {
            error_log("Failed to insert notification: " . mysqli_error($connect));
        }
        
        // 獲取導師的FCM令牌
        $tokenQuery = "SELECT token FROM fcm_tokens WHERE member_id = ? ORDER BY created_at DESC LIMIT 1";
        $stmt = mysqli_prepare($connect, $tokenQuery);
        mysqli_stmt_bind_param($stmt, "s", $tutorId);
        mysqli_stmt_execute($stmt);
        $tokenResult = mysqli_stmt_get_result($stmt);
        
        $fcm_debug = [];
        
        if ($tokenResult && $token_row = mysqli_fetch_assoc($tokenResult)) {
            $fcm_token = $token_row['token'];
            $fcm_debug["token_found"] = true;
            
            // 發送FCM通知
            // 讀取服務帳戶憑證
            $serviceAccount = json_decode(file_get_contents('D:/xampp/htdocs/firebase-service-account.json'), true);
            if (!$serviceAccount) {
                error_log("錯誤：無法讀取服務帳戶憑證");
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
                            "token" => $fcm_token,
                            "notification" => [
                                "title" => $notificationTitle,
                                "body" => $notificationMessage
                            ],
                            "android" => [
                                "notification" => [
                                    "channel_id" => "CircleA_Channel",
                                    "click_action" => "FLUTTER_NOTIFICATION_CLICK"
                                ]
                            ],
                            "data" => [
                                "type" => $notificationType,
                                "match_id" => strval($matchId),
                                "booking_id" => strval($bookingId),
                                "payment_id" => strval($paymentId),
                                "amount" => strval($amount),
                                "receipt_path" => $relativeFilePath,
                                "time_slot" => $timeSlotFormatted,
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
                    
                    $fcm_debug["fcm_response"] = json_decode($fcm_result, true);
                    $fcm_debug["fcm_status"] = $http_status;
                    
                    curl_close($ch);
                    
                    error_log("FCM send result: " . $fcm_result);
                    
                    if ($http_status !== 200) {
                        error_log("FCM Error: Status " . $http_status);
                        $fcm_debug["error"] = "FCM request failed with status " . $http_status;
                    }
                } catch (Exception $e) {
                    error_log("FCM Error: " . $e->getMessage());
                    $fcm_debug["error"] = $e->getMessage();
                }
            }
        } else {
            $fcm_debug["token_found"] = false;
            $fcm_debug["error"] = "No FCM token found for tutor ID: " . $tutorId;
            error_log($fcm_debug["error"]);
        }

        // 提交事務
        mysqli_commit($connect);
        
        echo json_encode([
            "success" => true,
            "message" => "Payment submitted successfully",
            "payment_id" => $paymentId,
            "receipt_path" => $relativeFilePath,
            "fcm_debug" => $fcm_debug
        ]);
        
    } catch (Exception $e) {
        // 回滾事務
        mysqli_rollback($connect);
        throw $e;
    }

    // 關閉數據庫連接
    mysqli_close($connect);
    
} catch (Exception $e) {
    error_log("Error in process_payment.php: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage(),
        "error_details" => [
            "error_code" => "PAYMENT_PROCESSING_ERROR",
            "step" => "payment_processing",
            "technical_details" => $e->getMessage()
        ]
    ]);
}
?>
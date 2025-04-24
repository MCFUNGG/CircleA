<?php
// 在腳本開頭設置錯誤處理函數，確保任何錯誤都能轉換為JSON響應
function handle_error($errno, $errstr, $errfile, $errline) {
    error_log("PHP Error: [$errno] $errstr in $errfile on line $errline");
    $response = [
        'success' => false,
        'message' => "Server error: $errstr",
        'error_details' => "$errfile:$errline",
        'status' => 'error'
    ];
    
    header('Content-Type: application/json');
    echo json_encode($response);
    exit;
}

// 設置錯誤處理函數
set_error_handler('handle_error');

// 設置異常處理函數
function handle_exception($e) {
    error_log("Uncaught Exception: " . $e->getMessage() . " in " . $e->getFile() . " on line " . $e->getLine());
    $response = [
        'success' => false,
        'message' => "Server exception: " . $e->getMessage(),
        'error_details' => $e->getFile() . ":" . $e->getLine(),
        'status' => 'error'
    ];
    
    header('Content-Type: application/json');
    echo json_encode($response);
    exit;
}

// 設置異常處理函數
set_exception_handler('handle_exception');

// 確保即使腳本超時也能輸出有效的錯誤響應
register_shutdown_function(function() {
    $error = error_get_last();
    if ($error !== null && in_array($error['type'], [E_ERROR, E_PARSE, E_CORE_ERROR, E_COMPILE_ERROR])) {
        error_log("Fatal Error: " . $error['message'] . " in " . $error['file'] . " on line " . $error['line']);
        $response = [
            'success' => false,
            'message' => "Fatal server error: " . $error['message'],
            'error_details' => $error['file'] . ":" . $error['line'],
            'status' => 'error'
        ];
        
        header('Content-Type: application/json');
        echo json_encode($response);
    }
});

// 標準HTTP頭
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// 設置錯誤報告級別和記錄方式
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', 'php_error.log');

// 記錄腳本執行開始
error_log("update_student_first_lesson_status.php started");

// 嘗試引入資料庫配置文件
try {
    if (file_exists('db_config.php')) {
        require_once 'db_config.php';
        error_log("Successfully included db_config.php");
    } else {
        throw new Exception("Database configuration file not found");
    }
    
    // 測試資料庫連接是否可用
    if (function_exists('getDbConnection')) {
        $testConn = getDbConnection();
        if (!$testConn) {
            throw new Exception("Could not establish database connection");
        }
        error_log("Database connection successful");
    } else {
        throw new Exception("getDbConnection function not available");
    }
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => "Database configuration error: " . $e->getMessage()
    ];
    echo json_encode($response);
    exit;
}

// 檢查是否存在FCM輔助函數文件
if (file_exists('fcm_helper.php')) {
    require_once 'fcm_helper.php';
    error_log("FCM helper included");
}

// 獲取輸入數據
try {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!$data) {
        $data = $_POST;
    }
    
    // 記錄收到的數據
    error_log("Received data: " . print_r($data, true));
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => "Error processing input data: " . $e->getMessage()
    ];
    echo json_encode($response);
    exit;
}

// 驗證必要字段
if (!isset($data['case_id']) || !isset($data['student_id']) || !isset($data['status'])) {
    $response = [
        'success' => false,
        'message' => 'Missing required fields (case_id, student_id, status)'
    ];
    error_log("Validation error: " . json_encode($response));
    echo json_encode($response);
    exit;
}

// 提取和清理數據
$case_id = trim($data['case_id']);
$student_id = trim($data['student_id']);
$status = strtolower(trim($data['status']));

// 對於incomplete狀態，確保提供了reason
if ($status === 'incomplete' && (!isset($data['reason']) || empty($data['reason']))) {
    $response = [
        'success' => false,
        'message' => 'For incomplete status, reason field is required'
    ];
    error_log("Validation error: " . json_encode($response));
    echo json_encode($response);
    exit;
}

$reason = isset($data['reason']) ? trim($data['reason']) : null;
$next_action = isset($data['next_action']) ? trim($data['next_action']) : null;

// 驗證狀態值
if (!in_array($status, ['completed', 'incomplete'])) {
    $response = [
        'success' => false,
        'message' => 'Invalid status value. Must be "completed" or "incomplete".'
    ];
    error_log("Invalid status: " . $status);
    echo json_encode($response);
    exit;
}

try {
    // 獲取數據庫連接
    $conn = getDbConnection();
    if (!$conn) {
        throw new Exception("Failed to connect to database");
    }
    
    // 開始事務
    $conn->begin_transaction();
    error_log("Transaction started");
    
    // 查找已確認的預約和相關用戶信息
    $stmt = $conn->prepare("SELECT b.booking_id, b.tutor_id, 
                         m.username as student_name, 
                         t.username as tutor_name,
                         ft.token as tutor_fcm_token
                         FROM booking b 
                         JOIN member m ON b.student_id = m.member_id
                         JOIN member t ON b.tutor_id = t.member_id
                         LEFT JOIN fcm_tokens ft ON b.tutor_id = ft.member_id
                         WHERE b.match_id = ? 
                         AND b.student_id = ? 
                         AND b.status = 'confirmed'
                         ORDER BY ft.updated_at DESC LIMIT 1");
    if (!$stmt) {
        throw new Exception("Prepare statement failed: " . $conn->error);
    }
    
    $stmt->bind_param("ss", $case_id, $student_id);
    if (!$stmt->execute()) {
        throw new Exception("Execute statement failed: " . $stmt->error);
    }
    
    $result = $stmt->get_result();
    $booking = $result->fetch_assoc();
    
    if (!$booking) {
        throw new Exception("No confirmed booking found for match ID: $case_id");
    }
    
    $booking_id = $booking['booking_id'];
    $tutor_id = $booking['tutor_id'];
    $student_name = $booking['student_name'];
    $tutor_name = $booking['tutor_name'];
    $tutor_fcm_token = $booking['tutor_fcm_token'];
    
    error_log("Found booking_id: $booking_id");
    
    // 查詢現有的課程狀態記錄
    $check_stmt = $conn->prepare("SELECT * FROM first_lesson WHERE booking_id = ?");
    if (!$check_stmt) {
        throw new Exception("Prepare check statement failed: " . $conn->error);
    }
    
    $check_stmt->bind_param("i", $booking_id);
    if (!$check_stmt->execute()) {
        throw new Exception("Execute check statement failed: " . $check_stmt->error);
    }
    
    $check_result = $check_stmt->get_result();
    $existing_record = $check_result->fetch_assoc();
    
    // 根據是否存在記錄決定是插入還是更新
    if (!$existing_record) {
        // 插入新記錄
        $insert_stmt = $conn->prepare("INSERT INTO first_lesson 
                                  (booking_id, ps_response, ps_reason, ps_response_time, next_action, response_deadline) 
                                  VALUES (?, ?, ?, NOW(), ?, DATE_ADD(NOW(), INTERVAL 72 HOUR))");
        if (!$insert_stmt) {
            throw new Exception("Prepare insert statement failed: " . $conn->error);
        }
        
        $insert_stmt->bind_param("isss", $booking_id, $status, $reason, $next_action);
        if (!$insert_stmt->execute()) {
            throw new Exception("Execute insert statement failed: " . $insert_stmt->error);
        }
        
        error_log("Inserted new record for booking_id: $booking_id");
    } else {
        // 更新現有記錄
        $update_stmt = $conn->prepare("UPDATE first_lesson 
                                SET ps_response = ?, 
                                    ps_reason = ?,
                                    ps_response_time = NOW(),
                                    next_action = ?
                                WHERE booking_id = ?");
        if (!$update_stmt) {
            throw new Exception("Prepare update statement failed: " . $conn->error);
        }
        
        $update_stmt->bind_param("sssi", $status, $reason, $next_action, $booking_id);
        if (!$update_stmt->execute()) {
            throw new Exception("Execute update statement failed: " . $update_stmt->error);
        }
        
        error_log("Updated record for booking_id: $booking_id");
    }
    
    // 獲取更新後的記錄
    $get_stmt = $conn->prepare("SELECT * FROM first_lesson WHERE booking_id = ?");
    if (!$get_stmt) {
        throw new Exception("Prepare get statement failed: " . $conn->error);
    }
    
    $get_stmt->bind_param("i", $booking_id);
    if (!$get_stmt->execute()) {
        throw new Exception("Execute get statement failed: " . $get_stmt->error);
    }
    
    $get_result = $get_stmt->get_result();
    $status_record = $get_result->fetch_assoc();
    
    // 準備響應
    $response = [
        'success' => true,
        'booking_id' => $booking_id
    ];
    
    // 處理不同的狀態場景
    if ($status_record['ps_response'] && $status_record['t_response']) {
        // 雙方都已回應
        if ($status_record['ps_response'] === 'completed' && $status_record['t_response'] === 'completed') {
            // CASE 1: 兩人都標記為已完成
            $update_booking_stmt = $conn->prepare("UPDATE booking SET status = 'completed' WHERE booking_id = ?");
            if (!$update_booking_stmt) {
                throw new Exception("Prepare update booking statement failed: " . $conn->error);
            }
            
            $update_booking_stmt->bind_param("i", $booking_id);
            if (!$update_booking_stmt->execute()) {
                throw new Exception("Execute update booking statement failed: " . $update_booking_stmt->error);
            }
            
            // 刪除同一個 match_id 的其他可用時間槽記錄
            $delete_other_slots_stmt = $conn->prepare("DELETE FROM booking WHERE match_id = ? AND booking_id != ? AND status = 'available'");
            if ($delete_other_slots_stmt) {
                $delete_other_slots_stmt->bind_param("si", $case_id, $booking_id);
                $delete_other_slots_stmt->execute();
                $affected_rows = $delete_other_slots_stmt->affected_rows;
                $delete_other_slots_stmt->close();
                error_log("Deleted $affected_rows other available bookings for match_id: $case_id");
            }
            
            $response['status'] = 'completed';
            $response['message'] = '您和導師均已確認課程完成。課酬已發送給導師。請提供反饋評價。';
            
            // 為導師創建通知
            $notificationTitle = "課程狀態已更新";
            $notificationMessage = "學生 $student_name 已回應第一堂課狀態，請查看詳情。";
            insertNotification($conn, $tutor_id, $notificationTitle, $notificationMessage, "lesson_completed", $booking_id);
            
            // 發送FCM推送給導師
            if (!empty($tutor_fcm_token)) {
                sendFCM($tutor_fcm_token, $notificationTitle, $notificationMessage, "lesson_completed", $case_id, $booking_id);
            } else {
                error_log("No FCM token found for tutor: $tutor_id");
            }
            
        } else if ($status_record['ps_response'] === 'incomplete' && $status_record['t_response'] === 'incomplete') {
            // CASE 2: 兩人都標記為未完成
            $update_booking_stmt = $conn->prepare("UPDATE booking SET status = 'confirmed' WHERE booking_id = ?");
            if (!$update_booking_stmt) {
                throw new Exception("Prepare update booking statement failed: " . $conn->error);
            }
            
            $update_booking_stmt->bind_param("i", $booking_id);
            if (!$update_booking_stmt->execute()) {
                throw new Exception("Execute update booking statement failed: " . $update_booking_stmt->error);
            }
            
            $response['status'] = 'both_incomplete';
            $response['reason'] = $reason;
            $response['message'] = '您和導師均已確認課程未完成。課酬將退還給您，您可以重新預約或尋找新導師。';
            
            // 為導師創建通知
            $notificationTitle = "課程狀態已更新";
            $notificationMessage = "學生 $student_name.已回應第一堂課狀態，請查看詳情。";
            insertNotification($conn, $tutor_id, $notificationTitle, $notificationMessage, "lesson_incomplete", $booking_id);
            
            // 發送FCM推送給導師
            if (!empty($tutor_fcm_token)) {
                sendFCM($tutor_fcm_token, $notificationTitle, $notificationMessage, "lesson_incomplete", $case_id, $booking_id);
            } else {
                error_log("No FCM token found for tutor: $tutor_id");
            }
            
        } else {
            // CASE 3: 學員和導師的狀態不一致
            $update_booking_stmt = $conn->prepare("UPDATE booking SET status = 'conflict' WHERE booking_id = ?");
            if (!$update_booking_stmt) {
                throw new Exception("Prepare update booking statement failed: " . $conn->error);
            }
            
            $update_booking_stmt->bind_param("i", $booking_id);
            if (!$update_booking_stmt->execute()) {
                throw new Exception("Execute update booking statement failed: " . $update_booking_stmt->error);
            }
            
            $conflict_message = "";
            if ($status_record['t_response'] === 'completed') {
                $conflict_message = "導師標記課程為已完成，但您標記為未完成。";
            } else {
                $conflict_message = "導師標記課程為未完成，但您標記為已完成。";
            }
            
            $response['status'] = 'conflict';
            $response['message'] = $conflict_message . " 此爭議將由管理員處理。請提供證據（截圖/通訊記錄）給管理員。";
            
            // 為導師創建通知
            $notificationTitle = "課程狀態已更新";
            $notificationMessage = "學生 $student_name 已回應第一堂課狀態，請查看詳情。";
            insertNotification($conn, $tutor_id, $notificationTitle, $notificationMessage, "lesson_conflict", $booking_id);
            
            // 發送FCM推送給導師
            if (!empty($tutor_fcm_token)) {
                sendFCM($tutor_fcm_token, $notificationTitle, $notificationMessage, "lesson_conflict", $case_id, $booking_id);
            } else {
                error_log("No FCM token found for tutor: $tutor_id");
            }
        }
    } else {
        // CASE 4: 只有學員回應，導師未回應
        $response['status'] = 'waiting';
        $response['message'] = '您的狀態已提交。等待導師回應，若導師3天內未回應，課酬將自動發送給導師。';
        
        // 設置回應截止時間（72小時後）
        $deadline_stmt = $conn->prepare("UPDATE first_lesson SET response_deadline = DATE_ADD(NOW(), INTERVAL 72 HOUR) WHERE booking_id = ? AND response_deadline IS NULL");
        if ($deadline_stmt) {
            $deadline_stmt->bind_param("i", $booking_id);
            $deadline_stmt->execute();
            $deadline_stmt->close();
        }
        
        // 為導師創建通知
        $notificationTitle = "請回應課程狀態";
        $notificationMessage = "學生 $student_name 已回應第一堂課狀態，請您在3天內回應，謝謝。";
        insertNotification($conn, $tutor_id, $notificationTitle, $notificationMessage, "lesson_status_request", $booking_id);
        
        // 發送FCM推送給導師
        if (!empty($tutor_fcm_token)) {
            sendFCM($tutor_fcm_token, $notificationTitle, $notificationMessage, "lesson_status_request", $case_id, $booking_id);
        } else {
            error_log("No FCM token found for tutor: $tutor_id");
        }
    }
    
    // 提交事務
    $conn->commit();
    error_log("Transaction committed");
    
    // 發送響應
    error_log("Sending successful response: " . json_encode($response));
    echo json_encode($response);
    
} catch (Exception $e) {
    // 記錄錯誤並回滾事務
    error_log("Error in update_student_first_lesson_status.php: " . $e->getMessage());
    
    if (isset($conn) && $conn instanceof mysqli) {
        try {
            $conn->rollback();
            error_log("Transaction rolled back");
        } catch (Exception $rollback_e) {
            error_log("Rollback failed: " . $rollback_e->getMessage());
        }
    }
    
    // 發送錯誤響應
    $response = [
        'success' => false,
        'message' => $e->getMessage(),
        'status' => 'error'
    ];
    
    echo json_encode($response);
} finally {
    // 關閉所有語句
    if (isset($stmt) && $stmt instanceof mysqli_stmt) $stmt->close();
    if (isset($check_stmt) && $check_stmt instanceof mysqli_stmt) $check_stmt->close();
    if (isset($insert_stmt) && $insert_stmt instanceof mysqli_stmt) $insert_stmt->close();
    if (isset($update_stmt) && $update_stmt instanceof mysqli_stmt) $update_stmt->close();
    if (isset($get_stmt) && $get_stmt instanceof mysqli_stmt) $get_stmt->close();
    if (isset($update_booking_stmt) && $update_booking_stmt instanceof mysqli_stmt) $update_booking_stmt->close();
    
    // 關閉數據庫連接
    if (isset($conn) && $conn instanceof mysqli) {
        try {
            $conn->close();
            error_log("Database connection closed");
        } catch (Exception $close_e) {
            error_log("Closing connection failed: " . $close_e->getMessage());
        }
    }
    
    error_log("update_student_first_lesson_status.php finished");
}

/**
 * 插入通知到數據庫
 */
function insertNotification($conn, $member_id, $title, $message, $type, $related_id) {
    try {
        $query = "INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) 
                 VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)";
        
        $stmt = $conn->prepare($query);
        if (!$stmt) {
            throw new Exception("Prepare notification statement failed: " . $conn->error);
        }
        
        $stmt->bind_param("ssssi", $member_id, $title, $message, $type, $related_id);
        
        if (!$stmt->execute()) {
            throw new Exception("Execute notification statement failed: " . $stmt->error);
        }
        
        error_log("Notification inserted for member: $member_id, type: $type");
        $stmt->close();
        return true;
    } catch (Exception $e) {
        error_log("Failed to insert notification: " . $e->getMessage());
        return false;
    }
}

/**
 * 發送FCM推送通知
 */
function sendFCM($token, $title, $message, $type, $case_id, $booking_id) {
    if (empty($token)) {
        error_log("FCM token is empty, cannot send notification");
        return false;
    }
    
    error_log("Sending FCM to token: $token");
    
    try {
        // 讀取服務帳戶憑證
        $serviceAccount = json_decode(file_get_contents('D:/xampp/htdocs/firebase-service-account.json'), true);
        if (!$serviceAccount) {
            error_log("錯誤：無法讀取服務帳戶憑證");
            return false;
        }
        
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
        if (!isset($data['access_token'])) {
            error_log("Failed to get access token: " . $response);
            return false;
        }
        
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
                    "match_id" => strval($case_id),
                    "booking_id" => strval($booking_id),
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
        curl_close($ch);
        
        error_log("FCM send result: " . $fcm_result);
        
        if ($http_status !== 200) {
            error_log("FCM Error: Status " . $http_status);
            return false;
        }
        
        return true;
    } catch (Exception $e) {
        error_log("FCM Error: " . $e->getMessage());
        return false;
    }
}
?>
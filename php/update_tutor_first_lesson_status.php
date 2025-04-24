<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// 設置錯誤處理
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', 'php_error.log');

// 記錄執行開始
error_log("update_tutor_first_lesson_status.php started");

// 包含數據庫配置
require_once 'db_config.php';
require_once 'fcm_helper.php';

try {
    // 獲取輸入數據
    $data = json_decode(file_get_contents("php://input"), true);
    if (!$data) {
        $data = $_POST;
    }
    
    // 記錄接收到的數據
    error_log("Received data: " . print_r($data, true));
    
    // 驗證必要字段
    if (!isset($data['case_id']) || !isset($data['tutor_id']) || !isset($data['status'])) {
        throw new Exception('Missing required fields (case_id, tutor_id, status)');
    }
    
    // 提取和清理數據
    $case_id = trim($data['case_id']);
    $tutor_id = trim($data['tutor_id']);
    $status = strtolower(trim($data['status']));
    $reason = isset($data['reason']) ? trim($data['reason']) : null;
    $next_action = isset($data['next_action']) ? trim($data['next_action']) : null;
    
    // 驗證狀態值
    if (!in_array($status, ['completed', 'incomplete'])) {
        throw new Exception('Invalid status value. Must be "completed" or "incomplete".');
    }
    
    // 獲取數據庫連接
    $conn = getDbConnection();
    
    // 開始事務
    $conn->begin_transaction();
    
    // 查找預約和相關信息
    $stmt = $conn->prepare("SELECT b.booking_id, b.student_id, 
                          m.username as student_name, 
                          t.username as tutor_name,
                          ft.token as tutor_fcm_token,
                          fs.token as student_fcm_token
                          FROM booking b 
                          JOIN member m ON b.student_id = m.member_id
                          JOIN member t ON b.tutor_id = t.member_id
                          LEFT JOIN fcm_tokens ft ON b.tutor_id = ft.member_id
                          LEFT JOIN fcm_tokens fs ON b.student_id = fs.member_id
                          WHERE b.match_id = ? 
                          AND b.tutor_id = ? 
                          AND b.status = 'confirmed'
                          ORDER BY ft.updated_at DESC, fs.updated_at DESC
                          LIMIT 1");
    
    $stmt->bind_param("ss", $case_id, $tutor_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $booking = $result->fetch_assoc();
    
    if (!$booking) {
        throw new Exception("No confirmed booking found for match ID: $case_id");
    }
    
    $booking_id = $booking['booking_id'];
    $student_id = $booking['student_id'];
    $student_name = $booking['student_name'];
    $tutor_name = $booking['tutor_name'];
    $student_fcm_token = $booking['student_fcm_token'];
    
    // 查詢現有的課程狀態記錄
    $check_stmt = $conn->prepare("SELECT * FROM first_lesson WHERE booking_id = ?");
    $check_stmt->bind_param("i", $booking_id);
    $check_stmt->execute();
    $check_result = $check_stmt->get_result();
    $existing_record = $check_result->fetch_assoc();
    
    // 根據是否存在記錄決定是插入還是更新
    if (!$existing_record) {
        // 插入新記錄
        $insert_stmt = $conn->prepare("INSERT INTO first_lesson 
                               (booking_id, t_response, t_reason, t_response_time, next_action, response_deadline) 
                               VALUES (?, ?, ?, NOW(), ?, DATE_ADD(NOW(), INTERVAL 72 HOUR))");
        
        $insert_stmt->bind_param("isss", $booking_id, $status, $reason, $next_action);
        $insert_stmt->execute();
    } else {
        // 更新現有記錄
        $update_stmt = $conn->prepare("UPDATE first_lesson 
                             SET t_response = ?, 
                                 t_reason = ?,
                                 t_response_time = NOW(),
                                 next_action = ?
                             WHERE booking_id = ?");
        
        $update_stmt->bind_param("sssi", $status, $reason, $next_action, $booking_id);
        $update_stmt->execute();
    }
    
    // 獲取更新後的記錄
    $get_stmt = $conn->prepare("SELECT * FROM first_lesson WHERE booking_id = ?");
    $get_stmt->bind_param("i", $booking_id);
    $get_stmt->execute();
    $get_result = $get_stmt->get_result();
    $status_record = $get_result->fetch_assoc();
    
    // 準備響應
    $response = [
        'success' => true,
        'booking_id' => $booking_id
    ];
    
    // 處理不同狀況
    if ($status_record['ps_response']) {
        // 雙方都已回應
        if ($status_record['ps_response'] === 'completed' && $status_record['t_response'] === 'completed') {
            // CASE 1: 兩人都標記為已完成
            $update_booking_stmt = $conn->prepare("UPDATE booking SET status = 'completed' WHERE booking_id = ?");
            $update_booking_stmt->bind_param("i", $booking_id);
            $update_booking_stmt->execute();
            
            $delete_other_slots_stmt = $conn->prepare("DELETE FROM booking WHERE match_id = ? AND booking_id != ? AND status = 'available'");
            if ($delete_other_slots_stmt) {
                $delete_other_slots_stmt->bind_param("si", $case_id, $booking_id);
                $delete_other_slots_stmt->execute();
                $affected_rows = $delete_other_slots_stmt->affected_rows;
                $delete_other_slots_stmt->close();
                error_log("Deleted $affected_rows other available bookings for match_id: $case_id");
            }   
            
            $response['status'] = 'completed';
            $response['message'] = '您和學生均已確認課程完成。課酬將在付款流程處理後發送給您。';
            
            // 為學生創建通知
            insertNotification($conn, $student_id, "課程狀態已更新", 
                            "導師 $tutor_name 已回應第一堂課狀態，請查看詳情。", 
                            "lesson_completed", $booking_id);
            
            // 發送FCM推送給學生
            if (!empty($student_fcm_token)) {
                sendFCM($student_fcm_token, "課程狀態已更新", 
                     "導師 $tutor_name 已回應第一堂課狀態，請查看詳情。", 
                     "lesson_completed", $case_id, $booking_id);
            }
            
        } else if ($status_record['ps_response'] === 'incomplete' && $status_record['t_response'] === 'incomplete') {
            // CASE 2: 兩人都標記為未完成
            $update_booking_stmt = $conn->prepare("UPDATE booking SET status = 'incomplete' WHERE booking_id = ?");
            $update_booking_stmt->bind_param("i", $booking_id);
            $update_booking_stmt->execute();
            
            // 更新付款狀態為退款 - 使用 match_id 而非 booking_id
            $payment_stmt = $conn->prepare("UPDATE payment SET status = 'refunded', verified_at = NOW() WHERE match_id = ?");
            if ($payment_stmt) {
                $payment_stmt->bind_param("i", $case_id);
                $payment_stmt->execute();
            }
            
            // 直接建立新的可用時間段，不檢查reset_status
            // 步驟1: 獲取當前booking的時間信息
            $get_time_stmt = $conn->prepare("SELECT start_time, end_time FROM booking WHERE booking_id = ?");
            $get_time_stmt->bind_param("i", $booking_id);
            $get_time_stmt->execute();
            $time_result = $get_time_stmt->get_result();
            
            if ($time_result->num_rows > 0) {
                $time_row = $time_result->fetch_assoc();
                
                // 步驟2: 檢查是否已存在相同時間段的可用預約
                $check_slot_stmt = $conn->prepare("SELECT COUNT(*) as slot_count 
                                                FROM booking 
                                                WHERE match_id = ? 
                                                AND tutor_id = ? 
                                                AND start_time = ? 
                                                AND end_time = ? 
                                                AND status = 'available' 
                                                AND booking_id != ?");
                $check_slot_stmt->bind_param("ssssi", $case_id, $tutor_id, $time_row['start_time'], $time_row['end_time'], $booking_id);
                $check_slot_stmt->execute();
                $slot_result = $check_slot_stmt->get_result();
                $slot_row = $slot_result->fetch_assoc();
                
                // 步驟3: 如果沒有重複的時間段，則創建新的
                if ($slot_row['slot_count'] == 0) {
                    // 創建新的可用時間段
                    $create_slot_stmt = $conn->prepare("INSERT INTO booking (match_id, tutor_id, start_time, end_time, status, created_at, updated_at) 
                                                        VALUES (?, ?, ?, ?, 'available', NOW(), NOW())");
                    $create_slot_stmt->bind_param("ssss", $case_id, $tutor_id, $time_row['start_time'], $time_row['end_time']);
                    $create_slot_stmt->execute();
                    
                    error_log("Created new available time slot for match_id: $case_id with time: {$time_row['start_time']} - {$time_row['end_time']}");
                } else {
                    error_log("Duplicate time slot found, skipped creation for match_id: $case_id");
                }
            }
            
            $response['status'] = 'both_incomplete';
            $response['reason'] = $reason;
            $response['message'] = '您和學生均已確認課程未完成。課酬將退還給學生，請提供新的時間安排。';
            
            // 為學生創建通知
            insertNotification($conn, $student_id, "課程狀態已更新", 
                            "導師 $tutor_name 已回應第一堂課狀態，請查看詳情。", 
                            "lesson_incomplete", $booking_id);
            
            // 發送FCM推送給學生
            if (!empty($student_fcm_token)) {
                sendFCM($student_fcm_token, "課程狀態已更新", 
                     "導師 $tutor_name 已回應第一堂課狀態，請查看詳情。", 
                     "lesson_incomplete", $case_id, $booking_id);
            }
            
        } else {
            // CASE 3: 狀態不一致
            $update_booking_stmt = $conn->prepare("UPDATE booking SET status = 'conflict' WHERE booking_id = ?");
            $update_booking_stmt->bind_param("i", $booking_id);
            $update_booking_stmt->execute();
            
            $conflict_message = "";
            if ($status_record['ps_response'] === 'completed') {
                $conflict_message = "學生標記課程為已完成，但您標記為未完成。";
            } else {
                $conflict_message = "學生標記課程為未完成，但您標記為已完成。";
            }
            
            $response['status'] = 'conflict';
            $response['message'] = $conflict_message . " 此爭議將由管理員處理。請提供證據（截圖/通訊記錄）給管理員。";
            
            // 為學生創建通知
            insertNotification($conn, $student_id, "課程狀態已更新", 
                            "導師 $tutor_name 已回應第一堂課狀態，請查看詳情。", 
                            "lesson_conflict", $booking_id);
            
            // 發送FCM推送給學生
            if (!empty($student_fcm_token)) {
                sendFCM($student_fcm_token, "課程狀態已更新", 
                     "導師 $tutor_name 已回應第一堂課狀態，請查看詳情。", 
                     "lesson_conflict", $case_id, $booking_id);
            }
        }
    } else {
        // CASE 5: 學生還未回應
        $response['status'] = 'waiting';
        $response['message'] = '您的狀態已提交。等待學生回應，若學生3天內未回應，課酬將自動發送給您。';
        
        // 為學生創建通知
        insertNotification($conn, $student_id, "請回應課程狀態", 
                        "導師 $tutor_name 已回應第一堂課狀態，請您在3天內回應，謝謝。", 
                        "lesson_status_request", $booking_id);
        
        // 發送FCM推送給學生
        if (!empty($student_fcm_token)) {
            sendFCM($student_fcm_token, "請回應課程狀態", 
                 "導師 $tutor_name 已回應第一堂課狀態，請您在3天內回應，謝謝。", 
                 "lesson_status_request", $case_id, $booking_id);
        }
    }
    
    // 提交事務
    $conn->commit();
    
    // 輸出JSON響應
    echo json_encode($response);
    
} catch (Exception $e) {
    // 記錄錯誤
    error_log("Error in update_tutor_first_lesson_status.php: " . $e->getMessage());
    
    // 回滾事務
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->rollback();
    }
    
    // 發送錯誤響應
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
} finally {
    // 關閉連接
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
    
    // 記錄執行結束
    error_log("update_tutor_first_lesson_status.php finished");
}

// 輔助函數：插入通知
function insertNotification($conn, $member_id, $title, $message, $type, $related_id) {
    try {
        $query = "INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) 
                 VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)";
        
        $stmt = $conn->prepare($query);
        $stmt->bind_param("ssssi", $member_id, $title, $message, $type, $related_id);
        $stmt->execute();
        
        return true;
    } catch (Exception $e) {
        error_log("Failed to insert notification: " . $e->getMessage());
        return false;
    }
}

// 輔助函數：發送FCM通知
function sendFCM($token, $title, $message, $type, $case_id, $booking_id) {
    if (empty($token)) {
        error_log("FCM token is empty, cannot send notification");
        return false;
    }
    
    try {
        // 使用fcm_helper如果存在
        if (function_exists('sendPushNotification')) {
            return sendPushNotification($token, $title, $message, $type);
        }
        
        // 讀取服務帳戶憑證
        $serviceAccount = json_decode(file_get_contents('firebase-service-account.json'), true);
        if (!$serviceAccount) {
            error_log("Could not read service account credentials");
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
        
        if ($http_status == 200) {
            return true;
        }
        
        error_log("FCM Error: Status " . $http_status);
        return false;
    } catch (Exception $e) {
        error_log("FCM Error: " . $e->getMessage());
        return false;
    }
} 
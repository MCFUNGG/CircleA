<?php
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', 'php_error.log');

header("Content-Type: application/json");

try {
    if (!isset($_POST['match_id']) || !isset($_POST['tutor_id']) || !isset($_POST['slots'])) {
        throw new Exception("Missing required parameters");
    }

    $matchId = (int)trim($_POST['match_id']);
    $tutorId = (int)trim($_POST['tutor_id']);
    $slots = json_decode($_POST['slots'], true);

    error_log("Received data: " . print_r($_POST, true));
    error_log("Decoded slots: " . print_r($slots, true));

    require_once 'db_config.php';

    // 创建数据库连接
    $connect = getDbConnection();
    if (!$connect) {
        throw new Exception("Database connection failed: " . mysqli_connect_error());
    }

    mysqli_begin_transaction($connect);
    
    try {
        $successCount = 0;
        foreach ($slots as $slot) {
            // Parse datetime strings into date and time components
            $startDateTime = new DateTime($slot['start_time']);
            $endDateTime = new DateTime($slot['end_time']);
            
            // Format components
            $date = $startDateTime->format('Y-m-d');
            $startTime = $startDateTime->format('H:i:s');
            $endTime = $endDateTime->format('H:i:s');
            $action = $slot['action'];

            if ($action === 'update' && isset($slot['slot_id'])) {
                // Update existing slot
                $query = "UPDATE booking 
                        SET date = ?,
                            start_time = ?,
                            end_time = ?
                        WHERE booking_id = ? 
                        AND tutor_id = ? 
                        AND status = 'available'";
    
                $stmt = mysqli_prepare($connect, $query);
                mysqli_stmt_bind_param($stmt, "sssis", 
                    $date,
                    $startTime,
                    $endTime,
                    $slot['slot_id'],
                    $tutorId
                );
            } else {
                // Insert new slot
                $query = "INSERT INTO booking (
                            match_id,
                            tutor_id,
                            date,
                            start_time,
                            end_time,
                            status
                        ) VALUES (?, ?, ?, ?, ?, 'available')";
    
                $stmt = mysqli_prepare($connect, $query);
                mysqli_stmt_bind_param($stmt, "iisss", 
                    $matchId,
                    $tutorId, 
                    $date,
                    $startTime,
                    $endTime
                );
            }
            
            if (!mysqli_stmt_execute($stmt)) {
                throw new Exception("Execute failed: " . mysqli_stmt_error($stmt));
            }
            
            $successCount++;
            mysqli_stmt_close($stmt);
        }

        if ($successCount == count($slots)) {
            // 成功保存時間段後，發送FCM通知給學生
            
            // 1. 獲取匹配信息以確定學生ID
            $match_query = "SELECT m.*, 
                           COALESCE(t.username, 'Unknown Tutor') as tutor_name 
                           FROM `match` m 
                           LEFT JOIN member t ON t.member_id = m.tutor_id 
                           WHERE m.match_id = ?";
            
            $stmt = mysqli_prepare($connect, $match_query);
            mysqli_stmt_bind_param($stmt, "i", $matchId);
            mysqli_stmt_execute($stmt);
            $match_result = mysqli_stmt_get_result($stmt);
            
            if ($match_result && mysqli_num_rows($match_result) > 0) {
                $match_data = mysqli_fetch_assoc($match_result);
                $student_id = $match_data['ps_id'];
                $tutor_name = $match_data['tutor_name'];
                
                // 2. 添加通知到數據庫
                $notification_title = "導師已更新可用時間";
                $notification_message = "導師 {$tutor_name} 已經更新了可預約的時間段，請前往查看並選擇合適的時間。";
                $notification_type = "new_request"; // 使用已知能正常工作的類型
                
                $insert_notification = "INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) 
                                      VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)";
                
                $stmt = mysqli_prepare($connect, $insert_notification);
                mysqli_stmt_bind_param($stmt, "ssssi", 
                    $student_id,
                    $notification_title,
                    $notification_message,
                    $notification_type,
                    $matchId
                );
                
                if (!mysqli_stmt_execute($stmt)) {
                    error_log("Failed to insert notification: " . mysqli_error($connect));
                }
                
                // 3. 獲取學生的FCM令牌
                $token_query = "SELECT token FROM fcm_tokens WHERE member_id = ? ORDER BY created_at DESC LIMIT 1";
                $stmt = mysqli_prepare($connect, $token_query);
                mysqli_stmt_bind_param($stmt, "s", $student_id);
                mysqli_stmt_execute($stmt);
                $token_result = mysqli_stmt_get_result($stmt);
                
                $fcm_debug = [];
                
                if ($token_result && $token_row = mysqli_fetch_assoc($token_result)) {
                    $fcm_token = $token_row['token'];
                    $fcm_debug["token_found"] = true;
                    
                    // 4. 發送FCM通知
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
                                        "title" => $notification_title,
                                        "body" => $notification_message
                                    ],
                                    "android" => [
                                        "notification" => [
                                            "channel_id" => "CircleA_Channel",
                                            "click_action" => "FLUTTER_NOTIFICATION_CLICK"
                                        ]
                                    ],
                                    "data" => [
                                        "type" => $notification_type,
                                        "match_id" => strval($matchId),
                                        "tutor_name" => $tutor_name,
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
                    $fcm_debug["error"] = "No FCM token found for student ID: " . $student_id;
                    error_log($fcm_debug["error"]);
                }
            }
            
            mysqli_commit($connect);
            echo json_encode([
                "success" => true,
                "message" => "Successfully saved " . $successCount . " time slots",
                "debug" => [
                    "match_id" => $matchId,
                    "tutor_id" => $tutorId,
                    "slots_processed" => $successCount,
                    "fcm_debug" => isset($fcm_debug) ? $fcm_debug : []
                ]
            ]);
        } else {
            throw new Exception("Only saved $successCount out of " . count($slots) . " slots");
        }
    } catch (Exception $e) {
        error_log("Database error: " . $e->getMessage());
        mysqli_rollback($connect);
        throw $e;
    }

    mysqli_close($connect);

} catch (Exception $e) {
    error_log("Error in post_time_slot.php: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage(),
        "debug" => [
            "error" => $e->getMessage(),
            "post_data" => $_POST
        ]
    ]);
}
?>
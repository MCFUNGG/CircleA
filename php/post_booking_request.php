<?php
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', 'php_error.log');

header("Content-Type: application/json");

try {
    // Database connection
    require_once 'db_config.php';

    // 创建数据库连接
    $connect = getDbConnection();
    if (!$connect) {
        throw new Exception("Database connection failed: " . mysqli_connect_error());
    }

    // Validate required parameters
    if (!isset($_POST['match_id']) || !isset($_POST['slot_id']) || !isset($_POST['student_id'])) {
        throw new Exception("Missing required parameters");
    }

    $matchId = mysqli_real_escape_string($connect, $_POST['match_id']);
    $slotId = mysqli_real_escape_string($connect, $_POST['slot_id']);
    $studentId = mysqli_real_escape_string($connect, $_POST['student_id']);

    mysqli_begin_transaction($connect);

    // First check if the slot is still available
    $checkQuery = "SELECT status FROM booking 
                  WHERE booking_id = ? 
                  AND match_id = ? 
                  AND status = 'available'";

    $stmt = mysqli_prepare($connect, $checkQuery);
    mysqli_stmt_bind_param($stmt, "ss", $slotId, $matchId);
    
    if (!mysqli_stmt_execute($stmt)) {
        throw new Exception("Error checking slot availability");
    }

    $result = mysqli_stmt_get_result($stmt);

    if (mysqli_num_rows($result) === 0) {
        throw new Exception("This time slot is no longer available");
    }

    // Check if student already has a pending or confirmed booking for this match
    $checkExistingQuery = "SELECT status FROM booking 
                          WHERE match_id = ? 
                          AND student_id = ? 
                          AND status IN ('pending', 'confirmed')";

    $stmt = mysqli_prepare($connect, $checkExistingQuery);
    mysqli_stmt_bind_param($stmt, "ss", $matchId, $studentId);
    
    if (!mysqli_stmt_execute($stmt)) {
        throw new Exception("Error checking existing bookings");
    }

    $existingResult = mysqli_stmt_get_result($stmt);

    if (mysqli_num_rows($existingResult) > 0) {
        throw new Exception("You already have a pending or confirmed booking for this case");
    }

    // Update the slot with student's booking request
    $updateQuery = "UPDATE booking 
                   SET status = 'pending',
                       student_id = ?,
                       updated_at = NOW()
                   WHERE booking_id = ? 
                   AND match_id = ? 
                   AND status = 'available'";

    $stmt = mysqli_prepare($connect, $updateQuery);
    mysqli_stmt_bind_param($stmt, "sss", $studentId, $slotId, $matchId);

    if (!mysqli_stmt_execute($stmt)) {
        throw new Exception("Failed to update booking status");
    }

    if (mysqli_affected_rows($connect) === 0) {
        throw new Exception("Failed to book the time slot. Please try again.");
    }

    // Get the time slot details for the notification
    $slotDetailsQuery = "SELECT b.date, b.start_time, b.end_time 
                         FROM booking b
                         WHERE b.booking_id = ?";
    
    $stmt = mysqli_prepare($connect, $slotDetailsQuery);
    mysqli_stmt_bind_param($stmt, "s", $slotId);
    
    if (!mysqli_stmt_execute($stmt)) {
        throw new Exception("Error retrieving time slot details");
    }
    
    $slotResult = mysqli_stmt_get_result($stmt);
    $slotDetails = mysqli_fetch_assoc($slotResult);
    
    // Format date and time for notification
    $date = $slotDetails['date'];
    $startTime = date('H:i', strtotime($slotDetails['start_time']));
    $endTime = date('H:i', strtotime($slotDetails['end_time']));
    $timeSlotFormatted = "$date $startTime - $endTime";

    // Get match details to determine tutor ID and names
    $matchDetailsQuery = "SELECT m.tutor_id, 
                         COALESCE(t.username, 'Unknown Tutor') as tutor_name,
                         COALESCE(s.username, 'Unknown Student') as student_name
                         FROM `match` m 
                         LEFT JOIN member t ON t.member_id = m.tutor_id
                         LEFT JOIN member s ON s.member_id = ?
                         WHERE m.match_id = ?";
                         
    $stmt = mysqli_prepare($connect, $matchDetailsQuery);
    mysqli_stmt_bind_param($stmt, "ss", $studentId, $matchId);
    
    if (!mysqli_stmt_execute($stmt)) {
        throw new Exception("Error retrieving match details");
    }
    
    $matchResult = mysqli_stmt_get_result($stmt);
    
    if (mysqli_num_rows($matchResult) === 0) {
        throw new Exception("Match details not found");
    }
    
    $matchDetails = mysqli_fetch_assoc($matchResult);
    $tutorId = $matchDetails['tutor_id'];
    $tutorName = $matchDetails['tutor_name'];
    $studentName = $matchDetails['student_name'];
    
    // Insert notification into database
    $notificationTitle = "新的預約時間請求";
    $notificationMessage = "{$studentName} 向您請求預約時間 {$timeSlotFormatted}，請查看並確認。";
    $notificationType = "new_request";
    
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
    
    // Get tutor's FCM token
    $tokenQuery = "SELECT token FROM fcm_tokens WHERE member_id = ? ORDER BY created_at DESC LIMIT 1";
    $stmt = mysqli_prepare($connect, $tokenQuery);
    mysqli_stmt_bind_param($stmt, "s", $tutorId);
    mysqli_stmt_execute($stmt);
    $tokenResult = mysqli_stmt_get_result($stmt);
    
    $fcm_debug = [];
    
    if ($tokenResult && $token_row = mysqli_fetch_assoc($tokenResult)) {
        $fcm_token = $token_row['token'];
        $fcm_debug["token_found"] = true;
        
        // Send FCM notification
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
                            "slot_id" => strval($slotId),
                            "student_name" => $studentName,
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

    // If everything is successful, commit the transaction
    mysqli_commit($connect);

    // Send success response
    echo json_encode([
        "success" => true,
        "message" => "Booking request sent successfully",
        "debug" => [
            "match_id" => $matchId,
            "slot_id" => $slotId,
            "student_id" => $studentId,
            "fcm_debug" => $fcm_debug
        ]
    ]);

} catch (Exception $e) {
    // If there's an error, rollback the transaction
    if (isset($connect)) {
        mysqli_rollback($connect);
    }
    
    echo json_encode([
        "success" => false,
        "message" => $e->getMessage()
    ]);
} finally {
    if (isset($connect)) {
        mysqli_close($connect);
    }
}
?>
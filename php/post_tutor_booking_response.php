<?php
header("Content-Type: application/json");
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', 'php_error.log');

try {
    // Log received POST data
    error_log("Received POST data: " . print_r($_POST, true));

    // Validate required parameters
    if (!isset($_POST['booking_id']) || !isset($_POST['action'])) {
        throw new Exception("Missing required parameters");
    }

    $bookingId = trim($_POST['booking_id']);
    $action = trim($_POST['action']);

    // Validate action
    if (!in_array($action, ['accept', 'reject'])) {
        throw new Exception("Invalid action parameter");
    }

    error_log("Processing booking response: booking_id={$bookingId}, action={$action}");

    // Database connection
    require_once 'db_config.php';

    // 创建数据库连接
    $connect = getDbConnection();
    if (!$connect) {
        throw new Exception("Database connection failed: " . mysqli_connect_error());
    }

    // Start transaction
    mysqli_begin_transaction($connect);

    try {
        // First, verify the booking exists and is in pending status
        $verifyQuery = "SELECT b.status, b.student_id, b.match_id, b.date, b.start_time, b.end_time, 
                              m.tutor_id,
                              COALESCE(s.username, 'Student') as student_name,
                              COALESCE(t.username, 'Tutor') as tutor_name
                        FROM booking b 
                        JOIN `match` m ON b.match_id = m.match_id
                        LEFT JOIN member s ON b.student_id = s.member_id
                        LEFT JOIN member t ON m.tutor_id = t.member_id
                        WHERE b.booking_id = ? AND b.status = 'pending'";
                        
        $stmt = mysqli_prepare($connect, $verifyQuery);
        if (!$stmt) {
            throw new Exception("Prepare verify query failed: " . mysqli_error($connect));
        }

        mysqli_stmt_bind_param($stmt, "s", $bookingId);
        
        if (!mysqli_stmt_execute($stmt)) {
            throw new Exception("Execute verify query failed: " . mysqli_stmt_error($stmt));
        }

        $result = mysqli_stmt_get_result($stmt);
        if (mysqli_num_rows($result) === 0) {
            throw new Exception("Booking not found or not in pending status");
        }
        
        // Get booking details for notification
        $bookingDetails = mysqli_fetch_assoc($result);
        $studentId = $bookingDetails['student_id'];
        $tutorId = $bookingDetails['tutor_id'];
        $matchId = $bookingDetails['match_id'];
        $studentName = $bookingDetails['student_name'];
        $tutorName = $bookingDetails['tutor_name'];
        
        // Format date and time for notification
        $date = $bookingDetails['date'];
        $startTime = date('H:i', strtotime($bookingDetails['start_time']));
        $endTime = date('H:i', strtotime($bookingDetails['end_time']));
        $timeSlotFormatted = "$date $startTime - $endTime";
        
        mysqli_stmt_close($stmt);

        if ($action === 'accept') {
            // For accept action: set status to confirmed
            $newStatus = 'confirmed';
            $updateQuery = "UPDATE booking 
                           SET status = ?,
                               updated_at = NOW()
                           WHERE booking_id = ?";
            $stmt = mysqli_prepare($connect, $updateQuery);
            mysqli_stmt_bind_param($stmt, "ss", $newStatus, $bookingId);
        } else {
            // For reject action: set status to available and clear student_id
            $newStatus = 'available';
            $updateQuery = "UPDATE booking 
                           SET status = ?,
                               student_id = NULL,
                               updated_at = NOW()
                           WHERE booking_id = ?";
            $stmt = mysqli_prepare($connect, $updateQuery);
            mysqli_stmt_bind_param($stmt, "ss", $newStatus, $bookingId);
        }

        if (!$stmt) {
            throw new Exception("Prepare update query failed: " . mysqli_error($connect));
        }
        
        if (!mysqli_stmt_execute($stmt)) {
            throw new Exception("Execute update query failed: " . mysqli_stmt_error($stmt));
        }

        if (mysqli_affected_rows($connect) === 0) {
            throw new Exception("No booking was updated");
        }
        
        // Prepare notification
        if ($action === 'accept') {
            $notificationTitle = "預約請求已接受";
            $notificationMessage = "導師 $tutorName 已接受您 $timeSlotFormatted 的預約請求。";
            $notificationType = "booking_accepted";
        } else {
            $notificationTitle = "預約請求已拒絕";
            $notificationMessage = "導師 $tutorName 已拒絕您 $timeSlotFormatted 的預約請求，請選擇其他時間段。";
            $notificationType = "booking_rejected";
        }
        
        // Insert notification into database
        $insertNotificationQuery = "INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) 
                                   VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)";
        
        $stmt = mysqli_prepare($connect, $insertNotificationQuery);
        mysqli_stmt_bind_param($stmt, "ssssi", 
            $studentId,
            $notificationTitle,
            $notificationMessage,
            $notificationType,
            $matchId
        );
        
        if (!mysqli_stmt_execute($stmt)) {
            error_log("Failed to insert notification: " . mysqli_error($connect));
        }
        
        // Get student's FCM token
        $tokenQuery = "SELECT token FROM fcm_tokens WHERE member_id = ? ORDER BY created_at DESC LIMIT 1";
        $stmt = mysqli_prepare($connect, $tokenQuery);
        mysqli_stmt_bind_param($stmt, "s", $studentId);
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
                                "booking_id" => strval($bookingId),
                                "status" => $newStatus,
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
            $fcm_debug["error"] = "No FCM token found for student ID: " . $studentId;
            error_log($fcm_debug["error"]);
        }

        // Commit transaction
        mysqli_commit($connect);

        $response = [
            "success" => true,
            "message" => "Booking " . ($action === 'accept' ? "accepted" : "rejected") . " successfully",
            "status" => $newStatus,
            "fcm_debug" => $fcm_debug
        ];

    } catch (Exception $e) {
        mysqli_rollback($connect);
        throw $e;
    }

    error_log("Sending response: " . print_r($response, true));
    echo json_encode($response);

    mysqli_close($connect);

} catch (Exception $e) {
    error_log("Error in post_tutor_booking_response.php: " . $e->getMessage());
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
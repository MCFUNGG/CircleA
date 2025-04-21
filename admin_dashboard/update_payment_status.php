<?php
session_start();

include 'includes/session.php'; // Ensure admin is logged in
include 'includes/db_connect.php';

$message = '';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    if (isset($_POST['payment_id']) && isset($_POST['new_status'])) {
        $payment_id = filter_var($_POST['payment_id'], FILTER_SANITIZE_NUMBER_INT);
        $new_status = $_POST['new_status'];

        // Validate the new status
        $allowed_statuses = ['pending', 'confirmed', 'rejected', 'not_submitted'];
        if ($payment_id && in_array($new_status, $allowed_statuses)) {

            // 獲取付款相關信息以便發送通知
            $payment_details_query = "SELECT p.match_id, p.student_id, p.amount, p.receipt_path, 
                                     m.tutor_id, 
                                     COALESCE(s.username, 'Unknown Student') as student_name,
                                     COALESCE(t.username, 'Unknown Tutor') as tutor_name,
                                     b.date, b.start_time, b.end_time,
                                     b.booking_id
                                     FROM payment p
                                     JOIN `match` m ON p.match_id = m.match_id
                                     LEFT JOIN member s ON p.student_id = s.member_id
                                     LEFT JOIN member t ON m.tutor_id = t.member_id
                                     LEFT JOIN booking b ON (p.match_id = b.match_id AND b.status = 'confirmed')
                                     WHERE p.payment_id = ?";
            
            $stmt_details = $conn->prepare($payment_details_query);
            if (!$stmt_details) {
                $_SESSION['error'] = "Error preparing details statement: " . $conn->error;
                header('location: payment.php');
                exit();
            }
            
            $stmt_details->bind_param("i", $payment_id);
            $stmt_details->execute();
            $result_details = $stmt_details->get_result();
            
            if ($result_details->num_rows == 0) {
                $_SESSION['error'] = "Payment details not found.";
                header('location: payment.php');
                exit();
            }
            
            $payment_details = $result_details->fetch_assoc();
            $stmt_details->close();
            
            // 格式化時間和日期用於通知
            $date = $payment_details['date'];
            $startTime = date('H:i', strtotime($payment_details['start_time']));
            $endTime = date('H:i', strtotime($payment_details['end_time']));
            $timeSlotFormatted = "$date $startTime - $endTime";

            // Determine if verified_at should be updated
            $verified_at_sql = ($new_status == 'confirmed' || $new_status == 'rejected') ? "NOW()" : "NULL";
            if ($new_status == 'pending' || $new_status == 'not_submitted'){
                // When changing back to pending or not_submitted, clear verified_at
                $stmt = $conn->prepare("UPDATE payment SET status = ?, verified_at = NULL WHERE payment_id = ?");
            } else {
                // When changing to confirmed or rejected, set verified_at
                $stmt = $conn->prepare("UPDATE payment SET status = ?, verified_at = NOW() WHERE payment_id = ?");
            }

            if ($stmt) {
                // Adjust bind_param based on which query is used
                if ($new_status == 'pending' || $new_status == 'not_submitted') {
                    $stmt->bind_param("si", $new_status, $payment_id);
                } else {
                    $stmt->bind_param("si", $new_status, $payment_id);
                    // Note: We are binding the same parameters $new_status and $payment_id
                    // The previous logic with $verified_at_sql was incorrect for prepared statements.
                    // The correct way is to use different SQL queries or conditional logic before prepare.
                    // The above separation into two prepare calls is clearer.
                }

                if ($stmt->execute()) {
                    if ($stmt->affected_rows > 0) {
                        $_SESSION['success'] = "Payment status updated successfully.";
                        
                        // 只有在確認或拒絕時發送通知
                        if ($new_status == 'confirmed' || $new_status == 'rejected') {
                            // 準備通知標題和消息
                            $student_id = $payment_details['student_id'];
                            $tutor_id = $payment_details['tutor_id'];
                            $student_name = $payment_details['student_name'];
                            $tutor_name = $payment_details['tutor_name'];
                            $match_id = $payment_details['match_id'];
                            $booking_id = $payment_details['booking_id'];
                            $amount = $payment_details['amount'];
                            
                            // 發送給學生的通知
                            if ($new_status == 'confirmed') {
                                $student_notification_title = "付款已確認";
                                $student_notification_message = "您為 {$timeSlotFormatted} 課程的 ${amount} 付款已確認。您現在可以查看導師聯絡資訊。";
                                $notification_type = "payment_confirmed";
                            } else {
                                $student_notification_title = "付款被拒絕";
                                $student_notification_message = "您為 {$timeSlotFormatted} 課程的 ${amount} 付款被拒絕。請聯繫客服了解更多詳情。";
                                $notification_type = "payment_rejected";
                            }
                            
                            // 發送給導師的通知
                            if ($new_status == 'confirmed') {
                                $tutor_notification_title = "學生付款已確認";
                                $tutor_notification_message = "{$student_name} 為 {$timeSlotFormatted} 課程的 ${amount} 付款已確認。學生現在可以查看您的聯絡資訊。";
                            } else {
                                $tutor_notification_title = "學生付款被拒絕";
                                $tutor_notification_message = "{$student_name} 為 {$timeSlotFormatted} 課程的 ${amount} 付款被拒絕。";
                            }
                            
                            // 發送數據庫通知 - 給學生
                            $insert_student_notification = "INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) 
                                                           VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)";
                            $stmt_student_notification = $conn->prepare($insert_student_notification);
                            $stmt_student_notification->bind_param("ssssi", $student_id, $student_notification_title, $student_notification_message, $notification_type, $match_id);
                            $stmt_student_notification->execute();
                            $stmt_student_notification->close();
                            
                            // 發送數據庫通知 - 給導師
                            $insert_tutor_notification = "INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) 
                                                        VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)";
                            $stmt_tutor_notification = $conn->prepare($insert_tutor_notification);
                            $stmt_tutor_notification->bind_param("ssssi", $tutor_id, $tutor_notification_title, $tutor_notification_message, $notification_type, $match_id);
                            $stmt_tutor_notification->execute();
                            $stmt_tutor_notification->close();
                            
                            // 發送 FCM 通知給學生
                            sendFCMNotification($conn, $student_id, $student_notification_title, $student_notification_message, $notification_type, $match_id, $booking_id, $payment_id);
                            
                            // 發送 FCM 通知給導師
                            sendFCMNotification($conn, $tutor_id, $tutor_notification_title, $tutor_notification_message, $notification_type, $match_id, $booking_id, $payment_id);
                        }
                    } else {
                        $_SESSION['error'] = "Payment status could not be updated (already updated or payment not found).";
                    }
                } else {
                    $_SESSION['error'] = "Error executing update: " . $stmt->error;
                }
                $stmt->close();
            } else {
                $_SESSION['error'] = "Error preparing statement: " . $conn->error;
            }
        } else {
            $_SESSION['error'] = "Invalid status value provided.";
        }
    } else {
        $_SESSION['error'] = "Missing payment ID or new status.";
    }
} else {
    // If the request method is not POST, redirect back
    $_SESSION['error'] = "Invalid request method.";
}

$conn->close();

// 發送 FCM 通知的函數
function sendFCMNotification($conn, $member_id, $title, $message, $notification_type, $match_id, $booking_id, $payment_id) {
    // 獲取會員的 FCM 令牌
    $token_query = "SELECT token FROM fcm_tokens WHERE member_id = ? ORDER BY created_at DESC LIMIT 1";
    $stmt = $conn->prepare($token_query);
    $stmt->bind_param("s", $member_id);
    $stmt->execute();
    $token_result = $stmt->get_result();
    
    if ($token_result && $token_row = $token_result->fetch_assoc()) {
        $fcm_token = $token_row['token'];
        
        // 讀取服務帳戶憑證
        $serviceAccount = json_decode(file_get_contents('D:/xampp/htdocs/firebase-service-account.json'), true);
        if (!$serviceAccount) {
            error_log("錯誤：無法讀取服務帳戶憑證");
            return false;
        }
        
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
                        "type" => $notification_type,
                        "match_id" => strval($match_id),
                        "booking_id" => strval($booking_id),
                        "payment_id" => strval($payment_id),
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
    
    return false;
}

// Redirect back to the payment management page
header('location: payment.php');
exit();
?> 
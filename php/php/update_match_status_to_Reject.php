<?php
// 關閉所有PHP錯誤顯示
error_reporting(E_ALL);
ini_set('display_errors', 1);

header("Content-Type: application/json"); // Set response type
header("Access-Control-Allow-Origin: *");

require_once 'db_config.php';

try {
    // Get database connection
    $conn = getDbConnection();
    
    // Check required parameters
    if (!isset($_POST['match_id']) || !isset($_POST['member_id'])) {
        throw new Exception("Missing required parameters: match_id or member_id");
    }
    
    $match_id = $_POST['match_id'];
    $member_id = strval($_POST['member_id']); // Convert to string to match database type
    
    error_log("Processing match_id: " . $match_id . ", member_id: " . $member_id);
    
    // Start transaction
    mysqli_begin_transaction($conn);
    
    // Get match information and sender's name
    $match_query = "SELECT m.*, COALESCE(mem.username, 'Unknown User') as sender_name 
                   FROM `match` m 
                   LEFT JOIN member mem ON mem.member_id = ? 
                   WHERE m.match_id = ?";
    
    $stmt = mysqli_prepare($conn, $match_query);
    mysqli_stmt_bind_param($stmt, "si", $member_id, $match_id);
    mysqli_stmt_execute($stmt);
    $result = mysqli_stmt_get_result($stmt);
    
    if (!$result || mysqli_num_rows($result) == 0) {
        throw new Exception("Match not found");
    }
    
    $match_data = mysqli_fetch_assoc($result);
    $sender_name = $match_data['sender_name'];
    
    error_log("Match data: " . json_encode($match_data));
    
    // Determine user roles
    $is_tutor = (strcmp($match_data['tutor_id'], $member_id) === 0);
    $notification_recipient_id = $is_tutor ? $match_data['ps_id'] : $match_data['tutor_id'];
    
    error_log("Is tutor: " . ($is_tutor ? "true" : "false"));
    error_log("Notification recipient ID: " . $notification_recipient_id);
    
    // Update match status to 'R' (Rejected)
    $update_query = "UPDATE `match` SET status = 'R' WHERE match_id = ?";
    $stmt = mysqli_prepare($conn, $update_query);
    mysqli_stmt_bind_param($stmt, "i", $match_id);
    
    if (!mysqli_stmt_execute($stmt)) {
        throw new Exception("Failed to update match status: " . mysqli_error($conn));
    }
    
    error_log("Match status updated successfully");
    
    // Insert notification
    $notification_title = "配對狀態更新";
    $notification_message = $is_tutor ? "導師 {$sender_name} 已拒絕您的配對請求" : "學生 {$sender_name} 已拒絕您的配對請求";
    $notification_type = "new_request"; // 使用已知能正常工作的類型
    
    $insert_notification = "INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) 
                          VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)";
    
    $stmt = mysqli_prepare($conn, $insert_notification);
    mysqli_stmt_bind_param($stmt, "ssssi", 
        $notification_recipient_id,
        $notification_title,
        $notification_message,
        $notification_type,
        $match_id
    );
    
    if (!mysqli_stmt_execute($stmt)) {
        throw new Exception("Failed to insert notification: " . mysqli_error($conn));
    }
    
    error_log("Notification inserted successfully");
    
    // Get recipient's FCM token
    $token_query = "SELECT token FROM fcm_tokens WHERE member_id = ? ORDER BY created_at DESC LIMIT 1";
    $stmt = mysqli_prepare($conn, $token_query);
    mysqli_stmt_bind_param($stmt, "s", $notification_recipient_id);
    mysqli_stmt_execute($stmt);
    $token_result = mysqli_stmt_get_result($stmt);
    
    $fcm_debug = [];
    
    if ($token_result && $token_row = mysqli_fetch_assoc($token_result)) {
        $fcm_token = $token_row['token'];
        $fcm_debug["token_found"] = true;
        $fcm_debug["token"] = $fcm_token;
        
        error_log("Found FCM token for recipient: " . $fcm_token);
        
        // 準備 FCM 消息，使用與成功案例相同的格式
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
                            "match_id" => strval($match_id),
                            "sender_name" => $sender_name,
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
        $fcm_debug["error"] = "No FCM token found for recipient ID: " . $notification_recipient_id;
        error_log($fcm_debug["error"]);
    }
    
    // Commit transaction
    mysqli_commit($conn);
    
    // Return success response
    echo json_encode(array(
        "success" => true,
        "message" => "Match status updated successfully",
        "match_id" => $match_id,
        "fcm_debug" => $fcm_debug
    ));
    
} catch (Exception $e) {
    // Rollback transaction on error
    if (isset($conn)) {
        mysqli_rollback($conn);
    }
    
    error_log("Error in update_match_status_to_Reject.php: " . $e->getMessage());
    
    // Return error response
    echo json_encode(array(
        "success" => false,
        "message" => $e->getMessage(),
        "fcm_debug" => isset($fcm_debug) ? $fcm_debug : []
    ));
    
} finally {
    // Close database connection
    if (isset($conn)) {
        mysqli_close($conn);
    }
}
?>  
<?php
header("Content-Type: application/json"); // Set response type
header("Access-Control-Allow-Origin: *");

require_once 'db_config.php';

// 创建数据库连接
$connect = getDbConnection() 
or die(json_encode(["success" => false, "message" => "Unable to connect to the database"]));

// Get POST data
$tutorAppId = trim($_POST["tutor_app_id"]); 
$psAppId = trim($_POST["ps_app_id"]); 
$tutorId = trim($_POST["tutor_id"]); 
$psId = trim($_POST["ps_id"]); 
$matchMark = trim($_POST["match_mark"]); 

// 獲取導師名稱，用於通知內容
$tutor_name = "";
$query_tutor_name = "SELECT m.name FROM member m WHERE m.member_id = '$tutorId'";
$result_tutor_name = mysqli_query($connect, $query_tutor_name);
if ($result_tutor_name && mysqli_num_rows($result_tutor_name) > 0) {
    $row = mysqli_fetch_assoc($result_tutor_name);
    $tutor_name = $row['name'];
}

$query = "INSERT INTO `match` (match_creator, tutor_id, tutor_app_id, ps_id, ps_app_id, match_mark, status) 
          VALUES ('T', '$tutorId', '$tutorAppId', '$psId', '$psAppId', '$matchMark', 'WPS')";
$result = mysqli_query($connect, $query);

if ($result) {
    // 獲取新插入的 match_id
    $match_id = mysqli_insert_id($connect);
    
    // 發送 FCM 通知給學生
    sendNotificationToStudent($psId, $tutor_name, $match_id, $connect);
    
    echo json_encode([
        "success" => true, 
        "message" => "Application submitted successfully!",
        "match_id" => $match_id
    ]);
} else {
    echo json_encode([
        "success" => false, 
        "message" => "Failed to submit application: " . mysqli_error($connect)
    ]);
}

// 發送通知給學生的函數
function sendNotificationToStudent($psId, $tutorName, $matchId, $connect) {
    // 檢查 FCM 令牌是否存在
    $token_query = "SELECT token FROM fcm_tokens WHERE member_id = '$psId'";
    $token_result = mysqli_query($connect, $token_query);
    
    if ($token_result && mysqli_num_rows($token_result) > 0) {
        $row = mysqli_fetch_assoc($token_result);
        $token = $row['token'];
        
        // 保存通知到數據庫
        $title = "新導師請求";
        $message = $tutorName . " 向您發送了導師請求";
        $type = "tutor_request";
        
        $notification_query = "INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) 
                              VALUES ('$psId', '$title', '$message', '$type', '$matchId', 0, NOW())";
        mysqli_query($connect, $notification_query);
        
        // 檢查 FCM 伺服器密鑰是否存在
        $server_key = 'BMZclDQt4JC1CCgjthueu0KwDADekway6R_PAVZZ2kC6Mj00x1E8537vx4P4J1L9D88AKGh_1yXa2HeDTaWbmXM';
        
        // 如果伺服器密鑰未設置，僅記錄通知但不發送
        if (empty($server_key)) {
            error_log("FCM server key not set. Notification saved to database only.");
            return;
        }
        
        // 準備 FCM 消息
        $url = 'https://fcm.googleapis.com/fcm/send';
        
        $fields = [
            'to' => $token,
            'notification' => [
                'title' => $title,
                'body' => $message,
                'sound' => 'default',
                'badge' => '1'
            ],
            'data' => [
                'type' => $type,
                'match_id' => $matchId
            ]
        ];
        
        $headers = [
            'Authorization: key=' . $server_key,
            'Content-Type: application/json'
        ];
        
        // 發送請求到 FCM
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
        
        $result = curl_exec($ch);
        
        if ($result === FALSE) {
            error_log('FCM Send Error: ' . curl_error($ch));
        } else {
            error_log('FCM notification sent: ' . $result);
        }
        
        curl_close($ch);
    } else {
        error_log("FCM token not found for student ID: $psId");
    }
}

// Close database connection
mysqli_close($connect);
?>
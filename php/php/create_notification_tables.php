<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");

require_once 'db_config.php';

// 创建数据库连接
$connect = getDbConnection()
or die(json_encode(["success" => false, "message" => "Unable to connect to the database"]));

// 檢查 fcm_tokens 表是否存在
$check_fcm_table = "SHOW TABLES LIKE 'fcm_tokens'";
$result_fcm = mysqli_query($connect, $check_fcm_table);

if (mysqli_num_rows($result_fcm) == 0) {
    // 創建 fcm_tokens 表
    $create_fcm_table = "CREATE TABLE fcm_tokens (
        id INT(11) NOT NULL AUTO_INCREMENT,
        member_id INT(11) NOT NULL,
        token VARCHAR(255) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (id),
        UNIQUE KEY (member_id)
    )";
    
    if (mysqli_query($connect, $create_fcm_table)) {
        echo json_encode(["success" => true, "message" => "fcm_tokens table created successfully"]);
    } else {
        echo json_encode(["success" => false, "message" => "Error creating fcm_tokens table: " . mysqli_error($connect)]);
        exit;
    }
} else {
    echo json_encode(["success" => true, "message" => "fcm_tokens table already exists"]);
}

// 檢查 notifications 表是否存在
$check_notifications_table = "SHOW TABLES LIKE 'notifications'";
$result_notifications = mysqli_query($connect, $check_notifications_table);

if (mysqli_num_rows($result_notifications) == 0) {
    // 創建 notifications 表
    $create_notifications_table = "CREATE TABLE notifications (
        id INT(11) NOT NULL AUTO_INCREMENT,
        member_id INT(11) NOT NULL,
        title VARCHAR(255) NOT NULL,
        message TEXT NOT NULL,
        type VARCHAR(50) DEFAULT 'general',
        related_id VARCHAR(50) DEFAULT NULL,
        is_read TINYINT(1) DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (id)
    )";
    
    if (mysqli_query($connect, $create_notifications_table)) {
        echo json_encode(["success" => true, "message" => "notifications table created successfully"]);
    } else {
        echo json_encode(["success" => false, "message" => "Error creating notifications table: " . mysqli_error($connect)]);
        exit;
    }
} else {
    echo json_encode(["success" => true, "message" => "notifications table already exists"]);
}

// 添加用於保存 FCM token 的 API
$create_save_token_file = true;
$save_token_file = "save_fcm_token.php";

if (!file_exists($save_token_file) && $create_save_token_file) {
    $save_token_content = '<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

require_once "db_config.php";

// 獲取參數
$member_id = isset($_POST["member_id"]) ? $_POST["member_id"] : "";
$fcm_token = isset($_POST["fcm_token"]) ? $_POST["fcm_token"] : "";

// 驗證參數
if (empty($member_id) || empty($fcm_token)) {
    echo json_encode(array(
        "success" => false,
        "message" => "Missing required parameters"
    ));
    exit;
}

// 創建數據庫連接
$connect = getDbConnection()
or die(json_encode(["success" => false, "message" => "Unable to connect to the database"]));

// 插入或更新 FCM token
$sql = "INSERT INTO fcm_tokens (member_id, token) VALUES (?, ?) 
        ON DUPLICATE KEY UPDATE token = ?, updated_at = CURRENT_TIMESTAMP";

$stmt = mysqli_prepare($connect, $sql);
mysqli_stmt_bind_param($stmt, "iss", $member_id, $fcm_token, $fcm_token);

if (mysqli_stmt_execute($stmt)) {
    echo json_encode(array(
        "success" => true,
        "message" => "FCM token saved successfully"
    ));
} else {
    echo json_encode(array(
        "success" => false,
        "message" => "Error saving FCM token: " . mysqli_error($connect)
    ));
}

mysqli_stmt_close($stmt);
mysqli_close($connect);
?>';

    if (file_put_contents($save_token_file, $save_token_content)) {
        echo json_encode(["success" => true, "message" => "save_fcm_token.php created successfully"]);
    } else {
        echo json_encode(["success" => false, "message" => "Error creating save_fcm_token.php"]);
    }
}

// 添加用於發送通知的通用 API
$create_send_notification_file = true;
$send_notification_file = "send_notification.php";

if (!file_exists($send_notification_file) && $create_send_notification_file) {
    $send_notification_content = '<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

require_once "db_config.php";

// 獲取 Firebase 伺服器金鑰
$server_key = "YOUR_FIREBASE_SERVER_KEY"; // 請替換為您的 Firebase 伺服器金鑰

/**
 * 發送 FCM 通知
 */
function sendFCM($token, $title, $body, $data = array()) {
    global $server_key;
    
    $url = "https://fcm.googleapis.com/fcm/send";
    
    $fields = array(
        "to" => $token,
        "notification" => array(
            "title" => $title,
            "body" => $body,
            "sound" => "default",
            "badge" => "1"
        ),
        "data" => $data
    );
    
    $headers = array(
        "Authorization: key=" . $server_key,
        "Content-Type: application/json"
    );
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
    
    $result = curl_exec($ch);
    
    if ($result === FALSE) {
        return array(
            "success" => false,
            "message" => "FCM Send Error: " . curl_error($ch)
        );
    }
    
    curl_close($ch);
    
    return array(
        "success" => true,
        "result" => json_decode($result, true)
    );
}

/**
 * 發送通知給特定會員
 */
function sendNotificationToMember($member_id, $title, $body, $data = array()) {
    // 創建數據庫連接
    $connect = getDbConnection()
    or die(json_encode(["success" => false, "message" => "Unable to connect to the database"]));
    
    // 獲取會員的 FCM token
    $sql = "SELECT token FROM fcm_tokens WHERE member_id = ?";
    $stmt = mysqli_prepare($connect, $sql);
    mysqli_stmt_bind_param($stmt, "i", $member_id);
    mysqli_stmt_execute($stmt);
    $result = mysqli_stmt_get_result($stmt);
    
    if (mysqli_num_rows($result) > 0) {
        $row = mysqli_fetch_assoc($result);
        $token = $row["token"];
        
        // 記錄通知到數據庫
        $type = isset($data["type"]) ? $data["type"] : "general";
        $related_id = isset($data["related_id"]) ? $data["related_id"] : null;
        
        $notification_sql = "INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) 
                             VALUES (?, ?, ?, ?, ?, 0, NOW())";
        $notification_stmt = mysqli_prepare($connect, $notification_sql);
        mysqli_stmt_bind_param($notification_stmt, "issss", $member_id, $title, $body, $type, $related_id);
        mysqli_stmt_execute($notification_stmt);
        
        // 發送通知
        return sendFCM($token, $title, $body, $data);
    } else {
        return array(
            "success" => false,
            "message" => "Member FCM token not found"
        );
    }
    
    mysqli_close($connect);
}

// API 入口點
if ($_SERVER["REQUEST_METHOD"] === "POST") {
    // 獲取參數
    $member_id = isset($_POST["member_id"]) ? $_POST["member_id"] : "";
    $title = isset($_POST["title"]) ? $_POST["title"] : "";
    $body = isset($_POST["body"]) ? $_POST["body"] : "";
    $type = isset($_POST["type"]) ? $_POST["type"] : "general";
    $related_id = isset($_POST["related_id"]) ? $_POST["related_id"] : "";
    
    // 驗證參數
    if (empty($member_id) || empty($title) || empty($body)) {
        echo json_encode(array(
            "success" => false,
            "message" => "Missing required parameters"
        ));
        exit;
    }
    
    // 附加數據
    $data = array(
        "type" => $type,
        "related_id" => $related_id
    );
    
    // 發送通知
    $result = sendNotificationToMember($member_id, $title, $body, $data);
    
    echo json_encode($result);
} else {
    echo json_encode(array(
        "success" => false,
        "message" => "Invalid request method"
    ));
}
?>';

    if (file_put_contents($send_notification_file, $send_notification_content)) {
        echo json_encode(["success" => true, "message" => "send_notification.php created successfully"]);
    } else {
        echo json_encode(["success" => false, "message" => "Error creating send_notification.php"]);
    }
}

mysqli_close($connect);
echo json_encode(["success" => true, "message" => "Setup completed"]);
?> 
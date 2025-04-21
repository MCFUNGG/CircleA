<?php
error_reporting(0);
ini_set('display_errors', 0);

header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");

require_once 'db_config.php';

// 创建数据库连接
$connect = getDbConnection();
if (!$connect) {
    echo json_encode(["success" => false, "message" => "Unable to connect to the database"]);
    exit;
}

// 獲取POST數據
$member_id = isset($_POST["member_id"]) ? trim($_POST["member_id"]) : '';
$token = isset($_POST["token"]) ? trim($_POST["token"]) : '';

if(empty($member_id) || empty($token)) {
    echo json_encode([
        "success" => false, 
        "message" => "Missing required parameters"
    ]);
    exit;
}

// 檢查是否已存在此用戶的令牌
$check_query = "SELECT id FROM fcm_tokens WHERE member_id = '$member_id'";
$check_result = mysqli_query($connect, $check_query);

if($check_result && mysqli_num_rows($check_result) > 0) {
    // 更新現有令牌
    $update_query = "UPDATE fcm_tokens SET token = '$token', updated_at = NOW() WHERE member_id = '$member_id'";
    $result = mysqli_query($connect, $update_query);
} else {
    // 插入新令牌
    $insert_query = "INSERT INTO fcm_tokens (member_id, token, created_at) VALUES ('$member_id', '$token', NOW())";
    $result = mysqli_query($connect, $insert_query);
}

if($result) {
    echo json_encode([
        "success" => true,
        "message" => "FCM token saved successfully"
    ]);
} else {
    echo json_encode([
        "success" => false,
        "message" => "Failed to save FCM token: " . mysqli_error($connect)
    ]);
}

mysqli_close($connect);
?> 
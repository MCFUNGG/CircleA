<?php
header("Content-Type: application/json");

require_once 'db_config.php';

// 创建数据库连接
$connect = getDbConnection();

if (!$connect) {
    echo json_encode(["success" => false, "message" => "Database connection failed: " . mysqli_connect_error()]);
    exit;
}

// 获取member_id参数
$memberId = isset($_POST['member_id']) ? $_POST['member_id'] : null;

if ($memberId === null) {
    echo json_encode(["success" => false, "message" => "Member ID not provided"]);
    exit;
}

// 查询用户的最新头像
$profileQuery = "SELECT profile FROM member_detail WHERE member_id = '$memberId' ORDER BY version DESC LIMIT 1";
$profileResult = mysqli_query($connect, $profileQuery);

if (!$profileResult) {
    echo json_encode(["success" => false, "message" => "Database query failed: " . mysqli_error($connect)]);
    exit;
}

if (mysqli_num_rows($profileResult) > 0) {
    $profileRow = mysqli_fetch_assoc($profileResult);
    $profilePath = trim($profileRow['profile']);
    
    // 记录原始路径以便调试
    error_log("Member ID: $memberId - Original profile path: $profilePath");
    
    // 获取用户名
    $usernameQuery = "SELECT username FROM member WHERE member_id = '$memberId'";
    $usernameResult = mysqli_query($connect, $usernameQuery);
    $username = "Unknown";
    if ($usernameRow = mysqli_fetch_assoc($usernameResult)) {
        $username = $usernameRow['username'];
    }
    
    echo json_encode([
        "success" => true,
        "data" => [
            "member_id" => $memberId,
            "username" => $username,
            "profile_icon" => $profilePath
        ]
    ]);
} else {
    echo json_encode(["success" => false, "message" => "No profile found for member ID: $memberId"]);
}

mysqli_close($connect);
?> 
<?php
header("Content-Type: application/json");
error_reporting(E_ALL);
ini_set('display_errors', 1);
ini_set('log_errors', 1);
ini_set('error_log', 'php_error.log');

require_once 'db_config.php';

// 创建数据库连接
$connect = getDbConnection();

if (!$connect) {
    echo json_encode([
        "success" => false, 
        "message" => "Database connection failed: " . mysqli_connect_error()
    ]);
    exit;
}

// 获取match_id参数
$matchId = isset($_POST['match_id']) ? $_POST['match_id'] : null;

if ($matchId === null) {
    echo json_encode([
        "success" => false, 
        "message" => "Match ID not provided"
    ]);
    exit;
}

// 查询match表获取tutor_app_id
$query = "SELECT tutor_app_id FROM `match` WHERE match_id = ?";
$stmt = $connect->prepare($query);

if (!$stmt) {
    echo json_encode([
        "success" => false, 
        "message" => "Query preparation failed: " . mysqli_error($connect)
    ]);
    exit;
}

$stmt->bind_param("s", $matchId);
if (!$stmt->execute()) {
    echo json_encode([
        "success" => false, 
        "message" => "Query execution failed: " . $stmt->error
    ]);
    exit;
}

$result = $stmt->get_result();
if ($result->num_rows === 0) {
    echo json_encode([
        "success" => false, 
        "message" => "Match not found"
    ]);
    exit;
}

$matchRow = $result->fetch_assoc();
$tutorAppId = $matchRow['tutor_app_id'];
$stmt->close();

// 返回tutor_app_id
echo json_encode([
    "success" => true,
    "data" => [
        [
            "app_id" => $tutorAppId
        ]
    ]
]);

// 关闭数据库连接
mysqli_close($connect);
?>
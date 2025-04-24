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

// Get parameters from POST
$match_id = isset($_POST['match_id']) ? $_POST['match_id'] : null;
$student_id = isset($_POST['student_id']) ? $_POST['student_id'] : null;
$booking_id = isset($_POST['booking_id']) ? $_POST['booking_id'] : null;

// 记录调用和参数
error_log("get_first_lesson_status.php called with: match_id=$match_id, student_id=$student_id, booking_id=$booking_id");

if ($match_id === null || $student_id === null) {
    echo json_encode([
        "success" => false, 
        "message" => "Match ID and Student ID are required"
    ]);
    exit;
}

// 如果没有提供booking_id，根据match_id和student_id查找当前有效的confirmed booking
if ($booking_id === null) {
    $findBookingQuery = "SELECT booking_id FROM booking 
                         WHERE match_id = ? AND student_id = ? AND status = 'confirmed'
                         ORDER BY created_at DESC LIMIT 1";
    $findStmt = $connect->prepare($findBookingQuery);
    if (!$findStmt) {
        echo json_encode([
            "success" => false, 
            "message" => "Query preparation failed: " . mysqli_error($connect)
        ]);
        exit;
    }
    
    $findStmt->bind_param("ss", $match_id, $student_id);
    $findStmt->execute();
    $findResult = $findStmt->get_result();
    
    if ($findResult->num_rows === 0) {
        echo json_encode([
            "success" => false, 
            "message" => "No confirmed booking found for this match and student"
        ]);
        exit;
    }
    
    $bookingRow = $findResult->fetch_assoc();
    $booking_id = $bookingRow['booking_id'];
    $findStmt->close();
    
    error_log("Found booking_id: $booking_id for match_id: $match_id and student_id: $student_id");
}

// Query to check lesson status
$query = "SELECT fl.booking_id, fl.ps_response as student_status, fl.t_response as tutor_status 
          FROM first_lesson fl
          JOIN booking b ON fl.booking_id = b.booking_id
          WHERE fl.booking_id = ? AND b.status = 'confirmed'";

$stmt = $connect->prepare($query);
if (!$stmt) {
    echo json_encode([
        "success" => false, 
        "message" => "Query preparation failed: " . mysqli_error($connect)
    ]);
    exit;
}

$stmt->bind_param("i", $booking_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    // 如果未找到first_lesson记录，则表示状态尚未设置
    echo json_encode([
        "success" => true,
        "data" => [
            "booking_id" => $booking_id,
            "status" => "pending",
            "student_status" => null,
            "tutor_status" => null,
            "both_completed" => "false",
            "feedback_submitted" => "false"
        ]
    ]);
    exit;
}

$row = $result->fetch_assoc();

// 检查两方是否均标记为已完成
$bothCompleted = ($row['student_status'] === 'completed' && $row['tutor_status'] === 'completed') ? "true" : "false";

// 确定整体状态
$status = "pending";
if ($row['student_status'] === 'completed' && $row['tutor_status'] === 'completed') {
    $status = "completed";
} else if ($row['student_status'] === 'incomplete' && $row['tutor_status'] === 'incomplete') {
    $status = "incomplete";
} else if ($row['student_status'] && $row['tutor_status'] && $row['student_status'] !== $row['tutor_status']) {
    $status = "conflict";
} else if ($row['student_status'] && !$row['tutor_status']) {
    $status = "waiting_tutor";
} else if (!$row['student_status'] && $row['tutor_status']) {
    $status = "waiting_student";
}

// 检查是否已提交反馈
$feedbackQuery = "SELECT rate_id FROM tutor_rating 
                 WHERE application_id = ? AND role = 'parent' AND member_id = ?";
$feedbackStmt = $connect->prepare($feedbackQuery);
$feedbackStmt->bind_param("is", $booking_id, $student_id);
$feedbackStmt->execute();
$feedbackResult = $feedbackStmt->get_result();
$feedbackSubmitted = $feedbackResult->num_rows > 0 ? "true" : "false";
$feedbackStmt->close();

echo json_encode([
    "success" => true,
    "data" => [
        "booking_id" => $row['booking_id'],
        "status" => $status,
        "student_status" => $row['student_status'],
        "tutor_status" => $row['tutor_status'],
        "both_completed" => $bothCompleted,
        "feedback_submitted" => $feedbackSubmitted
    ]
]);

// Close connections
$stmt->close();
mysqli_close($connect);
?>
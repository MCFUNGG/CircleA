<?php
// 設置允許跨域請求
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

// 包含數據庫配置文件
require_once "db_config.php";

// 檢查是否是POST請求
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    // 獲取POST數據
    $match_id = isset($_POST["match_id"]) ? $_POST["match_id"] : "";
    $student_id = isset($_POST["student_id"]) ? $_POST["student_id"] : "";
    $tutor_id = isset($_POST["tutor_id"]) ? $_POST["tutor_id"] : "";
    $check_all_statuses = isset($_POST["check_all_statuses"]) ? $_POST["check_all_statuses"] : "false";
    $check_is_new_match = isset($_POST["check_is_new_match"]) ? $_POST["check_is_new_match"] : "false";
    
    // 驗證必要參數
    if (empty($match_id)) {
        http_response_code(400);
        echo json_encode(array("success" => false, "message" => "Missing required parameter: match_id"));
        exit;
    }
    
    // 確保至少有學生ID或教師ID
    if (empty($student_id) && empty($tutor_id)) {
        http_response_code(400);
        echo json_encode(array("success" => false, "message" => "Missing required parameter: student_id or tutor_id"));
        exit;
    }
    
    // 創建數據庫連接
    $conn = new mysqli($servername, $username, $password, $dbname);
    
    // 檢查連接是否成功
    if ($conn->connect_error) {
        http_response_code(500);
        echo json_encode(array("success" => false, "message" => "Connection failed: " . $conn->connect_error));
        exit;
    }
    
    // 如果要檢查是否為新匹配
    if ($check_is_new_match === "true") {
        // 檢查此match_id是否有任何預約記錄
        $booking_check_query = "SELECT COUNT(*) AS booking_count FROM booking WHERE match_id = ?";
        $stmt = $conn->prepare($booking_check_query);
        $stmt->bind_param("s", $match_id);
        $stmt->execute();
        $result = $stmt->get_result();
        $row = $result->fetch_assoc();
        $booking_count = $row["booking_count"];
        
        // 如果沒有預約記錄，視為新匹配
        if ($booking_count == 0) {
            // 查看此match_id是否有可用時間段
            $slot_check_query = "SELECT COUNT(*) AS slot_count FROM time_slots WHERE match_id = ? AND status = 'available'";
            $stmt = $conn->prepare($slot_check_query);
            $stmt->bind_param("s", $match_id);
            $stmt->execute();
            $result = $stmt->get_result();
            $row = $result->fetch_assoc();
            $available_slot_count = $row["slot_count"];
            
            // 如果有可用時間段，則確認為新匹配
            if ($available_slot_count > 0) {
                echo json_encode(array(
                    "success" => true,
                    "is_new_match" => true,
                    "has_booking" => false,
                    "available_slot_count" => $available_slot_count,
                    "message" => "This is a new match with available time slots. Please select one."
                ));
                $stmt->close();
                $conn->close();
                exit;
            }
        }
    }
    
    // 準備查詢語句 - 獲取最新的預約或特定類型的預約
    $sql = "";
    $params = array();
    
    if (!empty($student_id)) {
        // 學生端查詢
        $sql = "SELECT b.*, 
                       m2.username as student_name,
                       (SELECT username FROM member WHERE member_id = m.tutor_id) as tutor_name
                FROM booking b
                JOIN `match` m ON b.match_id = m.match_id
                JOIN member m2 ON b.student_id = m2.member_id
                WHERE b.match_id = ? AND b.student_id = ?
                ORDER BY 
                    CASE 
                        WHEN b.status = 'confirmed' THEN 1
                        WHEN b.status = 'conflict' THEN 2
                        WHEN b.status = 'completed' THEN 3
                        WHEN b.status = 'pending' THEN 4
                        ELSE 5
                    END, 
                    b.created_at DESC
                LIMIT 1";
        $params = array($match_id, $student_id);
    } else if (!empty($tutor_id)) {
        // 教師端查詢 - 移除時間段表的關聯，直接使用booking表中的日期數據
        $sql = "SELECT b.*, 
                       m2.username as student_name,
                       (SELECT username FROM member WHERE member_id = m.tutor_id) as tutor_name
                FROM booking b
                JOIN `match` m ON b.match_id = m.match_id
                JOIN member m2 ON b.student_id = m2.member_id
                WHERE b.match_id = ? AND m.tutor_id = ?
                ORDER BY 
                    CASE 
                        WHEN b.status = 'confirmed' THEN 1
                        WHEN b.status = 'conflict' THEN 2
                        WHEN b.status = 'completed' THEN 3
                        WHEN b.status = 'pending' THEN 4
                        ELSE 5
                    END, 
                    b.created_at DESC
                LIMIT 1";
        $params = array($match_id, $tutor_id);
    }
    
    if (!empty($sql)) {
        $stmt = $conn->prepare($sql);
        
        if (count($params) == 2) {
            $stmt->bind_param("ss", $params[0], $params[1]);
        }
        
        $stmt->execute();
        $result = $stmt->get_result();
        
        if ($result->num_rows > 0) {
            $booking = $result->fetch_assoc();
            
            // 記錄有預約並返回詳情
            echo json_encode(array(
                "success" => true,
                "has_booking" => true,
                "booking" => array(
                    "booking_id" => $booking["booking_id"],
                    "match_id" => $booking["match_id"],
                    "student_id" => $booking["student_id"],
                    "student_name" => $booking["student_name"],
                    "tutor_name" => isset($booking["tutor_name"]) ? $booking["tutor_name"] : "",
                    "start_time" => $booking["start_time"],
                    "end_time" => $booking["end_time"],
                    "status" => $booking["status"],
                    "created_at" => $booking["created_at"],
                    "updated_at" => $booking["updated_at"]
                )
            ));
        } else {
            // 沒有找到預約
            echo json_encode(array(
                "success" => true,
                "has_booking" => false,
                "message" => "No bookings found for this match"
            ));
        }
    } else {
        // 沒有有效的查詢
        http_response_code(400);
        echo json_encode(array("success" => false, "message" => "Invalid query parameters"));
    }
    
    // 關閉數據庫連接
    if (isset($stmt)) {
        $stmt->close();
    }
    $conn->close();
} else {
    // 非POST請求
    http_response_code(405);
    echo json_encode(array("success" => false, "message" => "Method not allowed"));
}
?> 
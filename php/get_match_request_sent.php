<?php
header("Content-Type: application/json");

require_once 'db_config.php';

// 创建数据库连接
$connect = getDbConnection();

if (!$connect) {
    echo json_encode(["success" => false, "message" => "Database connection failed: " . mysqli_connect_error()]);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $memberId = isset($_POST['member_id']) ? $_POST['member_id'] : null;

    if ($memberId === null) {
        echo json_encode(["success" => false, "message" => "Member ID not provided"]);
        exit;
    }

    // Query for sent requests (both PS and Tutor roles) with status WT or WPS
        $stmt1 = $connect->prepare("(SELECT 
            m.*, 
            ps.username as ps_username,
            ps.member_id as ps_id,
            t.username as tutor_username,
            t.member_id as tutor_id,
            'PS' as sender_role
        FROM `match` m
        JOIN member ps ON m.ps_id = ps.member_id
        JOIN member t ON m.tutor_id = t.member_id
        WHERE m.ps_id = ? 
        AND m.status = 'WT'
        AND m.match_creator = 'PS')
        UNION
        (SELECT 
            m.*, 
            ps.username as ps_username,
            ps.member_id as ps_id,
            t.username as tutor_username,
            t.member_id as tutor_id,
            'T' as sender_role
        FROM `match` m
        JOIN member ps ON m.ps_id = ps.member_id
        JOIN member t ON m.tutor_id = t.member_id
        WHERE m.tutor_id = ? 
        AND m.status = 'WPS'
        AND m.match_creator = 'T')");

    $stmt1->bind_param("ss", $memberId, $memberId);
    $stmt1->execute();
    $result1 = $stmt1->get_result();

    if (!$result1) {
        echo json_encode(["success" => false, "message" => "Match query failed: " . mysqli_error($connect)]);
        exit;
    }

    $matchData = [];
    while ($row = $result1->fetch_assoc()) {
        // Get profile icon of the recipient
        $recipientId = ($row['sender_role'] == 'PS') ? $row['tutor_id'] : $row['ps_id'];
        
        // 获取接收者的最新个人资料图标
        $stmt2 = $connect->prepare("SELECT profile 
            FROM member_detail 
            WHERE member_id = ? 
            AND version = (
                SELECT MAX(version) 
                FROM member_detail 
                WHERE member_id = ?
            )");
        $stmt2->bind_param("ss", $recipientId, $recipientId);
        $stmt2->execute();
        $profileResult = $stmt2->get_result();
        
        $profilePath = '';
        if ($profileResult && $profileResult->num_rows > 0) {
            $profileRow = $profileResult->fetch_assoc();
            $profilePath = trim($profileRow['profile']);
            
            // 简化路径处理逻辑，使用统一的方式处理路径
            if (!empty($profilePath)) {
                // 如果路径包含 D:/xampp/htdocs
                if (strpos($profilePath, 'D:/xampp/htdocs') !== false) {
                    $profilePath = str_replace('D:/xampp/htdocs', '', $profilePath);
                }
                // 如果路径以 ./FYP 开头
                else if (strpos($profilePath, './FYP') === 0) {
                    $profilePath = str_replace('./FYP', '/FYP', $profilePath);
                }
                // 如果路径不包含 /FYP/ 但包含 images/
                else if (strpos($profilePath, '/FYP/') === false && strpos($profilePath, 'images/') !== false) {
                    $profilePath = '/FYP/' . $profilePath;
                }
                // 确保路径开头有/
                if (substr($profilePath, 0, 1) !== '/') {
                    $profilePath = '/' . $profilePath;
                }
            }
            
            $row['profile_icon'] = $profilePath;
        } else {
            $row['profile_icon'] = '';
        }
        
        // 确定请求的目标应用程序ID（接收者的应用程序）
        $targetAppId = '';
        if ($row['sender_role'] == 'PS') {
            // 如果发送者是PS，目标应用是tutor_app_id
            $targetAppId = $row['tutor_app_id'];
            $row['recipient_username'] = $row['tutor_username'];
            $row['recipient_app_id'] = $targetAppId;
        } else {
            // 如果发送者是Tutor，目标应用是ps_app_id
            $targetAppId = $row['ps_app_id'];
            $row['recipient_username'] = $row['ps_username'];
            $row['recipient_app_id'] = $targetAppId;
        }
        
        // 查询目标应用的详细信息
        $stmt3 = $connect->prepare("SELECT * FROM application WHERE app_id = ?");
        $stmt3->bind_param("s", $targetAppId);
        $stmt3->execute();
        $appResult = $stmt3->get_result();
        
        if ($appResult && $appResult->num_rows > 0) {
            $appData = $appResult->fetch_assoc();
            
            // 获取班级级别名称
            $stmt4 = $connect->prepare("SELECT class_level_name FROM class_level WHERE class_level_id = ?");
            $stmt4->bind_param("s", $appData['class_level_id']);
            $stmt4->execute();
            $classLevelResult = $stmt4->get_result();
            $classLevelName = ($classLevelResult && $classLevelResult->num_rows > 0) 
                ? $classLevelResult->fetch_assoc()['class_level_name'] 
                : 'N/A';
            
            // 获取科目
            $subjectNames = [];
            $stmt5 = $connect->prepare("SELECT s.subject_name 
                FROM application_subject as_rel
                JOIN subject s ON as_rel.subject_id = s.subject_id
                WHERE as_rel.app_id = ?");
            $stmt5->bind_param("s", $targetAppId);
            $stmt5->execute();
            $subjectResult = $stmt5->get_result();
            while ($subjectRow = $subjectResult->fetch_assoc()) {
                $subjectNames[] = $subjectRow['subject_name'];
            }
            
            // 获取地区
            $districtNames = [];
            $stmt6 = $connect->prepare("SELECT d.district_name 
                FROM application_district ad
                JOIN district d ON ad.district_id = d.district_id
                WHERE ad.app_id = ?");
            $stmt6->bind_param("s", $targetAppId);
            $stmt6->execute();
            $districtResult = $stmt6->get_result();
            while ($districtRow = $districtResult->fetch_assoc()) {
                $districtNames[] = $districtRow['district_name'];
            }
            
            // 组合所有应用程序详细信息
            $row['application_details'] = [
                'app_id' => $appData['app_id'],
                'class_level_name' => $classLevelName,
                'subject_names' => $subjectNames,
                'district_names' => $districtNames,
                'feePerHr' => $appData['feePerHr'],
                'description' => $appData['description'],
                'is_recipient_app' => true  // 明确标记这是接收者的应用
            ];
            
            $matchData[] = $row;
            
            // 关闭语句
            $stmt4->close();
            $stmt5->close();
            $stmt6->close();
        }
        
        $stmt2->close();
        $stmt3->close();
    }

    if (count($matchData) > 0) {
        echo json_encode([
            "success" => true,
            "data" => $matchData
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "No sent requests found"
        ]);
    }

    $stmt1->close();
} else {
    echo json_encode([
        "success" => false,
        "message" => "Invalid request method"
    ]);
}

mysqli_close($connect);
?>
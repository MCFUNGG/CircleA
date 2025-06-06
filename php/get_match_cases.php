<?php
header("Content-Type: application/json");

require_once 'db_config.php';

// 创建数据库连接
$connect = getDbConnection();

if (!$connect) {
    echo json_encode(["success" => false, "message" => "Database connection failed: " . mysqli_connect_error()]);
    exit;
}

// Get member_id from POST request
$memberId = isset($_POST['member_id']) ? $_POST['member_id'] : null;

if ($memberId === null) {
    echo json_encode(["success" => false, "message" => "Member ID not provided"]);
    exit;
}

// Query for cases with status P or A
$stmt1 = $connect->prepare("SELECT 
    m.match_id, 
    m.tutor_id,
    m.tutor_app_id, 
    m.ps_id, 
    m.ps_app_id, 
    m.match_mark,
    m.status,
    m.match_creator,
    ps.username as ps_username,
    t.username as tutor_username
FROM `match` m
JOIN member ps ON m.ps_id = ps.member_id
JOIN member t ON m.tutor_id = t.member_id
WHERE (m.tutor_id = ? OR m.ps_id = ?)
AND m.status IN ('P', 'A')");

$stmt1->bind_param("ss", $memberId, $memberId);
$stmt1->execute();
$result1 = $stmt1->get_result();

if (!$result1) {
    echo json_encode(["success" => false, "message" => "Match query failed: " . mysqli_error($connect)]);
    exit;
}

$matchData = [];
while ($row = $result1->fetch_assoc()) {
    // Get profile icon of the PS
    $stmt2 = $connect->prepare("SELECT profile 
        FROM member_detail 
        WHERE member_id = ? 
        AND version = (
            SELECT MAX(version) 
            FROM member_detail 
            WHERE member_id = ?
        )");
    $stmt2->bind_param("ss", $row['ps_id'], $row['ps_id']);
    $stmt2->execute();
    $profileResult = $stmt2->get_result();
    
    $ps_profile_path = '';
    if ($profileResult && $profileResult->num_rows > 0) {
        $profileRow = $profileResult->fetch_assoc();
        $ps_profile_path = trim($profileRow['profile']);
        
        // 简化路径处理逻辑，使用统一的方式处理路径
        if (!empty($ps_profile_path)) {
            // 如果路径包含 D:/xampp/htdocs
            if (strpos($ps_profile_path, 'D:/xampp/htdocs') !== false) {
                $ps_profile_path = str_replace('D:/xampp/htdocs', '', $ps_profile_path);
            }
            // 如果路径以 ./FYP 开头
            else if (strpos($ps_profile_path, './FYP') === 0) {
                $ps_profile_path = str_replace('./FYP', '/FYP', $ps_profile_path);
            }
            // 如果路径不包含 /FYP/ 但包含 images/
            else if (strpos($ps_profile_path, '/FYP/') === false && strpos($ps_profile_path, 'images/') !== false) {
                $ps_profile_path = '/FYP/' . $ps_profile_path;
            }
            // 确保路径开头有/
            if (substr($ps_profile_path, 0, 1) !== '/') {
                $ps_profile_path = '/' . $ps_profile_path;
            }
        }
    }
    $row['ps_profile_icon'] = $ps_profile_path;

    // Get profile icon of the Tutor
    $stmt3 = $connect->prepare("SELECT profile 
        FROM member_detail 
        WHERE member_id = ? 
        AND version = (
            SELECT MAX(version) 
            FROM member_detail 
            WHERE member_id = ?
        )");
    $stmt3->bind_param("ss", $row['tutor_id'], $row['tutor_id']);
    $stmt3->execute();
    $tutorProfileResult = $stmt3->get_result();
    
    $tutor_profile_path = '';
    if ($tutorProfileResult && $tutorProfileResult->num_rows > 0) {
        $tutorProfileRow = $tutorProfileResult->fetch_assoc();
        $tutor_profile_path = trim($tutorProfileRow['profile']);
        
        // 简化路径处理逻辑，使用统一的方式处理路径
        if (!empty($tutor_profile_path)) {
            // 如果路径包含 D:/xampp/htdocs
            if (strpos($tutor_profile_path, 'D:/xampp/htdocs') !== false) {
                $tutor_profile_path = str_replace('D:/xampp/htdocs', '', $tutor_profile_path);
            }
            // 如果路径以 ./FYP 开头
            else if (strpos($tutor_profile_path, './FYP') === 0) {
                $tutor_profile_path = str_replace('./FYP', '/FYP', $tutor_profile_path);
            }
            // 如果路径不包含 /FYP/ 但包含 images/
            else if (strpos($tutor_profile_path, '/FYP/') === false && strpos($tutor_profile_path, 'images/') !== false) {
                $tutor_profile_path = '/FYP/' . $tutor_profile_path;
            }
            // 确保路径开头有/
            if (substr($tutor_profile_path, 0, 1) !== '/') {
                $tutor_profile_path = '/' . $tutor_profile_path;
            }
        }
    }
    $row['tutor_profile_icon'] = $tutor_profile_path;

    // Get application details based on creator
    $appId = ($row['match_creator'] == 'PS') ? $row['ps_app_id'] : $row['tutor_app_id'];
    
    $stmt4 = $connect->prepare("SELECT * FROM application WHERE app_id = ?");
    $stmt4->bind_param("s", $appId);
    $stmt4->execute();
    $appResult = $stmt4->get_result();
    
    if ($appResult && $appResult->num_rows > 0) {
        $appData = $appResult->fetch_assoc();
        
        // Get class level name
        $stmt5 = $connect->prepare("SELECT class_level_name FROM class_level WHERE class_level_id = ?");
        $stmt5->bind_param("s", $appData['class_level_id']);
        $stmt5->execute();
        $classLevelResult = $stmt5->get_result();
        $classLevelName = ($classLevelResult && $classLevelResult->num_rows > 0) 
            ? $classLevelResult->fetch_assoc()['class_level_name'] 
            : 'N/A';
        
        // Get subjects
        $subjectNames = [];
        $stmt6 = $connect->prepare("SELECT s.subject_name 
            FROM application_subject as_rel
            JOIN subject s ON as_rel.subject_id = s.subject_id
            WHERE as_rel.app_id = ?");
        $stmt6->bind_param("s", $appId);
        $stmt6->execute();
        $subjectResult = $stmt6->get_result();
        while ($subjectRow = $subjectResult->fetch_assoc()) {
            $subjectNames[] = $subjectRow['subject_name'];
        }
        
        // Get districts
        $districtNames = [];
        $stmt7 = $connect->prepare("SELECT d.district_name 
            FROM application_district ad
            JOIN district d ON ad.district_id = d.district_id
            WHERE ad.app_id = ?");
        $stmt7->bind_param("s", $appId);
        $stmt7->execute();
        $districtResult = $stmt7->get_result();
        while ($districtRow = $districtResult->fetch_assoc()) {
            $districtNames[] = $districtRow['district_name'];
        }
        
        // Combine all application details
        $row['application_details'] = [
            'app_id' => $appData['app_id'],
            'class_level_name' => $classLevelName,
            'subject_names' => $subjectNames,
            'district_names' => $districtNames,
            'feePerHr' => $appData['feePerHr'],
            'description' => $appData['description']
        ];
        
        $matchData[] = $row;
        
        // Close statements
        $stmt5->close();
        $stmt6->close();
        $stmt7->close();
    }
    
    $stmt2->close();
    $stmt3->close();
    $stmt4->close();
}

if (count($matchData) > 0) {
    echo json_encode([
        "success" => true,
        "data" => $matchData
    ]);
} else {
    echo json_encode([
        "success" => false,
        "message" => "No cases found"
    ]);
}

$stmt1->close();
mysqli_close($connect);
?>
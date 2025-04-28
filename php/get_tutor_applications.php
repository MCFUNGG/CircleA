<?php
header("Content-Type: application/json");

require_once 'db_config.php';

// 创建数据库连接
$connect = getDbConnection();

if (!$connect) {
    echo json_encode(["success" => false, "message" => "Database connection failed: " . mysqli_connect_error()]);
    exit;
}

// Main query to get tutor applications with all necessary information
$query = "
    SELECT 
        a.app_id,
        m.member_id,
        m.username,
        cl.class_level_name,
        a.feePerHr,
        a.status,
        GROUP_CONCAT(DISTINCT s.subject_name) as subjects,
        GROUP_CONCAT(DISTINCT d.district_name) as districts,
        COALESCE(MAX(tr.rate_score), 0) as rating
    FROM application a
    JOIN member m ON a.member_id = m.member_id
    JOIN class_level cl ON a.class_level_id = cl.class_level_id
    LEFT JOIN application_subject as_table ON a.app_id = as_table.app_id
    LEFT JOIN subject s ON as_table.subject_id = s.subject_id
    LEFT JOIN application_district ad ON a.app_id = ad.app_id
    LEFT JOIN district d ON ad.district_id = d.district_id
    LEFT JOIN tutor_rating tr ON a.app_id = tr.application_id AND tr.role = 'tutor'
    WHERE a.app_creator = 'T'
    GROUP BY a.app_id, m.member_id, m.username, cl.class_level_name, a.feePerHr, a.status
    HAVING COALESCE(MAX(tr.rate_score), 0) >= 1";
    

$result = mysqli_query($connect, $query);

if (!$result) {
    echo json_encode([
        "success" => false, 
        "message" => "Query failed: " . mysqli_error($connect)
    ]);
    exit;
}

if (mysqli_num_rows($result) > 0) {
    $applicationData = [];
    
    while ($row = mysqli_fetch_assoc($result)) {
        // Convert comma-separated strings to arrays
        $subjects = $row['subjects'] ? explode(',', $row['subjects']) : [];
        $districts = $row['districts'] ? explode(',', $row['districts']) : [];
        
        // 获取成员最新版本的头像
        $memberId = $row['member_id'];
        $profileQuery = "SELECT profile FROM member_detail 
                        WHERE member_id = '$memberId' 
                        ORDER BY version DESC LIMIT 1";
        $profileResult = mysqli_query($connect, $profileQuery);
        $profilePath = '';
        
        if ($profileResult && mysqli_num_rows($profileResult) > 0) {
            $profileRow = mysqli_fetch_assoc($profileResult);
            $profilePath = trim($profileRow['profile']);
            
            // 处理文件路径，只保留URL可访问的部分
            if (strpos($profilePath, 'D:/xampp/htdocs') === 0) {
                // 处理绝对路径格式 D:/xampp/htdocs/FYP/images/file.jpg
                $profilePath = str_replace('D:/xampp/htdocs', '', $profilePath);
                error_log("Path processed (xampp format): $profilePath");
            } 
            else if (preg_match('/^D:\//', $profilePath)) {
                // 处理其他D:开头的路径
                $parts = explode('/', $profilePath);
                // 尝试找到FYP在路径中的位置
                $fypIndex = array_search('FYP', $parts);
                if ($fypIndex !== false) {
                    // 只保留从FYP开始的部分
                    $profilePath = '/' . implode('/', array_slice($parts, $fypIndex));
                    error_log("Path processed (D: drive format): $profilePath");
                }
            }
            else if (preg_match('/^\.\//', $profilePath)) {
                // 处理相对路径 ./FYP/images/file.jpg
                $profilePath = substr($profilePath, 1); // 移除开头的点
                error_log("Path processed (relative format): $profilePath");
            }
            else if (strpos($profilePath, 'images/icon_') !== false) {
                // 如果包含images/icon_，确保前面有/FYP/
                if (strpos($profilePath, '/FYP/') === false) {
                    $profilePath = '/FYP/' . $profilePath;
                    error_log("Added /FYP/ prefix: $profilePath");
                }
            }
            
            error_log("Final profile path for member $memberId: $profilePath");
        }
        
        $applicationData[] = [
            'app_id' => $row['app_id'],
            'member_id' => $row['member_id'],
            'username' => $row['username'],
            'class_level_name' => $row['class_level_name'],
            'feePerHr' => $row['feePerHr'],
            'status' => $row['status'],
            'subjects' => $subjects,
            'districts' => $districts,
            'rating' => number_format($row['rating'], 1),
            'profile' => $profilePath
        ];
    }

    echo json_encode([
        "success" => true,
        "data" => $applicationData
    ]);
} else {
    echo json_encode([
        "success" => false, 
        "message" => "No tutor applications found"
    ]);
}

mysqli_close($connect);
?> 
<?php
header("Content-Type: application/json");
require_once 'db_config.php';

// 创建数据库连接
$connect = getDbConnection();

if (!$connect) {
    echo json_encode(["success" => false, "message" => "Database connection failed: " . mysqli_connect_error()]);
    exit;
}

$memberId = $_POST['member_id'] ?? null;

// Get all PS applications, excluding the current user's applications, only active ones, and not completed
$query = "SELECT app_id, member_id, class_level_id, feePerHr FROM application 
          WHERE app_creator = 'PS' 
          AND member_id != '$memberId' 
          AND status='A'
          AND app_id NOT IN (
              SELECT m.ps_app_id
              FROM `match` m
              JOIN booking b ON m.match_id = b.match_id
              WHERE b.status = 'completed'
          )";
$result = mysqli_query($connect, $query);

if (!$result) {
    echo json_encode(["success" => false, "message" => "Query failed: " . mysqli_error($connect)]);
    exit;
}

if (mysqli_num_rows($result) > 0) {
    $applicationData = [];
    
    while ($row = mysqli_fetch_assoc($result)) {
        $applicationData[] = $row;
    }

    foreach ($applicationData as &$application) {
        // Get username
        $memberId = $application['member_id'];
        $usernameQuery = "SELECT username FROM member WHERE member_id = '$memberId'";
        $usernameResult = mysqli_query($connect, $usernameQuery);
        if ($usernameRow = mysqli_fetch_assoc($usernameResult)) {
            $application['username'] = $usernameRow['username'];
        } else {
            $application['username'] = 'N/A';
        }

        // Get class level name
        $classLevelId = $application['class_level_id'];
        $classLevelQuery = "SELECT class_level_name FROM class_level WHERE class_level_id = '$classLevelId'";
        $classLevelResult = mysqli_query($connect, $classLevelQuery);
        if ($classRow = mysqli_fetch_assoc($classLevelResult)) {
            $application['class_level_name'] = $classRow['class_level_name'];
        } else {
            $application['class_level_name'] = 'N/A';
        }

        // Get subjects
        $application['subject_names'] = [];
        $subjectQuery = "SELECT subject_id FROM application_subject WHERE app_id = '{$application['app_id']}'";
        $subjectResult = mysqli_query($connect, $subjectQuery);
        while ($subjectRow = mysqli_fetch_assoc($subjectResult)) {
            $subjectId = $subjectRow['subject_id'];
            $subjectNameQuery = "SELECT subject_name FROM subject WHERE subject_id = '$subjectId'";
            $subjectNameResult = mysqli_query($connect, $subjectNameQuery);
            if ($subjectNameRow = mysqli_fetch_assoc($subjectNameResult)) {
                $application['subject_names'][] = $subjectNameRow['subject_name'];
            }
        }

        // Get districts
        $application['district_names'] = [];
        $districtQuery = "SELECT district_id FROM application_district WHERE app_id = '{$application['app_id']}'";
        $districtResult = mysqli_query($connect, $districtQuery);
        while ($districtRow = mysqli_fetch_assoc($districtResult)) {
            $districtId = $districtRow['district_id'];
            $districtNameQuery = "SELECT district_name FROM district WHERE district_id = '$districtId'";
            $districtNameResult = mysqli_query($connect, $districtNameQuery);
            if ($districtNameRow = mysqli_fetch_assoc($districtNameResult)) {
                $application['district_names'][] = $districtNameRow['district_name'];
            }
        }

        // Get profile icon
        $profileQuery = "SELECT profile FROM member_detail WHERE member_id = '$memberId' ORDER BY version DESC LIMIT 1";
        $profileResult = mysqli_query($connect, $profileQuery);
        if ($profileRow = mysqli_fetch_assoc($profileResult)) {
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
            
            $application['profile_icon'] = $profilePath;
            error_log("Final profile path for member $memberId: " . $application['profile_icon']);
        } else {
            $application['profile_icon'] = '';
        }
    }

    // Format final output
    $finalData = array_map(function($application) {
        return [
            'app_id' => $application['app_id'],
            'member_id' => $application['member_id'],
            'username' => $application['username'],
            'class_level_name' => $application['class_level_name'],
            'subject_names' => $application['subject_names'],
            'district_names' => $application['district_names'],
            'feePerHr' => $application['feePerHr'],
            'profile_icon' => $application['profile_icon']
        ];
    }, $applicationData);

    echo json_encode([
        "success" => true,
        "data" => $finalData
    ]);
} else {
    echo json_encode([
        "success" => false, 
        "message" => "No student applications found"
    ]);
}

mysqli_close($connect);
?>
<?php
header("Content-Type: application/json");

require_once 'db_config.php';

// 创建数据库连接
$connect = getDbConnection();

if (!$connect) {
    echo json_encode(["success" => false, "message" => "Database connection failed: " . mysqli_connect_error()]);
    exit;
}

// 获取match_id
$matchId = $_POST['match_id'];

if ($matchId === null) {
    echo json_encode(["success" => false, "message" => "Match ID not provided"]);
    exit;
}

// 第一步：获取match信息，包括ps_app_id和tutor_app_id
$query1 = mysqli_query($connect, "SELECT ps_app_id, tutor_app_id, ps_member_id, tutor_member_id FROM `match` WHERE match_id = '$matchId'");
$row = mysqli_fetch_assoc($query1);
if (!$row) {
    echo json_encode(["success" => false, "message" => "Match not found"]);
    exit;
}

// 获取匹配信息
$psAppId = $row['ps_app_id'];
$tutorAppId = $row['tutor_app_id'];
$psMemberId = $row['ps_member_id'];
$tutorMemberId = $row['tutor_member_id'];

// 获取当前请求用户的ID（从请求参数中获取）
$currentUserId = isset($_POST['member_id']) ? $_POST['member_id'] : null;

// 确定目标应用ID
$targetAppId = null;
$targetMemberId = null;

// 如果当前用户是导师，则目标是学生
if ($currentUserId == $tutorMemberId) {
    $targetAppId = $psAppId;
    $targetMemberId = $psMemberId;
    $appCreator = 'PS'; // 学生申请
} 
// 如果当前用户是学生，则目标是导师
else if ($currentUserId == $psMemberId) {
    $targetAppId = $tutorAppId;
    $targetMemberId = $tutorMemberId;
    $appCreator = 'T'; // 导师申请
}
// 如果未提供用户ID或无法确定角色，默认获取学生申请
else {
    $targetAppId = $psAppId;
    $targetMemberId = $psMemberId;
    $appCreator = 'PS';
}

// 第二步：获取目标应用详情
$query2 = "SELECT app_id, member_id, class_level_id, feePerHr 
           FROM application 
           WHERE app_id = '$targetAppId'";
$result = mysqli_query($connect, $query2);

if (!$result) {
    echo json_encode(["success" => false, "message" => "Database query failed"]);
    exit;
}   

if (mysqli_num_rows($result) > 0) {
    $applicationData = [];
    
    while ($row = mysqli_fetch_assoc($result)) {
        $applicationData[] = $row;
    }

    foreach ($applicationData as &$application) {
        // 获取用户名
        $memberId = $application['member_id'];
        $usernameQuery = "SELECT username FROM member WHERE member_id = '$memberId'";
        $usernameResult = mysqli_query($connect, $usernameQuery);
        if ($usernameRow = mysqli_fetch_assoc($usernameResult)) {
            $application['username'] = $usernameRow['username'];
        } else {
            $application['username'] = 'N/A';
        }

        // 获取年级名称
        $classLevelId = $application['class_level_id'];
        $classLevelQuery = "SELECT class_level_name FROM class_level WHERE class_level_id = '$classLevelId'";
        $classLevelResult = mysqli_query($connect, $classLevelQuery);
        if ($classRow = mysqli_fetch_assoc($classLevelResult)) {
            $application['class_level_name'] = $classRow['class_level_name'];
        } else {
            $application['class_level_name'] = 'N/A';
        }

        // 获取科目
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

        // 获取地区
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

        // 获取头像
        $profileQuery = "SELECT profile FROM member_detail WHERE member_id = '$memberId' ORDER BY version DESC LIMIT 1";
        $profileResult = mysqli_query($connect, $profileQuery);
        if ($profileRow = mysqli_fetch_assoc($profileResult)) {
            // 添加调试信息
            error_log("Raw profile path for member_id $memberId: " . $profileRow['profile']);
            
            // 处理文件路径，只保留URL可访问的部分
            $profilePath = trim($profileRow['profile']);
            
            // 检查并处理多种可能的路径格式
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
            error_log("Final profile path: " . $application['profile_icon']);
        } else {
            error_log("No profile found for member_id $memberId");
            $application['profile_icon'] = '';
        }
    }

    // 格式化最终输出
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
        "message" => "No application found"
    ]);
}

mysqli_close($connect);
?> 
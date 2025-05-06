<?php
// check_tutor_video.php - 检查导师视频记录并返回给学生UI

// 设置响应头
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST');
header('Access-Control-Allow-Headers: Content-Type');

// 包含数据库配置
include_once 'db_config.php';

// 获取match_id参数
$match_id = isset($_POST['match_id']) ? $_POST['match_id'] : '';

// 记录请求信息
error_log("Received request to check_tutor_video with match_id: " . $match_id);

if (empty($match_id)) {
    echo json_encode([
        'success' => false,
        'message' => 'match_id is required'
    ]);
    exit;
}

// 创建数据库连接
$conn = getDbConnection();
if (!$conn) {
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed'
    ]);
    exit;
}

// 查询数据库检查视频记录
$sql = "SELECT video_file, video_mark, video_datetime, video_summary, video_analysis 
        FROM `match` 
        WHERE match_id = ? 
        AND (video_file IS NOT NULL OR video_mark IS NOT NULL OR video_datetime IS NOT NULL)";

$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $match_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    // 有视频记录，返回数据
    $video_data = $result->fetch_assoc();
    
    error_log("Video data found for match_id: " . $match_id);
    
    echo json_encode([
        'success' => true,
        'has_video' => true,
        'video_data' => [
            'video_mark' => $video_data['video_mark'],
            'video_datetime' => $video_data['video_datetime'],
            'video_summary' => $video_data['video_summary'],
            'video_analysis' => $video_data['video_analysis'],
            'video_file' => $video_data['video_file']
        ]
    ]);
} else {
    // 没有视频记录
    error_log("No video data found for match_id: " . $match_id);
    
    echo json_encode([
        'success' => true,
        'has_video' => false,
        'message' => 'No video record found for this match_id'
    ]);
}

// 关闭数据库连接
$stmt->close();
$conn->close();
?> 
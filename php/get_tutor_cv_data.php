<?php
// 設置響應為JSON格式
header('Content-Type: application/json');

// 引入數據庫連接文件
require_once 'db_config.php';

// 記錄請求
$request_data = [
    'method' => $_SERVER['REQUEST_METHOD'],
    'post_data' => $_POST,
    'time' => date('Y-m-d H:i:s')
];
error_log("Tutor CV data request: " . json_encode($request_data));

// 檢查是否有POST請求
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // 獲取會員ID
    $member_id = isset($_POST['member_id']) ? $_POST['member_id'] : null;

    if (!$member_id) {
        echo json_encode(['success' => false, 'message' => 'Member ID is required']);
        error_log("Error: Member ID is required");
        exit;
    }

    try {
        // 建立PDO連接
        $pdo = new PDO("mysql:host=" . DB_HOST . ";dbname=" . DB_NAME, DB_USER, DB_PASSWORD);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

        // 準備SQL查詢來獲取導師CV數據
        $sql = "SELECT cv.*, m.username, m.member_id, md.profile_description 
                FROM member_cv cv
                LEFT JOIN member m ON cv.member_id = m.member_id
                LEFT JOIN member_detail md ON cv.member_id = md.member_id
                WHERE cv.member_id = :member_id";

        $stmt = $pdo->prepare($sql);
        $stmt->bindParam(':member_id', $member_id, PDO::PARAM_STR);
        $stmt->execute();

        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        error_log("CV Query for member_id=$member_id: " . ($result ? "Found" : "Not found"));

        if ($result) {
            // 確保用戶名不為空
            if (empty($result['username'])) {
                // 嘗試直接從會員表獲取用戶名
                $nameSql = "SELECT username FROM member WHERE member_id = :member_id";
                $nameStmt = $pdo->prepare($nameSql);
                $nameStmt->bindParam(':member_id', $member_id, PDO::PARAM_STR);
                $nameStmt->execute();
                $nameResult = $nameStmt->fetch(PDO::FETCH_ASSOC);
                
                if ($nameResult && !empty($nameResult['username'])) {
                    $result['username'] = $nameResult['username'];
                    error_log("Updated username from member table: " . $result['username']);
                }
            }
            
            // 確保所有必要欄位存在
            $required_fields = ['education', 'skills', 'language', 'contact', 'other'];
            foreach ($required_fields as $field) {
                if (!isset($result[$field]) || $result[$field] === null) {
                    $result[$field] = '';
                    error_log("Field $field was missing, set to empty string");
                }
            }
            
            // 記錄返回的數據
            error_log("Returning CV data: " . json_encode([
                'success' => true,
                'username' => $result['username'] ?? 'Not available',
                'education' => $result['education'] ?? 'Not available',
                'skills' => $result['skills'] ?? '',
                'language' => $result['language'] ?? '',
                'contact' => $result['contact'] ?? '',
                'other' => $result['other'] ?? ''
            ]));
            
            echo json_encode([
                'success' => true,
                'data' => $result
            ]);
        } else {
            // 如果沒有CV數據，嘗試只獲取基本會員資料
            $memberSql = "SELECT m.username, m.member_id, md.profile_description 
                          FROM member m
                          LEFT JOIN member_detail md ON m.member_id = md.member_id
                          WHERE m.member_id = :member_id";
            
            $memberStmt = $pdo->prepare($memberSql);
            $memberStmt->bindParam(':member_id', $member_id, PDO::PARAM_STR);
            $memberStmt->execute();
            
            $memberResult = $memberStmt->fetch(PDO::FETCH_ASSOC);
            error_log("Member Query: " . ($memberResult ? "Found" : "Not found"));
            
            if ($memberResult) {
                // 返回基本會員資料，但沒有CV數據
                $memberResult['education'] = 'No education background information';
                $memberResult['skills'] = '';
                $memberResult['language'] = '';
                $memberResult['contact'] = '';
                $memberResult['other'] = '';
                
                // 記錄返回的會員數據
                error_log("Returning member data: " . json_encode([
                    'success' => true,
                    'username' => $memberResult['username'] ?? 'Not available',
                    'education' => $memberResult['education'] ?? 'Not available',
                    'skills' => $memberResult['skills'] ?? '',
                    'language' => $memberResult['language'] ?? '',
                    'contact' => $memberResult['contact'] ?? '',
                    'other' => $memberResult['other'] ?? ''
                ]));
                
                echo json_encode([
                    'success' => true,
                    'data' => $memberResult
                ]);
            } else {
                error_log("Error: No tutor data found for member_id=$member_id");
                echo json_encode([
                    'success' => false,
                    'message' => 'No tutor data found for this member'
                ]);
            }
        }
    } catch (PDOException $e) {
        error_log("Database error: " . $e->getMessage());
        echo json_encode([
            'success' => false,
            'message' => 'Database error: ' . $e->getMessage()
        ]);
    }
} else {
    error_log("Invalid request method: " . $_SERVER['REQUEST_METHOD']);
    echo json_encode([
        'success' => false,
        'message' => 'Invalid request method'
    ]);
}
?> 
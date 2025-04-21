<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Access-Control-Allow-Headers, Content-Type, Access-Control-Allow-Methods, Authorization, X-Requested-With');

include_once 'config.php';

// Get raw posted data
$data = json_decode(file_get_contents("php://input"), true);

if (!isset($data['member_id'])) {
    echo json_encode([
        'success' => false,
        'message' => 'Member ID is required'
    ]);
    exit();
}

$member_id = $data['member_id'];

try {
    $stmt = $conn->prepare("SELECT * FROM notifications WHERE member_id = ? ORDER BY created_at DESC");
    $stmt->bind_param("i", $member_id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $notifications = [];
    while ($row = $result->fetch_assoc()) {
        $notifications[] = [
            'id' => (int)$row['id'],
            'member_id' => (int)$row['member_id'],
            'title' => $row['title'],
            'message' => $row['message'],
            'type' => $row['type'],
            'related_id' => (int)$row['related_id'],
            'is_read' => (int)$row['is_read'],
            'created_at' => $row['created_at']
        ];
    }
    
    echo json_encode([
        'success' => true,
        'notifications' => $notifications
    ]);
    
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
}

$stmt->close();
$conn->close();
?> 
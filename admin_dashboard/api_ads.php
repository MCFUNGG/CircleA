<?php
// Database connection
$servername = "localhost";
$username = "root";
$password = ""; 
$dbname = "system001";

header('Content-Type: application/json');

// Connect to database
$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
  echo json_encode(['success' => false, 'message' => 'Connection failed: ' . $conn->connect_error]);
  exit();
}

// Get active advertisements ordered by sort_order and creation date
$sql = "SELECT ad_id, title, description, image_url, link_url, sort_order FROM ads 
        WHERE status = 'active' 
        ORDER BY sort_order ASC, created_at DESC";
$result = $conn->query($sql);

$ads = [];
if ($result->num_rows > 0) {
  while ($row = $result->fetch_assoc()) {
    // Convert relative image path to absolute URL
    $server_url = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . $_SERVER['HTTP_HOST'];
    $image_path = $row['image_url'];
    $row['image_url'] = $server_url . '/' . $image_path;
    
    $ads[] = $row;
  }
  echo json_encode(['success' => true, 'data' => $ads]);
} else {
  echo json_encode(['success' => true, 'data' => []]);
}

$conn->close();
?>

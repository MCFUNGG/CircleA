<?php
header("Content-Type: application/json");

$host = "127.0.0.1";
$username = "root";
$password = "";
$dbname = "system001";

$connect = mysqli_connect($host, $username, $password, $dbname);

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
        md.profile,
        a.feePerHr,
        a.status,
        GROUP_CONCAT(DISTINCT s.subject_name) as subjects,
        GROUP_CONCAT(DISTINCT d.district_name) as districts,
        COALESCE(MAX(tr.rate_score), 0) as rating
    FROM application a
    JOIN member m ON a.member_id = m.member_id
    LEFT JOIN member_detail md ON m.member_id = md.member_id
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
            'profile' => $row['profile']
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
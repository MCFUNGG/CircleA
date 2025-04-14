<?php
header("Content-Type: application/json");

require_once 'db_config.php';

// Create database connection
$connect = getDbConnection();

if (!$connect) {
    echo json_encode(["success" => false, "message" => "Database connection failed: " . mysqli_connect_error()]);
    exit;
}

// Get POST data
$appId = $_POST['app_id'] ?? null;
$memberId = $_POST['member_id'] ?? null;
$classLevelName = $_POST['class_level_name'] ?? null;
$subjectsJson = $_POST['subjects'] ?? null;
$districtsJson = $_POST['districts'] ?? null;
$fee = $_POST['fee'] ?? null;
$description = $_POST['description'] ?? null;
$appCreator = $_POST['app_creator'] ?? null;
$status = $_POST['status'] ?? null;

// Validate required fields
if (!$appId || !$memberId || !$classLevelName || !$subjectsJson || !$districtsJson || !$fee || !$appCreator) {
    echo json_encode(["success" => false, "message" => "Missing required fields"]);
    exit;
}

// Decode JSON arrays
try {
    $subjects = json_decode($subjectsJson);
    $districts = json_decode($districtsJson);
    
    if ($subjects === null || $districts === null) {
        throw new Exception("Invalid JSON format");
    }
} catch (Exception $e) {
    echo json_encode(["success" => false, "message" => "Error parsing JSON data: " . $e->getMessage()]);
    exit;
}

// Start transaction
mysqli_autocommit($connect, false);
$success = true;

try {
    // 1. Get the class_level_id from class_level_name
    $classLevelQuery = "SELECT class_level_id FROM class_level WHERE class_level_name = ?";
    $stmt = mysqli_prepare($connect, $classLevelQuery);
    mysqli_stmt_bind_param($stmt, "s", $classLevelName);
    mysqli_stmt_execute($stmt);
    $classLevelResult = mysqli_stmt_get_result($stmt);
    
    if (mysqli_num_rows($classLevelResult) == 0) {
        throw new Exception("Invalid class level");
    }
    
    $classLevelRow = mysqli_fetch_assoc($classLevelResult);
    $classLevelId = $classLevelRow['class_level_id'];
    
    // 2. Update the application table
    $updateAppQuery = "UPDATE application SET 
                      class_level_id = ?,
                      feePerHr = ?,
                      description = ?,
                      status = ?
                      WHERE app_id = ? AND member_id = ?";
    $stmt = mysqli_prepare($connect, $updateAppQuery);
    mysqli_stmt_bind_param($stmt, "issssi", $classLevelId, $fee, $description, $status, $appId, $memberId);
    
    if (!mysqli_stmt_execute($stmt)) {
        throw new Exception("Failed to update application: " . mysqli_error($connect));
    }
    
    // 3. Delete existing subject associations
    $deleteSubjectsQuery = "DELETE FROM application_subject WHERE app_id = ?";
    $stmt = mysqli_prepare($connect, $deleteSubjectsQuery);
    mysqli_stmt_bind_param($stmt, "s", $appId);
    
    if (!mysqli_stmt_execute($stmt)) {
        throw new Exception("Failed to remove old subjects: " . mysqli_error($connect));
    }
    
    // 4. Insert new subject associations
    $insertSubjectQuery = "INSERT INTO application_subject (app_id, subject_id) 
                          VALUES (?, (SELECT subject_id FROM subject WHERE subject_name = ?))";
    $stmt = mysqli_prepare($connect, $insertSubjectQuery);
    
    foreach ($subjects as $subject) {
        mysqli_stmt_bind_param($stmt, "ss", $appId, $subject);
        if (!mysqli_stmt_execute($stmt)) {
            throw new Exception("Failed to add subject '$subject': " . mysqli_error($connect));
        }
    }
    
    // 5. Delete existing district associations
    $deleteDistrictsQuery = "DELETE FROM application_district WHERE app_id = ?";
    $stmt = mysqli_prepare($connect, $deleteDistrictsQuery);
    mysqli_stmt_bind_param($stmt, "s", $appId);
    
    if (!mysqli_stmt_execute($stmt)) {
        throw new Exception("Failed to remove old districts: " . mysqli_error($connect));
    }
    
    // 6. Insert new district associations
    $insertDistrictQuery = "INSERT INTO application_district (app_id, district_id) 
                           VALUES (?, (SELECT district_id FROM district WHERE district_name = ?))";
    $stmt = mysqli_prepare($connect, $insertDistrictQuery);
    
    foreach ($districts as $district) {
        mysqli_stmt_bind_param($stmt, "ss", $appId, $district);
        if (!mysqli_stmt_execute($stmt)) {
            throw new Exception("Failed to add district '$district': " . mysqli_error($connect));
        }
    }
    
    // Commit transaction
    mysqli_commit($connect);
    
} catch (Exception $e) {
    // Rollback transaction on error
    mysqli_rollback($connect);
    $success = false;
    echo json_encode(["success" => false, "message" => $e->getMessage()]);
    exit;
} finally {
    // Restore autocommit mode
    mysqli_autocommit($connect, true);
}

// Return success response
if ($success) {
    echo json_encode([
        "success" => true, 
        "message" => "Application updated successfully",
        "app_id" => $appId
    ]);
} else {
    echo json_encode([
        "success" => false, 
        "message" => "Failed to update application"
    ]);
}

mysqli_close($connect);
?> 
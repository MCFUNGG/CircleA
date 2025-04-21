<?php
// Code Comment: Upload CV data to cv_data table and certificate files
ini_set('display_errors', 1);
error_reporting(E_ALL);

// Database connection parameters
require_once 'db_config.php';

// 创建数据库连接
$connect = getDbConnection();
$connect->set_charset("utf8mb4");
if ($connect->connect_error) {
    die(json_encode(array(
        'status' => 'error',
        'message' => "Connection failed: " . $connect->connect_error
    )));
}

$connect->begin_transaction();
try {
    error_log("【Debug】Received POST: " . print_r($_POST, true));
    error_log("【Debug】Received FILES: " . print_r($_FILES, true));
    
    if ($_SERVER['REQUEST_METHOD'] == 'POST' && isset($_POST['memberId'])) {
        $memberId    = $_POST['memberId'];
        $description = isset($_POST['description']) ? $_POST['description'] : '';
        $mId = intval($memberId);
        
        // For CV section
        $cvPath = '';
        $contact   = isset($_POST['contact'])   ? $_POST['contact']   : '';
        $skills    = isset($_POST['skills'])    ? $_POST['skills']    : '';
        $education = isset($_POST['education']) ? $_POST['education'] : '';
        $language  = isset($_POST['language'])  ? $_POST['language']  : '';
        $other     = isset($_POST['other'])     ? $_POST['other']     : '';
        
        // Process file uploads if they exist
        if (isset($_FILES['file'])) {
            // Ensure FILES are treated as array for multi-files
            if (!is_array($_FILES['file']['name'])) {
                $_FILES['file']['name'] = array($_FILES['file']['name']);
                $_FILES['file']['tmp_name'] = array($_FILES['file']['tmp_name']);
                $_FILES['file']['error'] = array($_FILES['file']['error']);
                $_FILES['file']['size'] = array($_FILES['file']['size']);
                $_FILES['file']['type'] = array($_FILES['file']['type']);
            }
            
            $certUploadDir = $_SERVER['DOCUMENT_ROOT'] . '/FYP/upload_cert/';
            if (!file_exists($certUploadDir)) {
                if (!mkdir($certUploadDir, 0777, true)) {
                    throw new Exception("Unable to create certificate upload directory");
                }
            }
            
            $newFileNames = array();
            
            // Process each uploaded file
            for ($i = 0; $i < count($_FILES['file']['name']); $i++) {
                if ($_FILES['file']['error'][$i] === UPLOAD_ERR_OK) {
                    $fileName = "CERT_" . $mId . "_" . time() . "_" . $i . ".jpg";
                    $targetPath = $certUploadDir . $fileName;
                    
                    if (move_uploaded_file($_FILES['file']['tmp_name'][$i], $targetPath)) {
                        $newFileNames[] = $fileName;
                        $cvPath = 'uploads/' . $fileName; // Use the first uploaded file as CV path
                    } else {
                        throw new Exception("Failed to move uploaded file #" . ($i + 1));
                    }
                } else {
                    throw new Exception("Error in uploaded file #" . ($i + 1) . ": " . $_FILES['file']['error'][$i]);
                }
            }
        }
        
        // Insert data into cv_data table
        $sql = "INSERT INTO cv_data (member_id, contact, skills, education, language, other, cv_path) 
                VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        $stmt = $connect->prepare($sql);
        if ($stmt === false) {
            throw new Exception("Preparing SQL statement failed: " . $connect->error);
        }
        
        $stmt->bind_param("issssss", 
            $mId, 
            $contact, 
            $skills, 
            $education, 
            $language, 
            $other, 
            $cvPath
        );
        
        if (!$stmt->execute()) {
            throw new Exception("Executing CV data save failed: " . $stmt->error);
        }
        
        $cvId = $stmt->insert_id;
        error_log("【Debug】CV data inserted with ID: " . $cvId);
        $stmt->close();
        
        // Commit the transaction
        $connect->commit();
        
        echo json_encode(array(
            'status' => 'success',
            'message' => 'CV data uploaded successfully',
            'cv_id' => $cvId,
            'cert_files' => $newFileNames
        ));
    } else {
        throw new Exception("Missing required parameters");
    }
} catch (Exception $e) {
    $connect->rollback();
    error_log("【Debug】Upload error: " . $e->getMessage());
    echo json_encode(array(
        'status' => 'error',
        'message' => $e->getMessage()
    ));
}
$connect->close();
?>

<?php

include 'includes/session.php';
include 'includes/db_connect.php';

// Handle status update
$message = '';
if ($_SERVER["REQUEST_METHOD"] == "POST" && isset($_POST['cert_id']) && isset($_POST['new_status'])) {
    $cert_id = $_POST['cert_id'];
    $new_status = $_POST['new_status'];

    $update_sql = "UPDATE member_cert SET status = ? WHERE member_cert_id = ?";
    if ($stmt = $conn->prepare($update_sql)) {
        $stmt->bind_param("si", $new_status, $cert_id);
        if ($stmt->execute()) {
            $message = '<div class="alert alert-success">Status updated successfully!</div>';
        } else {
            $message = '<div class="alert alert-danger">Update failed: ' . $stmt->error . '</div>';
        }
        $stmt->close();
    }
}

// 處理搜尋條件
$whereClause = "WHERE 1";
if (isset($_GET['member_id']) && $_GET['member_id'] !== '') {
    $memberId = $conn->real_escape_string($_GET['member_id']);
    $whereClause .= " AND cv.member_id = '$memberId'";
}
if (isset($_GET['email']) && $_GET['email'] !== '') {
    $email = $conn->real_escape_string($_GET['email']);
    $whereClause .= " AND m.email LIKE '%$email%'";
}
if (isset($_GET['phone']) && $_GET['phone'] !== '') {
    $phone = $conn->real_escape_string($_GET['phone']);
    $whereClause .= " AND cv.contact LIKE '%$phone%'";
}
if (isset($_GET['status']) && $_GET['status'] !== '') {
    $status = $conn->real_escape_string($_GET['status']);
    $whereClause .= " AND cv.status = '$status'";
}

// 主查詢：將搜尋條件套用到 member_cv 表的 status 中
$cvQuery = "SELECT 
            cv.cv_id, 
            cv.member_id,
            cv.contact,
            cv.skills,
            cv.education,
            cv.language,
            cv.other,
            cv.cv_score,
            cv.last_modified,
            cv.status,
            m.username,
            m.email
          FROM member_cv cv
          JOIN member m ON cv.member_id = m.member_id
          $whereClause
          ORDER BY cv.last_modified DESC";

$cvResult = $conn->query($cvQuery);

// Add error checking
if (!$cvResult) {
    die("Query failed: " . $conn->error);
}

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// 證書資料查詢 (1:M 關係)
$certQuery = "SELECT 
              member_id, 
              cert_file, 
              description,
              created_time 
            FROM member_cert";
$certResult = $conn->query($certQuery);

// 組織證書資料
$certificates = [];
while ($row = $certResult->fetch_assoc()) {
    $certificates[$row['member_id']][] = $row;
}

?>

<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CV Review System</title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons+Outlined" rel="stylesheet">
    <link rel="stylesheet" href="css/styles.css">
    <link rel="stylesheet" href="css/sidebar.css">
    <link rel="stylesheet" href="css/header.css">
    <style>
        .filter-form input[type="text"],
        .filter-form select,
        .btn-filter,
        .btn-refresh {
            padding: 8px;
            margin-right: 5px;
            border: 1px solid #ccc;
            border-radius: 4px;
            font-size: 14px;
        }

        .btn-filter,
        .btn-refresh {
            background-color: #007bff;
            color: white;
            border: none;
            cursor: pointer;
            transition: background-color 0.3s;
        }

        .btn-filter:hover,
        .btn-refresh:hover {
            background-color: #0056b3;
        }

        .btn-refresh {
            background-color: #4CAF50;
            /* Green */
            color: white;
            border: none;
            padding: 10px 20px;
            font-size: 16px;
            cursor: pointer;
            border-radius: 5px;
            transition: background-color 0.3s ease, transform 0.2s ease;
        }

        .btn-refresh:hover {
            background-color: #45a049;
            transform: scale(1.05);
        }

        .btn-refresh:active {
            background-color: #3e8e41;
            transform: scale(0.95);
        }

        .cert-badge { cursor: pointer; transition: transform 0.3s; }
        .cert-badge:hover { transform: translateY(-2px); }
        .cert-details { max-height: 300px; overflow-y: auto; }
        .status-select { width: 120px; }

        /* 狀態按鈕動畫 */
        .status-btn {
            transition: all 0.2s ease;
            position: relative;
            overflow: hidden;
            min-width: 40px;
        }

        .status-btn.active {
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            transform: scale(1.05);
        }

        .status-btn:not(.active):hover {
            transform: translateY(-1px);
        }

        /* 按鈕間分隔線 */
        .btn-group .status-btn:not(:last-child)::after {
            content: "";
            position: absolute;
            right: -1px;
            top: 50%;
            transform: translateY(-50%);
            height: 60%;
            width: 1px;
            background: rgba(0,0,0,0.1);
            z-index: 1;
        }

        /* 按鈕激活波紋效果 */
        .status-btn.active::before {
            content: "";
            position: absolute;
            top: 50%;
            left: 50%;
            width: 150%;
            padding-bottom: 150%;
            background: rgba(255,255,255,0.15);
            border-radius: 50%;
            transform: translate(-50%, -50%) scale(0);
            animation: ripple 0.6s ease-out;
        }

        @keyframes ripple {
            from { transform: translate(-50%, -50%) scale(0); opacity: 1; }
            to { transform: translate(-50%, -50%) scale(1); opacity: 0; }
        }
    </style>
</head>

<body>
    <div class="grid-container">

         <!-- header -->
         <?php include 'includes/header.php'; ?>
        <!-- end header -->
        <!-- Sidebar -->
        <?php include 'includes/sidebar.php'; ?>
        <!-- End Sidebar -->

        <!-- Main -->
        <main class="main-container">
            <div class="content-header">
                <form method="GET" class="search-form">
                    <input type="text" name="member_id" placeholder="Search by Member ID">
                    <input type="text" name="email" placeholder="Search by Email">
                    <input type="text" name="phone" placeholder="Search by Phone">
                    <select name="status">
                        <option value="">-- Select Status --</option>
                        <option value="W">Pending Review</option>
                        <option value="A">Approved</option>
                        <option value="N">Not Approved</option>
                    </select>
                    <button type="submit" class="btn-search">Search</button>
                    <button type="button" class="btn-refresh" onclick="refreshPage()">Refresh</button>
                </form>
            </div>

            <!-- 將訊息移到這裡 -->
            <?php if (!empty($message)): ?>
                <div class="alert-message" style="margin-top: 10px;">
                    <?php echo $message; ?>
                </div>
            <?php endif; ?>

            <div class="cert-table">
                <?php if ($cvResult && $cvResult->num_rows > 0): ?>
                    <table class="table table-bordered table-hover">
                        <thead class="table-light">
                            <tr>
                                <th>Username</th>
                                <th>Contact</th>
                                <th>Skills</th>
                                <th>Education</th>
                                <th>Languages</th>
                                <th>Other</th>
                                <th>Certificates</th>
                                <th>Last Updated</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php while ($cv = $cvResult->fetch_assoc()): 
                                $memberId = $cv['member_id'];
                                $cvCerts = $certificates[$memberId] ?? [];
                            ?>
                            <tr>
                                <td><?= htmlspecialchars($cv['username']) ?></td>
                                <td><?= htmlspecialchars($cv['contact'] ?? 'N/A') ?></td>
                                <td><?= htmlspecialchars($cv['skills'] ?? '') ?></td>
                                <td><?= htmlspecialchars($cv['education'] ?? '') ?></td>
                                <td><?= htmlspecialchars($cv['language'] ?? '') ?></td>
                                <td><?= htmlspecialchars($cv['other'] ?? '') ?></td>
                                <td>
                                    <?php if (!empty($cvCerts)): ?>
                                    <div class="dropdown">
                                        <button class="btn btn-sm btn-outline-secondary cert-badge dropdown-toggle" 
                                                type="button" 
                                                data-bs-toggle="dropdown"
                                                aria-expanded="false">
                                            View Certificates (<?= count($cvCerts) ?>)
                                        </button>
                                        <div class="dropdown-menu cert-details p-3">
                                            <?php foreach ($cvCerts as $cert): ?>
                                            <div class="mb-3">
                                                <div class="d-flex justify-content-between align-items-center">
                                                    <a href="http://localhost/FYP/upload_cert/<?= htmlspecialchars($cert['cert_file']) ?>" 
                                                       target="_blank"
                                                       class="text-decoration-none text-truncate"
                                                       style="max-width: 70%">

                                                        <?= htmlspecialchars($cert['cert_file']) ?>
                                                    </a>
                                                    <small class="text-muted text-nowrap">
                                                        <?= date('m/d/Y', strtotime($cert['created_time'])) ?>
                                                    </small>
                                                </div>
                                                <?php if (!empty($cert['description'])): ?>
                                                <div class="text-muted small mt-1">
                                                    <?= nl2br(htmlspecialchars($cert['description'])) ?>
                                                </div>
                                                <?php endif; ?>
                                            </div>
                                            <?php endforeach; ?>
                                        </div>
                                    </div>
                                    <?php else: ?>
                                    <span class="text-muted">No certificates</span>
                                    <?php endif; ?>
                                </td>
                                <td><?= date('m/d/Y H:i', strtotime($cv['last_modified'])) ?></td>
                                <td>
                                    <?php 
                                    $cv_id = $cv['cv_id'];
                                    $current_status = $cv['status'];
                                    
                                    // Determine classes and disabled state for each button
                                    $pending_class = ($current_status == 'W') ? 'btn-warning' : 'btn-outline-warning';
                                    $pending_disabled = ($current_status == 'W') ? 'disabled' : '';
                                    
                                    $approve_class = ($current_status == 'A') ? 'btn-success' : 'btn-outline-success';
                                    $approve_disabled = ($current_status == 'A') ? 'disabled' : '';
                                    
                                    $reject_class = ($current_status == 'N') ? 'btn-danger' : 'btn-outline-danger';
                                    $reject_disabled = ($current_status == 'N') ? 'disabled' : '';
                                    ?>
                                    
                                    <form action='update_cv_status.php' method='post' style='display: inline-block; margin-right: 3px;'>
                                        <input type='hidden' name='cv_id' value='<?= $cv_id ?>'>
                                        <input type='hidden' name='new_status' value='W'>
                                        <button type='submit' class='btn btn-sm <?= $pending_class ?>' <?= $pending_disabled ?> title="Set to Pending Review">
                                            <span class="material-icons-outlined" style='font-size: 1rem; vertical-align: middle;'>pending</span>
                                        </button>
                                    </form>

                                    <form action='update_cv_status.php' method='post' style='display: inline-block; margin-right: 3px;'>
                                        <input type='hidden' name='cv_id' value='<?= $cv_id ?>'>
                                        <input type='hidden' name='new_status' value='A'>
                                        <button type='submit' class='btn btn-sm <?= $approve_class ?>' <?= $approve_disabled ?> title="Approve this CV">
                                            <span class="material-icons-outlined" style='font-size: 1rem; vertical-align: middle;'>check_circle</span>
                                        </button>
                                    </form>

                                    <form action='update_cv_status.php' method='post' style='display: inline-block;'>
                                        <input type='hidden' name='cv_id' value='<?= $cv_id ?>'>
                                        <input type='hidden' name='new_status' value='N'>
                                        <button type='submit' class='btn btn-sm <?= $reject_class ?>' <?= $reject_disabled ?> title="Reject this CV">
                                            <span class="material-icons-outlined" style='font-size: 1rem; vertical-align: middle;'>cancel</span>
                                        </button>
                                    </form>
                                </td>
                            </tr>
                            <?php endwhile; ?>
                        </tbody>
                    </table>
                <?php else: ?>
                    <div class="empty-message">
                        <span class="material-icons-outlined">info</span>
                        <p>There are currently no cv pending verification</p>
                    </div>
                <?php endif; ?>
            </div>

        </main>

        <!-- End Main -->

    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
    function refreshPage() {
      window.location.href = 'verify_cv.php';
    }
    </script>
</body>
</html>
<?php
require_once 'includes/session.php';
// Database connection
require_once 'includes/db_connect.php';

// 在 SQL 查詢之前添加搜尋條件處理
$where_conditions = [];
$params = [];
$param_types = "";

// 擴展搜尋條件處理
if (isset($_GET['search'])) {
    $app_id = $_GET['app_id'] ?? '';
    $app_creator = $_GET['app_creator'] ?? '';
    $username = $_GET['username'] ?? '';
    $status = $_GET['status'] ?? '';
    $subject_id = $_GET['subject_id'] ?? '';
    $district_id = $_GET['district_id'] ?? '';
    $fee_min = $_GET['fee_min'] ?? '';
    $fee_max = $_GET['fee_max'] ?? '';
    $class_level_id = $_GET['class_level_id'] ?? '';
    $available_day = $_GET['available_day'] ?? '';

    // 現有的條件
    if (!empty($app_id)) {
        $where_conditions[] = "a.app_id = ?";
        $params[] = $app_id;
        $param_types .= "i";
    }
    
    if (!empty($app_creator)) {
        $where_conditions[] = "a.app_creator = ?";
        $params[] = $app_creator;
        $param_types .= "s";
    }
    
    if (!empty($username)) {
        $where_conditions[] = "m.username LIKE ?";
        $params[] = "%$username%";
        $param_types .= "s";
    }
    
    if ($status !== '') {
        $where_conditions[] = "a.status = ?";
        $params[] = $status;
        $param_types .= "s";
    }

    // 新增科目搜尋
    if (!empty($_GET['subject_id'])) {
        $subject_ids = $_GET['subject_id'];
        $subject_conditions = [];
        foreach ($subject_ids as $subject_id) {
            $subject_conditions[] = "EXISTS (
                SELECT 1 FROM application_subject aps 
                WHERE aps.app_id = a.app_id AND aps.subject_id = ?)";
            $params[] = $subject_id;
            $param_types .= "i";
        }
        $where_conditions[] = "(" . implode(" OR ", $subject_conditions) . ")";
    }

    // 新增地區搜尋
    if (!empty($_GET['district_id'])) {
        $district_ids = $_GET['district_id'];
        $district_conditions = [];
        foreach ($district_ids as $district_id) {
            $district_conditions[] = "EXISTS (
                SELECT 1 FROM application_district ad 
                WHERE ad.app_id = a.app_id AND ad.district_id = ?)";
            $params[] = $district_id;
            $param_types .= "i";
        }
        $where_conditions[] = "(" . implode(" OR ", $district_conditions) . ")";
    }

    // 費用範圍搜尋
    if (!empty($fee_min)) {
        $where_conditions[] = "a.feePerHr >= ?";
        $params[] = $fee_min;
        $param_types .= "d";
    }
    if (!empty($fee_max)) {
        $where_conditions[] = "a.feePerHr <= ?";
        $params[] = $fee_max;
        $param_types .= "d";
    }

    // 班級程度搜尋
    if (!empty($_GET['class_level_id'])) {
        $level_ids = $_GET['class_level_id'];
        $level_conditions = [];
        foreach ($level_ids as $level_id) {
            $level_conditions[] = "a.class_level_id = ?";
            $params[] = $level_id;
            $param_types .= "i";
        }
        $where_conditions[] = "(" . implode(" OR ", $level_conditions) . ")";
    }

    // 可教時間搜尋
    if (!empty($_GET['available_day'])) {
        $day_conditions = [];
        foreach ($_GET['available_day'] as $day) {
            $day_conditions[] = "EXISTS (
                SELECT 1 FROM application_date ad 
                WHERE ad.app_id = a.app_id AND ad.{$day}_time IS NOT NULL)";
        }
        $where_conditions[] = "(" . implode(" OR ", $day_conditions) . ")";
    }
}

// 構建 SQL 查詢
$sql = "SELECT a.*, m.username, m.email, m.phone 
        FROM application a 
        LEFT JOIN member m ON a.member_id = m.member_id";

if (!empty($where_conditions)) {
    $sql .= " WHERE " . implode(" AND ", $where_conditions);
}

$sql .= " ORDER BY a.app_id DESC";

// 準備和執行查詢
$stmt = $conn->prepare($sql);
if (!empty($params)) {
    $stmt->bind_param($param_types, ...$params);
}
$stmt->execute();
$result = $stmt->get_result();

// 獲取所有科目和地區供搜尋表單使用
$subjects = $conn->query("SELECT * FROM subject ORDER BY subject_name");
$districts = $conn->query("SELECT * FROM district ORDER BY district_name");
$class_levels = $conn->query("SELECT * FROM class_level ORDER BY class_level_id");
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Applications Management</title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons+Outlined" rel="stylesheet">
    <link rel="stylesheet" href="css/styles.css">
    <link rel="stylesheet" href="css/sidebar.css">
    <link rel="stylesheet" href="css/header.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-select@1.13.18/dist/css/bootstrap-select.min.css">
    <style>
    .bootstrap-select>.dropdown-toggle {
        background-color: #fff;
        border: 1px solid #ced4da;
        padding: 0.25rem 0.5rem;
        font-size: 0.875rem;
        line-height: 1.5;
        height: calc(1.5em + 0.5rem + 2px);
    }
    .bootstrap-select>.dropdown-toggle.bs-placeholder {
        color: #6c757d;
    }
    .bootstrap-select .dropdown-menu {
        padding: 0.25rem;
    }
    .bootstrap-select .dropdown-menu li {
        margin-bottom: 0.125rem;
    }
    .bootstrap-select .dropdown-menu .inner {
        max-height: 200px;
    }
    .bootstrap-select .filter-option-inner-inner {
        line-height: 1.5;
        overflow: hidden;
        text-overflow: ellipsis;
    }
    .alert {
        margin-top: 10px;
        margin-bottom: 10px;
    }
    </style>
</head>
<body>
    <div class="grid-container">
        <?php include 'includes/header.php'; ?>
        <?php include 'includes/sidebar.php'; ?>

        <main class="main-container">
            <div class="container-fluid">
                <?php
                // 顯示錯誤信息
                if (isset($_GET['error']) && !empty($_GET['error'])) {
                    echo '<div class="alert alert-danger alert-dismissible fade show" role="alert">';
                    echo '<strong>錯誤：</strong> ' . htmlspecialchars(urldecode($_GET['error']));
                    echo '<button type="button" class="close" data-dismiss="alert" aria-label="Close">';
                    echo '<span aria-hidden="true">&times;</span>';
                    echo '</button>';
                    echo '</div>';
                }
                
                // 顯示成功信息
                if (isset($_GET['success']) && !empty($_GET['success'])) {
                    echo '<div class="alert alert-success alert-dismissible fade show" role="alert">';
                    echo htmlspecialchars(urldecode($_GET['success']));
                    echo '<button type="button" class="close" data-dismiss="alert" aria-label="Close">';
                    echo '<span aria-hidden="true">&times;</span>';
                    echo '</button>';
                    echo '</div>';
                }
                ?>
                
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <button class="btn btn-primary" type="button" data-toggle="collapse" data-target="#searchForm">
                        <span class="material-icons-outlined">filter_list</span>
                    </button>
                </div>
                
                <div class="collapse mb-3" id="searchForm">
                    <div class="card card-body p-2">
                        <form method="GET" class="row g-2">
                            <div class="col-md-2">
                                <label class="small">Application ID</label>
                                <input type="text" class="form-control form-control-sm" name="app_id" 
                                       value="<?php echo $_GET['app_id'] ?? ''; ?>">
                            </div>
                            <div class="col-md-2">
                                <label class="small">Creator Type</label>
                                <select class="form-control form-control-sm" name="app_creator">
                                    <option value="">All</option>
                                    <option value="T" <?php echo (isset($_GET['app_creator']) && $_GET['app_creator'] == 'T') ? 'selected' : ''; ?>>Tutor</option>
                                    <option value="PS" <?php echo (isset($_GET['app_creator']) && $_GET['app_creator'] == 'PS') ? 'selected' : ''; ?>>Parent/Student</option>
                                </select>
                            </div>
                            
                            <div class="col-md-3">
                                <label class="small">Subject</label>
                                <select class="selectpicker" name="subject_id[]" multiple 
                                        data-live-search="true"
                                        data-actions-box="true"
                                        data-selected-text-format="count > 2"
                                        data-width="100%"
                                        title="Select Subjects">
                                    <?php while($subject = $subjects->fetch_assoc()): ?>
                                        <option value="<?php echo $subject['subject_id']; ?>"
                                            <?php echo (isset($_GET['subject_id']) && in_array($subject['subject_id'], (array)$_GET['subject_id'])) ? 'selected' : ''; ?>>
                                            <?php echo $subject['subject_name']; ?>
                                        </option>
                                    <?php endwhile; ?>
                                </select>
                            </div>

                            <div class="col-md-3">
                                <label class="small">District</label>
                                <select class="selectpicker" name="district_id[]" multiple 
                                        data-live-search="true" 
                                        data-width="100%"
                                        data-selected-text-format="count > 3"
                                        title="All Districts">
                                    <?php while($district = $districts->fetch_assoc()): ?>
                                        <option value="<?php echo $district['district_id']; ?>"
                                            <?php echo (isset($_GET['district_id']) && in_array($district['district_id'], $_GET['district_id'] ?? [])) ? 'selected' : ''; ?>>
                                            <?php echo $district['district_name']; ?>
                                        </option>
                                    <?php endwhile; ?>
                                </select>
                            </div>

                            <div class="col-md-2">
                                <label class="small">Fee Range (HKD)</label>
                                <div class="input-group input-group-sm">
                                    <input type="number" class="form-control form-control-sm" name="fee_min" 
                                           placeholder="Min" value="<?php echo $_GET['fee_min'] ?? ''; ?>">
                                    <input type="number" class="form-control form-control-sm" name="fee_max" 
                                           placeholder="Max" value="<?php echo $_GET['fee_max'] ?? ''; ?>">
                                </div>
                            </div>

                            <div class="col-md-3">
                                <label class="small">Class Level</label>
                                <select class="selectpicker" name="class_level_id[]" multiple 
                                        data-live-search="true" 
                                        data-width="100%"
                                        data-selected-text-format="count > 3"
                                        title="All Levels">
                                    <?php while($level = $class_levels->fetch_assoc()): ?>
                                        <option value="<?php echo $level['class_level_id']; ?>"
                                            <?php echo (isset($_GET['class_level_id']) && in_array($level['class_level_id'], $_GET['class_level_id'] ?? [])) ? 'selected' : ''; ?>>
                                            <?php echo $level['class_level_name']; ?>
                                        </option>
                                    <?php endwhile; ?>
                                </select>
                            </div>

                            <div class="col-md-3">
                                <label class="small">Available Day</label>
                                <select class="selectpicker" name="available_day[]" multiple 
                                        data-width="100%"
                                        data-selected-text-format="count > 3"
                                        title="Any Day">
                                    <option value="monday" <?php echo (isset($_GET['available_day']) && in_array('monday', $_GET['available_day'] ?? [])) ? 'selected' : ''; ?>>Monday</option>
                                    <option value="tuesday" <?php echo (isset($_GET['available_day']) && in_array('tuesday', $_GET['available_day'] ?? [])) ? 'selected' : ''; ?>>Tuesday</option>
                                    <option value="wednesday" <?php echo (isset($_GET['available_day']) && in_array('wednesday', $_GET['available_day'] ?? [])) ? 'selected' : ''; ?>>Wednesday</option>
                                    <option value="thursday" <?php echo (isset($_GET['available_day']) && in_array('thursday', $_GET['available_day'] ?? [])) ? 'selected' : ''; ?>>Thursday</option>
                                    <option value="friday" <?php echo (isset($_GET['available_day']) && in_array('friday', $_GET['available_day'] ?? [])) ? 'selected' : ''; ?>>Friday</option>
                                    <option value="saturday" <?php echo (isset($_GET['available_day']) && in_array('saturday', $_GET['available_day'] ?? [])) ? 'selected' : ''; ?>>Saturday</option>
                                    <option value="sunday" <?php echo (isset($_GET['available_day']) && in_array('sunday', $_GET['available_day'] ?? [])) ? 'selected' : ''; ?>>Sunday</option>
                                </select>
                            </div>

                            <div class="col-12 text-end">
                                <button type="submit" name="search" class="btn btn-primary btn-sm">
                                    <span class="material-icons-outlined">search</span> Search
                                </button>
                                <a href="applications.php" class="btn btn-secondary btn-sm">
                                    <span class="material-icons-outlined">clear</span> Clear
                                </a>
                            </div>
                        </form>
                    </div>
                </div>
                
                <div class="table-responsive">
                    <table class="table table-sm table-hover">
                        <thead class="thead-light">
                            <tr>
                                <th>ID</th>
                                <th>Creator</th>
                                <th>Member</th>
                                <th>Subjects</th>
                                <th>Level</th>
                                <th>Districts</th>
                                <th>Fee/Hr</th>
                                <th>Time</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php
                            if ($result->num_rows > 0) {
                                while($row = $result->fetch_assoc()) {
                                    // 獲取科目
                                    $subject_sql = "SELECT s.subject_name 
                                                  FROM application_subject as_table 
                                                  JOIN subject s ON as_table.subject_id = s.subject_id 
                                                  WHERE as_table.app_id = ?";
                                    $stmt = $conn->prepare($subject_sql);
                                    $stmt->bind_param("i", $row['app_id']);
                                    $stmt->execute();
                                    $subjects_result = $stmt->get_result();
                                    $subjects = [];
                                    while($subject = $subjects_result->fetch_assoc()) {
                                        $subjects[] = $subject['subject_name'];
                                    }

                                    // 獲取地區
                                    $district_sql = "SELECT d.district_name 
                                                   FROM application_district ad 
                                                   JOIN district d ON ad.district_id = d.district_id 
                                                   WHERE ad.app_id = ?";
                                    $stmt = $conn->prepare($district_sql);
                                    $stmt->bind_param("i", $row['app_id']);
                                    $stmt->execute();
                                    $districts_result = $stmt->get_result();
                                    $districts = [];
                                    while($district = $districts_result->fetch_assoc()) {
                                        $districts[] = $district['district_name'];
                                    }

                                    // 獲取時間表
                                    $time_sql = "SELECT * FROM application_date WHERE app_id = ?";
                                    $stmt = $conn->prepare($time_sql);
                                    $stmt->bind_param("i", $row['app_id']);
                                    $stmt->execute();
                                    $time_result = $stmt->get_result();
                                    $time_row = $time_result->fetch_assoc();
                                    
                                    echo "<tr>";
                                    echo "<td>".$row['app_id']."</td>";
                                    echo "<td>".$row['app_creator']."</td>";
                                    echo "<td>".($row['username'] ?? 'N/A')."<br>".
                                         "<small>".$row['email']."<br>".$row['phone']."</small></td>";
                                    echo "<td>".implode("<br>", $subjects)."</td>";
                                    echo "<td>".$row['class_level_id']."</td>";
                                    echo "<td>".implode("<br>", $districts)."</td>";
                                    echo "<td>$".$row['feePerHr']."</td>";
                                    echo "<td>";
                                    if ($time_row) {
                                        foreach(['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'] as $day) {
                                            $time_key = $day.'_time';
                                            if (!empty($time_row[$time_key])) {
                                                echo ucfirst($day).": ".$time_row[$time_key]."<br>";
                                            }
                                        }
                                    }
                                    echo "</td>";
                                    
                                    // Status display column
                                    echo "<td>";
                                    $status_text = '';
                                    $status_class = '';
                                    switch ($row['status']) {
                                        case 'A':
                                            $status_text = 'Approved';
                                            $status_class = 'badge badge-success';
                                            break;
                                        case 'R':
                                            $status_text = 'Rejected';
                                            $status_class = 'badge badge-danger';
                                            break;
                                        case 'P':
                                            $status_text = 'Pending';
                                            $status_class = 'badge badge-warning';
                                            break;
                                        default:
                                            $status_text = 'Unknown';
                                            $status_class = 'badge badge-secondary';
                                            break;
                                    }
                                    echo "<span class='" . $status_class . "'>" . $status_text . "</span>";
                                    echo "</td>";

                                    // Actions column
                                    echo "<td>";
                                    // Form for Approve
                                    echo "<form action='update_application_status.php' method='post' style='display: inline-block; margin-right: 3px;'>";
                                    echo "<input type='hidden' name='app_id' value='" . htmlspecialchars($row['app_id']) . "'>";
                                    echo "<input type='hidden' name='new_status' value='A'>";
                                    echo "<button type='submit' class='btn btn-success btn-sm'" . ($row['status'] == 'A' ? ' disabled' : '') . " title='Approve'>";
                                    echo "<span class='material-icons-outlined' style='font-size: 1rem; vertical-align: middle;'>check_circle</span>";
                                    echo "</button>";
                                    echo "</form>";

                                    // Form for Reject
                                    echo "<form action='update_application_status.php' method='post' style='display: inline-block; margin-right: 3px;'>";
                                    echo "<input type='hidden' name='app_id' value='" . htmlspecialchars($row['app_id']) . "'>";
                                    echo "<input type='hidden' name='new_status' value='R'>";
                                    echo "<button type='submit' class='btn btn-danger btn-sm'" . ($row['status'] == 'R' ? ' disabled' : '') . " title='Reject'>";
                                    echo "<span class='material-icons-outlined' style='font-size: 1rem; vertical-align: middle;'>cancel</span>";
                                    echo "</button>";
                                    echo "</form>";

                                    // Form for Pending
                                    echo "<form action='update_application_status.php' method='post' style='display: inline-block;'>";
                                    echo "<input type='hidden' name='app_id' value='" . htmlspecialchars($row['app_id']) . "'>";
                                    echo "<input type='hidden' name='new_status' value='P'>";
                                    echo "<button type='submit' class='btn btn-warning btn-sm'" . ($row['status'] == 'P' ? ' disabled' : '') . " title='Pending'>";
                                    echo "<span class='material-icons-outlined' style='font-size: 1rem; vertical-align: middle;'>pending</span>";
                                    echo "</button>";
                                    echo "</form>";

                                    // Add the View Detail button back, opening in new tab
                                    echo "<a href='application_detail.php?id=".htmlspecialchars($row['app_id'])."' class='btn btn-info btn-sm ml-1' title='View Details' target='_blank'>"; // Added target='_blank'
                                    echo "<span class='material-icons-outlined' style='font-size: 1rem; vertical-align: middle;'>visibility</span>";
                                    echo "</a>";

                                    echo "</td>";
                                    echo "</tr>";
                                }
                            } else {
                                echo "<tr><td colspan='10' class='text-center'>No applications found</td></tr>";
                            }
                            ?>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    </div>

    <script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.1/dist/umd/popper.min.js"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap-select@1.13.18/dist/js/bootstrap-select.min.js"></script>
    <script src="js/scripts.js"></script>
    <script>
    // REMOVED JavaScript for status update
    // document.addEventListener('DOMContentLoaded', function() {
    //     document.querySelectorAll('.status-btn').forEach(function(button) {
    //         button.addEventListener('click', function() {
    //             if (this.classList.contains('active')) {
    //                 return;
    //             }
    //
    //             var appId = this.getAttribute('data-app-id');
    //             var status = this.getAttribute('data-status');
    //
    //             fetch('update_application_status.php', {
    //                 method: 'POST',
    //                 headers: {
    //                     'Content-Type': 'application/json'
    //                 },
    //                 body: JSON.stringify({ app_id: appId, status: status })
    //             })
    //             .then(response => response.text())
    //             .then(data => {
    //                 const jsonData = JSON.parse(data);
    //                 if (jsonData.success) {
    //                     updateStatusIcons(appId, status);
    //                 }
    //             });
    //         });
    //     });
    // });
    //
    // function getStatusClass(status) {
    //     switch(status) {
    //         case 'A': return 'success';
    //         case 'R': return 'danger';
    //         case 'P': return 'warning';
    //         default: return '';
    //     }
    // }
    //
    // function updateStatusIcons(appId, newStatus) {
    //     const buttons = document.querySelectorAll(`.status-btn[data-app-id='${appId}']`);
    //
    //     buttons.forEach(button => {
    //         button.classList.remove('active');
    //
    //         const status = button.getAttribute('data-status');
    //         const statusClass = getStatusClass(status);
    //
    //         button.classList.remove(`btn-${statusClass}`);
    //         button.classList.add(`btn-outline-${statusClass}`);
    //
    //         if (status === newStatus) {
    //             button.classList.remove(`btn-outline-${statusClass}`);
    //             button.classList.add(`btn-${statusClass}`, 'active');
    //         }
    //     });
    // }
    </script>
</body>
</html>

<?php
$conn->close();
?> 
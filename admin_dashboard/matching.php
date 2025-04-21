<?php
session_start(); // Start session

include 'includes/session.php';

include 'includes/db_connect.php';

$message = ''; // For success/error messages
$admin_id = intval($_SESSION['member_id']);

// --- Handle Admin Approve/Reject Actions ---
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && isset($_POST['match_id']) && $admin_id > 0) {
    $match_id_to_process = intval($_POST['match_id']);
    $action = $_POST['action']; // 'admin_approve' or 'admin_reject'

    $new_admin_status = '';
    if ($action === 'admin_approve') {
        $new_admin_status = 'approved';
    } elseif ($action === 'admin_reject') {
        $new_admin_status = 'rejected';
    }

    if (!empty($new_admin_status)) {
        // Prepare update statement for admin approval columns
        $stmt = $conn->prepare("UPDATE `match` SET 
                                admin_approval_status = ?, 
                                admin_approved_by = ?, 
                                admin_approved_at = CURRENT_TIMESTAMP 
                                WHERE match_id = ? AND admin_approval_status = 'pending'"); // Only process if admin status is 'pending'

        if ($stmt) {
            $stmt->bind_param("sii", $new_admin_status, $admin_id, $match_id_to_process);
            if ($stmt->execute()) {
                if ($stmt->affected_rows > 0) {
                    $message = "<div class='alert alert-success'>Match #{$match_id_to_process} successfully {$new_admin_status}.</div>";
                } else {
                    $message = "<div class='alert alert-warning'>Match #{$match_id_to_process} could not be processed (maybe already processed or admin status is not 'pending').</div>";
                }
            } else {
                $message = "<div class='alert alert-danger'>Error processing match: " . $stmt->error . "</div>";
            }
            $stmt->close();
        } else {
            $message = "<div class='alert alert-danger'>Error preparing statement: " . $conn->error . "</div>";
        }
    }
}
// --- End Handle Admin Actions ---

// --- Filtering and Sorting Logic ---
$where_clauses = [];
$params = [];
$types = '';

// Filter by User Status
$filter_user_status = $_GET['user_status'] ?? '';
if (!empty($filter_user_status)) {
    $where_clauses[] = "m.status = ?";
    $params[] = $filter_user_status;
    $types .= 's';
}

// Filter by Admin Approval Status
$filter_admin_status = $_GET['admin_status'] ?? '';
if (!empty($filter_admin_status)) {
    $where_clauses[] = "m.admin_approval_status = ?";
    $params[] = $filter_admin_status;
    $types .= 's';
}

// Filter by Tutor ID
$filter_tutor_id = $_GET['tutor_id'] ?? '';
if (!empty($filter_tutor_id)) {
    $where_clauses[] = "m.tutor_id = ?";
    $params[] = $filter_tutor_id;
    $types .= 's'; 
}

// Filter by Parent/Student ID
$filter_ps_id = $_GET['ps_id'] ?? '';
if (!empty($filter_ps_id)) {
    $where_clauses[] = "m.ps_id = ?";
    $params[] = $filter_ps_id;
    $types .= 's'; 
}

// Build WHERE clause
$sql_where = '';
if (!empty($where_clauses)) {
    $sql_where = "WHERE " . implode(" AND ", $where_clauses);
}

// --- Modified Sorting Logic ---
$sort_by_input = $_GET['sort_by'] ?? 'default_pending_user_status'; // New default key
$sort_order_input = $_GET['sort_order'] ?? 'DESC'; // Default secondary/tertiary order

$sql_order_by = '';
$allowed_sort_columns = [ 
    'default_pending_user_status', // New default identifier
    'm.match_id', 
    'm.match_mark', 
    'm.video_datetime', 
    'm.admin_approved_at', 
    'm.status', 
    'm.admin_approval_status'
];

// Validate sort column and order
if (!in_array($sort_by_input, $allowed_sort_columns)) {
    $sort_by_input = 'default_pending_user_status'; // Reset to new default if invalid
}
// Validate secondary/tertiary order (used for match_id in default, or the primary column if user selected one)
$sort_order = strtoupper($sort_order_input) === 'ASC' ? 'ASC' : 'DESC'; 

// Construct ORDER BY clause
if ($sort_by_input === 'default_pending_user_status') {
    // Default sort: 
    // 1. Admin Status 'pending' first
    // 2. Then User Status (APR > PND > WT/WPS > REJ > Others)
    // 3. Then Match ID (ASC/DESC based on user selection)
    $sql_order_by = "ORDER BY 
                        CASE WHEN m.admin_approval_status = 'pending' THEN 0 ELSE 1 END ASC, 
                        CASE m.status 
                            WHEN 'APR' THEN 0 
                            WHEN 'PND' THEN 1 
                            WHEN 'WT' THEN 2 
                            WHEN 'WPS' THEN 2 
                            WHEN 'REJ' THEN 3 
                            ELSE 4 
                        END ASC, 
                        m.match_id {$sort_order}";
} else {
    // User selected a specific column to sort by
     $sql_order_by = "ORDER BY {$sort_by_input} {$sort_order}";
     // Optional: Add a secondary sort for consistency 
     if ($sort_by_input !== 'm.match_id') { 
        $sql_order_by .= ", m.match_id DESC"; 
     }
}
// --- End Modified Sorting Logic ---

// --- Fetch Distinct Values for Filters ---
$user_statuses = []; // Initialize as empty array
$admin_statuses = []; // Initialize as empty array

// Check if $conn is valid before querying
if ($conn) { 
    // Query for distinct user statuses
    $result_user = $conn->query("SELECT DISTINCT status FROM `match` WHERE status IS NOT NULL AND status != '' ORDER BY status ASC");
    if (!$result_user) {
        // Display error if query fails, but don't stop the page
        $message .= "<div class='alert alert-danger'>Error fetching user statuses: " . $conn->error . "</div>";
    } else {
        $user_statuses = $result_user->fetch_all(MYSQLI_ASSOC);
        $result_user->free(); // Free result set
    }

    // Query for distinct admin statuses
    $result_admin = $conn->query("SELECT DISTINCT admin_approval_status FROM `match` WHERE admin_approval_status IS NOT NULL AND admin_approval_status != '' ORDER BY admin_approval_status ASC");
     if (!$result_admin) {
        // Display error if query fails
        $message .= "<div class='alert alert-danger'>Error fetching admin statuses: " . $conn->error . "</div>";
    } else {
        $admin_statuses = $result_admin->fetch_all(MYSQLI_ASSOC);
        $result_admin->free(); // Free result set
    }
    // Debugging: Uncomment to see the fetched values
    // var_dump($user_statuses);
    // var_dump($admin_statuses);

} else {
     $message .= "<div class='alert alert-danger'>Database connection is not available for fetching filter options.</div>";
}
// --- End Fetch Distinct Values ---

// --- Query for Matches ---
$result_matches = null; // Initialize result
if ($conn) { // Check connection again before main query
    $sql_base = "SELECT
        m.match_id, m.tutor_id, t_mem.username as tutor_username, 
        m.ps_id, p_mem.username as ps_username, 
        m.match_mark as match_score, m.video_datetime as apply_date,
        m.status, m.admin_approval_status, m.admin_approved_by,
        a_mem.username as admin_username, m.admin_approved_at
    FROM `match` m
    INNER JOIN application t_app ON m.tutor_app_id = t_app.app_id
    INNER JOIN member t_mem ON m.tutor_id = t_mem.member_id 
    INNER JOIN application p_app ON m.ps_app_id = p_app.app_id
    INNER JOIN member p_mem ON m.ps_id = p_mem.member_id 
    LEFT JOIN member a_mem ON m.admin_approved_by = a_mem.member_id 
    ";

    $sql_matches = $sql_base . $sql_where . " " . $sql_order_by;

    // Prepare statement for fetching matches if filters are applied
    if (!empty($params)) {
        $stmt_select = $conn->prepare($sql_matches);
        if ($stmt_select) {
            $stmt_select->bind_param($types, ...$params);
            if ($stmt_select->execute()) {
                 $result_matches = $stmt_select->get_result();
            } else {
                 $message .= "<div class='alert alert-danger'>Error executing match query: " . $stmt_select->error . "</div>";
            }
            // $stmt_select->close(); // Close later, after the loop
        } else {
            $message .= "<div class='alert alert-danger'>Error preparing match statement: " . $conn->error . "</div>"; 
        }
    } else {
        // Execute directly if no filters
        $result_matches = $conn->query($sql_matches);
         if (!$result_matches) {
            $message .= "<div class='alert alert-danger'>Error executing match query: " . $conn->error . "</div>"; 
        }
    }
} // End if ($conn)
// --- End Query for Matches ---

$conn->close(); // Close connection
?>

<!DOCTYPE html>
<html lang="en">

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Admin Dashboard - Matching Management</title>

  <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600&display=swap" rel="stylesheet">
  <link href="https://fonts.googleapis.com/icon?family=Material+Icons+Outlined" rel="stylesheet">
   <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.3/css/all.min.css">
  <link rel="stylesheet" href="css/styles.css">
  <link rel="stylesheet" href="css/sidebar.css">
  <link rel="stylesheet" href="css/header.css">
</head>

<body>
  <div class="grid-container">

    <?php
    $page_title = "Matching Management";
    include 'includes/header.php';
    ?>
    <!-- Sidebar -->
    <?php include 'includes/sidebar.php'; ?>
    <!-- End Sidebar -->

    <!-- Main -->
    <main class="main-container">
        <div class="main-title">
        </div>

        <?php echo $message; // Display success/error messages (now includes query errors) ?>

        <!-- Filter and Sort Form -->
        <div class="card mb-4">
            <div class="card-body">
                <form action="matching.php" method="GET" class="form-inline">
                    <!-- User Status Dropdown -->
                    <div class="form-group mr-3 mb-2">
                        <label for="user_status" class="mr-2">User Status:</label>
                        <select name="user_status" id="user_status" class="form-control form-control-sm">
                            <option value="">All</option>
                            <?php if (!empty($user_statuses)): // Check if array is not empty ?>
                                <?php foreach ($user_statuses as $status_row): ?>
                                    <?php // Check if the key 'status' exists before accessing ?>
                                    <?php if (isset($status_row['status'])): ?>
                                        <option value="<?= htmlspecialchars($status_row['status']) ?>" <?= ($filter_user_status == $status_row['status']) ? 'selected' : '' ?>>
                                            <?= strtoupper(htmlspecialchars($status_row['status'])) ?>
                                        </option>
                                    <?php endif; ?>
                                <?php endforeach; ?>
                            <?php endif; ?>
                        </select>
                    </div>
                    <!-- Admin Status Dropdown -->
                    <div class="form-group mr-3 mb-2">
                        <label for="admin_status" class="mr-2">Admin Approval:</label>
                        <select name="admin_status" id="admin_status" class="form-control form-control-sm">
                            <option value="">All</option>
                             <?php if (!empty($admin_statuses)): // Check if array is not empty ?>
                                <?php foreach ($admin_statuses as $status_row): ?>
                                     <?php // Check if the key 'admin_approval_status' exists ?>
                                     <?php if (isset($status_row['admin_approval_status'])): ?>
                                        <option value="<?= htmlspecialchars($status_row['admin_approval_status']) ?>" <?= ($filter_admin_status == $status_row['admin_approval_status']) ? 'selected' : '' ?>>
                                            <?= ucfirst(htmlspecialchars($status_row['admin_approval_status'])) ?>
                                        </option>
                                     <?php endif; ?>
                                <?php endforeach; ?>
                             <?php endif; ?>
                        </select>
                    </div>
                    <!-- Other filters and sort options -->
                    <div class="form-group mr-3 mb-2">
                        <label for="tutor_id" class="mr-2">Tutor ID:</label>
                        <input type="text" name="tutor_id" id="tutor_id" class="form-control form-control-sm" value="<?= htmlspecialchars($filter_tutor_id) ?>" placeholder="Enter Tutor ID">
                    </div>
                     <div class="form-group mr-3 mb-2">
                        <label for="ps_id" class="mr-2">P/S ID:</label>
                        <input type="text" name="ps_id" id="ps_id" class="form-control form-control-sm" value="<?= htmlspecialchars($filter_ps_id) ?>" placeholder="Enter P/S ID">
                    </div>
                    <div class="form-group mr-3 mb-2">
                        <label for="sort_by" class="mr-2">Sort By:</label>
                        <select name="sort_by" id="sort_by" class="form-control form-control-sm">
                            <option value="default_pending_user_status" <?= ($sort_by_input == 'default_pending_user_status') ? 'selected' : '' ?>>Default (Pending > User Status)</option>
                            <option value="m.match_id" <?= ($sort_by_input == 'm.match_id') ? 'selected' : '' ?>>Match ID</option>
                            <option value="m.match_mark" <?= ($sort_by_input == 'm.match_mark') ? 'selected' : '' ?>>Match Score</option>
                            <option value="m.video_datetime" <?= ($sort_by_input == 'm.video_datetime') ? 'selected' : '' ?>>Apply Date</option>
                            <option value="m.admin_approved_at" <?= ($sort_by_input == 'm.admin_approved_at') ? 'selected' : '' ?>>Admin Approval Date</option>
                             <option value="m.status" <?= ($sort_by_input == 'm.status') ? 'selected' : '' ?>>User Status</option>
                            <option value="m.admin_approval_status" <?= ($sort_by_input == 'm.admin_approval_status') ? 'selected' : '' ?>>Admin Status</option>
                        </select>
                    </div>
                    <div class="form-group mr-3 mb-2">
                         <label for="sort_order" class="mr-2">Order:</label>
                        <select name="sort_order" id="sort_order" class="form-control form-control-sm">
                            <option value="DESC" <?= ($sort_order == 'DESC') ? 'selected' : '' ?>>Descending</option>
                            <option value="ASC" <?= ($sort_order == 'ASC') ? 'selected' : '' ?>>Ascending</option>
                        </select>
                    </div>

                    <button type="submit" class="btn btn-primary btn-sm mb-2 mr-2">Filter / Sort</button>
                    <a href="matching.php" class="btn btn-secondary btn-sm mb-2">Reset</a>
                </form>
            </div>
        </div>
        <!-- End Filter and Sort Form -->

        <div class="table-responsive">
            <table class="table table-hover table-striped table-bordered">
                <thead class="thead-dark">
                    <tr>
                        <th>User Status</th>
                        <th>Match ID</th>
                        <th>Tutor (ID: Name)</th>
                        <th>P/S (ID: Name)</th>
                        <th>Match Score</th>
                        <th>Apply Date</th>
                        <th>Admin Approval</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if ($result_matches && $result_matches->num_rows > 0): ?>
                        <?php while($row = $result_matches->fetch_assoc()): ?>
                        <tr>
                            <td> <?php // User agreement status with full names ?>
                                <?php
                                $user_status_code = strtoupper($row['status'] ?? 'N/A');
                                $user_status_text = 'Unknown'; // Default text
                                $user_badge_class = 'badge-secondary'; // Default badge

                                switch ($user_status_code) {
                                    case 'APR':
                                        $user_status_text = 'Approved';
                                        $user_badge_class = 'badge-primary'; // Both agreed
                                        break;
                                    case 'PND':
                                        $user_status_text = 'Pending';
                                        $user_badge_class = 'badge-light text-dark'; // Initial state?
                                        break;
                                    case 'WT':
                                        $user_status_text = 'Waiting Tutor';
                                        $user_badge_class = 'badge-info'; // Waiting for tutor's response
                                        break;
                                    case 'WPS':
                                        $user_status_text = 'Waiting Parent/Student';
                                        $user_badge_class = 'badge-info'; // Waiting for parent/student's response
                                        break;
                                    case 'REJ':
                                        $user_status_text = 'Rejected';
                                        $user_badge_class = 'badge-danger'; // One party rejected
                                        break;
                                    default:
                                        // Keep default 'Unknown' and 'badge-secondary'
                                        $user_status_text = $user_status_code; // Show the code if not recognized
                                        break;
                                }
                                ?>
                                <span class="badge <?= $user_badge_class ?>"><?= htmlspecialchars($user_status_text) ?></span>
                            </td>
                            <td>#<?= htmlspecialchars($row['match_id'] ?? 'N/A') ?></td>
                            <td><?= htmlspecialchars($row['tutor_id'] ?? 'N/A') ?>: <?= htmlspecialchars($row['tutor_username'] ?? 'N/A') ?></td>
                            <td><?= htmlspecialchars($row['ps_id'] ?? 'N/A') ?>: <?= htmlspecialchars($row['ps_username'] ?? 'N/A') ?></td>
                            <td><?= number_format((float)($row['match_score'] ?? 0), 1) ?> pts</td>
                            <td>
                                <?php if (!empty($row['apply_date'])): ?>
                                    <?= date('Y-m-d', strtotime($row['apply_date'])) ?>
                                <?php else: ?>
                                    N/A
                                <?php endif; ?>
                            </td>
                            <td> <?php // Display admin approval info ?>
                                <?php
                                $admin_status = $row['admin_approval_status'] ?? 'pending';
                                $admin_badge_class = 'badge-warning';
                                $admin_icon = 'fa-clock';
                                if ($admin_status === 'approved') {
                                    $admin_badge_class = 'badge-success';
                                    $admin_icon = 'fa-check-circle';
                                } elseif ($admin_status === 'rejected') {
                                    $admin_badge_class = 'badge-danger';
                                    $admin_icon = 'fa-times-circle';
                                }
                                ?>
                                <span class="badge <?= $admin_badge_class ?>"><i class="fas <?= $admin_icon ?>"></i> <?= ucfirst(htmlspecialchars($admin_status)) ?></span>
                                <?php if (!empty($row['admin_approved_by'])): ?>
                                     <br><small>By: <?= htmlspecialchars($row['admin_username'] ?? $row['admin_approved_by']) ?> at <?= date('Y-m-d H:i', strtotime($row['admin_approved_at'])) ?></small>
                                <?php endif; ?>
                            </td>
                            <td>
                                <?php // Show Approve/Reject buttons only if admin status is 'pending' ?>
                                <?php if ($admin_status === 'pending'): ?>
                                    <form action="matching.php<?= !empty($_SERVER['QUERY_STRING']) ? '?' . $_SERVER['QUERY_STRING'] : '' ?>" method="POST" style="display: inline-block; margin-bottom: 5px;">
                                        <input type="hidden" name="action" value="admin_approve">
                                        <input type="hidden" name="match_id" value="<?= htmlspecialchars($row['match_id']) ?>">
                                        <button type="submit" class="btn btn-success btn-sm" title="Approve Match">
                                            <i class="fas fa-check"></i> <span class="d-none d-md-inline">Approve</span>
                                        </button>
                                    </form>
                                    <form action="matching.php<?= !empty($_SERVER['QUERY_STRING']) ? '?' . $_SERVER['QUERY_STRING'] : '' ?>" method="POST" style="display: inline-block;">
                                        <input type="hidden" name="action" value="admin_reject">
                                        <input type="hidden" name="match_id" value="<?= htmlspecialchars($row['match_id']) ?>">
                                        <button type="submit" class="btn btn-danger btn-sm" title="Reject Match">
                                            <i class="fas fa-times"></i> <span class="d-none d-md-inline">Reject</span>
                                        </button>
                                    </form>
                                <?php else: ?>
                                     <span class="text-muted">Processed</span>
                                <?php endif; ?>
                            </td>
                        </tr>
                        <?php endwhile; ?>
                         <?php // Close the prepared statement if it was used
                             if (isset($stmt_select)) {
                                 $stmt_select->close();
                             }
                         ?>
                    <?php else: ?>
                        <tr>
                           <td colspan="8" class="text-center">No matching records found <?= !empty($sql_where) ? 'with the current filters' : '' ?>.</td>
                        </tr>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
    </main>
    <!-- End Main -->

  </div>

  <!-- Scripts -->
  <script src="https://code.jquery.com/jquery-3.5.1.slim.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.5.4/dist/umd/popper.min.js"></script>
  <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>
  <script src="js/scripts.js"></script>
</body>

</html>
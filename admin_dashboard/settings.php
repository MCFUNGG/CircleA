<?php
session_start(); // Start session

// --- Language Setup ---
if (isset($_GET['lang']) && ($_GET['lang'] == 'en' || $_GET['lang'] == 'zh-TW')) {
    $_SESSION['language'] = $_GET['lang']; // Update language in session if selected via GET
}
$language = $_SESSION['language'] ?? 'en'; // Default to English if no language is set

// Load the language file
$lang_file = __DIR__ . '/languages/lang_' . $language . '.php';
if (file_exists($lang_file)) {
    include $lang_file;
} else {
    // Fallback to English if selected language file doesn't exist
    include __DIR__ . '/languages/lang_en.php'; 
}
// --- End Language Setup ---

include 'includes/session.php'; // Checks if admin is logged in, might set $_SESSION['admin_id']
include 'includes/db_connect.php';

$update_message = '';
$error_message = '';

// Check if the form is submitted
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    // Ensure admin ID is available in session
    if (!isset($_SESSION['member_id'])) {
        $error_message = $LANG['error_admin_session_not_found']; // Use LANG variable
    } else {
        $admin_id = $_SESSION['member_id'];
        $current_password = $_POST['current_password'] ?? '';
        $new_password = $_POST['new_password'] ?? '';
        $confirm_password = $_POST['confirm_password'] ?? '';

        // Basic validation
        if (empty($current_password) || empty($new_password) || empty($confirm_password)) {
            $error_message = $LANG['error_fill_all_fields'];
        } elseif ($new_password !== $confirm_password) {
            $error_message = $LANG['error_password_mismatch'];
        } else {
            // Fetch current password hash from DB
            $stmt_select = $conn->prepare("SELECT password FROM member WHERE member_id = ?");
            if ($stmt_select) {
                $stmt_select->bind_param("i", $admin_id);
                $stmt_select->execute();
                $result = $stmt_select->get_result();
                
                if ($result->num_rows === 1) {
                    $user = $result->fetch_assoc();
                    $current_hash = $user['password'];

                    // Verify current password
                    if (password_verify($current_password, $current_hash)) {
                        // Hash the new password
                        $new_hash = password_hash($new_password, PASSWORD_DEFAULT);

                        // Update the password in the DB
                        $stmt_update = $conn->prepare("UPDATE member SET password = ? WHERE member_id = ?");
                        if ($stmt_update) {
                            $stmt_update->bind_param("si", $new_hash, $admin_id);
                            if ($stmt_update->execute()) {
                                $update_message = $LANG['password_update_success'];
                            } else {
                                $error_message = $LANG['error_updating_password'] . $stmt_update->error;
                            }
                            $stmt_update->close();
                        } else {
                             $error_message = $LANG['error_preparing_update'] . $conn->error;
                        }
                    } else {
                        $error_message = $LANG['error_incorrect_current_password'];
                    }
                } else {
                    $error_message = $LANG['error_admin_user_not_found'];
                }
                $stmt_select->close();
             } else {
                 $error_message = $LANG['error_preparing_select'] . $conn->error;
             }
        }
    }
}

$conn->close();
?>
<!DOCTYPE html>
<!-- Set html lang attribute based on selected language -->
<html lang="<?php echo ($language == 'zh-TW') ? 'zh-Hant' : 'en'; ?>">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- Update title using LANG variable -->
    <title><?php echo $LANG['page_title_settings']; ?></title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons+Outlined" rel="stylesheet">
    <link rel="stylesheet" href="css/styles.css">
    <link rel="stylesheet" href="css/sidebar.css">
    <link rel="stylesheet" href="css/header.css">
</head>
<body>
    <div class="grid-container">
        <?php 
        // $page_title is likely set within header.php now based on $LANG, or needs to be passed
        // Assuming header.php will handle its own title based on $LANG 
        include 'includes/header.php'; 
        ?>
        <?php include 'includes/sidebar.php'; ?>
        <main class="main-container">
             <h3 class="mb-4"><?php echo $LANG['account_settings_title']; ?></h3>

             <div class="card">
                 <div class="card-body">
                    <h5 class="card-title"><?php echo $LANG['change_password_title']; ?></h5>
                    
                    <?php if ($update_message): ?>
                        <div class="alert alert-success" role="alert">
                            <?php echo $update_message; ?>
                        </div>
                    <?php endif; ?>
                    <?php if ($error_message): ?>
                        <div class="alert alert-danger" role="alert">
                            <?php echo $error_message; ?>
                        </div>
                    <?php endif; ?>

                    <form action="<?php echo htmlspecialchars($_SERVER["PHP_SELF"]); ?>" method="post">
                        <div class="form-group">
                            <label for="current_password"><?php echo $LANG['current_password_label']; ?></label>
                            <input type="password" class="form-control" id="current_password" name="current_password" required>
                        </div>
                        <div class="form-group">
                            <label for="new_password"><?php echo $LANG['new_password_label']; ?></label>
                            <input type="password" class="form-control" id="new_password" name="new_password" required>
                        </div>
                        <div class="form-group">
                            <label for="confirm_password"><?php echo $LANG['confirm_password_label']; ?></label>
                            <input type="password" class="form-control" id="confirm_password" name="confirm_password" required>
                        </div>
                        <button type="submit" class="btn btn-primary"><?php echo $LANG['update_password_button']; ?></button>
                    </form>
                 </div>
             </div>

             <!-- Language Settings Card -->
             <div class="card mt-4">
                 <div class="card-body">
                    <h5 class="card-title"><?php echo $LANG['language_settings_title'] ?? 'Language Settings'; ?></h5>
                    <p><?php echo $LANG['current_language_label'] ?? 'Current Language'; ?>: 
                        <strong><?php echo ($language == 'zh-TW') ? ($LANG['switch_to_zh'] ?? '繁體中文') : ($LANG['switch_to_en'] ?? 'English'); ?></strong>
                    </p>

                    <?php
                    // --- Language Switcher Logic (from header) ---
                    $current_url = basename($_SERVER['PHP_SELF']);
                    $query_params = $_GET;
                    
                    // Link for English
                    $query_params['lang'] = 'en';
                    $en_link = $current_url . '?' . http_build_query($query_params);
                    
                    // Link for Traditional Chinese
                    $query_params['lang'] = 'zh-TW';
                    $zh_link = $current_url . '?' . http_build_query($query_params);
                    ?>
                    
                    <div class="mt-3">
                        <?php echo $LANG['select_language_label'] ?? 'Select Language'; ?>:
                        <?php if ($language !== 'en'): ?>
                            <a href="<?php echo htmlspecialchars($en_link); ?>" class="btn btn-outline-secondary btn-sm ml-2"><?php echo $LANG['switch_to_en'] ?? 'English'; ?></a>
                        <?php else: ?>
                            <button class="btn btn-secondary btn-sm ml-2" disabled><?php echo $LANG['switch_to_en'] ?? 'English'; ?></button>
                        <?php endif; ?>
                        
                        <?php if ($language !== 'zh-TW'): ?>
                            <a href="<?php echo htmlspecialchars($zh_link); ?>" class="btn btn-outline-secondary btn-sm ml-1"><?php echo $LANG['switch_to_zh'] ?? '繁體中文'; ?></a>
                        <?php else: ?>
                            <button class="btn btn-secondary btn-sm ml-1" disabled><?php echo $LANG['switch_to_zh'] ?? '繁體中文'; ?></button>
                        <?php endif; ?>
                    </div>

                 </div>
            </div>
             
             <!-- Add other settings sections below if needed -->

        </main>
    </div>
</body>
</html> 
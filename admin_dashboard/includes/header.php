<header class="header">
    <div class="menu-icon" onclick="openSidebar()">
        <span class="material-icons-outlined">menu</span>
    </div>
    <div class="header-title">
        <?php
        $header_current_page = basename($_SERVER['PHP_SELF']);
        
        switch($header_current_page) {
            case 'index.php':
                echo 'Dashboard';
                break;
            case 'matching.php':
                echo 'Matching Management';
                break;
            case 'applications.php':
                echo 'Applications Management';
                break;
            case 'payment.php':
                echo 'Payment Management';
                break;
            case 'verify_cv.php':
                echo 'Verify CV';
                break;
            case 'ads.php':
                echo 'Ad Management';
                break;
            case 'member.php':
                echo 'Member Management';
                break;
            case 'reports.php':
                echo 'Reports';
                break;
            case 'settings.php':
                echo 'Settings';
                break;
            case 'member_main.php':
                // 引入資料庫設定 (修正路徑)
                require_once($_SERVER['DOCUMENT_ROOT'] . '/admin_dashboard/includes/config.php');
                
                // 建立資料庫連線
                $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);
                
                // 檢查連線
                if ($conn->connect_error) {
                    die("資料庫連線失敗: " . $conn->connect_error);
                }
                
                // 使用準備好的語句防止SQL注入
                $stmt = $conn->prepare("SELECT username, email FROM member WHERE member_id = ?");
                $stmt->bind_param("i", $_GET['id']);
                $stmt->execute();
                $result = $stmt->get_result();
                
                if($result->num_rows > 0) {
                    $row = $result->fetch_assoc();
                    $member_id = $_GET['id'];
                    $username = $row['username'] ?? 'N/A';
                    $email = $row['email'] ?? 'N/A';
                    echo 'Member: ' . $member_id . ' | Username: ' . $username . ' | Email: ' . $email;
                } else {
                    echo 'Member Not Found';
                }
                
                $stmt->close();
                $conn->close();
                break;
            default:
                echo 'CircleA Admin';
        }
        ?>
    </div>
    <div class="user-controls">
        <span class="material-icons-outlined">account_circle</span>
        <span class="user-email"><?php echo htmlspecialchars($_SESSION['email'] ?? 'admin'); ?></span>
        
        <a href="logout.php" class="logout-link" title="Logout">
            <span class="material-icons-outlined">logout</span>
        </a>
    </div>
</header>

<script>
function openSidebar() {
    document.getElementById('sidebar').classList.add('sidebar-responsive');
}
</script>
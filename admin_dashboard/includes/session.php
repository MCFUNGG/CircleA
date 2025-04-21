<?php
// 確保已經開始 session
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

// 定義登入頁面路徑（如果不在根目錄，請修改）
$login_page = 'login.html'; 

// 檢查用戶是否已登入
if (!isset($_SESSION['loggedin']) || $_SESSION['loggedin'] !== true) {
    header("Location: {$login_page}?error=not_logged_in"); 
    exit();
}

// **新增**: 檢查 member_id 是否設置且大於 0
if (!isset($_SESSION['member_id']) || intval($_SESSION['member_id']) <= 0) {
     // 如果 member_id 無效或不存在，重新導向到登入頁面
     header("Location: {$login_page}?error=invalid_member_id"); 
     exit();
}

// 如果所有檢查都通過，腳本將繼續執行引入此文件的頁面
?> 
<?php
session_start(); // 開始會話
$_SESSION = []; // 清空會話變數
session_destroy(); // 銷毀會話
header("Location: login.html"); // 重定向到登錄頁面
exit();
?>
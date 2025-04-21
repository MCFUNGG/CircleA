<?php
require_once 'D:/xampp/htdocs/vendor/autoload.php';

use Kreait\Firebase\Factory;
use Kreait\Firebase\Messaging\CloudMessage;
use Kreait\Firebase\Messaging\Notification;

class FCMHelper {
    private $messaging;
    
    public function __construct() {
        try {
            $factory = (new Factory)
                ->withServiceAccount('D:/xampp/htdocs/firebase-service-account.json');
            
            $this->messaging = $factory->createMessaging();
        } catch (Exception $e) {
            error_log('Firebase initialization error: ' . $e->getMessage());
            throw $e;
        }
    }
    
    public function sendNotification($token, $title, $body, $data = []) {
        try {
            if (empty($token)) {
                error_log('FCM token is empty');
                return false;
            }
            
            $message = CloudMessage::withTarget('token', $token)
                ->withNotification(Notification::create($title, $body))
                ->withData($data);
            
            $this->messaging->send($message);
            error_log('FCM notification sent successfully to token: ' . $token);
            return true;
        } catch (Exception $e) {
            error_log('FCM send error: ' . $e->getMessage());
            return false;
        }
    }
    
    public function getFCMToken($member_id, $conn) {
        try {
            $stmt = $conn->prepare("SELECT token FROM fcm_tokens WHERE member_id = ? ORDER BY updated_at DESC LIMIT 1");
            $stmt->bind_param("s", $member_id);
            $stmt->execute();
            $result = $stmt->get_result();
            
            if ($row = $result->fetch_assoc()) {
                return $row['token'];
            }
            
            return null;
        } catch (Exception $e) {
            error_log('Error getting FCM token: ' . $e->getMessage());
            return null;
        }
    }
} 
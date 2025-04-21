<?php
session_start(); // Start session

include 'includes/session.php';

include 'includes/db_connect.php';

// Handle AJAX requests
if (isset($_POST['ajax_action'])) {
  $action = $_POST['ajax_action'];
  $response = ['success' => false, 'message' => 'Invalid action'];

  switch ($action) {
    case 'add':
    case 'edit':
      // Define $is_new_ad FIRST within this block
      $ad_id = isset($_POST['ad_id']) ? intval($_POST['ad_id']) : 0;
      $is_new_ad = ($action === 'add' || $ad_id === 0);
      
      // Get other form data
      $title = $conn->real_escape_string($_POST['title']);
      $description = isset($_POST['description']) ? $conn->real_escape_string($_POST['description']) : '';
      $link_url = isset($_POST['link_url']) && !empty($_POST['link_url']) ? $conn->real_escape_string($_POST['link_url']) : NULL;
      $status = $conn->real_escape_string($_POST['status']);
      $sort_order = isset($_POST['sort_order']) ? intval($_POST['sort_order']) : 0;
      
      $db_image_url = null; // Initialize variable for DB image path
      
      // --- Modified Image Upload Handling ---
      if (isset($_FILES['ad_image']) && $_FILES['ad_image']['error'] === UPLOAD_ERR_OK && $_FILES['ad_image']['size'] > 0) {
        
        // 1. Define the upload directory relative to THIS script (__DIR__)
        $uploadDirRelativeToScript = '../FYP/ads/'; 
        // Construct the absolute FILESYSTEM path for uploads
        $filesystemUploadDir = realpath(__DIR__ . '/' . $uploadDirRelativeToScript); // Use realpath for robustness

        // 2. Define the WEB path prefix to store in DB (relative to web root)
        //    IMPORTANT: Ensure this path correctly corresponds to the filesystem path above
        //    relative to your document root (e.g., htdocs).
        $dbPathPrefix = '/FYP/ads/'; // Path to store in DB

        // Check if the resolved filesystem directory path is valid
        if ($filesystemUploadDir === false) {
             echo json_encode(['success' => false, 'message' => 'Upload directory path is invalid or does not exist. Check relative path: ' . $uploadDirRelativeToScript]);
             exit();
        }
        $filesystemUploadDir .= '/'; // Add trailing slash after realpath check

        // Create the directory if it doesn't exist (use filesystem path)
        if (!file_exists($filesystemUploadDir)) {
            // Use 0775 or 0755 for permissions usually
            if (!mkdir($filesystemUploadDir, 0775, true)) { 
                 echo json_encode(['success' => false, 'message' => 'Failed to create upload directory. Check permissions. Path: ' . $filesystemUploadDir]);
                 exit();
            }
        } elseif (!is_writable($filesystemUploadDir)) {
             echo json_encode(['success' => false, 'message' => 'Upload directory is not writable. Check permissions. Path: ' . $filesystemUploadDir]);
             exit();
        }

        // Generate unique filename
        $fileName = basename($_FILES["ad_image"]["name"]);
        $fileExtension = pathinfo($fileName, PATHINFO_EXTENSION);
        $uniqueFileName = time() . '_' . uniqid() . '.' . strtolower($fileExtension); 

        // Construct the full FILESYSTEM path for the new file
        $targetFilesystemPath = $filesystemUploadDir . $uniqueFileName;

        // Construct the WEB path to store in the DB
        $targetDbPath = $dbPathPrefix . $uniqueFileName; 

        // Allow certain file formats
        $allowTypes = array('jpg', 'png', 'jpeg', 'gif', 'webp');
        if (in_array(strtolower($fileExtension), $allowTypes)) {
            
            // Attempt to move the uploaded file using the FILESYSTEM path
            if (move_uploaded_file($_FILES["ad_image"]["tmp_name"], $targetFilesystemPath)) {
                // File uploaded successfully, set the DB path
                $db_image_url = $targetDbPath; // Store the web-relative path

                // If editing, delete the old image
                if (!$is_new_ad && $ad_id > 0) {
                    $sql_old_img = "SELECT image_url FROM ads WHERE ad_id = ?";
                    $stmt_old = $conn->prepare($sql_old_img);
                    if($stmt_old){
                        $stmt_old->bind_param("i", $ad_id);
                        $stmt_old->execute();
                        $result_old = $stmt_old->get_result();
                        if ($result_old->num_rows > 0) {
                            $old_row = $result_old->fetch_assoc();
                            $old_db_path = $old_row['image_url']; // e.g., /FYP/ads/old_image.jpg

                            // --- Construct old filesystem path using DOCUMENT_ROOT and DB Path ---
                            // Assumes the DB path is relative to the document root
                            if (!empty($old_db_path)) {
                                $old_filesystem_path = $_SERVER['DOCUMENT_ROOT'] . $old_db_path;
                                // Basic check to prevent deleting outside expected structure
                                if (strpos($old_filesystem_path, realpath($_SERVER['DOCUMENT_ROOT'])) === 0 && file_exists($old_filesystem_path) && is_file($old_filesystem_path)) {
                                    unlink($old_filesystem_path); // Delete old image file
                                }
                            }
                            // --- End construct old filesystem path ---
                        }
                        $stmt_old->close();
                    }
                }
            } else {
                echo json_encode(['success' => false, 'message' => 'Error uploading file to server. Check path and permissions: ' . $targetFilesystemPath]);
                exit();
            }
        } else {
            echo json_encode(['success' => false, 'message' => 'Only JPG, JPEG, PNG, GIF, and WEBP files are allowed']);
            exit();
        }
      } else if ($is_new_ad) {
        // New ad requires an image
        // Check if there was an upload error other than no file submitted
         if (isset($_FILES['ad_image']) && $_FILES['ad_image']['error'] !== UPLOAD_ERR_NO_FILE) {
              echo json_encode(['success' => false, 'message' => 'Image upload failed with error code: ' . $_FILES['ad_image']['error']]);
              exit();
         } else {
              echo json_encode(['success' => false, 'message' => 'Image is required for new advertisements']);
              exit();
         }
      }
      // --- End Modified Image Upload Handling ---
      
      // --- Database Insert/Update Logic (Using $db_image_url which holds the web path) ---
      if ($is_new_ad) {
        if ($db_image_url === null) {
             echo json_encode(['success' => false, 'message' => 'Image upload failed or missing for new ad.']);
             exit();
        }
        $link_value = $link_url === NULL ? NULL : $link_url; 
        $sql = "INSERT INTO ads (title, description, image_url, link_url, status, sort_order) VALUES (?, ?, ?, ?, ?, ?)";
        $stmt = $conn->prepare($sql);
        if($stmt){
             $stmt->bind_param("sssssi", $title, $description, $db_image_url, $link_value, $status, $sort_order); 
        } else { /* Handle prepare error */ echo json_encode(['success' => false, 'message' => 'DB prepare error (INSERT): ' . $conn->error]); exit(); }
      } else {
        // Update existing ad
        $link_value = $link_url === NULL ? NULL : $link_url;
        if ($db_image_url !== null) { // New image uploaded
            $sql = "UPDATE ads SET title = ?, description = ?, link_url = ?, status = ?, sort_order = ?, image_url = ? WHERE ad_id = ?";
            $stmt = $conn->prepare($sql);
             if($stmt){ $stmt->bind_param("ssssisi", $title, $description, $link_value, $status, $sort_order, $db_image_url, $ad_id); } 
             else { /* Handle prepare error */ echo json_encode(['success' => false, 'message' => 'DB prepare error (UPDATE with image): ' . $conn->error]); exit(); }
        } else { // No new image
            $sql = "UPDATE ads SET title = ?, description = ?, link_url = ?, status = ?, sort_order = ? WHERE ad_id = ?";
            $stmt = $conn->prepare($sql);
             if($stmt){ $stmt->bind_param("ssssii", $title, $description, $link_value, $status, $sort_order, $ad_id); } 
             else { /* Handle prepare error */ echo json_encode(['success' => false, 'message' => 'DB prepare error (UPDATE without image): ' . $conn->error]); exit(); }
        }
      }
      
      // Execute the prepared statement
      if ($stmt && $stmt->execute()) {
        echo json_encode(['success' => true, 'message' => $is_new_ad ? 'Advertisement added successfully' : 'Advertisement updated successfully']);
      } else {
        echo json_encode(['success' => false, 'message' => 'DB Error: ' . ($stmt ? $stmt->error : $conn->error)]);
      }
      if(isset($stmt)) $stmt->close(); 
      exit(); 
      
    case 'delete':
      // --- Modified Delete Logic ---
      $ad_id = intval($_POST['ad_id']);
      
      // Get the image DB path to delete the file
      $sql_get = "SELECT image_url FROM ads WHERE ad_id = ?";
      $stmt_get = $conn->prepare($sql_get);
      
      if ($stmt_get) {
          $stmt_get->bind_param("i", $ad_id);
          $stmt_get->execute();
          $result = $stmt_get->get_result();
          
          if ($result->num_rows > 0) {
              $row = $result->fetch_assoc();
              $db_path = $row['image_url']; // e.g., /FYP/ads/image.jpg
              
              // Delete from database first
              $sql_delete = "DELETE FROM ads WHERE ad_id = ?";
              $stmt_delete = $conn->prepare($sql_delete);
              if ($stmt_delete) {
                  $stmt_delete->bind_param("i", $ad_id);
                  if ($stmt_delete->execute()) {
                      // DB delete successful, now delete the file
                      if (!empty($db_path)) {
                           // Construct filesystem path from DOCUMENT_ROOT and DB Path
                           $filesystem_path = $_SERVER['DOCUMENT_ROOT'] . $db_path;
                           // Basic security check and file existence check
                           if (strpos($filesystem_path, realpath($_SERVER['DOCUMENT_ROOT'])) === 0 && file_exists($filesystem_path) && is_file($filesystem_path)) {
                                unlink($filesystem_path); 
                           }
                      }
                      echo json_encode(['success' => true, 'message' => 'Advertisement deleted successfully']);
                  } else {
                      echo json_encode(['success' => false, 'message' => 'Error deleting advertisement from DB: ' . $stmt_delete->error]);
                  }
                   $stmt_delete->close();
              } else {
                   echo json_encode(['success' => false, 'message' => 'Error preparing delete statement: ' . $conn->error]);
              }
          } else {
              echo json_encode(['success' => false, 'message' => 'Advertisement not found for deletion.']);
          }
          $stmt_get->close();
      } else {
           echo json_encode(['success' => false, 'message' => 'Error preparing select statement for delete: ' . $conn->error]);
      }
      exit();
      // --- End Modified Delete Logic ---
      
    case 'get':
      // Get ad details - Ensure image_url path is correctly formed if needed for display here
      // This logic likely doesn't need changes unless you display image paths directly from this response
      $ad_id = intval($_POST['ad_id']);
      $sql = "SELECT * FROM ads WHERE ad_id = ?";
      $stmt_get = $conn->prepare($sql);
       if($stmt_get){
           $stmt_get->bind_param("i", $ad_id);
           $stmt_get->execute();
           $result = $stmt_get->get_result();
            if ($result->num_rows > 0) {
                $ad_data = $result->fetch_assoc();
                // Optionally ensure the image_url starts with '/' if consistency is needed
                // if (isset($ad_data['image_url']) && substr($ad_data['image_url'], 0, 1) !== '/') {
                //     $ad_data['image_url'] = '/' . ltrim($ad_data['image_url'], '/');
                // }
                echo json_encode(['success' => true, 'data' => $ad_data]);
            } else {
                echo json_encode(['success' => false, 'message' => 'Advertisement not found']);
            }
            $stmt_get->close();
       } else {
            echo json_encode(['success' => false, 'message' => 'Error preparing get statement: ' . $conn->error]);
       }
      exit();
  }
}

// Get all advertisements for display - Ensure image URLs are displayed correctly
$advertisements = [];
// Use prepared statement or ensure $conn is valid
if ($conn) {
    $sql_ads = "SELECT * FROM ads ORDER BY sort_order ASC, created_at DESC";
    $result_ads = $conn->query($sql_ads);
    
    if ($result_ads) {
        while ($row = $result_ads->fetch_assoc()) {
            // Optionally ensure image_url starts with '/' for consistency in display
             // if (isset($row['image_url']) && substr($row['image_url'], 0, 1) !== '/') {
             //     $row['image_url'] = '/' . ltrim($row['image_url'], '/');
             // }
            $advertisements[] = $row;
        }
         $result_ads->free();
    } else {
        // Handle query error for displaying list
        $message .= "<div class='alert alert-danger'>Error fetching advertisement list: " . $conn->error . "</div>";
    }
} else {
    $message .= "<div class='alert alert-danger'>Database connection error. Cannot fetch advertisements.</div>";
}

if ($conn instanceof mysqli) { // Check if connection is still open before closing
    $conn->close(); 
}
?>

<!DOCTYPE html>
<html lang="en">

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Ad Management</title>

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
    // Set current page title
    $page_title = "Ad Management";
    include 'includes/header.php';
    ?>
    <!-- Sidebar -->
    <?php include 'includes/sidebar.php'; ?>
    <!-- End Sidebar -->

    <!-- Main -->
    <main class="main-container">

      <!-- Advertisement Management Section -->
      <div class="ad-management mt-4">
        <div class="row mb-4">
          <div class="col-12">
            <div class="card">
              <div class="card-header bg-primary text-white">
                <h5 class="mb-0">Add/Edit Advertisement</h5>
              </div>
              <div class="card-body">
                <!-- Add New Advertisement Form -->
                <form id="adForm" method="post" enctype="multipart/form-data">
                  <input type="hidden" name="ajax_action" id="ajax_action" value="add">
                  <input type="hidden" name="ad_id" id="ad_id" value="">
                  
                  <div class="form-row">
                    <div class="form-group col-md-6">
                      <label for="title">Ad Title</label>
                      <input type="text" class="form-control" id="title" name="title" required>
                    </div>
                    <div class="form-group col-md-6">
                      <label for="link_url">Ad Link</label>
                      <input type="url" class="form-control" id="link_url" name="link_url">
                      <small class="form-text text-muted">Optional. Enter a URL if the ad should link to a page.</small>
                    </div>
                  </div>
                  
                  <div class="form-group">
                    <label for="description">Ad Description</label>
                    <textarea class="form-control" id="description" name="description" rows="3"></textarea>
                  </div>
                  
                  <div class="form-group">
                    <label for="ad_image">Ad Image</label>
                    <input type="file" class="form-control-file" id="ad_image" name="ad_image" accept="image/*">
                    <small class="form-text text-muted">Recommended size: 1200 x 628 pixels</small>
                    <div id="imagePreview" class="mt-2" style="display:none">
                      <img id="previewImg" src="#" alt="Preview" style="max-width:300px; max-height:200px">
                      <button type="button" class="btn btn-sm btn-outline-danger ml-2" id="removeImage">Remove</button>
                    </div>
                  </div>
                  
                  <div class="form-row">
                    <div class="form-group col-md-6">
                      <label for="status">Status</label>
                      <select class="form-control" id="status" name="status">
                        <option value="active">Active</option>
                        <option value="inactive">Inactive</option>
                      </select>
                    </div>
                    <div class="form-group col-md-6">
                      <label for="sort_order">Display Order</label>
                      <input type="number" class="form-control" id="sort_order" name="sort_order" value="0" min="0">
                      <small class="form-text text-muted">Lower numbers display first (0 is highest priority)</small>
                    </div>
                  </div>
                  
                  <div class="form-group">
                    <button type="submit" class="btn btn-primary" id="submitBtn">Add Advertisement</button>
                    <button type="button" class="btn btn-secondary" id="resetBtn">Reset</button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Advertisement List -->
        <div class="row">
          <div class="col-12">
            <div class="card">
              <div class="card-header bg-primary text-white">
                <h5 class="mb-0">Advertisement List</h5>
              </div>
              <div class="card-body">
                <div class="alert alert-info mt-2 mb-4">
                  <h5><i class="fas fa-info-circle"></i> Display Order Information</h5>
                  <p>Advertisements are displayed in the app based on their order value. Lower numbers will be shown first.</p>
                  <ul>
                    <li>Use <strong>0</strong> for the highest priority ads (shown first)</li>
                    <li>Use higher numbers for lower priority ads</li>
                    <li>Ads with the same order value will be sorted by creation date (newest first)</li>
                  </ul>
                </div>
                <div class="table-responsive">
                  <table class="table table-bordered table-hover">
                    <thead class="thead-light">
                      <tr>
                        <th>ID</th>
                        <th>Preview</th>
                        <th>Title</th>
                        <th>Description</th>
                        <th>Link</th>
                        <th>Status</th>
                        <th>Order</th>
                        <th>Created Date</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody id="adListBody">
                      <?php if (count($advertisements) > 0): ?>
                        <?php foreach ($advertisements as $ad): ?>
                          <tr>
                            <td><?php echo $ad['ad_id']; ?></td>
                            <td>
                              <?php // Use the DB path directly, assuming it's web-accessible ?>
                              <a href="<?= htmlspecialchars($ad['image_url'] ?? '#') ?>" target="_blank">
                                <img src="<?= htmlspecialchars($ad['image_url'] ?? '#') ?>" alt="<?= htmlspecialchars($ad['title'] ?? '') ?>" style="width:100px; height:auto; object-fit: cover;" 
                                     onerror="this.style.display='none'; this.onerror=null;"> <?php // Basic error handling ?>
                              </a>
                            </td>
                             <!-- Other columns remain the same -->
                            <td><?php echo htmlspecialchars($ad['title'] ?? ''); ?></td>
                            <td><?php echo htmlspecialchars($ad['description'] ? $ad['description'] : '-'); ?></td>
                            <td>
                              <?php if (!empty($ad['link_url'])): ?>
                                <a href="<?php echo htmlspecialchars($ad['link_url']); ?>" target="_blank"><?php echo htmlspecialchars($ad['link_url']); ?></a>
                              <?php else: ?>
                                <span class="text-muted">No link</span>
                              <?php endif; ?>
                            </td>
                            <td><?php echo $ad['status'] === 'active' ? '<span class="badge badge-success">Active</span>' : '<span class="badge badge-secondary">Inactive</span>'; ?></td>
                            <td><?php echo htmlspecialchars($ad['sort_order'] ?? 0); ?></td>
                            <td><?php echo date('Y-m-d H:i:s', strtotime($ad['created_at'] ?? time())); ?></td>
                            <td>
                              <button type="button" class="btn btn-sm btn-primary edit-ad" data-id="<?php echo $ad['ad_id']; ?>">Edit</button>
                              <button type="button" class="btn btn-sm btn-danger delete-ad" data-id="<?php echo $ad['ad_id']; ?>">Delete</button>
                            </td>
                          </tr>
                        <?php endforeach; ?>
                      <?php else: ?>
                        <tr>
                          <td colspan="9" class="text-center">No advertisements found</td>
                        </tr>
                      <?php endif; ?>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
    <!-- End Main -->

  </div>

  <!-- Image Preview Modal -->
  <div class="modal fade" id="imagePreviewModal" tabindex="-1" role="dialog" aria-labelledby="imagePreviewModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="imagePreviewModalLabel">Image Preview</h5>
          <button type="button" class="close" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
        <div class="modal-body text-center">
          <img id="modalImage" src="" alt="Full size image" style="max-width:100%;">
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
        </div>
      </div>
    </div>
  </div>

  <!-- Scripts -->
  <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.1/dist/umd/popper.min.js"></script>
  <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>
  
  <!-- Ad Management Script -->
  <script>
    $(document).ready(function() {
      // Image preview for form
      $('#ad_image').change(function() {
        const file = this.files[0];
        if (file) {
          const reader = new FileReader();
          reader.onload = function(e) {
            $('#previewImg').attr('src', e.target.result);
            $('#imagePreview').show();
          };
          reader.readAsDataURL(file);
        }
      });
      
      // Remove image preview
      $('#removeImage').click(function() {
        $('#ad_image').val('');
        $('#imagePreview').hide();
      });
      
      // Preview image in modal when clicked in the table
      $('.preview-image').click(function() {
        const imageUrl = $(this).data('image');
        const imageTitle = $(this).data('title');
        
        $('#modalImage').attr('src', imageUrl);
        $('#imagePreviewModalLabel').text(imageTitle);
        $('#imagePreviewModal').modal('show');
      });
      
      // Reset form
      $('#resetBtn').click(function() {
        resetForm();
      });
      
      // Edit advertisement
      $('.edit-ad').click(function() {
        const adId = $(this).data('id');
        
        $.ajax({
          url: 'ads.php',
          type: 'POST',
          data: {
            ajax_action: 'get',
            ad_id: adId
          },
          dataType: 'json',
          success: function(response) {
            if (response.success) {
              const ad = response.data;
              $('#ad_id').val(ad.ad_id);
              $('#title').val(ad.title);
              $('#description').val(ad.description || '');
              $('#link_url').val(ad.link_url || '');
              $('#status').val(ad.status);
              $('#sort_order').val(ad.sort_order || 0);
              $('#ajax_action').val('edit');
              $('#submitBtn').text('Update Advertisement');
              
              // Show image preview
              if(ad.image_url) {
                $('#previewImg').attr('src', ad.image_url);
                $('#imagePreview').show();
              } else {
                $('#imagePreview').hide();
              }
              
              // Ad image is not required when editing
              $('#ad_image').removeAttr('required');
              
              // Scroll to form
              $('html, body').animate({
                scrollTop: $('#adForm').offset().top - 100
              }, 500);
            } else {
              alert(response.message);
            }
          },
          error: function(xhr, status, error) {
            alert('Error occurred: ' + error);
          }
        });
      });
      
      // Delete advertisement
      $('.delete-ad').click(function() {
        if (confirm('Are you sure you want to delete this advertisement?')) {
          const adId = $(this).data('id');
          
          $.ajax({
            url: 'ads.php',
            type: 'POST',
            data: {
              ajax_action: 'delete',
              ad_id: adId
            },
            dataType: 'json',
            success: function(response) {
              alert(response.message);
              if (response.success) {
                location.reload(); // Reload the page
              }
            },
            error: function(xhr, status, error) {
              alert('Error occurred: ' + error);
            }
          });
        }
      });
      
      // Form submission
      $('#adForm').submit(function(e) {
        e.preventDefault();
        
        const formData = new FormData(this);
        const action = $('#ajax_action').val();
        const adId = $('#ad_id').val();
        
        // Validate image for new advertisements
        if (action === 'add' && document.getElementById('ad_image').files.length === 0) {
          alert('Please select an image for the advertisement');
          return false;
        }
        
        $.ajax({
          url: 'ads.php',
          type: 'POST',
          data: formData,
          contentType: false,
          processData: false,
          dataType: 'json',
          success: function(response) {
            alert(response.message);
            if (response.success) {
              resetForm();
              location.reload(); // Reload the page
            }
          },
          error: function(xhr, status, error) {
            alert('Error occurred: ' + error);
          }
        });
      });
      
      // Function to reset the form
      function resetForm() {
        $('#adForm')[0].reset();
        $('#ad_id').val('');
        $('#ajax_action').val('add');
        $('#submitBtn').text('Add Advertisement');
        $('#imagePreview').hide();
        $('#ad_image').attr('required', 'required');
      }
    });
  </script>
  
  <!-- Custom JS -->
  <script src="js/scripts.js"></script>
</body>

</html>
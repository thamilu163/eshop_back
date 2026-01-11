package com.eshop.app.service;

import com.eshop.app.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

/**
 * Secure File Upload Service
 * <p>
 * Provides comprehensive validation and security measures for file uploads:
 * <ul>
 *   <li><b>File Type Validation:</b> MIME type detection using Apache Tika</li>
 *   <li><b>Size Validation:</b> Enforces maximum file size limits</li>
 *   <li><b>Dimension Validation:</b> Validates image dimensions</li>
 *   <li><b>Path Traversal Prevention:</b> Sanitizes filenames</li>
 *   <li><b>Content Validation:</b> Verifies actual file content matches declared type</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecureFileUploadService {
    
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    );
    
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "webp"
    );
    
    @Value("${app.upload.max-file-size:5242880}") // 5MB default
    private long maxFileSize;
    
    @Value("${app.upload.max-image-width:4096}")
    private int maxImageWidth;
    
    @Value("${app.upload.max-image-height:4096}")
    private int maxImageHeight;
    
    private final Tika tika = new Tika();
    
    /**
     * Validate uploaded image file
     * 
     * @param file The multipart file to validate
     * @throws FileUploadException if validation fails
     */
    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is empty or null");
        }
        
        // Validate file size
        if (file.getSize() > maxFileSize) {
            throw new FileUploadException(
                String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)", 
                    file.getSize(), maxFileSize)
            );
        }
        
        // Sanitize and validate filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new FileUploadException("Filename is missing");
        }
        
        String cleanFilename = StringUtils.cleanPath(originalFilename);
        
        // Prevent path traversal attacks
        if (cleanFilename.contains("..")) {
            throw new FileUploadException("Filename contains invalid path sequence");
        }
        
        // Validate file extension
        String extension = getFileExtension(cleanFilename).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new FileUploadException(
                String.format("File extension '%s' is not allowed. Allowed: %s", 
                    extension, ALLOWED_IMAGE_EXTENSIONS)
            );
        }
        
        try {
            // Detect actual MIME type from file content (not from header)
            String detectedMimeType = tika.detect(file.getInputStream());
            
            if (!ALLOWED_IMAGE_MIME_TYPES.contains(detectedMimeType)) {
                throw new FileUploadException(
                    String.format("File type '%s' is not allowed. Allowed: %s",
                        detectedMimeType, ALLOWED_IMAGE_MIME_TYPES)
                );
            }
            
            // Verify declared type matches detected type
            String declaredMimeType = file.getContentType();
            if (declaredMimeType != null && !detectedMimeType.equals(declaredMimeType)) {
                log.warn("MIME type mismatch: declared={}, detected={}", 
                    declaredMimeType, detectedMimeType);
                // Continue with detected type (more reliable)
            }
            
            // Validate image content and dimensions
            validateImageContent(file);
            
        } catch (IOException e) {
            log.error("Error validating file: {}", e.getMessage(), e);
            throw new FileUploadException("Failed to validate file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate image content and dimensions
     */
    private void validateImageContent(MultipartFile file) throws IOException {
        byte[] content = file.getBytes();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
        
        if (image == null) {
            throw new FileUploadException("File is not a valid image");
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        if (width > maxImageWidth || height > maxImageHeight) {
            throw new FileUploadException(
                String.format("Image dimensions (%dx%d) exceed maximum allowed (%dx%d)",
                    width, height, maxImageWidth, maxImageHeight)
            );
        }
        
        if (width < 1 || height < 1) {
            throw new FileUploadException("Invalid image dimensions");
        }
        
        log.debug("Image validation passed: {}x{}, size={} bytes", 
            width, height, file.getSize());
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
    
    /**
     * Generate safe filename
     */
    public String generateSafeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "unnamed_" + System.currentTimeMillis();
        }
        
        String cleanName = StringUtils.cleanPath(originalFilename);
        String extension = getFileExtension(cleanName);
        String nameWithoutExt = cleanName.substring(0, cleanName.lastIndexOf('.'));
        
        // Remove special characters, keep only alphanumeric, dash, and underscore
        String safeName = nameWithoutExt.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        // Limit length
        if (safeName.length() > 100) {
            safeName = safeName.substring(0, 100);
        }
        
        // Add timestamp to ensure uniqueness
        return String.format("%s_%d.%s", safeName, System.currentTimeMillis(), extension);
    }
}

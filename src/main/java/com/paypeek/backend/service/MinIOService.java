package com.paypeek.backend.service;

import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.minio.http.Method;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MinIOService {

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucketName;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(minioUrl)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket {} created successfully", bucketName);
            }
        } catch (Exception e) {
            log.error("Error initializing MinIO", e);
            throw new RuntimeException("Error initializing MinIO", e);
        }
    }

    /**
     * Upload file from MultipartFile
     */
    public String uploadFile(MultipartFile file, String filePath) {
        try {
            String objectName = filePath + "/" + System.currentTimeMillis() + "-" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), 10485760)
                            .contentType(file.getContentType())
                            .build());

            log.info("File uploaded successfully: {}", objectName);

            return generatePresignedUrl(objectName);
        } catch (Exception e) {
            log.error("Error uploading file to MinIO", e);
            throw new RuntimeException("Error uploading file to MinIO: " + e.getMessage());
        }
    }

    public String generatePresignedUrl(String objectName) throws Exception {
        try {
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(7, TimeUnit.DAYS)
                            .build());

            log.info("Presigned URL generated for 7 days: {}", objectName);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Error generating presigned URL for: {}", objectName, e);
            throw e;
        }
    }


    public String extractObjectName(String urlOrObjectName) {
        if (urlOrObjectName.contains("http")) {
            return urlOrObjectName
                    .substring(urlOrObjectName.indexOf(bucketName) + bucketName.length() + 1)
                    .split("\\?")[0];
        }
        return urlOrObjectName;
    }


    /**
     * Upload byte array as file
     */
    public String uploadByteArray(byte[] fileBytes, String objectName) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, fileBytes.length, 10485760) // 10MB default part size
                            .contentType("application/octet-stream")
                            .build());

            log.info("Byte array uploaded successfully: {}", objectName);
            return generateFileUrl(objectName);
        } catch (Exception e) {
            log.error("Error uploading byte array to MinIO", e);
            throw new RuntimeException("Error uploading byte array to MinIO: " + e.getMessage());
        }
    }

    /**
     * Upload payslip from InputStream
     */
    public String uploadPayslip(InputStream inputStream, String filename, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(inputStream, -1, 10485760) // 10MB default part size
                            .contentType(contentType)
                            .build());

            log.info("Payslip uploaded successfully: {}", filename);
            return filename; // Return object name as path/url key
        } catch (Exception e) {
            log.error("Error uploading payslip to MinIO", e);
            throw new RuntimeException("Error uploading payslip to MinIO: " + e.getMessage());
        }
    }

    /**
     * Get file as InputStream
     */
    public InputStream getFile(String filename) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build());

            log.info("File retrieved successfully: {}", filename);
            return stream;
        } catch (Exception e) {
            log.error("Error getting file from MinIO", e);
            throw new RuntimeException("Error getting file from MinIO: " + e.getMessage());
        }
    }

    /**
     * Delete file from MinIO
     */
    public void deleteFile(String presignedUrlOrObjectName) {
        try {
            String objectName = extractObjectName(presignedUrlOrObjectName);
            objectName = sanitizeObjectName(objectName);

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());

            log.info("File deleted successfully: {}", objectName);
        } catch (Exception e) {
            log.error("Error deleting file from MinIO: {}", presignedUrlOrObjectName, e);
        }
    }

    private String sanitizeObjectName(String objectName) {
        if (objectName == null) return null;

        // Rimuovi query parameters (? e tutto dopo)
        objectName = objectName.split("\\?")[0];

        // Sostituisci caratteri non supportati
        objectName = objectName.replaceAll("[^a-zA-Z0-9/._-]", "_");

        // Rimuovi spazi multipli
        objectName = objectName.replaceAll("\\s+", "_");

        // Rimuovi slash iniziali/finali
        objectName = objectName.replaceAll("^/+", "").replaceAll("/+$", "");

        log.debug("Sanitized objectName: {} → {}", objectName);
        return objectName;
    }


    /**
     * Delete file by URL (extracts object name from URL)
     */
    public void deleteFileByUrl(String fileUrl) {
        try {
            // Extract object name from URL if needed
            // e.g., from "http://minio:9000/bucket/users/507f/profile.jpg" get "users/507f/profile.jpg"
            String objectName = extractObjectNameFromUrl(fileUrl);
            deleteFile(objectName);
        } catch (Exception e) {
            log.error("Error deleting file by URL: {}", fileUrl, e);
            throw new RuntimeException("Error deleting file by URL: " + e.getMessage());
        }
    }

    /**
     * Generate full file URL from object name
     */
    private String generateFileUrl(String objectName) {
        return minioUrl + "/" + bucketName + "/" + objectName;
    }

    /**
     * Extract object name from full URL
     */
    private String extractObjectNameFromUrl(String fileUrl) {
        // Remove protocol and bucket from URL
        // e.g., "http://minio:9000/bucket/path/to/file.jpg" -> "path/to/file.jpg"
        if (fileUrl.contains(bucketName + "/")) {
            return fileUrl.substring(fileUrl.indexOf(bucketName + "/") + bucketName.length() + 1);
        }
        return fileUrl;
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            return true;
        } catch (Exception e) {
            log.warn("File not found: {}", objectName);
            return false;
        }
    }
}

package com.anverraglobal.insurance.controller;

import com.anverraglobal.insurance.integration.storage.BlobStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final BlobStorageService blobStorageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Only JPEG, PNG and WEBP images are allowed."
                ));
            }

            // Validate file size (e.g. max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "File size exceeds the 5MB limit."
                ));
            }

            String fileUrl = blobStorageService.uploadFile(file, file.getOriginalFilename());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", fileUrl
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Failed to upload file: " + e.getMessage()
            ));
        }
    }
}

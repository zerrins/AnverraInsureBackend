package com.anverraglobal.insurance.integration.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalBlobStorageService implements BlobStorageService {

    private final Path fileStorageLocation;

    public LocalBlobStorageService(@Value("${app.storage.local.dir:./uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String fileName) throws IOException {
        String cleanFileName = StringUtils.cleanPath(fileName);
        if (cleanFileName.contains("..")) {
            throw new SecurityException("Filename contains invalid path sequence");
        }
        String uniqueFileName = UUID.randomUUID().toString() + "_" + cleanFileName;
        Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName).normalize();
        
        if (!targetLocation.startsWith(this.fileStorageLocation)) {
            throw new SecurityException("Cannot store file outside current directory.");
        }

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        // Return a local URL path. We will serve this using a WebMvcConfigurer
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(uniqueFileName)
                .toUriString();
        return fileDownloadUri;
    }

    @Override
    public byte[] downloadFile(String fileUrl) throws IOException {
        String fileName = extractAndValidateFileName(fileUrl);
        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
        if (!filePath.startsWith(this.fileStorageLocation)) {
            throw new SecurityException("Cannot read file outside current directory.");
        }
        return Files.readAllBytes(filePath);
    }

    @Override
    public void deleteFile(String fileUrl) throws IOException {
        String fileName = extractAndValidateFileName(fileUrl);
        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
        if (!filePath.startsWith(this.fileStorageLocation)) {
            throw new SecurityException("Cannot delete file outside current directory.");
        }
        Files.deleteIfExists(filePath);
    }

    private String extractAndValidateFileName(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        String cleanFileName = StringUtils.cleanPath(fileName);
        if (cleanFileName.contains("..")) {
            throw new SecurityException("Filename contains invalid path sequence");
        }
        return cleanFileName;
    }
}

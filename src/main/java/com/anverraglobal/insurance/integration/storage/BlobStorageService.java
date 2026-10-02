package com.anverraglobal.insurance.integration.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface BlobStorageService {
    String uploadFile(MultipartFile file, String fileName) throws IOException;
    byte[] downloadFile(String fileUrl) throws IOException;
    void deleteFile(String fileUrl) throws IOException;
}

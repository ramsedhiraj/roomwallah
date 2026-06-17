package com.roomwallah.common.service;

import java.io.InputStream;

public interface ObjectStorageService {
    
    /**
     * Upload a file to object storage.
     *
     * @param inputStream the file input stream
     * @param fileName the original filename
     * @param contentType the file content type (e.g. image/jpeg)
     * @return the public URL of the uploaded file
     */
    String uploadFile(InputStream inputStream, String fileName, String contentType);
    
    /**
     * Delete a file from object storage.
     *
     * @param fileUrl the public URL of the file to delete
     */
    void deleteFile(String fileUrl);
}

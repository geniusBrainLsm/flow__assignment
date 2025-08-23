package com.assignment.fileextension.service;

import com.assignment.fileextension.entity.UploadedFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface StorageService {
    
    /**
     * 파일을 저장합니다.
     */
    UploadedFile storeFile(MultipartFile file) throws IOException;
    
    /**
     * 특정 확장자의 활성 파일들을 삭제합니다.
     */
    void deleteFilesByExtension(String extension);
    
    /**
     * 물리적 파일을 삭제합니다.
     */
    void deletePhysicalFile(Long fileId) throws IOException;
    
    /**
     * 파일의 삭제 예외 설정을 변경합니다.
     */
    void setDeletionException(Long fileId, Boolean deletionException);
    
    
    /**
     * 상태별 파일 목록을 조회합니다.
     */
    List<UploadedFile> getFilesByStatus(UploadedFile.FileStatus status);
    
    /**
     * 확장자별 파일 목록을 조회합니다.
     */
    List<UploadedFile> getFilesByExtension(String extension);
    
    /**
     * ID로 파일을 조회합니다.
     */
    UploadedFile findById(Long fileId);
}
package com.assignment.fileextension.service;

import com.assignment.fileextension.entity.UploadedFile;
import com.assignment.fileextension.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class FileStorageService implements StorageService {
    
    private final UploadedFileRepository uploadedFileRepository;
    
    @Value("${app.file.upload-dir:uploads}")
    private String uploadBaseDir;
    
    @Value("${app.file.max-size:10485760}") // 10MB
    private long maxFileSize;
    
    @Override
    public UploadedFile storeFile(MultipartFile file) throws IOException {
        validateFile(file);
        
        // 파일 저장 경로 생성 (년/월/일 구조)
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path uploadPath = Paths.get(uploadBaseDir, datePath);
        
        // 디렉토리 생성
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String storedFilename = generateUniqueFilename(extension);
        
        // 파일 저장
        Path filePath = uploadPath.resolve(storedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // 메타데이터 저장
        UploadedFile uploadedFile = UploadedFile.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .filePath(filePath.toString())
                .extension(extension)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .status(UploadedFile.FileStatus.ACTIVE)
                .build();
        
        UploadedFile saved = uploadedFileRepository.save(uploadedFile);
        
        log.info("파일 저장 완료: {} -> {}", originalFilename, filePath);
        return saved;
    }
    
    @Override
    public void deleteFilesByExtension(String extension) {
        List<UploadedFile> activeFiles = uploadedFileRepository
                .findByExtensionAndStatus(extension, UploadedFile.FileStatus.ACTIVE);
        
        int deletedCount = 0;
        int protectedCount = 0;
        
        for (UploadedFile file : activeFiles) {
            // 삭제 예외 설정된 파일은 건너뛰기
            if (file.isProtectedFromDeletion()) {
                log.info("파일 삭제 예외 적용: {} (확장자 {} 차단에도 불구하고 보호됨)", 
                        file.getOriginalFilename(), extension);
                protectedCount++;
                continue;
            }
            
            try {
                // 물리적 파일 삭제
                Path filePath = Paths.get(file.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("물리적 파일 삭제: {}", filePath);
                }
                
                // DB에서 완전 삭제
                uploadedFileRepository.delete(file);
                log.warn("파일 삭제: {} (확장자 {} 차단으로 인함)", file.getOriginalFilename(), extension);
                deletedCount++;
                
            } catch (IOException e) {
                log.error("파일 삭제 실패: {} - {}", file.getOriginalFilename(), e.getMessage());
                // 물리적 파일 삭제 실패해도 DB는 삭제
                uploadedFileRepository.delete(file);
                deletedCount++;
            }
        }
        
        if (deletedCount > 0 || protectedCount > 0) {
            log.info("확장자 {} 처리 완료 - 삭제: {}개, 보호: {}개", extension, deletedCount, protectedCount);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UploadedFile> getFilesByStatus(UploadedFile.FileStatus status) {
        return uploadedFileRepository.findByStatus(status);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UploadedFile> getFilesByExtension(String extension) {
        return uploadedFileRepository.findByExtension(extension);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UploadedFile findById(Long fileId) {
        return uploadedFileRepository.findById(fileId).orElse(null);
    }
    
    @Override
    public void deletePhysicalFile(Long fileId) throws IOException {
        UploadedFile file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));
        
        Path filePath = Paths.get(file.getFilePath());
        
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("물리적 파일 삭제: {}", filePath);
        }
        
        file.markAsDeleted();
        uploadedFileRepository.save(file);
    }
    
    @Override
    public void setDeletionException(Long fileId, Boolean deletionException) {
        UploadedFile file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));
        
        file.setDeletionException(deletionException);
        uploadedFileRepository.save(file);
        
        log.info("파일 삭제 예외 설정 변경: {} - {} -> {}", 
                file.getOriginalFilename(), 
                !deletionException ? "보호 해제" : "보호 설정",
                deletionException ? "삭제 예외 적용" : "일반 파일");
    }
    
    
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 선택되지 않았습니다.");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("파일 크기가 최대 허용 크기를 초과합니다.");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다.");
        }
    }
    
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    private String generateUniqueFilename(String extension) {
        String uuid = UUID.randomUUID().toString();
        return extension.isEmpty() ? uuid : uuid + "." + extension;
    }
}
package com.assignment.fileextension.service;

import com.assignment.fileextension.common.FileExtensionConstants;
import com.assignment.fileextension.enums.BlockReason;
import com.assignment.fileextension.exception.FileValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileValidationService {
    
    private final ExtensionService extensionService;
    
    /**
     * 파일 검증 (빈 파일, 크기, 확장자 우회 등)
     */
    public FileValidationResult validateFile(MultipartFile file) {
        // 빈 파일 검증
        validateBasicFile(file);
        
        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();
        
        // 파일 크기 검증
        validateFileSize(fileSize);
        
        // 파일명 검증
        validateFileName(originalFilename);
        
        // 파일명 우회 공격 검증
        FileValidationResult bypassResult = validateFileNameBypass(originalFilename);
        if (bypassResult.isBlocked()) {
            return bypassResult;
        }
        
        return FileValidationResult.allowed();
    }
    
    /**
     * 기본 파일 검증 (빈 파일 체크)
     */
    private void validateBasicFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileValidationException("파일이 선택되지 않았습니다.");
        }
    }
    
    /**
     * 파일 크기를 검증합니다.
     */
    private void validateFileSize(long fileSize) {
        if (fileSize > FileExtensionConstants.FileLimit.MAX_FILE_SIZE_BYTES) {
            throw new FileValidationException(FileExtensionConstants.Messages.FILE_TOO_LARGE);
        }
    }
    
    /**
     * 파일명을 검증합니다.
     */
    private void validateFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new FileValidationException(FileExtensionConstants.Messages.INVALID_FILENAME);
        }
    }
    
    /**
     * 파일명 우회 공격 검증
     */
    private FileValidationResult validateFileNameBypass(String filename) {
        // 모든 확장자 추출
        List<String> allExtensions = extractAllExtensions(filename);
        
        log.debug("파일명 '{}' 에서 추출된 확장자들: {}", filename, allExtensions);
        
        // 각 확장자별로 차단 여부 확인
        for (String extension : allExtensions) {
            if (extensionService.isExtensionBlocked(filename, extension)) {
                return FileValidationResult.blocked(
                    String.format("%s: %s", FileExtensionConstants.Messages.FILE_BLOCKED, extension),
                    BlockReason.BLOCKED_EXTENSION,
                    extension
                );
            }
        }
        
        return FileValidationResult.allowed();
    }
    
    /**
     * 파일명에서 모든 확장자를 추출
     * 예: "document.backup.exe.txt" -> ["txt", "exe", "backup"]
     */
    private List<String> extractAllExtensions(String filename) {
        // 파일명을 '.'으로 분리
        String[] parts = filename.split("\\.");
        
        // 첫 번째 부분(파일명)을 제외한 나머지를 확장자로 간주
        if (parts.length <= 1) {
            return Arrays.asList(); // 확장자 없음
        }
        
        // 마지막 부분부터 역순으로 확장자 추출 (우선순위 고려)
        Set<String> extensions = new HashSet<>();
        for (int i = 1; i < parts.length; i++) {
            String extension = parts[i].toLowerCase().trim();
            
            // 빈 문자열이나 숫자만 있는 경우 제외
            if (!extension.isEmpty() && !extension.matches("\\d+")) {
                extensions.add(extension);
            }
        }
        
        return new ArrayList<>(extensions);
    }
    
    /**
     * 파일 검증 결과를 담는 클래스
     */
    public static class FileValidationResult {
        private final boolean blocked;
        private final String reason;
        private final BlockReason blockReason;
        private final String blockedExtension;
        
        private FileValidationResult(boolean blocked, String reason, BlockReason blockReason, String blockedExtension) {
            this.blocked = blocked;
            this.reason = reason;
            this.blockReason = blockReason;
            this.blockedExtension = blockedExtension;
        }
        
        public static FileValidationResult allowed() {
            return new FileValidationResult(false, null, null, null);
        }
        
        public static FileValidationResult blocked(String reason, BlockReason blockReason) {
            return new FileValidationResult(true, reason, blockReason, null);
        }
        
        public static FileValidationResult blocked(String reason, BlockReason blockReason, String blockedExtension) {
            return new FileValidationResult(true, reason, blockReason, blockedExtension);
        }
        
        public boolean isBlocked() {
            return blocked;
        }
        
        public boolean isAllowed() {
            return !blocked;
        }
        
        public String getReason() {
            return reason;
        }
        
        public BlockReason getBlockReason() {
            return blockReason;
        }
        
        public String getBlockedExtension() {
            return blockedExtension;
        }
    }
    
}
package com.assignment.fileextension.fixtures;

import com.assignment.fileextension.entity.*;
import com.assignment.fileextension.enums.BlockReason;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 테스트 데이터 생성을 위한 Fixture 클래스
 */
public class TestDataFixture {

    // === MultipartFile Fixtures ===
    
    public static MultipartFile createValidTextFile() {
        return new MockMultipartFile(
            "file",
            "document.txt",
            "text/plain",
            "Valid text content".getBytes()
        );
    }

    public static MultipartFile createValidPdfFile() {
        return new MockMultipartFile(
            "file", 
            "report.pdf",
            "application/pdf",
            "PDF content".getBytes()
        );
    }

    public static MultipartFile createBlockedExeFile() {
        return new MockMultipartFile(
            "file",
            "malware.exe", 
            "application/octet-stream",
            "Executable content".getBytes()
        );
    }

    public static MultipartFile createBypassAttemptFile() {
        return new MockMultipartFile(
            "file",
            "document.pdf.exe",
            "application/octet-stream", 
            "Bypass attempt content".getBytes()
        );
    }

    public static MultipartFile createEmptyFile() {
        return new MockMultipartFile(
            "file",
            "empty.txt",
            "text/plain",
            new byte[0]
        );
    }

    public static MultipartFile createLargeFile() {
        return new MockMultipartFile(
            "file",
            "large.txt",
            "text/plain", 
            new byte[101 * 1024 * 1024] // 101MB
        );
    }

    public static MultipartFile createFileWithNullName() {
        return new MockMultipartFile(
            "file",
            null,
            "text/plain",
            "Content".getBytes()
        );
    }

    // === Entity Fixtures ===

    public static CustomExtension createCustomExtension(String extension) {
        return CustomExtension.builder()
            .extension(extension.toLowerCase())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    public static List<CustomExtension> createCustomExtensions(String... extensions) {
        return Arrays.stream(extensions)
            .map(TestDataFixture::createCustomExtension)
            .toList();
    }

    public static FixedExtensionSetting createFixedExtensionSetting(String extension, boolean isBlocked) {
        return FixedExtensionSetting.builder()
            .extension(extension.toLowerCase())
            .isBlocked(isBlocked)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    public static List<FixedExtensionSetting> createDefaultFixedExtensions() {
        return Arrays.asList(
            createFixedExtensionSetting("bat", false),
            createFixedExtensionSetting("cmd", false), 
            createFixedExtensionSetting("com", false),
            createFixedExtensionSetting("cpl", false),
            createFixedExtensionSetting("exe", true),  // exe는 기본 차단
            createFixedExtensionSetting("scr", false),
            createFixedExtensionSetting("js", false)
        );
    }

    public static UploadedFile createUploadedFile() {
        return UploadedFile.builder()
            .originalFilename("test.txt")
            .storedFilename("uuid-test.txt")
            .filePath("/uploads/test/uuid-test.txt")
            .extension("txt")
            .fileSize(1024L)
            .contentType("text/plain")
            .status(UploadedFile.FileStatus.ACTIVE)
            .deletionException(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    public static UploadedFile createUploadedFileWithExtension(String extension) {
        String filename = "test." + extension;
        return UploadedFile.builder()
            .originalFilename(filename)
            .storedFilename("uuid-" + filename)
            .filePath("/uploads/test/uuid-" + filename)
            .extension(extension.toLowerCase())
            .fileSize(1024L)
            .contentType("application/octet-stream")
            .status(UploadedFile.FileStatus.ACTIVE)
            .deletionException(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    public static UploadedFile createProtectedFile() {
        UploadedFile file = createUploadedFile();
        file.setDeletionException(true);
        return file;
    }

    public static FileAuditLog createAuditLog(boolean blocked) {
        return FileAuditLog.builder()
            .userId("testUser")
            .filename("test.txt")
            .fileSize(1024L)
            .uploadTime(LocalDateTime.now())
            .blocked(blocked)
            .blockReason(blocked ? "차단된 확장자: exe" : null)
            .blockedExtension(blocked ? "exe" : null)
            .ipAddress("127.0.0.1")
            .userAgent("Test-Agent")
            .blockReasonType(blocked ? BlockReason.BLOCKED_EXTENSION : null)
            .build();
    }

    public static FileAuditLog createBlockedAuditLog() {
        return createAuditLog(true);
    }

    public static FileAuditLog createSuccessAuditLog() {
        return createAuditLog(false);
    }

    // === 테스트 시나리오별 데이터 ===

    public static class Scenarios {
        
        public static List<String> getBlockedExtensions() {
            return Arrays.asList("exe", "bat", "cmd", "malware");
        }

        public static List<String> getAllowedExtensions() {
            return Arrays.asList("txt", "pdf", "jpg", "png", "docx");
        }

        public static List<MultipartFile> getBypassAttemptFiles() {
            return Arrays.asList(
                new MockMultipartFile("file", "doc.pdf.exe", "application/octet-stream", "content".getBytes()),
                new MockMultipartFile("file", "image.jpg.bat", "application/octet-stream", "content".getBytes()),
                new MockMultipartFile("file", "backup.2024.exe", "application/octet-stream", "content".getBytes())
            );
        }

        public static List<MultipartFile> getInvalidFiles() {
            return Arrays.asList(
                createEmptyFile(),
                createLargeFile(),
                createFileWithNullName()
            );
        }
    }
}
package com.assignment.fileextension.unit.service;

import com.assignment.fileextension.common.FileExtensionConstants;
import com.assignment.fileextension.enums.BlockReason;
import com.assignment.fileextension.exception.FileValidationException;
import com.assignment.fileextension.service.ExtensionService;
import com.assignment.fileextension.service.FileValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileValidationService 단위 테스트")
class FileValidationServiceTest {

    @Mock
    private ExtensionService extensionService;

    @InjectMocks
    private FileValidationService fileValidationService;

    @Test
    @DisplayName("유효한 파일 검증 성공")
    void validateFile_success() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "document.pdf", "application/pdf", "content".getBytes()
        );

        given(extensionService.isExtensionBlocked(eq("document.pdf"), eq("pdf")))
            .willReturn(false);

        FileValidationService.FileValidationResult result = fileValidationService.validateFile(file);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getReason()).isNull();
    }

    @Test
    @DisplayName("빈 파일 검증 실패")
    void validateFile_emptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "empty.txt", "text/plain", new byte[0]
        );

        assertThatThrownBy(() -> fileValidationService.validateFile(emptyFile))
            .isInstanceOf(FileValidationException.class)
            .hasMessage("파일이 선택되지 않았습니다.");
    }

    @Test
    @DisplayName("파일 크기 초과 검증 실패")
    void validateFile_fileTooLarge() {
        byte[] largeContent = new byte[(int) (FileExtensionConstants.FileLimit.MAX_FILE_SIZE_BYTES + 1)];
        MockMultipartFile largeFile = new MockMultipartFile(
            "file", "large.txt", "text/plain", largeContent
        );

        assertThatThrownBy(() -> fileValidationService.validateFile(largeFile))
            .isInstanceOf(FileValidationException.class)
            .hasMessage(FileExtensionConstants.Messages.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("null 파일명 검증 실패")
    void validateFile_nullFileName() {
        MockMultipartFile fileWithNullName = new MockMultipartFile(
            "file", null, "text/plain", "content".getBytes()
        );

        assertThatThrownBy(() -> fileValidationService.validateFile(fileWithNullName))
            .isInstanceOf(FileValidationException.class)
            .hasMessage(FileExtensionConstants.Messages.INVALID_FILENAME);
    }

    @Test
    @DisplayName("빈 파일명 검증 실패")
    void validateFile_emptyFileName() {
        MockMultipartFile fileWithEmptyName = new MockMultipartFile(
            "file", "", "text/plain", "content".getBytes()
        );

        assertThatThrownBy(() -> fileValidationService.validateFile(fileWithEmptyName))
            .isInstanceOf(FileValidationException.class)
            .hasMessage(FileExtensionConstants.Messages.INVALID_FILENAME);
    }

    @Test
    @DisplayName("단일 확장자 차단 검증")
    void validateFile_blockedSingleExtension() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "malware.exe", "application/octet-stream", "malware".getBytes()
        );

        given(extensionService.isExtensionBlocked(eq("malware.exe"), eq("exe")))
            .willReturn(true);

        FileValidationService.FileValidationResult result = fileValidationService.validateFile(file);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.getReason()).contains("exe");
        assertThat(result.getBlockReason()).isEqualTo(BlockReason.BLOCKED_EXTENSION);
        assertThat(result.getBlockedExtension()).isEqualTo("exe");
    }

    @Test
    @DisplayName("다중 확장자 우회 공격 차단")
    void validateFile_multipleExtensionsBypass() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "document.backup.exe.txt", "text/plain", "content".getBytes()
        );

        // exe가 차단된 상태
        given(extensionService.isExtensionBlocked(eq("document.backup.exe.txt"), eq("txt")))
            .willReturn(false);
        given(extensionService.isExtensionBlocked(eq("document.backup.exe.txt"), eq("exe")))
            .willReturn(true);
        given(extensionService.isExtensionBlocked(eq("document.backup.exe.txt"), eq("backup")))
            .willReturn(false);

        FileValidationService.FileValidationResult result = fileValidationService.validateFile(file);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.getReason()).contains("exe");
        assertThat(result.getBlockReason()).isEqualTo(BlockReason.BLOCKED_EXTENSION);
        assertThat(result.getBlockedExtension()).isEqualTo("exe");
    }

    @Test
    @DisplayName("확장자 없는 파일 허용")
    void validateFile_noExtension() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "README", "text/plain", "content".getBytes()
        );

        FileValidationService.FileValidationResult result = fileValidationService.validateFile(file);

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("숫자만 있는 확장자 무시")
    void validateFile_numericExtension() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "backup.2024.txt", "text/plain", "content".getBytes()
        );

        given(extensionService.isExtensionBlocked(eq("backup.2024.txt"), eq("txt")))
            .willReturn(false);

        FileValidationService.FileValidationResult result = fileValidationService.validateFile(file);

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("복잡한 파일명 처리")
    void validateFile_complexFileName() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "my.project.v1.final.doc.backup.pdf", "application/pdf", "content".getBytes()
        );

        FileValidationService.FileValidationResult result = fileValidationService.validateFile(file);

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("FileValidationResult - allowed 정적 메서드")
    void fileValidationResult_allowed() {
        FileValidationService.FileValidationResult result = 
            FileValidationService.FileValidationResult.allowed();

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getReason()).isNull();
        assertThat(result.getBlockReason()).isNull();
        assertThat(result.getBlockedExtension()).isNull();
    }

    @Test
    @DisplayName("FileValidationResult - blocked 정적 메서드")
    void fileValidationResult_blocked() {
        FileValidationService.FileValidationResult result = 
            FileValidationService.FileValidationResult.blocked(
                "Test reason", BlockReason.BLOCKED_EXTENSION, "exe"
            );

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getReason()).isEqualTo("Test reason");
        assertThat(result.getBlockReason()).isEqualTo(BlockReason.BLOCKED_EXTENSION);
        assertThat(result.getBlockedExtension()).isEqualTo("exe");
    }
}
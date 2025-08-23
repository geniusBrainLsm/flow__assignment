package com.assignment.fileextension.unit.service;

import com.assignment.fileextension.exception.FileValidationException;
import com.assignment.fileextension.service.ExtensionService;
import com.assignment.fileextension.service.FileValidationService;
import com.assignment.fileextension.fixtures.TestDataFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileValidationService 단위 테스트")
class FileValidationServiceTest {

    @Mock
    private ExtensionService extensionService;

    @InjectMocks
    private FileValidationService fileValidationService;

    @Test
    @DisplayName("정상 파일 검증 - 성공")
    void validateFile_ValidFile_Success() {
        // given
        MultipartFile validFile = TestDataFixture.createValidTextFile();
        when(extensionService.isExtensionBlocked(eq("document.txt"), eq("txt")))
            .thenReturn(false);

        // when
        FileValidationService.FileValidationResult result = fileValidationService.validateFile(validFile);

        // then
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getReason()).isNull();
        assertThat(result.getBlockReason()).isNull();
        assertThat(result.getBlockedExtension()).isNull();
        
        verify(extensionService).isExtensionBlocked("document.txt", "txt");
    }

    @Test
    @DisplayName("빈 파일 검증 - 예외 발생")
    void validateFile_EmptyFile_ThrowsException() {
        // given
        MultipartFile emptyFile = TestDataFixture.createEmptyFile();

        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFile(emptyFile))
            .isInstanceOf(FileValidationException.class)
            .hasMessage("파일이 선택되지 않았습니다.");
        
        verifyNoInteractions(extensionService);
    }

    @Test
    @DisplayName("큰 파일 검증 - 예외 발생")
    void validateFile_LargeFile_ThrowsException() {
        // given
        MultipartFile largeFile = TestDataFixture.createLargeFile();

        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFile(largeFile))
            .isInstanceOf(FileValidationException.class)
            .hasMessage("파일 크기가 최대 허용 크기(100MB)를 초과했습니다");
        
        verifyNoInteractions(extensionService);
    }

    @Test
    @DisplayName("null 파일명 검증 - 예외 발생")
    void validateFile_NullFileName_ThrowsException() {
        // given
        MultipartFile nullNameFile = TestDataFixture.createFileWithNullName();

        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFile(nullNameFile))
            .isInstanceOf(FileValidationException.class)
            .hasMessage("파일명이 올바르지 않습니다");
        
        verifyNoInteractions(extensionService);
    }

    @Test
    @DisplayName("차단된 확장자 파일 검증 - 차단")
    void validateFile_BlockedExtension_Blocked() {
        // given
        MultipartFile blockedFile = TestDataFixture.createBlockedExeFile();
        when(extensionService.isExtensionBlocked(eq("malware.exe"), eq("exe")))
            .thenReturn(true);

        // when
        FileValidationService.FileValidationResult result = fileValidationService.validateFile(blockedFile);

        // then
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getBlockedExtension()).isEqualTo("exe");
        assertThat(result.getReason()).contains("exe");
        
        verify(extensionService).isExtensionBlocked("malware.exe", "exe");
    }

    @Test
    @DisplayName("우회 공격 파일 검증 - 차단 (다중 확장자)")
    void validateFile_BypassAttempt_Blocked() {
        // given
        MultipartFile bypassFile = TestDataFixture.createBypassAttemptFile();
        
        // pdf는 허용하지만 exe는 차단
        when(extensionService.isExtensionBlocked(eq("document.pdf.exe"), eq("exe")))
            .thenReturn(true);
        when(extensionService.isExtensionBlocked(eq("document.pdf.exe"), eq("pdf")))
            .thenReturn(false);

        // when
        FileValidationService.FileValidationResult result = fileValidationService.validateFile(bypassFile);

        // then
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.getBlockedExtension()).isEqualTo("exe");
        assertThat(result.getReason()).contains("exe");
        
        verify(extensionService).isExtensionBlocked("document.pdf.exe", "exe");
        // pdf도 확인했지만 exe에서 먼저 차단됨
    }

    @Test
    @DisplayName("복합 확장자 파일 검증 - 허용 (모든 확장자가 허용됨)")
    void validateFile_MultipleExtensions_AllAllowed() {
        // given
        MultipartFile multiExtFile = TestDataFixture.createValidPdfFile();
        when(extensionService.isExtensionBlocked(eq("report.pdf"), eq("pdf")))
            .thenReturn(false);

        // when
        FileValidationService.FileValidationResult result = fileValidationService.validateFile(multiExtFile);

        // then
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.isBlocked()).isFalse();
        
        verify(extensionService).isExtensionBlocked("report.pdf", "pdf");
    }

    @Test
    @DisplayName("확장자 추출 테스트 - 단일 확장자")
    void extractAllExtensions_SingleExtension() {
        // given
        String filename = "document.txt";

        // when
        FileValidationService.FileValidationResult result = fileValidationService.validateFile(
            TestDataFixture.createValidTextFile()
        );

        // then
        // extractAllExtensions는 private 메서드이므로 간접적으로 테스트
        verify(extensionService).isExtensionBlocked("document.txt", "txt");
    }

    @Test
    @DisplayName("확장자 추출 테스트 - 다중 확장자 with 숫자")
    void extractAllExtensions_MultipleExtensionsWithNumbers() {
        // given
        MultipartFile fileWithNumbers = new org.springframework.mock.web.MockMultipartFile(
            "file", "backup.2024.txt", "text/plain", "content".getBytes()
        );
        
        when(extensionService.isExtensionBlocked(eq("backup.2024.txt"), eq("txt")))
            .thenReturn(false);
        // 숫자는 확장자에서 제외되므로 2024는 체크하지 않음

        // when
        FileValidationService.FileValidationResult result = fileValidationService.validateFile(fileWithNumbers);

        // then
        assertThat(result.isAllowed()).isTrue();
        verify(extensionService).isExtensionBlocked("backup.2024.txt", "txt");
        // 2024는 숫자이므로 확장자로 간주하지 않음
        verify(extensionService, never()).isExtensionBlocked(anyString(), eq("2024"));
    }

    @Test
    @DisplayName("확장자 추출 테스트 - 확장자 없는 파일")
    void extractAllExtensions_NoExtension() {
        // given
        MultipartFile noExtFile = new org.springframework.mock.web.MockMultipartFile(
            "file", "document", "application/octet-stream", "content".getBytes()
        );

        // when
        FileValidationService.FileValidationResult result = fileValidationService.validateFile(noExtFile);

        // then
        assertThat(result.isAllowed()).isTrue();
        // 확장자가 없으므로 extensionService 호출되지 않음
        verifyNoInteractions(extensionService);
    }

    @Test
    @DisplayName("파일 검증 결과 객체 테스트 - 허용")
    void fileValidationResult_Allowed() {
        // when
        FileValidationService.FileValidationResult result = 
            FileValidationService.FileValidationResult.allowed();

        // then
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getReason()).isNull();
        assertThat(result.getBlockReason()).isNull();
        assertThat(result.getBlockedExtension()).isNull();
    }

    @Test
    @DisplayName("파일 검증 결과 객체 테스트 - 차단 (확장자 포함)")
    void fileValidationResult_BlockedWithExtension() {
        // when
        FileValidationService.FileValidationResult result = 
            FileValidationService.FileValidationResult.blocked(
                "차단된 확장자: exe",
                com.assignment.fileextension.enums.BlockReason.BLOCKED_EXTENSION,
                "exe"
            );

        // then
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getReason()).isEqualTo("차단된 확장자: exe");
        assertThat(result.getBlockReason()).isEqualTo(com.assignment.fileextension.enums.BlockReason.BLOCKED_EXTENSION);
        assertThat(result.getBlockedExtension()).isEqualTo("exe");
    }

    @Test
    @DisplayName("파일 검증 결과 객체 테스트 - 차단 (확장자 없음)")
    void fileValidationResult_BlockedWithoutExtension() {
        // when
        FileValidationService.FileValidationResult result = 
            FileValidationService.FileValidationResult.blocked(
                "파일 크기 초과",
                com.assignment.fileextension.enums.BlockReason.FILE_SIZE_EXCEEDED
            );

        // then
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getReason()).isEqualTo("파일 크기 초과");
        assertThat(result.getBlockReason()).isEqualTo(com.assignment.fileextension.enums.BlockReason.FILE_SIZE_EXCEEDED);
        assertThat(result.getBlockedExtension()).isNull();
    }
}
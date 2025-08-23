package com.assignment.fileextension.unit.controller;

import com.assignment.fileextension.controller.FileUploadController;
import com.assignment.fileextension.entity.UploadedFile;
import com.assignment.fileextension.enums.BlockReason;
import com.assignment.fileextension.service.AuditService;
import com.assignment.fileextension.service.FileValidationService;
import com.assignment.fileextension.service.StorageService;
import com.assignment.fileextension.fixtures.TestDataFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = FileUploadController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
})
@TestPropertySource(properties = {
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
@DisplayName("FileUploadController 단위 테스트")
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileValidationService fileValidationService;

    @MockBean
    private StorageService storageService;

    @MockBean
    private AuditService auditService;

    @Test
    @DisplayName("파일 업로드 - 성공")
    void uploadFile_Success() throws Exception {
        // given
        UploadedFile uploadedFile = TestDataFixture.createUploadedFile();
        FileValidationService.FileValidationResult validationResult = 
            FileValidationService.FileValidationResult.allowed();

        when(fileValidationService.validateFile(any(MultipartFile.class)))
            .thenReturn(validationResult);
        when(storageService.storeFile(any(MultipartFile.class)))
            .thenReturn(uploadedFile);

        // when & then
        mockMvc.perform(multipart("/api/upload/file")
                .file("file", "Valid content".getBytes())
                .param("filename", "document.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("파일 업로드가 완료되었습니다."))
                .andExpect(jsonPath("$.fileId").value(uploadedFile.getId()))
                .andExpect(jsonPath("$.originalFileName").value(uploadedFile.getOriginalFilename()))
                .andExpect(jsonPath("$.fileSize").value(uploadedFile.getFileSize()));

        verify(auditService).logUploadAttempt(any(MultipartFile.class), any());
        verify(fileValidationService).validateFile(any(MultipartFile.class));
        verify(storageService).storeFile(any(MultipartFile.class));
        verify(auditService).logSuccessfulUpload(any(MultipartFile.class), any());
    }

    @Test
    @DisplayName("파일 업로드 - 차단된 파일")
    void uploadFile_BlockedFile() throws Exception {
        // given
        FileValidationService.FileValidationResult validationResult = 
            FileValidationService.FileValidationResult.blocked(
                "차단된 확장자: exe", 
                BlockReason.BLOCKED_EXTENSION, 
                "exe"
            );

        when(fileValidationService.validateFile(any(MultipartFile.class)))
            .thenReturn(validationResult);

        // when & then
        mockMvc.perform(multipart("/api/upload/file")
                .file("file", "Malware content".getBytes())
                .param("filename", "malware.exe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("차단된 확장자: exe"))
                .andExpect(jsonPath("$.fileName").exists())
                .andExpect(jsonPath("$.blockReason").value("BLOCKED_EXTENSION"))
                .andExpect(jsonPath("$.blockedExtension").value("exe"));

        verify(auditService).logUploadAttempt(any(MultipartFile.class), any());
        verify(fileValidationService).validateFile(any(MultipartFile.class));
        verify(auditService).logBlockedUpload(any(MultipartFile.class), any(), eq(validationResult));
        verify(storageService, never()).storeFile(any(MultipartFile.class));
        verify(auditService, never()).logSuccessfulUpload(any(MultipartFile.class), any());
    }

    @Test
    @DisplayName("파일 검증만 수행 - 허용된 파일")
    void checkFileOnly_AllowedFile() throws Exception {
        // given
        FileValidationService.FileValidationResult validationResult = 
            FileValidationService.FileValidationResult.allowed();

        when(fileValidationService.validateFile(any(MultipartFile.class)))
            .thenReturn(validationResult);

        // when & then
        mockMvc.perform(multipart("/api/upload/check")
                .file("file", "Valid content".getBytes())
                .param("filename", "document.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").exists())
                .andExpect(jsonPath("$.fileSize").exists())
                .andExpect(jsonPath("$.result").value("allowed"))
                .andExpect(jsonPath("$.message").value("파일 업로드가 허용됩니다."));

        verify(fileValidationService).validateFile(any(MultipartFile.class));
        verify(storageService, never()).storeFile(any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 검증만 수행 - 차단된 파일")
    void checkFileOnly_BlockedFile() throws Exception {
        // given
        FileValidationService.FileValidationResult validationResult = 
            FileValidationService.FileValidationResult.blocked(
                "차단된 확장자: exe", 
                BlockReason.BLOCKED_EXTENSION, 
                "exe"
            );

        when(fileValidationService.validateFile(any(MultipartFile.class)))
            .thenReturn(validationResult);

        // when & then
        mockMvc.perform(multipart("/api/upload/check")
                .file("file", "Malware content".getBytes())
                .param("filename", "virus.exe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").exists())
                .andExpect(jsonPath("$.fileSize").exists())
                .andExpect(jsonPath("$.result").value("blocked"))
                .andExpect(jsonPath("$.message").value("차단된 확장자: exe"))
                .andExpect(jsonPath("$.blockReason").value("BLOCKED_EXTENSION"))
                .andExpect(jsonPath("$.blockedExtension").value("exe"));

        verify(fileValidationService).validateFile(any(MultipartFile.class));
        verify(auditService).logBlockedUpload(any(MultipartFile.class), any(), eq(validationResult));
        verify(storageService, never()).storeFile(any(MultipartFile.class));
    }

    @Test
    @DisplayName("빈 파일 검증 - 에러")
    void checkFileOnly_EmptyFile() throws Exception {
        // when & then
        mockMvc.perform(multipart("/api/upload/check")
                .file("file", new byte[0])
                .param("filename", "empty.txt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("파일이 선택되지 않았습니다."));

        verify(fileValidationService, never()).validateFile(any(MultipartFile.class));
        verify(storageService, never()).storeFile(any(MultipartFile.class));
    }

    @Test
    @DisplayName("우회 공격 파일 업로드 - 차단")
    void uploadFile_BypassAttempt_Blocked() throws Exception {
        // given
        FileValidationService.FileValidationResult validationResult = 
            FileValidationService.FileValidationResult.blocked(
                "차단된 확장자: exe", 
                BlockReason.BLOCKED_EXTENSION, 
                "exe"
            );

        when(fileValidationService.validateFile(any(MultipartFile.class)))
            .thenReturn(validationResult);

        // when & then
        mockMvc.perform(multipart("/api/upload/file")
                .file("file", "Bypass attempt content".getBytes())
                .param("filename", "document.pdf.exe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("차단된 확장자: exe"))
                .andExpect(jsonPath("$.blockReason").value("BLOCKED_EXTENSION"))
                .andExpect(jsonPath("$.blockedExtension").value("exe"));

        verify(auditService).logUploadAttempt(any(MultipartFile.class), any());
        verify(fileValidationService).validateFile(any(MultipartFile.class));
        verify(auditService).logBlockedUpload(any(MultipartFile.class), any(), eq(validationResult));
        verify(storageService, never()).storeFile(any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 저장 중 IO 예외 발생")
    void uploadFile_StorageIOException_InternalServerError() throws Exception {
        // given
        FileValidationService.FileValidationResult validationResult = 
            FileValidationService.FileValidationResult.allowed();

        when(fileValidationService.validateFile(any(MultipartFile.class)))
            .thenReturn(validationResult);
        when(storageService.storeFile(any(MultipartFile.class)))
            .thenThrow(new java.io.IOException("Storage error"));

        // when & then
        mockMvc.perform(multipart("/api/upload/file")
                .file("file", "Valid content".getBytes())
                .param("filename", "document.txt"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("파일 업로드 중 오류가 발생했습니다."));

        verify(auditService).logUploadAttempt(any(MultipartFile.class), any());
        verify(fileValidationService).validateFile(any(MultipartFile.class));
        verify(storageService).storeFile(any(MultipartFile.class));
        verify(auditService, never()).logSuccessfulUpload(any(MultipartFile.class), any());
    }

    @Test
    @DisplayName("예상치 못한 예외 발생")
    void uploadFile_UnexpectedException_InternalServerError() throws Exception {
        // given
        when(fileValidationService.validateFile(any(MultipartFile.class)))
            .thenThrow(new RuntimeException("Unexpected error"));

        // when & then
        mockMvc.perform(multipart("/api/upload/file")
                .file("file", "Valid content".getBytes())
                .param("filename", "document.txt"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("파일 업로드 중 예상치 못한 오류가 발생했습니다."));

        verify(auditService).logUploadAttempt(any(MultipartFile.class), any());
        verify(fileValidationService).validateFile(any(MultipartFile.class));
        verify(storageService, never()).storeFile(any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 매개변수 누락")
    void uploadFile_MissingFileParameter_BadRequest() throws Exception {
        // when & then
        mockMvc.perform(post("/api/upload/file"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(fileValidationService);
        verifyNoInteractions(storageService);
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("큰 파일 업로드 시도")
    void uploadFile_LargeFile_BadRequest() throws Exception {
        // given
        when(fileValidationService.validateFile(any(MultipartFile.class)))
            .thenThrow(new com.assignment.fileextension.exception.FileValidationException(
                "파일 크기가 최대 허용 크기(100MB)를 초과했습니다"));

        // when & then
        mockMvc.perform(multipart("/api/upload/file")
                .file("file", new byte[101 * 1024 * 1024]) // 101MB
                .param("filename", "large.txt"))
                .andExpect(status().isBadRequest());

        verify(auditService).logUploadAttempt(any(MultipartFile.class), any());
        verify(fileValidationService).validateFile(any(MultipartFile.class));
        verify(storageService, never()).storeFile(any(MultipartFile.class));
    }
}
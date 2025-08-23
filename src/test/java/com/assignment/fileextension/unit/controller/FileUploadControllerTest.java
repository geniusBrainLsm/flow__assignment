package com.assignment.fileextension.unit.controller;

import com.assignment.fileextension.controller.FileUploadController;
import com.assignment.fileextension.entity.UploadedFile;
import com.assignment.fileextension.enums.BlockReason;
import com.assignment.fileextension.service.AuditService;
import com.assignment.fileextension.service.FileValidationService;
import com.assignment.fileextension.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileUploadController.class)
@DisplayName("FileUploadController 단위 테스트")
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageService storageService;

    @MockBean
    private FileValidationService fileValidationService;

    @MockBean
    private AuditService auditService;

    @Test
    @DisplayName("파일 업로드 성공")
    void uploadFile_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.txt", "text/plain", "test content".getBytes()
        );
        
        UploadedFile uploadedFile = UploadedFile.builder()
            .id(1L)
            .originalFilename("test.txt")
            .storedFilename("stored-test.txt")
            .fileSize(12L)
            .filePath("/uploads/stored-test.txt")
            .createdAt(LocalDateTime.now())
            .build();

        given(fileValidationService.validateFile(any()))
            .willReturn(FileValidationService.FileValidationResult.allowed());
        given(storageService.storeFile(any())).willReturn(uploadedFile);
        doNothing().when(auditService).logUploadAttempt(any(), any());
        doNothing().when(auditService).logSuccessfulUpload(any(), any());

        mockMvc.perform(multipart("/api/upload/file").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("파일 업로드가 완료되었습니다."))
                .andExpect(jsonPath("$.originalFileName").value("test.txt"))
                .andExpect(jsonPath("$.storedFileName").value("stored-test.txt"));
    }

    @Test
    @DisplayName("파일 업로드 실패 - 차단된 확장자")
    void uploadFile_blocked() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "malware.exe", "application/octet-stream", "malware content".getBytes()
        );

        FileValidationService.FileValidationResult blockedResult = 
            FileValidationService.FileValidationResult.blocked(
                "차단된 확장자입니다: exe", 
                BlockReason.BLOCKED_EXTENSION, 
                "exe"
            );

        given(fileValidationService.validateFile(any())).willReturn(blockedResult);
        doNothing().when(auditService).logUploadAttempt(any(), any());
        doNothing().when(auditService).logBlockedUpload(any(), any(), any());

        mockMvc.perform(multipart("/api/upload/file").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("차단된 확장자입니다: exe"))
                .andExpect(jsonPath("$.fileName").value("malware.exe"))
                .andExpect(jsonPath("$.blockedExtension").value("exe"));
    }

    @Test
    @DisplayName("파일 검증만 수행 - 허용된 파일")
    void checkFileOnly_allowed() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "document.pdf", "application/pdf", "pdf content".getBytes()
        );

        given(fileValidationService.validateFile(any()))
            .willReturn(FileValidationService.FileValidationResult.allowed());

        mockMvc.perform(multipart("/api/upload/check").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("allowed"))
                .andExpect(jsonPath("$.message").value("파일 업로드가 허용됩니다."))
                .andExpect(jsonPath("$.fileName").value("document.pdf"));
    }

    @Test
    @DisplayName("파일 검증만 수행 - 차단된 파일")
    void checkFileOnly_blocked() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "script.bat", "application/octet-stream", "script content".getBytes()
        );

        FileValidationService.FileValidationResult blockedResult = 
            FileValidationService.FileValidationResult.blocked(
                "차단된 확장자입니다: bat", 
                BlockReason.BLOCKED_EXTENSION, 
                "bat"
            );

        given(fileValidationService.validateFile(any())).willReturn(blockedResult);
        doNothing().when(auditService).logBlockedUpload(any(), any(), any());

        mockMvc.perform(multipart("/api/upload/check").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("blocked"))
                .andExpect(jsonPath("$.message").value("차단된 확장자입니다: bat"))
                .andExpect(jsonPath("$.fileName").value("script.bat"))
                .andExpect(jsonPath("$.blockedExtension").value("bat"));
    }

    @Test
    @DisplayName("빈 파일 업로드 시도")
    void checkFileOnly_emptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "", "text/plain", new byte[0]
        );

        mockMvc.perform(multipart("/api/upload/check").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("파일이 선택되지 않았습니다."));
    }
}
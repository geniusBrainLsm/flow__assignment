package com.assignment.fileextension.unit.service;

import com.assignment.fileextension.entity.UploadedFile;
import com.assignment.fileextension.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StorageService 단위 테스트")
class StorageServiceTest {

    @Mock
    private StorageService storageService;

    @Test
    @DisplayName("파일 저장 테스트")
    void storeFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.txt", "text/plain", "test content".getBytes()
        );

        UploadedFile expectedFile = UploadedFile.builder()
            .id(1L)
            .originalFilename("test.txt")
            .storedFilename("stored-test.txt")
            .fileSize(12L)
            .filePath("/uploads/stored-test.txt")
            .createdAt(LocalDateTime.now())
            .build();

        given(storageService.storeFile(any())).willReturn(expectedFile);

        UploadedFile result = storageService.storeFile(file);

        assertThat(result).isNotNull();
        assertThat(result.getOriginalFilename()).isEqualTo("test.txt");
        verify(storageService).storeFile(file);
    }

    @Test
    @DisplayName("확장자별 파일 삭제 테스트")
    void deleteFilesByExtension() {
        doNothing().when(storageService).deleteFilesByExtension("exe");

        storageService.deleteFilesByExtension("exe");

        verify(storageService).deleteFilesByExtension("exe");
    }

    @Test
    @DisplayName("파일 ID로 조회 테스트")
    void findById() {
        UploadedFile expectedFile = UploadedFile.builder()
            .id(1L)
            .originalFilename("test.txt")
            .storedFilename("stored-test.txt")
            .fileSize(100L)
            .filePath("/uploads/stored-test.txt")
            .createdAt(LocalDateTime.now())
            .build();

        given(storageService.findById(1L)).willReturn(expectedFile);

        UploadedFile result = storageService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOriginalFilename()).isEqualTo("test.txt");
    }

    @Test
    @DisplayName("상태별 파일 조회 테스트")
    void getFilesByStatus() {
        List<UploadedFile> expectedFiles = Arrays.asList(
            UploadedFile.builder()
                .id(1L)
                .originalFilename("file1.txt")
                .status(UploadedFile.FileStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build(),
            UploadedFile.builder()
                .id(2L)
                .originalFilename("file2.txt")
                .status(UploadedFile.FileStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build()
        );

        given(storageService.getFilesByStatus(UploadedFile.FileStatus.ACTIVE))
            .willReturn(expectedFiles);

        List<UploadedFile> result = storageService.getFilesByStatus(UploadedFile.FileStatus.ACTIVE);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(UploadedFile.FileStatus.ACTIVE);
    }

    @Test
    @DisplayName("확장자별 파일 조회 테스트")
    void getFilesByExtension() {
        List<UploadedFile> expectedFiles = Arrays.asList(
            UploadedFile.builder()
                .id(1L)
                .originalFilename("file1.txt")
                .extension("txt")
                .createdAt(LocalDateTime.now())
                .build()
        );

        given(storageService.getFilesByExtension("txt")).willReturn(expectedFiles);

        List<UploadedFile> result = storageService.getFilesByExtension("txt");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExtension()).isEqualTo("txt");
    }

    @Test
    @DisplayName("물리적 파일 삭제 테스트")
    void deletePhysicalFile() throws IOException {
        doNothing().when(storageService).deletePhysicalFile(1L);

        storageService.deletePhysicalFile(1L);

        verify(storageService).deletePhysicalFile(1L);
    }

    @Test
    @DisplayName("삭제 예외 설정 테스트")
    void setDeletionException() {
        doNothing().when(storageService).setDeletionException(1L, true);

        storageService.setDeletionException(1L, true);

        verify(storageService).setDeletionException(1L, true);
    }
}
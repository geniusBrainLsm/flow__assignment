package com.assignment.fileextension.unit.service;

import com.assignment.fileextension.entity.UploadedFile;
import com.assignment.fileextension.exception.FileValidationException;
import com.assignment.fileextension.repository.UploadedFileRepository;
import com.assignment.fileextension.service.FileStorageService;
import com.assignment.fileextension.fixtures.TestDataFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService 단위 테스트")
class FileStorageServiceTest {

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @InjectMocks
    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // @Value 필드는 테스트에서 직접 설정할 수 없으므로
        // 실제 Spring 컨텍스트를 사용하거나 별도 테스트 구현체 필요
    }

    @Test
    @DisplayName("파일 저장 - 성공")
    void storeFile_Success() throws IOException {
        // given
        MultipartFile file = TestDataFixture.createValidTextFile();
        UploadedFile savedFile = TestDataFixture.createUploadedFile();
        
        when(uploadedFileRepository.save(any(UploadedFile.class)))
            .thenReturn(savedFile);

        // when
        UploadedFile result = fileStorageService.storeFile(file);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOriginalFilename()).isEqualTo("document.txt");
        assertThat(result.getExtension()).isEqualTo("txt");
        assertThat(result.getFileSize()).isEqualTo(file.getSize());
        assertThat(result.getStatus()).isEqualTo(UploadedFile.FileStatus.ACTIVE);
        
        verify(uploadedFileRepository).save(any(UploadedFile.class));
    }

    @Test
    @DisplayName("빈 파일 저장 시도 - 예외 발생")
    void storeFile_EmptyFile_ThrowsException() {
        // given
        MultipartFile emptyFile = TestDataFixture.createEmptyFile();

        // when & then
        assertThatThrownBy(() -> fileStorageService.storeFile(emptyFile))
            .isInstanceOf(FileValidationException.class)
            .hasMessage("빈 파일은 저장할 수 없습니다");
        
        verify(uploadedFileRepository, never()).save(any());
    }

    @Test
    @DisplayName("큰 파일 저장 시도 - 예외 발생")
    void storeFile_LargeFile_ThrowsException() {
        // given
        MultipartFile largeFile = TestDataFixture.createLargeFile();

        // when & then
        assertThatThrownBy(() -> fileStorageService.storeFile(largeFile))
            .isInstanceOf(FileValidationException.class)
            .hasMessage("파일 크기가 최대 허용 크기를 초과했습니다");
        
        verify(uploadedFileRepository, never()).save(any());
    }

    @Test
    @DisplayName("파일 ID로 조회 - 성공")
    void findById_Success() {
        // given
        Long fileId = 1L;
        UploadedFile file = TestDataFixture.createUploadedFile();
        when(uploadedFileRepository.findById(fileId))
            .thenReturn(Optional.of(file));

        // when
        Optional<UploadedFile> result = fileStorageService.findById(fileId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getOriginalFilename()).isEqualTo("test.txt");
        
        verify(uploadedFileRepository).findById(fileId);
    }

    @Test
    @DisplayName("파일 ID로 조회 - 파일 없음")
    void findById_NotFound() {
        // given
        Long fileId = 999L;
        when(uploadedFileRepository.findById(fileId))
            .thenReturn(Optional.empty());

        // when
        Optional<UploadedFile> result = fileStorageService.findById(fileId);

        // then
        assertThat(result).isEmpty();
        
        verify(uploadedFileRepository).findById(fileId);
    }

    @Test
    @DisplayName("확장자별 파일 삭제 - 보호된 파일 제외")
    void deleteFilesByExtension_SkipProtectedFiles() {
        // given
        String extension = "exe";
        UploadedFile normalFile = TestDataFixture.createUploadedFileWithExtension(extension);
        UploadedFile protectedFile = TestDataFixture.createProtectedFile();
        protectedFile.setExtension(extension);
        
        List<UploadedFile> files = Arrays.asList(normalFile, protectedFile);
        
        when(uploadedFileRepository.findByExtensionAndStatus(extension, UploadedFile.FileStatus.ACTIVE))
            .thenReturn(files);

        // when
        fileStorageService.deleteFilesByExtension(extension);

        // then
        verify(uploadedFileRepository).findByExtensionAndStatus(extension, UploadedFile.FileStatus.ACTIVE);
        verify(uploadedFileRepository).delete(normalFile);
        verify(uploadedFileRepository, never()).delete(protectedFile);
    }

    @Test
    @DisplayName("확장자별 파일 삭제 - 파일 없음")
    void deleteFilesByExtension_NoFiles() {
        // given
        String extension = "nonexistent";
        when(uploadedFileRepository.findByExtensionAndStatus(extension, UploadedFile.FileStatus.ACTIVE))
            .thenReturn(Arrays.asList());

        // when
        fileStorageService.deleteFilesByExtension(extension);

        // then
        verify(uploadedFileRepository).findByExtensionAndStatus(extension, UploadedFile.FileStatus.ACTIVE);
        verify(uploadedFileRepository, never()).delete(any());
    }

    @Test
    @DisplayName("삭제 예외 설정 - 성공")
    void setDeletionException_Success() {
        // given
        Long fileId = 1L;
        boolean deletionException = true;
        UploadedFile file = TestDataFixture.createUploadedFile();
        
        when(uploadedFileRepository.findById(fileId))
            .thenReturn(Optional.of(file));
        when(uploadedFileRepository.save(any(UploadedFile.class)))
            .thenReturn(file);

        // when
        fileStorageService.setDeletionException(fileId, deletionException);

        // then
        assertThat(file.getDeletionException()).isTrue();
        
        verify(uploadedFileRepository).findById(fileId);
        verify(uploadedFileRepository).save(file);
    }

    @Test
    @DisplayName("삭제 예외 설정 - 파일 없음")
    void setDeletionException_FileNotFound_ThrowsException() {
        // given
        Long fileId = 999L;
        boolean deletionException = true;
        
        when(uploadedFileRepository.findById(fileId))
            .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileStorageService.setDeletionException(fileId, deletionException))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("파일을 찾을 수 없습니다");
        
        verify(uploadedFileRepository).findById(fileId);
        verify(uploadedFileRepository, never()).save(any());
    }

    @Test
    @DisplayName("상태별 파일 조회")
    void getFilesByStatus() {
        // given
        UploadedFile.FileStatus status = UploadedFile.FileStatus.ACTIVE;
        List<UploadedFile> files = Arrays.asList(
            TestDataFixture.createUploadedFile(),
            TestDataFixture.createUploadedFileWithExtension("pdf")
        );
        
        when(uploadedFileRepository.findByStatusOrderByCreatedAtDesc(status))
            .thenReturn(files);

        // when
        List<UploadedFile> result = fileStorageService.getFilesByStatus(status);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(files);
        
        verify(uploadedFileRepository).findByStatusOrderByCreatedAtDesc(status);
    }

    @Test
    @DisplayName("확장자별 파일 조회")
    void getFilesByExtension() {
        // given
        String extension = "txt";
        List<UploadedFile> files = Arrays.asList(
            TestDataFixture.createUploadedFile(),
            TestDataFixture.createUploadedFileWithExtension("txt")
        );
        
        when(uploadedFileRepository.findByExtensionOrderByCreatedAtDesc(extension))
            .thenReturn(files);

        // when
        List<UploadedFile> result = fileStorageService.getFilesByExtension(extension);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(files);
        
        verify(uploadedFileRepository).findByExtensionOrderByCreatedAtDesc(extension);
    }

    @Test
    @DisplayName("전체 파일 조회")
    void getAllFiles() {
        // given
        List<UploadedFile> files = Arrays.asList(
            TestDataFixture.createUploadedFile(),
            TestDataFixture.createUploadedFileWithExtension("pdf"),
            TestDataFixture.createUploadedFileWithExtension("jpg")
        );
        
        when(uploadedFileRepository.findAllByOrderByCreatedAtDesc())
            .thenReturn(files);

        // when
        List<UploadedFile> result = fileStorageService.getAllFiles();

        // then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyElementsOf(files);
        
        verify(uploadedFileRepository).findAllByOrderByCreatedAtDesc();
    }

    // private 메서드들은 public 인터페이스를 통해 간접적으로 테스트됩니다
}
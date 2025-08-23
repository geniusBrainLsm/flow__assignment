package com.assignment.fileextension.unit.service;

import com.assignment.fileextension.entity.CustomExtension;
import com.assignment.fileextension.entity.FixedExtensionSetting;
import com.assignment.fileextension.repository.CustomExtensionRepository;
import com.assignment.fileextension.repository.FixedExtensionSettingRepository;
import com.assignment.fileextension.service.ExtensionService;
import com.assignment.fileextension.service.StorageService;
import com.assignment.fileextension.fixtures.TestDataFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExtensionService 단위 테스트")
class ExtensionServiceTest {

    @Mock
    private FixedExtensionSettingRepository fixedExtensionSettingRepository;

    @Mock 
    private CustomExtensionRepository customExtensionRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private ExtensionService extensionService;

    @BeforeEach
    void setUp() {
        // 기본 고정 확장자 설정 mock
        when(fixedExtensionSettingRepository.findByExtension("exe"))
            .thenReturn(Optional.of(TestDataFixture.createFixedExtensionSetting("exe", true)));
    }

    @Test
    @DisplayName("고정 확장자 차단 여부 확인 - 차단된 확장자")
    void isExtensionBlocked_FixedExtension_Blocked() {
        // given
        String filename = "malware.exe";
        String extension = "exe";

        // when
        boolean result = extensionService.isExtensionBlocked(filename, extension);

        // then
        assertThat(result).isTrue();
        verify(fixedExtensionSettingRepository).findByExtension(extension);
    }

    @Test
    @DisplayName("고정 확장자 차단 여부 확인 - 허용된 확장자")
    void isExtensionBlocked_FixedExtension_Allowed() {
        // given
        String filename = "script.bat";
        String extension = "bat";
        when(fixedExtensionSettingRepository.findByExtension("bat"))
            .thenReturn(Optional.of(TestDataFixture.createFixedExtensionSetting("bat", false)));

        // when
        boolean result = extensionService.isExtensionBlocked(filename, extension);

        // then
        assertThat(result).isFalse();
        verify(fixedExtensionSettingRepository).findByExtension(extension);
    }

    @Test
    @DisplayName("커스텀 확장자 차단 여부 확인 - 차단된 확장자")
    void isExtensionBlocked_CustomExtension_Blocked() {
        // given
        String filename = "malware.virus";
        String extension = "virus";
        when(fixedExtensionSettingRepository.findByExtension("virus"))
            .thenReturn(Optional.empty());
        when(customExtensionRepository.existsByExtension("virus"))
            .thenReturn(true);

        // when
        boolean result = extensionService.isExtensionBlocked(filename, extension);

        // then
        assertThat(result).isTrue();
        verify(fixedExtensionSettingRepository).findByExtension(extension);
        verify(customExtensionRepository).existsByExtension(extension);
    }

    @Test
    @DisplayName("확장자 차단 여부 확인 - 허용된 확장자")
    void isExtensionBlocked_AllowedExtension() {
        // given
        String filename = "document.txt";
        String extension = "txt";
        when(fixedExtensionSettingRepository.findByExtension("txt"))
            .thenReturn(Optional.empty());
        when(customExtensionRepository.existsByExtension("txt"))
            .thenReturn(false);

        // when
        boolean result = extensionService.isExtensionBlocked(filename, extension);

        // then
        assertThat(result).isFalse();
        verify(fixedExtensionSettingRepository).findByExtension(extension);
        verify(customExtensionRepository).existsByExtension(extension);
    }

    @Test
    @DisplayName("고정 확장자 설정 변경 - 차단으로 변경 시 기존 파일 삭제")
    void updateFixedExtensionSetting_BlockExtension_DeleteExistingFiles() {
        // given
        String extension = "bat";
        boolean isBlocked = true;
        FixedExtensionSetting setting = TestDataFixture.createFixedExtensionSetting(extension, false);
        
        when(fixedExtensionSettingRepository.findByExtension(extension))
            .thenReturn(Optional.of(setting));
        when(fixedExtensionSettingRepository.save(any(FixedExtensionSetting.class)))
            .thenReturn(setting);

        // when
        extensionService.updateFixedExtensionSetting(extension, isBlocked);

        // then
        verify(fixedExtensionSettingRepository).findByExtension(extension);
        verify(fixedExtensionSettingRepository).save(setting);
        verify(storageService).deleteFilesByExtension(extension);
        assertThat(setting.getIsBlocked()).isTrue();
    }

    @Test
    @DisplayName("고정 확장자 설정 변경 - 허용으로 변경 시 파일 삭제 안함")
    void updateFixedExtensionSetting_AllowExtension_DoNotDeleteFiles() {
        // given
        String extension = "bat";
        boolean isBlocked = false;
        FixedExtensionSetting setting = TestDataFixture.createFixedExtensionSetting(extension, true);
        
        when(fixedExtensionSettingRepository.findByExtension(extension))
            .thenReturn(Optional.of(setting));
        when(fixedExtensionSettingRepository.save(any(FixedExtensionSetting.class)))
            .thenReturn(setting);

        // when
        extensionService.updateFixedExtensionSetting(extension, isBlocked);

        // then
        verify(fixedExtensionSettingRepository).findByExtension(extension);
        verify(fixedExtensionSettingRepository).save(setting);
        verify(storageService, never()).deleteFilesByExtension(extension);
        assertThat(setting.getIsBlocked()).isFalse();
    }

    @Test
    @DisplayName("커스텀 확장자 추가 - 성공")
    void addCustomExtension_Success() {
        // given
        String extension = "malware";
        when(customExtensionRepository.existsByExtension(extension))
            .thenReturn(false);
        when(customExtensionRepository.countCustomExtensions())
            .thenReturn(199L);
        when(customExtensionRepository.save(any(CustomExtension.class)))
            .thenReturn(TestDataFixture.createCustomExtension(extension));

        // when
        extensionService.addCustomExtension(extension);

        // then
        verify(customExtensionRepository).existsByExtension(extension);
        verify(customExtensionRepository).countCustomExtensions();
        verify(customExtensionRepository).save(any(CustomExtension.class));
        verify(storageService).deleteFilesByExtension(extension);
    }

    @Test
    @DisplayName("커스텀 확장자 추가 - 중복 확장자로 실패")
    void addCustomExtension_DuplicateExtension_ThrowsException() {
        // given
        String extension = "malware";
        when(customExtensionRepository.existsByExtension(extension))
            .thenReturn(true);

        // when & then
        assertThatThrownBy(() -> extensionService.addCustomExtension(extension))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 확장자");

        verify(customExtensionRepository).existsByExtension(extension);
        verify(customExtensionRepository, never()).save(any());
        verify(storageService, never()).deleteFilesByExtension(anyString());
    }

    @Test
    @DisplayName("커스텀 확장자 추가 - 200개 제한 초과로 실패")
    void addCustomExtension_ExceedsLimit_ThrowsException() {
        // given
        String extension = "malware";
        when(customExtensionRepository.existsByExtension(extension))
            .thenReturn(false);
        when(customExtensionRepository.countCustomExtensions())
            .thenReturn(200L);

        // when & then
        assertThatThrownBy(() -> extensionService.addCustomExtension(extension))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("최대 200개");

        verify(customExtensionRepository).existsByExtension(extension);
        verify(customExtensionRepository).countCustomExtensions();
        verify(customExtensionRepository, never()).save(any());
    }

    @Test
    @DisplayName("커스텀 확장자 삭제 - 성공")
    void deleteCustomExtension_Success() {
        // given
        Long extensionId = 1L;
        CustomExtension extension = TestDataFixture.createCustomExtension("malware");
        when(customExtensionRepository.findById(extensionId))
            .thenReturn(Optional.of(extension));

        // when
        extensionService.deleteCustomExtension(extensionId);

        // then
        verify(customExtensionRepository).findById(extensionId);
        verify(customExtensionRepository).delete(extension);
    }

    @Test
    @DisplayName("커스텀 확장자 삭제 - 존재하지 않는 ID로 실패")
    void deleteCustomExtension_NotFound_ThrowsException() {
        // given
        Long extensionId = 999L;
        when(customExtensionRepository.findById(extensionId))
            .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> extensionService.deleteCustomExtension(extensionId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("존재하지 않는 확장자");

        verify(customExtensionRepository).findById(extensionId);
        verify(customExtensionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("모든 차단된 확장자 조회")
    void getAllBlockedExtensions() {
        // given
        List<String> fixedBlocked = Arrays.asList("exe", "bat");
        List<String> customBlocked = Arrays.asList("malware", "virus");
        
        when(fixedExtensionSettingRepository.findBlockedExtensions())
            .thenReturn(fixedBlocked);
        when(customExtensionRepository.findAllExtensions())
            .thenReturn(customBlocked);

        // when
        List<String> result = extensionService.getAllBlockedExtensions();

        // then
        assertThat(result).hasSize(4);
        assertThat(result).containsExactlyInAnyOrder("exe", "bat", "malware", "virus");
        verify(fixedExtensionSettingRepository).findBlockedExtensions();
        verify(customExtensionRepository).findAllExtensions();
    }

    @Test
    @DisplayName("확장자 정규화 테스트")
    void normalizeExtension_VariousCases() {
        // given & when & then
        assertThat(extensionService.normalizeExtension("EXE")).isEqualTo("exe");
        assertThat(extensionService.normalizeExtension(".bat")).isEqualTo("bat");
        assertThat(extensionService.normalizeExtension(" CMD ")).isEqualTo("cmd");
        assertThat(extensionService.normalizeExtension(null)).isEqualTo("");
        assertThat(extensionService.normalizeExtension("")).isEqualTo("");
        assertThat(extensionService.normalizeExtension("   ")).isEqualTo("");
    }

    @Test
    @DisplayName("고정 확장자 여부 확인")
    void isFixedExtension_VariousCases() {
        // given & when & then
        assertThat(extensionService.isFixedExtension("exe")).isTrue();
        assertThat(extensionService.isFixedExtension("EXE")).isTrue();
        assertThat(extensionService.isFixedExtension("bat")).isTrue();
        assertThat(extensionService.isFixedExtension("txt")).isFalse();
        assertThat(extensionService.isFixedExtension("pdf")).isFalse();
        assertThat(extensionService.isFixedExtension(null)).isFalse();
        assertThat(extensionService.isFixedExtension("")).isFalse();
    }
}
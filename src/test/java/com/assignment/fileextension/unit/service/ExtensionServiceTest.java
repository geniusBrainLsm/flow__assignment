package com.assignment.fileextension.unit.service;

import com.assignment.fileextension.common.FileExtensionConstants;
import com.assignment.fileextension.dto.CustomExtensionDto;
import com.assignment.fileextension.dto.ExtensionRequest;
import com.assignment.fileextension.dto.FixedExtensionSettingDto;
import com.assignment.fileextension.entity.CustomExtension;
import com.assignment.fileextension.entity.FixedExtensionSetting;
import com.assignment.fileextension.exception.ExtensionNotFoundException;
import com.assignment.fileextension.repository.CustomExtensionRepository;
import com.assignment.fileextension.repository.FixedExtensionSettingRepository;
import com.assignment.fileextension.service.ExtensionService;
import com.assignment.fileextension.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExtensionService 단위 테스트")
class ExtensionServiceTest {

    @Mock
    private CustomExtensionRepository customExtensionRepository;

    @Mock
    private FixedExtensionSettingRepository fixedExtensionSettingRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private ExtensionService extensionService;

    private FixedExtensionSetting fixedExtensionSetting;
    private CustomExtension customExtension;

    @BeforeEach
    void setUp() {
        fixedExtensionSetting = FixedExtensionSetting.builder()
            .id(1L)
            .extension("exe")
            .isBlocked(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        customExtension = CustomExtension.builder()
            .id(1L)
            .extension("custom")
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("고정 확장자 설정 전체 조회")
    void getAllFixedExtensionSettings() {
        List<FixedExtensionSetting> settings = Arrays.asList(fixedExtensionSetting);
        given(fixedExtensionSettingRepository.findAllOrderByExtension()).willReturn(settings);

        List<FixedExtensionSettingDto> result = extensionService.getAllFixedExtensionSettings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExtension()).isEqualTo("exe");
        assertThat(result.get(0).getIsBlocked()).isFalse();
    }

    @Test
    @DisplayName("고정 확장자 설정 변경 - 차단으로 변경")
    void updateFixedExtensionSetting_toBlocked() {
        given(fixedExtensionSettingRepository.findByExtension("exe"))
            .willReturn(Optional.of(fixedExtensionSetting));
        given(fixedExtensionSettingRepository.save(any())).willReturn(fixedExtensionSetting);
        doNothing().when(storageService).deleteFilesByExtension("exe");

        FixedExtensionSettingDto result = extensionService.updateFixedExtensionSetting("exe", true);

        assertThat(result.getExtension()).isEqualTo("exe");
        verify(storageService).deleteFilesByExtension("exe");
    }

    @Test
    @DisplayName("고정 확장자 설정 변경 - 존재하지 않는 확장자")
    void updateFixedExtensionSetting_notFound() {
        given(fixedExtensionSettingRepository.findByExtension("unknown"))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> extensionService.updateFixedExtensionSetting("unknown", true))
            .isInstanceOf(ExtensionNotFoundException.class);
    }

    @Test
    @DisplayName("커스텀 확장자 전체 조회")
    void getAllCustomExtensions() {
        List<CustomExtension> extensions = Arrays.asList(customExtension);
        given(customExtensionRepository.findAll()).willReturn(extensions);

        List<CustomExtensionDto> result = extensionService.getAllCustomExtensions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExtension()).isEqualTo("custom");
    }

    @Test
    @DisplayName("커스텀 확장자 추가 성공")
    void addCustomExtension_success() {
        ExtensionRequest request = new ExtensionRequest("newext");
        given(customExtensionRepository.existsByExtension("newext")).willReturn(false);
        given(customExtensionRepository.countCustomExtensions()).willReturn(0L);
        given(customExtensionRepository.save(any())).willReturn(customExtension);
        doNothing().when(storageService).deleteFilesByExtension("newext");

        CustomExtensionDto result = extensionService.addCustomExtension(request);

        assertThat(result).isNotNull();
        verify(storageService).deleteFilesByExtension("newext");
    }

    @Test
    @DisplayName("커스텀 확장자 추가 실패 - 중복")
    void addCustomExtension_duplicate() {
        ExtensionRequest request = new ExtensionRequest("duplicate");
        given(customExtensionRepository.existsByExtension("duplicate")).willReturn(true);

        assertThatThrownBy(() -> extensionService.addCustomExtension(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(FileExtensionConstants.Messages.EXTENSION_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("커스텀 확장자 추가 실패 - 최대 개수 초과")
    void addCustomExtension_maxExceeded() {
        ExtensionRequest request = new ExtensionRequest("maxtest");
        given(customExtensionRepository.existsByExtension("maxtest")).willReturn(false);
        given(customExtensionRepository.countCustomExtensions())
            .willReturn((long) FileExtensionConstants.FileLimit.MAX_CUSTOM_EXTENSIONS);

        assertThatThrownBy(() -> extensionService.addCustomExtension(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(FileExtensionConstants.Messages.MAX_EXTENSIONS_EXCEEDED);
    }

    @Test
    @DisplayName("커스텀 확장자 삭제")
    void deleteCustomExtension() {
        given(customExtensionRepository.findById(1L)).willReturn(Optional.of(customExtension));
        doNothing().when(customExtensionRepository).deleteById(1L);

        extensionService.deleteCustomExtension(1L);

        verify(customExtensionRepository).deleteById(1L);
    }

    @Test
    @DisplayName("커스텀 확장자 삭제 실패 - 존재하지 않음")
    void deleteCustomExtension_notFound() {
        given(customExtensionRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> extensionService.deleteCustomExtension(1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("해당 확장자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("확장자 차단 여부 확인 - 고정 확장자 차단")
    void isExtensionBlocked_fixedExtension_blocked() {
        given(fixedExtensionSettingRepository.findByExtension("exe"))
            .willReturn(Optional.of(FixedExtensionSetting.builder()
                .extension("exe")
                .isBlocked(true)
                .build()));

        boolean result = extensionService.isExtensionBlocked("test.exe", "exe");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("확장자 차단 여부 확인 - 고정 확장자 허용")
    void isExtensionBlocked_fixedExtension_allowed() {
        given(fixedExtensionSettingRepository.findByExtension("exe"))
            .willReturn(Optional.of(FixedExtensionSetting.builder()
                .extension("exe")
                .isBlocked(false)
                .build()));

        boolean result = extensionService.isExtensionBlocked("test.exe", "exe");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("확장자 차단 여부 확인 - 커스텀 확장자")
    void isExtensionBlocked_customExtension() {
        given(customExtensionRepository.findAllExtensions())
            .willReturn(Arrays.asList("custom", "blocked"));

        boolean result = extensionService.isExtensionBlocked("test.custom", "custom");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("확장자 차단 여부 확인 - 고정 확장자 상태 Map으로 전달")
    void isExtensionBlocked_withFixedStates() {
        Map<String, Boolean> fixedStates = new HashMap<>();
        fixedStates.put("exe", true);

        boolean result = extensionService.isExtensionBlocked("test.exe", "exe", fixedStates);

        assertThat(result).isTrue();
        verifyNoInteractions(fixedExtensionSettingRepository);
    }

    @Test
    @DisplayName("파일명에서 확장자 추출하여 차단 확인")
    void isExtensionBlocked_fromFileName() {
        boolean result = extensionService.isExtensionBlocked("document.txt");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("확장자 없는 파일 차단 확인")
    void isExtensionBlocked_noExtension() {
        boolean result = extensionService.isExtensionBlocked("noextension");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("null 파일명 차단 확인")
    void isExtensionBlocked_nullFileName() {
        boolean result = extensionService.isExtensionBlocked(null);

        assertThat(result).isFalse();
    }
}
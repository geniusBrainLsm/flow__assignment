package com.assignment.fileextension.service;

import com.assignment.fileextension.common.FileExtensionConstants;
import com.assignment.fileextension.dto.CustomExtensionDto;
import com.assignment.fileextension.dto.ExtensionRequest;
import com.assignment.fileextension.dto.FixedExtensionSettingDto;
import com.assignment.fileextension.entity.CustomExtension;
import com.assignment.fileextension.entity.FixedExtensionSetting;
import com.assignment.fileextension.exception.ExtensionNotFoundException;
import com.assignment.fileextension.repository.CustomExtensionRepository;
import com.assignment.fileextension.repository.FixedExtensionSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExtensionService {
    
    private final CustomExtensionRepository customExtensionRepository;
    private final FixedExtensionSettingRepository fixedExtensionSettingRepository;
    private final StorageService storageService;
    
    public List<FixedExtensionSettingDto> getAllFixedExtensionSettings() {
        return fixedExtensionSettingRepository.findAllOrderByExtension().stream()
                .map(FixedExtensionSettingDto::from)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public FixedExtensionSettingDto updateFixedExtensionSetting(String extension, Boolean isBlocked) {
        FixedExtensionSetting setting = findFixedExtensionSetting(extension);
        
        setting.updateBlockStatus(isBlocked);
        FixedExtensionSetting saved = fixedExtensionSettingRepository.save(setting);
        
        // 확장자가 차단으로 변경된 경우 해당 확장자의 모든 파일 삭제
        if (isBlocked) {
            log.info("확장자 {} 차단으로 인한 기존 파일 삭제 시작", extension);
            storageService.deleteFilesByExtension(extension);
        }
        
        log.info(FileExtensionConstants.LogMessages.EXTENSION_SETTING_CHANGED, 
                extension, isBlocked ? "차단" : "허용");
        return FixedExtensionSettingDto.from(saved);
    }
    
    /**
     * 고정 확장자를 조회합니다.
     */
    private FixedExtensionSetting findFixedExtensionSetting(String extension) {
        return fixedExtensionSettingRepository.findByExtension(extension)
                .orElseThrow(() -> new ExtensionNotFoundException(
                        FileExtensionConstants.Messages.EXTENSION_NOT_FOUND, extension));
    }
    
    public List<CustomExtensionDto> getAllCustomExtensions() {
        return customExtensionRepository.findAll().stream()
                .map(CustomExtensionDto::from)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public CustomExtensionDto addCustomExtension(ExtensionRequest request) {
        String extension = normalizeExtension(request.getExtension());
        
        validateCustomExtension(extension);
        
        CustomExtension customExtension = CustomExtension.builder()
                .extension(extension)
                .build();
        
        CustomExtension saved = customExtensionRepository.save(customExtension);
        
        // 커스텀 확장자 추가 시 해당 확장자의 모든 파일 삭제
        log.info("커스텀 확장자 {} 추가로 인한 기존 파일 삭제 시작", extension);
        storageService.deleteFilesByExtension(extension);
        
        return CustomExtensionDto.from(saved);
    }
    
    /**
     * 확장자 문자열을 정규화합니다.
     */
    private String normalizeExtension(String extension) {
        if (extension == null) {
            return "";
        }
        
        String normalized = extension.toLowerCase().trim();
        
        // 점(.)으로 시작하는 경우 제거
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        
        return normalized;
    }
    
    /**
     * 커스텀 확장자 추가 전 검증
     */
    private void validateCustomExtension(String extension) {
        // 중복 확인
        if (customExtensionRepository.existsByExtension(extension)) {
            throw new IllegalArgumentException(FileExtensionConstants.Messages.EXTENSION_ALREADY_EXISTS);
        }
        
        // 최대 개수 확인
        long currentCount = customExtensionRepository.countCustomExtensions();
        if (currentCount >= FileExtensionConstants.FileLimit.MAX_CUSTOM_EXTENSIONS) {
            throw new IllegalArgumentException(FileExtensionConstants.Messages.MAX_EXTENSIONS_EXCEEDED);
        }
    }
    
    @Transactional
    public void deleteCustomExtension(Long id) {
        CustomExtension customExtension = customExtensionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 확장자를 찾을 수 없습니다."));
        
        customExtensionRepository.deleteById(id);
    }
    
    public boolean isExtensionBlocked(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return false;
        }
        
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return isExtensionBlocked(fileName, extension);
    }
    
    /**
     * 특정 확장자가 차단되었는지 확인
     */
    public boolean isExtensionBlocked(String fileName, String extension) {
        return isExtensionBlocked(fileName, extension, null);
    }
    
    /**
     * 고정 확장자 상태를 고려하여 특정 확장자가 차단되었는지 확인
     */
    public boolean isExtensionBlocked(String fileName, String extension, Map<String, Boolean> fixedExtensionStates) {
        if (extension == null || extension.trim().isEmpty()) {
            return false;
        }
        
        String cleanExtension = extension.toLowerCase().trim();
        
        // 고정 확장자 확인
        if (FileExtensionConstants.FIXED_EXTENSIONS.contains(cleanExtension)) {
            if (fixedExtensionStates != null && fixedExtensionStates.containsKey(cleanExtension)) {
                return fixedExtensionStates.get(cleanExtension);
            }
            // fixedExtensionStates가 없으면 DB에서 조회
            return fixedExtensionSettingRepository.findByExtension(cleanExtension)
                    .map(FixedExtensionSetting::getIsBlocked)
                    .orElse(false);
        }
        
        // 커스텀 확장자 확인
        List<String> customExtensions = customExtensionRepository.findAllExtensions();
        return customExtensions.contains(cleanExtension);
    }
    
    /**
     * 파일명 전체에 대해 고정 확장자 상태를 고려한 차단 여부 확인
     */
    public boolean isExtensionBlocked(String fileName, Map<String, Boolean> fixedExtensionStates) {
        if (fileName == null || !fileName.contains(".")) {
            return false;
        }
        
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return isExtensionBlocked(fileName, extension, fixedExtensionStates);
    }
}
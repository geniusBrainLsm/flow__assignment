package com.assignment.fileextension.config;

import com.assignment.fileextension.common.FileExtensionConstants;
import com.assignment.fileextension.entity.FixedExtensionSetting;
import com.assignment.fileextension.repository.FixedExtensionSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final FixedExtensionSettingRepository fixedExtensionSettingRepository;
    
    @Override
    public void run(String... args) throws Exception {
        initializeFixedExtensions();
    }
    
    private void initializeFixedExtensions() {
        long existingCount = fixedExtensionSettingRepository.count();
        
        if (existingCount == 0) {
            log.info("고정 확장자 초기 데이터 생성 시작");
            
            FileExtensionConstants.FIXED_EXTENSIONS.forEach(ext -> {
                FixedExtensionSetting setting = FixedExtensionSetting.builder()
                        .extension(ext)
                        .isBlocked(false)
                        .build();
                fixedExtensionSettingRepository.save(setting);
                log.debug("고정 확장자 생성: {}", ext);
            });
            
            log.info("고정 확장자 초기 데이터 생성 완료: {} 개", 
                    FileExtensionConstants.FIXED_EXTENSIONS.size());
        } else {
            log.info("고정 확장자 데이터가 이미 존재합니다: {} 개", existingCount);
        }
    }
}
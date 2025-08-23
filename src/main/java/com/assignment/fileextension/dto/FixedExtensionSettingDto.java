package com.assignment.fileextension.dto;

import com.assignment.fileextension.entity.FixedExtensionSetting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedExtensionSettingDto {
    
    private Long id;
    private String extension;
    private Boolean isBlocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static FixedExtensionSettingDto from(FixedExtensionSetting entity) {
        return FixedExtensionSettingDto.builder()
                .id(entity.getId())
                .extension(entity.getExtension())
                .isBlocked(entity.getIsBlocked())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
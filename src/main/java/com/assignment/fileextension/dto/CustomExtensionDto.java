package com.assignment.fileextension.dto;

import com.assignment.fileextension.entity.CustomExtension;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "커스텀 확장자 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomExtensionDto {
    @Schema(description = "확장자 ID", example = "1")
    private Long id;
    
    @Schema(description = "확장자", example = "zip")
    private String extension;
    
    public static CustomExtensionDto from(CustomExtension customExtension) {
        return CustomExtensionDto.builder()
                .id(customExtension.getId())
                .extension(customExtension.getExtension())
                .build();
    }
}
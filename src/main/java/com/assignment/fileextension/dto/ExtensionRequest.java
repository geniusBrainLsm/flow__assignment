package com.assignment.fileextension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "확장자 추가 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionRequest {
    
    @Schema(description = "추가할 확장자", example = "pdf", maxLength = 20)
    @NotBlank(message = "확장자는 필수입니다")
    @Size(max = 20, message = "확장자는 최대 20자까지 입력 가능합니다")
    private String extension;
}
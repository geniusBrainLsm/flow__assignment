package com.assignment.fileextension.controller;

import com.assignment.fileextension.common.FileExtensionConstants;
import com.assignment.fileextension.dto.CustomExtensionDto;
import com.assignment.fileextension.dto.ExtensionRequest;
import com.assignment.fileextension.dto.FixedExtensionSettingDto;
import com.assignment.fileextension.exception.ExtensionNotFoundException;
import com.assignment.fileextension.service.ExtensionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "확장자 관리", description = "파일 확장자 차단 설정을 관리하는 API")
@RestController
@RequestMapping("/api/extensions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExtensionController {
    
    private final ExtensionService extensionService;
    private final ObjectMapper objectMapper;
    //고정확장자 설정 조회 (체크인지 언체크인지)
    @Operation(summary = "고정 확장자 설정 상태 목록 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", 
                    content = @Content(schema = @Schema(implementation = FixedExtensionSettingDto.class)))
    })
    @GetMapping("/fixed")
    public ResponseEntity<List<FixedExtensionSettingDto>> getFixedExtensionSettings() {
        List<FixedExtensionSettingDto> settings = extensionService.getAllFixedExtensionSettings();
        return ResponseEntity.ok(settings);
    }

    //고정확장자 설정 변경 (체크 -> 언체크 혹은 언체크 -> 체크)
    @Operation(summary = "고정 확장자 설정 변경")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "변경 성공", 
                    content = @Content(schema = @Schema(implementation = FixedExtensionSettingDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "해당 확장자를 찾을 수 없음")
    })
    @PutMapping("/fixed/{extension}")
    public ResponseEntity<FixedExtensionSettingDto> updateFixedExtensionSetting(
            @Parameter(description = "확장자명", required = true, example = "exe") 
            @PathVariable String extension,
            @RequestBody Map<String, Boolean> request) {
        
        Boolean isBlocked = request.get("isBlocked");
        FixedExtensionSettingDto updated = extensionService.updateFixedExtensionSetting(extension, isBlocked);
        return ResponseEntity.ok(updated);
    }

    //커스텀확장자 조회
    @Operation(summary = "커스텀 확장자 목록 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", 
                    content = @Content(schema = @Schema(implementation = CustomExtensionDto.class)))
    })
    @GetMapping("/custom")
    public ResponseEntity<List<CustomExtensionDto>> getCustomExtensions() {
        List<CustomExtensionDto> extensions = extensionService.getAllCustomExtensions();
        return ResponseEntity.ok(extensions);
    }

    //커스텀 확장자 추가
    @Operation(summary = "커스텀 확장자 추가", description = "(최대 200개, 각 확장자는 20자 이하)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추가 성공", 
                    content = @Content(schema = @Schema(implementation = CustomExtensionDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (중복된 확장자, 최대 개수 초과 등)")
    })
    @PostMapping("/custom")
    public ResponseEntity<CustomExtensionDto> addCustomExtension(@Valid @RequestBody ExtensionRequest request) {
        CustomExtensionDto created = extensionService.addCustomExtension(request);
        return ResponseEntity.ok(created);
    }


    //커스텀 확장자 삭제
    @Operation(summary = "커스텀 확장자 삭제")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "해당 확장자를 찾을 수 없음")
    })
    @DeleteMapping("/custom/{id}")
    public ResponseEntity<Void> deleteCustomExtension(
            @Parameter(description = "확장자 ID", required = true) @PathVariable Long id) {
        extensionService.deleteCustomExtension(id);
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "파일 확장자 차단 여부 확인", description = "주어진 파일명의 확장자가 차단되었는지 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "확인 완료")
    })
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkExtension(
            @Parameter(description = "확인할 파일명", required = true, example = "test.exe") 
            @RequestParam String fileName,
            @Parameter(description = "고정 확장자 차단 상태 (JSON)", example = "{\"exe\":true,\"bat\":false}")
            @RequestParam(required = false) String fixedExtensionStates) {
        
        Map<String, Boolean> fixedStates = parseFixedExtensionStatesIfPresent(fixedExtensionStates);
        boolean isBlocked = extensionService.isExtensionBlocked(fileName, fixedStates);
        
        return ResponseEntity.ok(createBlockedResponse(isBlocked));
    }
    
    // isBlocked : ~~~ << 이거 파싱
    private Map<String, Boolean> parseFixedExtensionStatesIfPresent(String fixedExtensionStates) {
        if (fixedExtensionStates == null || fixedExtensionStates.isEmpty()) {
            return null;
        }
        
        try {
            return parseFixedExtensionStates(fixedExtensionStates);
        } catch (Exception e) {
            return null;
        }
    }

    // isBlocked : ~~~ << 이거 파싱
    private Map<String, Boolean> createBlockedResponse(boolean isBlocked) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("isBlocked", isBlocked);
        return response;
    }
    
    /**
     * ObjectMapper를 사용하여 고정 확장자 상태를 Map으로 변환
     */
    private Map<String, Boolean> parseFixedExtensionStates(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, new TypeReference<Map<String, Boolean>>() {});
        } catch (Exception e) {
            throw new RuntimeException("JSON 파싱 실패: " + jsonString, e);
        }
    }
}
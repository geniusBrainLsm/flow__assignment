package com.assignment.fileextension.controller;

import com.assignment.fileextension.common.FileExtensionConstants;
import com.assignment.fileextension.entity.UploadedFile;
import com.assignment.fileextension.exception.FileValidationException;
import com.assignment.fileextension.service.AuditService;
import com.assignment.fileextension.service.ExtensionService;
import com.assignment.fileextension.service.FileValidationService;
import com.assignment.fileextension.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "파일 업로드", description = "파일 업로드 확장자 검증 API")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileUploadController {
    
    private final StorageService storageService;
    private final FileValidationService fileValidationService;
    private final AuditService auditService;
    
    @Operation(summary = "파일 업로드")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업로드 성공"),
            @ApiResponse(responseCode = "400", description = "업로드 차단 또는 잘못된 요청")
    })
    @PostMapping("/file")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @Parameter(description = "업로드할 파일", required = true) 
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 업로드 시도 로그 기록
            auditService.logUploadAttempt(file, request);
            
            // 파일 검증 (빈 파일, 크기, 파일명 우회 공격 등)
            FileValidationService.FileValidationResult validationResult =
                    fileValidationService.validateFile(file);
            
            log.info("=== 파일 업로드 검증 결과 ===");
            log.info("파일명: {}", file.getOriginalFilename());
            log.info("차단 여부: {}", validationResult.isBlocked());
            log.info("차단 사유: {}", validationResult.getReason());
            
            if (validationResult.isBlocked()) {
                log.info("handleBlockedFile 호출");
                return handleBlockedFile(file, request, validationResult, response);
            }
            
            // 실제 파일 저장 및 성공 응답
            return handleSuccessfulUpload(file, request, response);
            
        } catch (FileValidationException e) {
            // 파일 검증 예외는 GlobalExceptionHandler에서 처리됨
            throw e;
        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", e.getMessage(), e);
            response.put("error", "파일 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            log.error("예상치 못한 파일 업로드 오류", e);
            response.put("error", "파일 업로드 중 예상치 못한 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    
    /**
     * 차단된 파일 업로드 처리
     */
    private ResponseEntity<Map<String, Object>> handleBlockedFile(
            MultipartFile file, HttpServletRequest request,
            FileValidationService.FileValidationResult validationResult,
            Map<String, Object> response) {
        
        // 차단된 파일 업로드 로그 기록
        auditService.logBlockedUpload(file, request, validationResult);
        
        response.put("error", validationResult.getReason());
        response.put("fileName", file.getOriginalFilename());
        response.put("blockReason", validationResult.getBlockReason());
        if (validationResult.getBlockedExtension() != null) {
            response.put("blockedExtension", validationResult.getBlockedExtension());
        }
        
        log.warn(FileExtensionConstants.LogMessages.FILE_UPLOAD_BLOCKED, 
                file.getOriginalFilename(), validationResult.getReason());
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 성공한 파일 업로드 처리
     */
    private ResponseEntity<Map<String, Object>> handleSuccessfulUpload(
            MultipartFile file, HttpServletRequest request,
            Map<String, Object> response) throws IOException {
        
        // 실제 파일 저장
        UploadedFile uploadedFile = storageService.storeFile(file);
        
        // 성공한 파일 업로드 로그 기록
        auditService.logSuccessfulUpload(file, request);
        
        response.put("success", true);
        response.put("message", "파일 업로드가 완료되었습니다.");
        response.put("fileId", uploadedFile.getId());
        response.put("originalFileName", uploadedFile.getOriginalFilename());
        response.put("storedFileName", uploadedFile.getStoredFilename());
        response.put("fileSize", uploadedFile.getFileSize());
        response.put("filePath", uploadedFile.getFilePath());
        
        log.info(FileExtensionConstants.LogMessages.FILE_UPLOAD_SUCCESS, 
                file.getOriginalFilename(), uploadedFile.getId());
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "파일 업로드 안하고 검증만")
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkFileOnly(
            @Parameter(description = "검증할 파일", required = true) 
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        if (file.isEmpty()) {
            response.put("error", "파일이 선택되지 않았습니다.");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 파일 검증 (크기, 파일명 우회 공격 등)
        FileValidationService.FileValidationResult validationResult = fileValidationService.validateFile(file);
        
        response.put("fileName", file.getOriginalFilename());
        response.put("fileSize", file.getSize());
        
        if (validationResult.isBlocked()) {
            // 차단된 파일 검증 시도 로그 기록
            auditService.logBlockedUpload(file, request, validationResult);
            
            response.put("result", "blocked");
            response.put("message", validationResult.getReason());
            response.put("blockReason", validationResult.getBlockReason());
            if (validationResult.getBlockedExtension() != null) {
                response.put("blockedExtension", validationResult.getBlockedExtension());
            }
        } else {
            response.put("result", "allowed");
            response.put("message", "파일 업로드가 허용됩니다.");
        }
        
        return ResponseEntity.ok(response);
    }
}
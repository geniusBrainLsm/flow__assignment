package com.assignment.fileextension.controller;

import com.assignment.fileextension.common.FileExtensionConstants;
import com.assignment.fileextension.entity.UploadedFile;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "파일 관리", description = "업로드된 파일의 관리 API")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileManagementController {
    
    private final StorageService storageService;
    
    @Operation(summary = "전체 파일 목록 조회")
    @GetMapping
    public ResponseEntity<List<UploadedFile>> getAllFiles() {
        List<UploadedFile> files = storageService.getFilesByStatus(UploadedFile.FileStatus.ACTIVE);
        return ResponseEntity.ok(files);
    }
    
    @Operation(summary = "특정 파일 정보 조회")
    @GetMapping("/{fileId}")
    public ResponseEntity<UploadedFile> getFileById(
            @Parameter(description = "파일 ID", required = true)
            @PathVariable Long fileId) {
        
        UploadedFile file = storageService.findById(fileId);
        
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(file);
    }
    
    @Operation(summary = "파일 다운로드")
    @GetMapping("/{fileId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(
            @Parameter(description = "파일 ID", required = true)
            @PathVariable Long fileId) {
        
        try {
            // 파일 정보 조회
            UploadedFile uploadedFile = storageService.findById(fileId);
            
            if (uploadedFile == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 파일 리소스 로드
            java.nio.file.Path filePath = java.nio.file.Paths.get(uploadedFile.getFilePath());
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            // Content-Disposition 헤더 설정
            String contentDisposition = "attachment; filename=\"" + uploadedFile.getOriginalFilename() + "\"";
            
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("파일 다운로드 실패: ID {} - {}", fileId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    
    @Operation(summary = "상태별 파일 목록 조회")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<UploadedFile>> getFilesByStatus(
            @Parameter(description = "파일 상태", required = true)
            @PathVariable UploadedFile.FileStatus status) {
        List<UploadedFile> files = storageService.getFilesByStatus(status);
        return ResponseEntity.ok(files);
    }
    
    @Operation(summary = "확장자별 파일 목록 조회")
    @GetMapping("/extension/{extension}")
    public ResponseEntity<List<UploadedFile>> getFilesByExtension(
            @Parameter(description = "파일 확장자", required = true)
            @PathVariable String extension) {
        List<UploadedFile> files = storageService.getFilesByExtension(extension.toLowerCase());
        return ResponseEntity.ok(files);
    }
    
    
    
    @Operation(summary = "파일 삭제")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "삭제 실패")
    })
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, String>> deleteFile(
            @Parameter(description = "파일 ID", required = true)
            @PathVariable Long fileId) {
        
        try {
            storageService.deletePhysicalFile(fileId);
            return createSuccessResponse("파일이 성공적으로 삭제되었습니다.", fileId);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
            
        } catch (IOException e) {
            return handleFileDeleteError(fileId, e);
        }
    }
    
    @Operation(summary = "파일 삭제 예외 설정")
    @PutMapping("/{fileId}/protection")
    public ResponseEntity<Map<String, Object>> setDeletionException(
            @Parameter(description = "파일 ID", required = true)
            @PathVariable Long fileId,
            @RequestBody Map<String, Boolean> request) {
        
        try {
            Boolean deletionException = request.get("deletionException");
            if (deletionException == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "deletionException 필드가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            storageService.setDeletionException(fileId, deletionException);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", deletionException ? 
                "파일이 삭제 예외로 설정되었습니다." : "파일의 삭제 예외가 해제되었습니다.");
            response.put("deletionException", deletionException);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("삭제 예외 설정 실패: ID {} - {}", fileId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "삭제 예외 설정 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 파일 삭제 성공 응답을 생성합니다.
     */
    private ResponseEntity<Map<String, String>> createSuccessResponse(String message, Long fileId) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        log.info(FileExtensionConstants.LogMessages.FILE_DELETE_SUCCESS, fileId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 파일 삭제 오류를 처리합니다.
     */
    private ResponseEntity<Map<String, String>> handleFileDeleteError(Long fileId, IOException e) {
        Map<String, String> response = new HashMap<>();
        log.error("파일 삭제 실패: ID {} - {}", fileId, e.getMessage(), e);
        response.put("error", "파일 삭제 중 오류가 발생했습니다.");
        return ResponseEntity.internalServerError().body(response);
    }

}
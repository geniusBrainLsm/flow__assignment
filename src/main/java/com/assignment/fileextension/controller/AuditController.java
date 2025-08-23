package com.assignment.fileextension.controller;

import com.assignment.fileextension.common.FileExtensionConstants;
import com.assignment.fileextension.entity.FileAuditLog;
import com.assignment.fileextension.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "감사 로그", description = "파일 업로드 감사 로그 조회 API")
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuditController {
    
    private final AuditService auditService;
    
    @Operation(summary = "차단된 업로드 시도 조회")
    @GetMapping("/blocked")
    public ResponseEntity<Page<FileAuditLog>> getBlockedUploads(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = createPageable(page, size);
        Page<FileAuditLog> blockedUploads = auditService.getBlockedUploads(pageable);
        
        return ResponseEntity.ok(blockedUploads);
    }
    private Pageable createPageable(int page, int size) {
        return PageRequest.of(page, size);
    }
}
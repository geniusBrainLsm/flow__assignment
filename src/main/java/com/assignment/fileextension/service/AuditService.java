package com.assignment.fileextension.service;

import com.assignment.fileextension.common.FileExtensionConstants;
import com.assignment.fileextension.dto.AuditLogDto;
import com.assignment.fileextension.entity.FileAuditLog;
import com.assignment.fileextension.enums.BlockReason;
import com.assignment.fileextension.repository.FileAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {
    
    private final FileAuditLogRepository auditLogRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    
    /**
     * 파일 업로드 시도 로그 기록
     */
    @Transactional
    public void logUploadAttempt(MultipartFile file, HttpServletRequest request) {
        try {
            FileAuditLog auditLog = createUploadAttemptLog(file, request);
            auditLogRepository.save(auditLog);
            log.debug("파일 업로드 시도 로그 기록: {}", file.getOriginalFilename());
        } catch (Exception e) {
            log.error(FileExtensionConstants.LogMessages.AUDIT_LOG_FAILED, "업로드 시도", e.getMessage());
        }
    }
    
    /**
     * 업로드 시도 로그를 생성합니다.
     */
    private FileAuditLog createUploadAttemptLog(MultipartFile file, HttpServletRequest request) {
        return FileAuditLog.createUploadAttempt(
                file.getOriginalFilename(),
                file.getSize(),
                getClientIpAddress(request),
                getUserAgent(request)
        );
    }
    
    /**
     * 차단된 파일 업로드 로그 기록
     */
    @Transactional
    public void logBlockedUpload(MultipartFile file, HttpServletRequest request, 
                               FileValidationService.FileValidationResult validationResult) {
        try {
            log.info("=== 차단된 업로드 로그 시작 ===");
            log.info("파일명: {}", file.getOriginalFilename());
            log.info("IP: {}", getClientIpAddress(request));
            log.info("차단 사유: {}", validationResult.getReason());
            
            FileAuditLog auditLog = createBlockedUploadLog(file, request, validationResult);
            log.info("감사 로그 생성 완료: {}", auditLog);
            
            FileAuditLog savedLog = auditLogRepository.save(auditLog);
            log.info("감사 로그 저장 완료: ID = {}", savedLog.getId());
            
            // WebSocket으로 실시간 알림 전송
            AuditLogDto auditLogDto = AuditLogDto.fromEntity(savedLog);
            webSocketNotificationService.sendAuditLogUpdate(auditLogDto);
            
            log.warn("차단된 파일 업로드 로그 기록: {} - {}", 
                    file.getOriginalFilename(), validationResult.getReason());
        } catch (Exception e) {
            log.error("차단된 업로드 로그 저장 실패", e);
            log.error(FileExtensionConstants.LogMessages.AUDIT_LOG_FAILED, "차단된 업로드", e.getMessage());
        }
    }
    
    /**
     * 차단된 업로드 로그를 생성합니다.
     */
    private FileAuditLog createBlockedUploadLog(MultipartFile file, HttpServletRequest request,
                                              FileValidationService.FileValidationResult validationResult) {
        return FileAuditLog.createBlockedUpload(
                file.getOriginalFilename(),
                file.getSize(),
                getClientIpAddress(request),
                getUserAgent(request),
                validationResult.getReason(),
                validationResult.getBlockedExtension(),
                validationResult.getBlockReason()
        );
    }
    
    /**
     * 성공한 파일 업로드 로그 기록
     */
    @Transactional
    public void logSuccessfulUpload(MultipartFile file, HttpServletRequest request) {
        try {
            FileAuditLog auditLog = createSuccessfulUploadLog(file, request);
            auditLogRepository.save(auditLog);
            log.info("성공한 파일 업로드 로그 기록: {}", file.getOriginalFilename());
        } catch (Exception e) {
            log.error(FileExtensionConstants.LogMessages.AUDIT_LOG_FAILED, "성공 업로드", e.getMessage());
        }
    }
    
    /**
     * 성공한 업로드 로그를 생성합니다.
     */
    private FileAuditLog createSuccessfulUploadLog(MultipartFile file, HttpServletRequest request) {
        return FileAuditLog.createSuccessfulUpload(
                file.getOriginalFilename(),
                file.getSize(),
                getClientIpAddress(request),
                getUserAgent(request)
        );
    }
    
    /**
     * 차단된 업로드 시도 조회
     */
    public Page<FileAuditLog> getBlockedUploads(Pageable pageable) {
        return auditLogRepository.findByBlockedTrueOrderByUploadTimeDesc(pageable);
    }
    
    /**
     * User-Agent 헤더를 추출합니다.
     */
    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
    
    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // X-Forwarded-For 헤더 확인
        String ipAddress = getIpFromHeader(request, "X-Forwarded-For");
        if (isValidIpAddress(ipAddress)) {
            return ipAddress.split(",")[0].trim();
        }
        
        // X-Real-IP 헤더 확인
        ipAddress = getIpFromHeader(request, "X-Real-IP");
        if (isValidIpAddress(ipAddress)) {
            return ipAddress;
        }
        
        // 기본 Remote Address 반환
        return request.getRemoteAddr();
    }
    
    /**
     * 헤더에서 IP 주소를 추출합니다.
     */
    private String getIpFromHeader(HttpServletRequest request, String headerName) {
        return request.getHeader(headerName);
    }
    
    /**
     * IP 주소의 유효성을 검증합니다.
     */
    private boolean isValidIpAddress(String ipAddress) {
        return ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress);
    }
    
}
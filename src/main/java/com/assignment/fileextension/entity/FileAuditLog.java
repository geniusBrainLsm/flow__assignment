package com.assignment.fileextension.entity;

import com.assignment.fileextension.enums.BlockReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "userId"),
    @Index(name = "idx_audit_blocked", columnList = "blocked"),
    @Index(name = "idx_audit_upload_time", columnList = "uploadTime"),
    @Index(name = "idx_audit_ip_address", columnList = "ipAddress")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class FileAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = true)
    private String userId; // 사용자 ID (추후 인증 시스템 연동 시 사용)
    
    @Column(nullable = false, length = 500)
    private String filename;
    
    @Column(nullable = false)
    private Long fileSize;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadTime;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean blocked = false;
    
    @Column(length = 1000)
    private String blockReason;
    
    @Column(length = 50)
    private String blockedExtension;
    
    @Column(nullable = false, length = 45) // IPv6 최대 길이
    private String ipAddress;
    
    @Column(length = 1000)
    private String userAgent;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ActionType actionType = ActionType.UPLOAD_ATTEMPT;
    
    @Enumerated(EnumType.STRING)
    @Column
    private BlockReason blockReasonType;
    
    public enum ActionType {
        UPLOAD_ATTEMPT,     // 파일 업로드 시도
        UPLOAD_SUCCESS,     // 파일 업로드 성공
        UPLOAD_BLOCKED,     // 파일 업로드 차단
        EXTENSION_CHANGED,  // 확장자 설정 변경
        FILE_QUARANTINED,   // 파일 격리
        FILE_RESTORED       // 파일 복구
    }
    
    
    public static FileAuditLog createUploadAttempt(String filename, Long fileSize, String ipAddress, String userAgent) {
        return FileAuditLog.builder()
                .filename(filename)
                .fileSize(fileSize)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .actionType(ActionType.UPLOAD_ATTEMPT)
                .blocked(false)
                .build();
    }
    
    public static FileAuditLog createBlockedUpload(String filename, Long fileSize, String ipAddress, 
                                                   String userAgent, String blockReason, String blockedExtension, 
                                                   BlockReason blockReasonType) {
        return FileAuditLog.builder()
                .filename(filename)
                .fileSize(fileSize)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .actionType(ActionType.UPLOAD_BLOCKED)
                .blocked(true)
                .blockReason(blockReason)
                .blockedExtension(blockedExtension)
                .blockReasonType(blockReasonType)
                .build();
    }
    
    public static FileAuditLog createSuccessfulUpload(String filename, Long fileSize, String ipAddress, String userAgent) {
        return FileAuditLog.builder()
                .filename(filename)
                .fileSize(fileSize)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .actionType(ActionType.UPLOAD_SUCCESS)
                .blocked(false)
                .build();
    }
}
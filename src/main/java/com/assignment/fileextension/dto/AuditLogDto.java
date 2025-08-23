package com.assignment.fileextension.dto;

import com.assignment.fileextension.entity.FileAuditLog;
import com.assignment.fileextension.enums.BlockReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private String filename;
    private Long fileSize;
    private String ipAddress;
    private String userAgent;
    private Boolean blocked;
    private String message;
    private String blockedExtension;
    private BlockReason blockReason;
    private LocalDateTime uploadTime;

    public static AuditLogDto fromEntity(FileAuditLog entity) {
        return AuditLogDto.builder()
                .id(entity.getId())
                .filename(entity.getFilename())
                .fileSize(entity.getFileSize())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .blocked(entity.getBlocked())
                .message(entity.getBlockReason())
                .blockedExtension(entity.getBlockedExtension())
                .blockReason(entity.getBlockReasonType())
                .uploadTime(entity.getUploadTime())
                .build();
    }
}
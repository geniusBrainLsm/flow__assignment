package com.assignment.fileextension.service;

import com.assignment.fileextension.dto.AuditLogDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public void sendAuditLogUpdate(AuditLogDto auditLog) {
        try {
            messagingTemplate.convertAndSend("/topic/audit-logs", auditLog);
            log.debug("웹소켓으로 감사 로그 전송: {}", auditLog.getMessage());
        } catch (Exception e) {
            log.error("웹소켓 감사 로그 전송 실패: {}", e.getMessage(), e);
        }
    }
}
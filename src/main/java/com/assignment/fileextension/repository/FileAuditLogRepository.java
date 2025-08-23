package com.assignment.fileextension.repository;

import com.assignment.fileextension.entity.FileAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileAuditLogRepository extends JpaRepository<FileAuditLog, Long> {
    
    // 차단된 파일 업로드 시도 조회
    Page<FileAuditLog> findByBlockedTrueOrderByUploadTimeDesc(Pageable pageable);
}
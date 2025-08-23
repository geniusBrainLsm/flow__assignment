package com.assignment.fileextension.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "uploaded_files")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UploadedFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String originalFilename;
    
    @Column(nullable = false, unique = true)
    private String storedFilename;
    
    @Column(nullable = false)
    private String filePath;
    
    @Column(length = 20)
    private String extension;
    
    @Column(nullable = false)
    private Long fileSize;
    
    @Column
    private String contentType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FileStatus status = FileStatus.ACTIVE;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean deletionException = false;
    
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum FileStatus {
        ACTIVE,      // 활성 파일
        DELETED      // 삭제된 파일
    }
    
    
    public void markAsDeleted() {
        this.status = FileStatus.DELETED;
    }
    
    public void setDeletionException(Boolean deletionException) {
        this.deletionException = deletionException;
    }
    
    public boolean isProtectedFromDeletion() {
        return this.deletionException != null && this.deletionException;
    }
}
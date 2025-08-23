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
@Table(name = "fixed_extension_settings")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class FixedExtensionSetting {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 20)
    private String extension;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isBlocked = false;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public void updateBlockStatus(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }
    
    public static FixedExtensionSetting of(String extension, Boolean isBlocked) {
        return FixedExtensionSetting.builder()
                .extension(extension)
                .isBlocked(isBlocked)
                .build();
    }
}
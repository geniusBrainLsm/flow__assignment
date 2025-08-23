package com.assignment.fileextension.repository;

import com.assignment.fileextension.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    
    Optional<UploadedFile> findByStoredFilename(String storedFilename);
    
    List<UploadedFile> findByExtension(String extension);
    
    List<UploadedFile> findByStatus(UploadedFile.FileStatus status);
    
    @Query("SELECT f FROM UploadedFile f WHERE f.extension = :extension AND f.status = :status")
    List<UploadedFile> findByExtensionAndStatus(@Param("extension") String extension, 
                                                @Param("status") UploadedFile.FileStatus status);
    
    @Query("SELECT f FROM UploadedFile f WHERE f.extension IN :extensions AND f.status = 'ACTIVE'")
    List<UploadedFile> findActiveFilesByExtensions(@Param("extensions") List<String> extensions);
    
    long countByStatus(UploadedFile.FileStatus status);
}
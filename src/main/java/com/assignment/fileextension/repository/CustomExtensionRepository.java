package com.assignment.fileextension.repository;

import com.assignment.fileextension.entity.CustomExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomExtensionRepository extends JpaRepository<CustomExtension, Long> {
    
    Optional<CustomExtension> findByExtension(String extension);
    
    boolean existsByExtension(String extension);
    
    @Query("SELECT COUNT(c) FROM CustomExtension c")
    long countCustomExtensions();
    
    @Query("SELECT c.extension FROM CustomExtension c")
    List<String> findAllExtensions();
}
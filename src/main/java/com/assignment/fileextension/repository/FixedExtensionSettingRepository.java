package com.assignment.fileextension.repository;

import com.assignment.fileextension.entity.FixedExtensionSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FixedExtensionSettingRepository extends JpaRepository<FixedExtensionSetting, Long> {
    
    Optional<FixedExtensionSetting> findByExtension(String extension);
    
    @Query("SELECT f.extension FROM FixedExtensionSetting f WHERE f.isBlocked = true")
    List<String> findBlockedExtensions();
    
    @Query("SELECT f FROM FixedExtensionSetting f ORDER BY f.extension")
    List<FixedExtensionSetting> findAllOrderByExtension();
}
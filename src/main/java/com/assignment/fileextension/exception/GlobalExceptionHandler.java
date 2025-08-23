package com.assignment.fileextension.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.validation.FieldError;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리기
 * 애플리케이션에서 발생하는 예외들을 일관성 있게 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 파일 검증 실패 예외 처리
     */
    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<Map<String, Object>> handleFileValidationException(FileValidationException e) {
        log.warn("파일 검증 실패: {}", e.getReason());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", e.getReason());
        response.put("type", "FILE_VALIDATION_ERROR");
        
        if (e.hasBlockedExtension()) {
            response.put("blockedExtension", e.getBlockedExtension());
        }
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 확장자 찾을 수 없음 예외 처리
     */
    @ExceptionHandler(ExtensionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleExtensionNotFoundException(ExtensionNotFoundException e) {
        log.warn("확장자 조회 실패: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", e.getMessage());
        response.put("type", "EXTENSION_NOT_FOUND");
        response.put("extension", e.getExtension());
        
        return ResponseEntity.notFound().build();
    }
    
    /**
     * 파일 크기 초과 예외 처리 (Spring Boot 기본)
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException e) {
        log.warn("파일 크기 초과: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "파일 크기가 최대 허용 크기(100MB)를 초과했습니다");
        response.put("type", "FILE_SIZE_EXCEEDED");
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 일반적인 IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("잘못된 요청: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", e.getMessage());
        response.put("type", "INVALID_ARGUMENT");
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Bean Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("입력 값 검증 실패: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        
        // 첫 번째 필드 오류를 가져옴
        FieldError fieldError = e.getBindingResult().getFieldErrors().get(0);
        response.put("error", fieldError.getDefaultMessage());
        response.put("type", "VALIDATION_ERROR");
        response.put("field", fieldError.getField());
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        log.error("예상치 못한 오류 발생", e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "서버 내부 오류가 발생했습니다");
        response.put("type", "INTERNAL_SERVER_ERROR");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
package com.assignment.fileextension.exception;

/**
 * 파일 검증 실패 시 발생하는 예외
 */
public class FileValidationException extends RuntimeException {
    
    private final String reason;
    private final String blockedExtension;
    
    public FileValidationException(String message) {
        super(message);
        this.reason = message;
        this.blockedExtension = null;
    }
    
    public FileValidationException(String message, String blockedExtension) {
        super(message);
        this.reason = message;
        this.blockedExtension = blockedExtension;
    }
    
    public FileValidationException(String message, Throwable cause) {
        super(message, cause);
        this.reason = message;
        this.blockedExtension = null;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getBlockedExtension() {
        return blockedExtension;
    }
    
    public boolean hasBlockedExtension() {
        return blockedExtension != null;
    }
}
package com.assignment.fileextension.exception;

/**
 * 확장자를 찾을 수 없을 때 발생하는 예외
 */
public class ExtensionNotFoundException extends RuntimeException {
    
    private final String extension;
    
    public ExtensionNotFoundException(String extension) {
        super("확장자를 찾을 수 없습니다: " + extension);
        this.extension = extension;
    }
    
    public ExtensionNotFoundException(String message, String extension) {
        super(message);
        this.extension = extension;
    }
    
    public String getExtension() {
        return extension;
    }
}
package com.assignment.fileextension.common;

import java.util.Set;

/**
 * 파일 확장자 시스템 전체에서 사용하는 상수들
 */
public final class FileExtensionConstants {
    
    private FileExtensionConstants() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
    
    /**
     * 고정 확장자 목록
     */
    public static final Set<String> FIXED_EXTENSIONS = Set.of(
            "bat", "cmd", "com", "cpl", "exe", "scr", "js"
    );
    
    /**
     * 파일 제한 관련 상수
     */
    public static final class FileLimit {
        public static final int MAX_CUSTOM_EXTENSIONS = 200;
        public static final int MAX_EXTENSION_NAME_LENGTH = 20;
        public static final long MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024L; // 100MB
        
        private FileLimit() {}
    }
    
    /**
     * 파일 상태 관련 메시지
     */
    public static final class Messages {
        public static final String FILE_BLOCKED = "차단된 확장자가 포함되어 있습니다";
        public static final String FILE_TOO_LARGE = "파일 크기가 최대 허용 크기(100MB)를 초과했습니다";
        public static final String INVALID_FILENAME = "파일명이 올바르지 않습니다";
        public static final String EXTENSION_NOT_FOUND = "해당 확장자를 찾을 수 없습니다";
        public static final String EXTENSION_ALREADY_EXISTS = "이미 등록된 확장자입니다";
        public static final String MAX_EXTENSIONS_EXCEEDED = "커스텀 확장자는 최대 " + FileLimit.MAX_CUSTOM_EXTENSIONS + "개까지 추가 가능합니다";
        
        private Messages() {}
    }
    
    /**
     * 로그 메시지 템플릿
     */
    public static final class LogMessages {
        public static final String EXTENSION_SETTING_CHANGED = "고정 확장자 '{}' 설정 변경: {}";
        public static final String FILE_UPLOAD_SUCCESS = "파일 업로드 성공: {} (ID: {})";
        public static final String FILE_UPLOAD_BLOCKED = "차단된 파일 업로드 시도: {} - {}";
        public static final String FILE_DELETE_SUCCESS = "파일 삭제 완료: ID {}";
        public static final String AUDIT_LOG_FAILED = "{} 로그 기록 실패: {}";
        
        private LogMessages() {}
    }
}
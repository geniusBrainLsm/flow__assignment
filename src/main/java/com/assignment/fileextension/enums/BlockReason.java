package com.assignment.fileextension.enums;

/**
 * 파일 업로드 차단 사유
 */
public enum BlockReason {
    FILE_SIZE_EXCEEDED,    // 파일 크기 초과
    BLOCKED_EXTENSION,     // 차단된 확장자
    INVALID_FILENAME,      // 올바르지 않은 파일명
    BYPASS_ATTEMPT         // 우회 시도
}
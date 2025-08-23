package com.assignment.fileextension.integration;

import com.assignment.fileextension.entity.CustomExtension;
import com.assignment.fileextension.entity.FixedExtensionSetting;
import com.assignment.fileextension.entity.UploadedFile;
import com.assignment.fileextension.repository.CustomExtensionRepository;
import com.assignment.fileextension.repository.FixedExtensionSettingRepository;
import com.assignment.fileextension.repository.UploadedFileRepository;
import com.assignment.fileextension.fixtures.TestDataFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("파일 확장자 차단 시스템 통합 테스트")
class FileExtensionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FixedExtensionSettingRepository fixedExtensionSettingRepository;

    @Autowired
    private CustomExtensionRepository customExtensionRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 기본 고정 확장자 설정 초기화
        fixedExtensionSettingRepository.deleteAll();
        customExtensionRepository.deleteAll();
        uploadedFileRepository.deleteAll();

        List<FixedExtensionSetting> defaultSettings = TestDataFixture.createDefaultFixedExtensions();
        fixedExtensionSettingRepository.saveAll(defaultSettings);
    }

    @Test
    @DisplayName("전체 워크플로우 테스트 - 커스텀 확장자 관리")
    void fullWorkflow_CustomExtensionManagement() throws Exception {
        // 1. 커스텀 확장자 추가
        mockMvc.perform(post("/api/extensions/custom")
                .param("extension", "malware"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extension").value("malware"));

        // 2. 커스텀 확장자 목록 확인
        mockMvc.perform(get("/api/extensions/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].extension").value("malware"));

        // 3. 차단된 확장자로 파일 업로드 시도 (실패)
        MockMultipartFile blockedFile = new MockMultipartFile(
            "file", "virus.malware", "application/octet-stream", "content".getBytes()
        );

        mockMvc.perform(multipart("/api/upload/file").file(blockedFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.blockedExtension").value("malware"));

        // 4. 허용된 확장자로 파일 업로드 (성공)
        MockMultipartFile allowedFile = new MockMultipartFile(
            "file", "document.txt", "text/plain", "content".getBytes()
        );

        mockMvc.perform(multipart("/api/upload/file").file(allowedFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.originalFileName").value("document.txt"));

        // 5. 업로드된 파일 확인
        List<UploadedFile> uploadedFiles = uploadedFileRepository.findAll();
        assertThat(uploadedFiles).hasSize(1);
        assertThat(uploadedFiles.get(0).getOriginalFilename()).isEqualTo("document.txt");

        // 6. 커스텀 확장자 삭제
        CustomExtension malwareExt = customExtensionRepository.findByExtension("malware").orElseThrow();
        mockMvc.perform(delete("/api/extensions/custom/{id}", malwareExt.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("확장자가 삭제되었습니다."));

        // 7. 삭제 후 해당 확장자 파일 업로드 허용 확인
        mockMvc.perform(get("/api/extensions/check")
                .param("filename", "virus.malware"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(false));
    }

    @Test
    @DisplayName("고정 확장자 설정 변경 워크플로우")
    void fixedExtensionSettingWorkflow() throws Exception {
        // 1. 초기 상태에서 exe는 차단
        mockMvc.perform(get("/api/extensions/check")
                .param("filename", "malware.exe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));

        // 2. exe 확장자를 허용으로 변경
        mockMvc.perform(put("/api/extensions/fixed/exe")
                .param("blocked", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extension").value("exe"))
                .andExpect(jsonPath("$.blocked").value(false));

        // 3. 변경 후 exe 파일 업로드 허용 확인
        MockMultipartFile exeFile = new MockMultipartFile(
            "file", "program.exe", "application/octet-stream", "content".getBytes()
        );

        mockMvc.perform(multipart("/api/upload/file").file(exeFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. exe를 다시 차단으로 변경
        mockMvc.perform(put("/api/extensions/fixed/exe")
                .param("blocked", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));

        // 5. 차단 후 기존 exe 파일들이 삭제되었는지 확인
        List<UploadedFile> exeFiles = uploadedFileRepository.findByExtension("exe");
        assertThat(exeFiles).isEmpty(); // deleteFilesByExtension에 의해 삭제됨
    }

    @Test
    @DisplayName("다중 확장자 우회 공격 방지 테스트")
    void bypassAttackPrevention() throws Exception {
        // 1. 커스텀 확장자 추가
        mockMvc.perform(post("/api/extensions/custom")
                .param("extension", "virus"))
                .andExpect(status().isOk());

        // 2. 다양한 우회 시도 패턴 테스트
        String[] bypassAttempts = {
            "document.pdf.exe",     // exe는 기본 차단
            "image.jpg.virus",      // virus는 커스텀 차단
            "backup.2024.exe.txt",  // 숫자 포함 다중 확장자
            "file.exe.pdf.txt"      // 다중 확장자 체인
        };

        for (String filename : bypassAttempts) {
            MockMultipartFile bypassFile = new MockMultipartFile(
                "file", filename, "application/octet-stream", "content".getBytes()
            );

            mockMvc.perform(multipart("/api/upload/check").file(bypassFile))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("blocked"))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Test
    @DisplayName("파일 관리 워크플로우 - 업로드, 다운로드, 삭제 보호")
    void fileManagementWorkflow() throws Exception {
        // 1. 파일 업로드
        MockMultipartFile file1 = new MockMultipartFile(
            "file", "document1.txt", "text/plain", "content1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
            "file", "document2.pdf", "application/pdf", "content2".getBytes()
        );

        String response1 = mockMvc.perform(multipart("/api/upload/file").file(file1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String response2 = mockMvc.perform(multipart("/api/upload/file").file(file2))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 2. 업로드된 파일 목록 확인
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        // 3. 특정 확장자 파일 조회
        mockMvc.perform(get("/api/files/extension/txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].extension").value("txt"));

        // 4. 파일 삭제 보호 설정
        List<UploadedFile> files = uploadedFileRepository.findAll();
        Long fileId = files.get(0).getId();

        mockMvc.perform(put("/api/files/{fileId}/protection", fileId)
                .param("protected", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // 5. 확장자 차단 설정 변경으로 파일 삭제 시도 (보호된 파일은 삭제되지 않음)
        mockMvc.perform(post("/api/extensions/custom")
                .param("extension", "txt"))
                .andExpect(status().isOk());

        // 6. 보호된 파일은 여전히 존재하는지 확인
        UploadedFile protectedFile = uploadedFileRepository.findById(fileId).orElseThrow();
        assertThat(protectedFile.getDeletionException()).isTrue();
        assertThat(protectedFile.getStatus()).isEqualTo(UploadedFile.FileStatus.ACTIVE);
    }

    @Test
    @DisplayName("에러 시나리오 테스트 - 중복 확장자 추가")
    void errorScenario_DuplicateExtension() throws Exception {
        // 1. 커스텀 확장자 추가
        mockMvc.perform(post("/api/extensions/custom")
                .param("extension", "malware"))
                .andExpect(status().isOk());

        // 2. 동일한 확장자 다시 추가 시도 (실패)
        mockMvc.perform(post("/api/extensions/custom")
                .param("extension", "malware"))
                .andExpectStatus().isBadRequest();

        // 3. 고정 확장자와 동일한 커스텀 확장자 추가 시도 (실패)
        mockMvc.perform(post("/api/extensions/custom")
                .param("extension", "exe"))
                .andExpectStatus().isBadRequest();
    }

    @Test
    @DisplayName("파일 크기 제한 테스트")
    void fileSizeLimitTest() throws Exception {
        // 큰 파일 업로드 시도 (실패)
        MockMultipartFile largeFile = new MockMultipartFile(
            "file", "large.txt", "text/plain", new byte[101 * 1024 * 1024] // 101MB
        );

        mockMvc.perform(multipart("/api/upload/file").file(largeFile))
                .andExpectStatus().isBadRequest();

        // 검증만 수행 시에도 실패
        mockMvc.perform(multipart("/api/upload/check").file(largeFile))
                .andExpectStatus().isBadRequest();
    }

    @Test
    @DisplayName("감사 로그 워크플로우 테스트")
    void auditLogWorkflow() throws Exception {
        // 1. 차단된 파일 업로드 시도
        MockMultipartFile blockedFile = new MockMultipartFile(
            "file", "malware.exe", "application/octet-stream", "content".getBytes()
        );

        mockMvc.perform(multipart("/api/upload/file").file(blockedFile))
                .andExpectStatus().isBadRequest();

        // 2. 성공적인 파일 업로드
        MockMultipartFile allowedFile = new MockMultipartFile(
            "file", "document.txt", "text/plain", "content".getBytes()
        );

        mockMvc.perform(multipart("/api/upload/file").file(allowedFile))
                .andExpect(status().isOk());

        // 3. 차단된 업로드 로그 조회
        mockMvc.perform(get("/api/audit/blocked"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].filename").value("malware.exe"))
                .andExpect(jsonPath("$.content[0].blocked").value(true));
    }

    @Test
    @DisplayName("동시성 테스트 - 동일한 확장자 동시 추가")
    void concurrencyTest_SimultaneousExtensionAddition() throws Exception {
        // 동시에 같은 확장자 추가 시도
        // 이는 실제 동시성 테스트는 아니지만, 기본적인 중복 처리 로직을 확인
        
        mockMvc.perform(post("/api/extensions/custom")
                .param("extension", "test"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/extensions/custom")
                .param("extension", "test"))
                .andExpectStatus().isBadRequest();

        // DB에는 하나만 존재하는지 확인
        List<CustomExtension> extensions = customExtensionRepository.findAll();
        long testExtCount = extensions.stream()
                .filter(ext -> "test".equals(ext.getExtension()))
                .count();
        assertThat(testExtCount).isEqualTo(1);
    }
}
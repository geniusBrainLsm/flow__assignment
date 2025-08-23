package com.assignment.fileextension.unit.controller;

import com.assignment.fileextension.controller.ExtensionController;
import com.assignment.fileextension.dto.CustomExtensionDto;
import com.assignment.fileextension.dto.FixedExtensionSettingDto;
import com.assignment.fileextension.service.ExtensionService;
import com.assignment.fileextension.fixtures.TestDataFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ExtensionController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
})
@TestPropertySource(properties = {
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
@DisplayName("ExtensionController 단위 테스트")
class ExtensionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExtensionService extensionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("고정 확장자 설정 조회 - 성공")
    void getFixedExtensionSettings_Success() throws Exception {
        // given
        List<FixedExtensionSettingDto> settings = Arrays.asList(
            FixedExtensionSettingDto.from(TestDataFixture.createFixedExtensionSetting("exe", true)),
            FixedExtensionSettingDto.from(TestDataFixture.createFixedExtensionSetting("bat", false))
        );
        when(extensionService.getFixedExtensionSettings()).thenReturn(settings);

        // when & then
        mockMvc.perform(get("/api/extensions/fixed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].extension").value("exe"))
                .andExpect(jsonPath("$[0].blocked").value(true))
                .andExpect(jsonPath("$[1].extension").value("bat"))
                .andExpect(jsonPath("$[1].blocked").value(false));

        verify(extensionService).getFixedExtensionSettings();
    }

    @Test
    @DisplayName("고정 확장자 설정 변경 - 성공")
    void updateFixedExtensionSetting_Success() throws Exception {
        // given
        String extension = "exe";
        boolean isBlocked = true;
        FixedExtensionSettingDto updatedSetting = FixedExtensionSettingDto.from(
            TestDataFixture.createFixedExtensionSetting(extension, isBlocked)
        );
        
        when(extensionService.updateFixedExtensionSetting(extension, isBlocked))
            .thenReturn(updatedSetting);

        // when & then
        mockMvc.perform(put("/api/extensions/fixed/{extension}", extension)
                .param("blocked", String.valueOf(isBlocked)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extension").value(extension))
                .andExpect(jsonPath("$.blocked").value(isBlocked));

        verify(extensionService).updateFixedExtensionSetting(extension, isBlocked);
    }

    @Test
    @DisplayName("고정 확장자 설정 변경 - 잘못된 확장자")
    void updateFixedExtensionSetting_InvalidExtension_BadRequest() throws Exception {
        // given
        String invalidExtension = "txt";  // 고정 확장자가 아님
        
        when(extensionService.updateFixedExtensionSetting(invalidExtension, true))
            .thenThrow(new IllegalArgumentException("고정 확장자가 아닙니다"));

        // when & then
        mockMvc.perform(put("/api/extensions/fixed/{extension}", invalidExtension)
                .param("blocked", "true"))
                .andExpect(status().isBadRequest());

        verify(extensionService).updateFixedExtensionSetting(invalidExtension, true);
    }

    @Test
    @DisplayName("커스텀 확장자 목록 조회 - 성공")
    void getCustomExtensions_Success() throws Exception {
        // given
        List<CustomExtensionDto> extensions = Arrays.asList(
            CustomExtensionDto.from(TestDataFixture.createCustomExtension("malware")),
            CustomExtensionDto.from(TestDataFixture.createCustomExtension("virus"))
        );
        when(extensionService.getCustomExtensions()).thenReturn(extensions);

        // when & then
        mockMvc.perform(get("/api/extensions/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].extension").value("malware"))
                .andExpect(jsonPath("$[1].extension").value("virus"));

        verify(extensionService).getCustomExtensions();
    }

    @Test
    @DisplayName("커스텀 확장자 추가 - 성공")
    void addCustomExtension_Success() throws Exception {
        // given
        String extension = "malware";
        CustomExtensionDto addedExtension = CustomExtensionDto.from(
            TestDataFixture.createCustomExtension(extension)
        );
        
        when(extensionService.addCustomExtension(extension)).thenReturn(addedExtension);

        // when & then
        mockMvc.perform(post("/api/extensions/custom")
                .param("extension", extension))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extension").value(extension));

        verify(extensionService).addCustomExtension(extension);
    }

    @Test
    @DisplayName("커스텀 확장자 추가 - 중복 확장자")
    void addCustomExtension_DuplicateExtension_BadRequest() throws Exception {
        // given
        String extension = "existing";
        
        when(extensionService.addCustomExtension(extension))
            .thenThrow(new IllegalArgumentException("이미 존재하는 확장자입니다"));

        // when & then
        mockMvc.perform(post("/api/extensions/custom")
                .param("extension", extension))
                .andExpect(status().isBadRequest());

        verify(extensionService).addCustomExtension(extension);
    }

    @Test
    @DisplayName("커스텀 확장자 추가 - 200개 제한 초과")
    void addCustomExtension_ExceedsLimit_BadRequest() throws Exception {
        // given
        String extension = "newext";
        
        when(extensionService.addCustomExtension(extension))
            .thenThrow(new IllegalArgumentException("커스텀 확장자는 최대 200개까지 추가할 수 있습니다"));

        // when & then
        mockMvc.perform(post("/api/extensions/custom")
                .param("extension", extension))
                .andExpect(status().isBadRequest());

        verify(extensionService).addCustomExtension(extension);
    }

    @Test
    @DisplayName("커스텀 확장자 삭제 - 성공")
    void deleteCustomExtension_Success() throws Exception {
        // given
        Long extensionId = 1L;
        doNothing().when(extensionService).deleteCustomExtension(extensionId);

        // when & then
        mockMvc.perform(delete("/api/extensions/custom/{id}", extensionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("확장자가 삭제되었습니다."));

        verify(extensionService).deleteCustomExtension(extensionId);
    }

    @Test
    @DisplayName("커스텀 확장자 삭제 - 존재하지 않는 ID")
    void deleteCustomExtension_NotFound_BadRequest() throws Exception {
        // given
        Long extensionId = 999L;
        
        doThrow(new IllegalArgumentException("존재하지 않는 확장자입니다"))
            .when(extensionService).deleteCustomExtension(extensionId);

        // when & then
        mockMvc.perform(delete("/api/extensions/custom/{id}", extensionId))
                .andExpect(status().isBadRequest());

        verify(extensionService).deleteCustomExtension(extensionId);
    }

    @Test
    @DisplayName("확장자 차단 여부 확인 - 차단된 확장자")
    void checkExtension_BlockedExtension() throws Exception {
        // given
        String filename = "malware.exe";
        
        when(extensionService.isExtensionBlocked(filename)).thenReturn(true);

        // when & then
        mockMvc.perform(get("/api/extensions/check")
                .param("filename", filename))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(filename))
                .andExpect(jsonPath("$.blocked").value(true))
                .andExpect(jsonPath("$.message").value("업로드가 차단된 파일입니다."));

        verify(extensionService).isExtensionBlocked(filename);
    }

    @Test
    @DisplayName("확장자 차단 여부 확인 - 허용된 확장자")
    void checkExtension_AllowedExtension() throws Exception {
        // given
        String filename = "document.txt";
        
        when(extensionService.isExtensionBlocked(filename)).thenReturn(false);

        // when & then
        mockMvc.perform(get("/api/extensions/check")
                .param("filename", filename))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(filename))
                .andExpect(jsonPath("$.blocked").value(false))
                .andExpect(jsonPath("$.message").value("업로드가 허용된 파일입니다."));

        verify(extensionService).isExtensionBlocked(filename);
    }

    @Test
    @DisplayName("확장자 차단 여부 확인 - 파일명 누락")
    void checkExtension_MissingFilename_BadRequest() throws Exception {
        // when & then
        mockMvc.perform(get("/api/extensions/check"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(extensionService);
    }

    @Test
    @DisplayName("전체 확장자 목록 조회 - 성공")
    void getAllExtensions_Success() throws Exception {
        // given
        List<FixedExtensionSettingDto> fixedExtensions = Arrays.asList(
            FixedExtensionSettingDto.from(TestDataFixture.createFixedExtensionSetting("exe", true)),
            FixedExtensionSettingDto.from(TestDataFixture.createFixedExtensionSetting("bat", false))
        );
        
        List<CustomExtensionDto> customExtensions = Arrays.asList(
            CustomExtensionDto.from(TestDataFixture.createCustomExtension("malware"))
        );
        
        when(extensionService.getFixedExtensionSettings()).thenReturn(fixedExtensions);
        when(extensionService.getCustomExtensions()).thenReturn(customExtensions);

        // when & then
        mockMvc.perform(get("/api/extensions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fixedExtensions").isArray())
                .andExpect(jsonPath("$.fixedExtensions.length()").value(2))
                .andExpect(jsonPath("$.customExtensions").isArray())
                .andExpect(jsonPath("$.customExtensions.length()").value(1))
                .andExpect(jsonPath("$.fixedExtensions[0].extension").value("exe"))
                .andExpect(jsonPath("$.customExtensions[0].extension").value("malware"));

        verify(extensionService).getFixedExtensionSettings();
        verify(extensionService).getCustomExtensions();
    }

    @Test
    @DisplayName("차단된 확장자 목록 조회 - 성공")
    void getBlockedExtensions_Success() throws Exception {
        // given
        List<String> blockedExtensions = Arrays.asList("exe", "bat", "malware", "virus");
        when(extensionService.getAllBlockedExtensions()).thenReturn(blockedExtensions);

        // when & then
        mockMvc.perform(get("/api/extensions/blocked"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0]").value("exe"))
                .andExpect(jsonPath("$[1]").value("bat"))
                .andExpect(jsonPath("$[2]").value("malware"))
                .andExpect(jsonPath("$[3]").value("virus"));

        verify(extensionService).getAllBlockedExtensions();
    }
}
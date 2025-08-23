package com.assignment.fileextension.unit.controller;

import com.assignment.fileextension.controller.ExtensionController;
import com.assignment.fileextension.dto.CustomExtensionDto;
import com.assignment.fileextension.dto.ExtensionRequest;
import com.assignment.fileextension.dto.FixedExtensionSettingDto;
import com.assignment.fileextension.service.ExtensionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExtensionController.class)
@DisplayName("ExtensionController 단위 테스트")
class ExtensionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExtensionService extensionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("고정 확장자 설정 목록 조회")
    void getFixedExtensionSettings() throws Exception {
        List<FixedExtensionSettingDto> settings = Arrays.asList(
            new FixedExtensionSettingDto(1L, "exe", true, LocalDateTime.now(), LocalDateTime.now()),
            new FixedExtensionSettingDto(2L, "bat", false, LocalDateTime.now(), LocalDateTime.now())
        );
        
        given(extensionService.getAllFixedExtensionSettings()).willReturn(settings);

        mockMvc.perform(get("/api/extensions/fixed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].extension").value("exe"))
                .andExpect(jsonPath("$[0].isBlocked").value(true))
                .andExpect(jsonPath("$[1].extension").value("bat"))
                .andExpect(jsonPath("$[1].isBlocked").value(false));
    }

    @Test
    @DisplayName("고정 확장자 설정 변경")
    void updateFixedExtensionSetting() throws Exception {
        FixedExtensionSettingDto updatedSetting = 
            new FixedExtensionSettingDto(1L, "exe", true, LocalDateTime.now(), LocalDateTime.now());
        
        given(extensionService.updateFixedExtensionSetting("exe", true))
            .willReturn(updatedSetting);

        Map<String, Boolean> request = new HashMap<>();
        request.put("isBlocked", true);

        mockMvc.perform(put("/api/extensions/fixed/exe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extension").value("exe"))
                .andExpect(jsonPath("$.isBlocked").value(true));
    }

    @Test
    @DisplayName("커스텀 확장자 목록 조회")
    void getCustomExtensions() throws Exception {
        List<CustomExtensionDto> customExtensions = Arrays.asList(
            new CustomExtensionDto(1L, "custom1"),
            new CustomExtensionDto(2L, "custom2")
        );
        
        given(extensionService.getAllCustomExtensions()).willReturn(customExtensions);

        mockMvc.perform(get("/api/extensions/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].extension").value("custom1"))
                .andExpect(jsonPath("$[1].extension").value("custom2"));
    }

    @Test
    @DisplayName("커스텀 확장자 추가")
    void addCustomExtension() throws Exception {
        ExtensionRequest request = new ExtensionRequest("newext");
        CustomExtensionDto createdExtension = 
            new CustomExtensionDto(1L, "newext");
        
        given(extensionService.addCustomExtension(any(ExtensionRequest.class)))
            .willReturn(createdExtension);

        mockMvc.perform(post("/api/extensions/custom")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extension").value("newext"));
    }

    @Test
    @DisplayName("커스텀 확장자 삭제")
    void deleteCustomExtension() throws Exception {
        doNothing().when(extensionService).deleteCustomExtension(1L);

        mockMvc.perform(delete("/api/extensions/custom/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("파일 확장자 차단 여부 확인 - 차단된 파일")
    void checkExtension_blocked() throws Exception {
        given(extensionService.isExtensionBlocked(eq("test.exe"), (Map<String, Boolean>) any()))
            .willReturn(true);

        mockMvc.perform(get("/api/extensions/check")
                .param("fileName", "test.exe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(true));
    }

    @Test
    @DisplayName("파일 확장자 차단 여부 확인 - 허용된 파일")
    void checkExtension_allowed() throws Exception {
        given(extensionService.isExtensionBlocked(eq("test.txt"), (Map<String, Boolean>) any()))
            .willReturn(false);

        mockMvc.perform(get("/api/extensions/check")
                .param("fileName", "test.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(false));
    }

    @Test
    @DisplayName("파일 확장자 차단 여부 확인 - 고정 확장자 상태 포함")
    void checkExtension_withFixedStates() throws Exception {
        Map<String, Boolean> fixedStates = new HashMap<>();
        fixedStates.put("exe", true);
        
        given(extensionService.isExtensionBlocked(eq("test.exe"), eq(fixedStates)))
            .willReturn(true);

        mockMvc.perform(get("/api/extensions/check")
                .param("fileName", "test.exe")
                .param("fixedExtensionStates", "{\"exe\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(true));
    }
}
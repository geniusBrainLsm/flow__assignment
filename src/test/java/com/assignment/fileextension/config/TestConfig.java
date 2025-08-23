package com.assignment.fileextension.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

@TestConfiguration
@ActiveProfiles("test")
public class TestConfig {

    @Bean
    @Primary
    public String testUploadDir() throws IOException {
        Path tempDir = Files.createTempDirectory("test-uploads");
        tempDir.toFile().deleteOnExit();
        return tempDir.toString();
    }
}
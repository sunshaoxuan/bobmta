package com.bob.mta.modules.file.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.common.i18n.TestMessageResolverFactory;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.audit.service.impl.InMemoryAuditService;
import com.bob.mta.modules.file.dto.FileResponse;
import com.bob.mta.modules.file.dto.RegisterFileRequest;
import com.bob.mta.modules.file.service.impl.InMemoryFileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class FileControllerTest {

    private FileController controller;
    private MessageResolver messageResolver;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        messageResolver = TestMessageResolverFactory.create();
        controller = new FileController(new InMemoryFileService(),
                new AuditRecorder(new InMemoryAuditService(), new ObjectMapper()), messageResolver);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "pass", "ROLE_ADMIN"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void shouldRegisterFile() {
        RegisterFileRequest request = new RegisterFileRequest();
        request.setFileName("summary.txt");
        request.setContentType("text/plain");
        request.setSize(512);
        request.setBucket("files");

        ApiResponse<FileResponse> response = controller.register(request);
        assertThat(response.getData().getFileName()).isEqualTo("summary.txt");
    }
}

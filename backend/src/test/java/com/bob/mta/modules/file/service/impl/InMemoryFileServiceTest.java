package com.bob.mta.modules.file.service.impl;

import com.bob.mta.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryFileServiceTest {

    private final InMemoryFileService service = new InMemoryFileService();

    @Test
    void shouldRegisterAndRetrieveFile() {
        var metadata = service.register("report.pdf", "application/pdf", 1024, "files", "CUSTOMER", "cust-1", "admin");
        assertThat(service.get(metadata.getId()).getFileName()).isEqualTo("report.pdf");
        assertThat(service.buildDownloadUrl(metadata)).contains(metadata.getObjectKey());
    }

    @Test
    void shouldThrowWhenFileMissing() {
        assertThatThrownBy(() -> service.get("missing"))
                .isInstanceOf(BusinessException.class);
    }
}

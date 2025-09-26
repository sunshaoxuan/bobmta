package com.bob.mta.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageResponseTest {

    @Test
    @DisplayName("page response captures metadata and performs defensive copy")
    void shouldCreatePageResponse() {
        final List<String> rows = List.of("a", "b");
        final PageResponse<String> page = PageResponse.of(rows, 10, 1, 20);

        assertThat(page.getList()).containsExactly("a", "b");
        assertThat(page.getTotal()).isEqualTo(10);
        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getPageSize()).isEqualTo(20);

        // ensure defensive copy
        assertThat(page.getList()).isNotSameAs(rows);
    }
}


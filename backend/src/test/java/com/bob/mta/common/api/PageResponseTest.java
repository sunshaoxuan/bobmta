package com.bob.mta.common.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void ofShouldExposePaginationInfo() {
        PageResponse<String> response = PageResponse.of(List.of("a", "b"), 5, 1, 2);
        assertThat(response.getItems()).containsExactly("a", "b");
        assertThat(response.getTotal()).isEqualTo(5);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(2);
    }
}

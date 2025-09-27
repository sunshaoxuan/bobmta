package com.bob.mta.common.api;

<<<<<<< HEAD
=======
<<<<<<< HEAD
>>>>>>> origin/main
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
<<<<<<< HEAD
=======
=======
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
>>>>>>> origin/main
>>>>>>> origin/main

class PageResponseTest {

    @Test
<<<<<<< HEAD
=======
<<<<<<< HEAD
>>>>>>> origin/main
    void ofShouldExposePaginationInfo() {
        PageResponse<String> response = PageResponse.of(List.of("a", "b"), 5, 1, 2);
        assertThat(response.getItems()).containsExactly("a", "b");
        assertThat(response.getTotal()).isEqualTo(5);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(2);
    }
}
<<<<<<< HEAD
=======
=======
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

>>>>>>> origin/main
>>>>>>> origin/main

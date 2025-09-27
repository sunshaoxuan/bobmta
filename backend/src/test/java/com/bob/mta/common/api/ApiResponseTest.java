package com.bob.mta.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successShouldContainData() {
        ApiResponse<String> response = ApiResponse.success("ok");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("ok");
        assertThat(response.getCode()).isEqualTo("OK");
        assertThat(response.getMessage()).isEqualTo("success");
    }

    @Test
    void failureShouldContainErrorInfo() {
        ApiResponse<Object> response = ApiResponse.failure("ERR", "failure");
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("ERR");
        assertThat(response.getMessage()).isEqualTo("failure");
        assertThat(response.getData()).isNull();
    }
}

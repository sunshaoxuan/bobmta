package com.bob.mta.common.api;

<<<<<<< HEAD
=======
<<<<<<< HEAD
>>>>>>> origin/main
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
<<<<<<< HEAD
=======
=======
import static org.assertj.core.api.Assertions.assertThat;

import com.bob.mta.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    @DisplayName("success factory method wraps payload and default metadata")
    void shouldCreateSuccessResponse() {
        final ApiResponse<String> response = ApiResponse.success("payload");

        assertThat(response.getCode()).isEqualTo(ErrorCode.OK.getCode());
        assertThat(response.getMessage()).isEqualTo(ErrorCode.OK.getDefaultMessage());
        assertThat(response.getData()).isEqualTo("payload");
    }

    @Test
    @DisplayName("failure factory method allows overriding message and data")
    void shouldCreateFailureResponse() {
        final ApiResponse<String> response = ApiResponse.failure(ErrorCode.NOT_FOUND, "missing", "context");

        assertThat(response.getCode()).isEqualTo(ErrorCode.NOT_FOUND.getCode());
        assertThat(response.getMessage()).isEqualTo("missing");
        assertThat(response.getData()).isEqualTo("context");
    }
}

>>>>>>> origin/main
>>>>>>> origin/main

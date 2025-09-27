package com.bob.mta.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.bob.mta.common.api.ApiResponse;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PingControllerTest {

    @Test
    @DisplayName("ping endpoint returns ok status")
    void shouldReturnOkStatus() {
        final PingController controller = new PingController();

        final ApiResponse<Map<String, String>> response = controller.ping();

        assertThat(response.getData()).containsEntry("status", "ok");
    }
}

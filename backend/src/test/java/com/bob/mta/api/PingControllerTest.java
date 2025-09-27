package com.bob.mta.api;

import com.bob.mta.common.api.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PingControllerTest {

    @Test
    void pingShouldReturnOk() {
        PingController controller = new PingController();
        ApiResponse<?> response = controller.ping();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("status", "ok");
    }
}

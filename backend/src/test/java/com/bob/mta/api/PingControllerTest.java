package com.bob.mta.api;

<<<<<<< HEAD
=======
<<<<<<< HEAD
>>>>>>> origin/main
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
<<<<<<< HEAD
=======
=======
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("ping endpoint is publicly accessible")
    void shouldReturnOkStatus() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("ok"));
    }
}

>>>>>>> origin/main
>>>>>>> origin/main

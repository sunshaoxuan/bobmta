package com.bob.mta.api;

import com.bob.mta.common.api.ApiResponse;
<<<<<<< HEAD
=======
import java.util.Map;
import org.springframework.http.MediaType;
>>>>>>> origin/main
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

<<<<<<< HEAD
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.success(Map.of(
                "status", "ok",
                "timestamp", OffsetDateTime.now().toString()
        ));
=======
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class PingController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.success(Map.of("status", "ok"));
>>>>>>> origin/main
    }
}

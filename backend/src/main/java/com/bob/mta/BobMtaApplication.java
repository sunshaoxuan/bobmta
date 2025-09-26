package com.bob.mta;

import com.bob.mta.common.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class BobMtaApplication {
    public static void main(String[] args) {
        SpringApplication.run(BobMtaApplication.class, args);
    }
}

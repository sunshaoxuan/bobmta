package com.bob.mta;

import com.bob.mta.common.security.JwtProperties;
import com.bob.mta.modules.notification.NotificationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class
})
@EnableConfigurationProperties({JwtProperties.class, NotificationProperties.class})
public class BobMtaApplication {
    public static void main(String[] args) {
        SpringApplication.run(BobMtaApplication.class, args);
    }
}

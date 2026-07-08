package com.anverraglobal.insurance.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
@Getter
@Setter
public class AppProperties {

    private String frontendUrl;
    private Sms sms = new Sms();

    @Getter
    @Setter
    public static class Sms {
        private String apiUrl;
        private String apiKey;
    }
}

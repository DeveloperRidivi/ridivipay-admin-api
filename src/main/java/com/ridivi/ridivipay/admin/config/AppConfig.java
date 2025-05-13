package com.ridivi.ridivipay.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:${spring.profiles.active}.properties")
public class AppConfig {
}

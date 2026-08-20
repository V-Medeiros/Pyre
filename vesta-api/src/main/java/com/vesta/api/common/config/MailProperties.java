package com.vesta.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("vesta.mail")
public record MailProperties(String from, String frontendUrl) {
}


package com.ecommerce.project.security;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    public static final Logger LOG = LoggerFactory.getLogger(WebConfig.class);

    @Value("${frontend.url}")
    private String frontendUrls;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Split by comma, trim each origin, and remove empty entries
        String[] frontendUrlList = Arrays.stream(frontendUrls.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> StringUtils.removeEnd(s, "/"))
                .toArray(String[]::new);
        LOG.info("CORS allowed origins (normalized): {}", Arrays.asList(frontendUrlList));

        registry.addMapping("/**")
                .allowedOrigins(frontendUrlList)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}

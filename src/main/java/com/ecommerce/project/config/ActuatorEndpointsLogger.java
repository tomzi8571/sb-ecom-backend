package com.ecommerce.project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
public class ActuatorEndpointsLogger implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger log = LoggerFactory.getLogger(ActuatorEndpointsLogger.class);
    private final ApplicationContext ctx;
    private final Environment env;

    private static final String[] SECRETS = {
            "PASSWORD","PASS","SECRET","TOKEN","KEY","CREDENTIAL","JWT","API_KEY","AWS","DB_PASSWORD"
    };

    public ActuatorEndpointsLogger(ApplicationContext ctx, Environment env) {
        this.ctx = ctx;
        this.env = env;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("=== Actuator debug information ===");
        String exposure = env.getProperty("management.endpoints.web.exposure.include");
        log.info("management.endpoints.web.exposure.include={}", exposure);

        boolean hasWebEndpointMapping = ctx.containsBean("webEndpointServletHandlerMapping");
        boolean hasMvcEndpointMapping = ctx.containsBean("webMvcEndpointHandlerMapping");
        boolean hasEndpointHandlerMapping = hasWebEndpointMapping || hasMvcEndpointMapping;
        log.info("webEndpointServletHandlerMapping present: {}", hasWebEndpointMapping);
        log.info("webMvcEndpointHandlerMapping present: {}", hasMvcEndpointMapping);

        String[] actuatorBeans = Arrays.stream(ctx.getBeanDefinitionNames())
                .filter(name -> name.toLowerCase().contains("actuator") || name.toLowerCase().contains("endpoint"))
                .sorted()
                .toArray(String[]::new);
        log.info("Actuator-related beans (partial list): {}", (Object) actuatorBeans);

        log.info("=== End Actuator debug information ===");

        log.info("=== Environment variables ===");
        for (Map.Entry<String, String> e : System.getenv().entrySet()) {
            log.info("{}={}", e.getKey(), maskIfSensitive(e.getKey(), e.getValue()));
        }
        log.info("=== End environment variables ===");
    }
    private String maskIfSensitive(String key, String value) {
        if (value == null) return null;
        String up = key.toUpperCase();
        for (String s : SECRETS) {
            if (up.contains(s)) return "****";
        }
        return value;
    }
}


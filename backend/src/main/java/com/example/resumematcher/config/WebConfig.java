package com.example.resumematcher.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AppProperties appProperties;

    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = Arrays.stream(appProperties.corsOrigins().split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        if (origins.contains("*")) {
            registry.addMapping("/api/**").allowedOriginPatterns("*").allowedMethods("*").allowedHeaders("*");
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}

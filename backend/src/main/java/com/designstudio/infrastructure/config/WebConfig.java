package com.designstudio.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig {

    @Value("${cors.allowed-origins:http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false)
                        .maxAge(3600);

                registry.addMapping("/uploads/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false)
                        .maxAge(3600);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // Получаем абсолютный путь к папке uploads
                String workingDir = System.getProperty("user.dir");
                String uploadsPath = "file:" + workingDir + "/uploads/";

                // Регистрируем обработчик для всех файлов в uploads
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations(uploadsPath);

                // Также добавляем обработчик для поддиректории design-orders
                String designOrdersPath = "file:" + workingDir + "/uploads/design-orders/";
                registry.addResourceHandler("/uploads/design-orders/**")
                        .addResourceLocations(designOrdersPath);

                // Выводим пути для отладки
                System.out.println("Uploads path: " + uploadsPath);
                System.out.println("Design orders path: " + designOrdersPath);
                System.out.println("Working directory: " + workingDir);
            }
        };
    }
}
package com.example.cloud.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурационный класс для настройки веб-приложения Spring.
 * Реализует интерфейс WebMvcConfigurer для настройки CORS (Cross-Origin Resource Sharing).
 */
@Configuration
@EnableWebMvc
class AppConfig implements WebMvcConfigurer {

    /**
     * Настраивает правила CORS для обработки запросов из разных источников.
     * Этот метод позволяет задавать допустимые источники, методы, заголовки и другие параметры CORS.
     *
     * @param registry объект CorsRegistry для регистрации правил CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // добавляем настройки CORS для всех путей
        registry.addMapping("/**") // Разрешаем CORS для всех endpoints
                .allowCredentials(true) // Разрешаем отправку учетных данных
                .allowedOrigins("http://localhost:8080") // Разрешенный источник запросов
                .allowedMethods("*") // Разрешены все HTTP-методы (GET, POST, PUT, DELETE и т.д.)
                .allowedHeaders("*") // Разрешены все заголовки в запросах
                .allowPrivateNetwork(true); // Разрешаем доступ из частных сетей
    }
}
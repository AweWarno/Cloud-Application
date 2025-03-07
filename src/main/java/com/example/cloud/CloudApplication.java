package com.example.cloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Главный класс приложения CloudApplication.
 * Является точкой входа для запуска Spring Boot приложения,
 * обеспечивая конфигурацию и инициализацию всех компонентов.
 */
@SpringBootApplication
/*
 * Аннотация @SpringBootApplication включает:
 * - @EnableAutoConfiguration: автоматическая настройка Spring Boot на основе зависимостей;
 * - @ComponentScan: сканирование компонентов (контроллеры, сервисы и т.д.) в пакете com.example.cloud;
 * - @Configuration: объявление класса как источника конфигурации Spring.
 */
@EnableJpaRepositories(basePackages = "com.example.cloud.repository")
/*
 * Включает поддержку JPA-репозиториев и указывает Spring сканировать
 * интерфейсы репозиториев в пакете com.example.cloud.repository.
 */
public class CloudApplication {

    private static final Logger logger = LoggerFactory.getLogger(CloudApplication.class);

    /**
     * Точка входа в приложение.
     * Запускает Spring Boot приложение и логирует сообщение о старте.
     *
     * @param args Аргументы командной строки, передаваемые при запуске.
     */
    public static void main(String[] args) {
        SpringApplication.run(CloudApplication.class, args);
        logger.info("Приложение Spring Boot успешно запущено");
    }
}
package com.example.cloud.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Модель пользователя для хранения в базе данных.
 * Содержит учетные данные и токен авторизации.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "users")
public class User {
    /**
     * Уникальный идентификатор пользователя в базе данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    /**
     * Учетные данные пользователя (логин и пароль).
     * Используется в запросе POST /login.
     */
    @Embedded
    private Credentials credentials;

    /**
     * Токен авторизации пользователя.
     * Используется в ответе POST /login и запросе POST /logout.
     */
    @Embedded
    private Token token;

    /**
     * Вложенный класс для учетных данных пользователя.
     * Соответствует схеме тела запроса POST /login.
     */
    @Embeddable
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Credentials implements Serializable {
        /**
         * Логин пользователя.
         * Обязательное поле в теле запроса POST /login (свойство `login`).
         */
        @Column(name = "username", nullable = false, unique = true)
        @NotEmpty(message = "Логин не может быть пустым")
        private String login;

        /**
         * Пароль пользователя.
         * Обязательное поле в теле запроса POST /login (свойство `password`).
         */
        @Column(name = "password", nullable = false)
        @NotEmpty(message = "Пароль не может быть пустым")
        private String password;

        @Override
        public String toString() {
            return "Credentials{login='" + login + "'}";
        }
    }

    /**
     * Вложенный класс для токена авторизации.
     * Соответствует схеме ответа POST /login (`auth-token`).
     */
    @Embeddable
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Token implements Serializable {
        /**
         * Токен авторизации пользователя.
         * Возвращается как `auth-token` в ответе POST /login и используется в заголовке POST /logout.
         */
        @Column(name = "auth_token", nullable = false)
        private String authToken;

        /**
         * Генерирует новый токен авторизации.
         * Использует безопасный генератор случайных чисел и кодировку Base64.
         *
         * @return Сгенерированный токен.
         */
        public String generateNewToken() {
            byte[] randomBytes = new byte[24];
            new SecureRandom().nextBytes(randomBytes);
            return Base64.getUrlEncoder().encodeToString(randomBytes);
        }

        /**
         * Сбрасывает токен авторизации.
         * Устанавливает пустое значение для завершения сессии.
         */
        public void invalidateToken() {
            authToken = "";
        }
    }
}
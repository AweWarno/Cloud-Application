package com.example.cloud.controller;

import com.example.cloud.error.ErrorResponse;
import com.example.cloud.model.User;
import com.example.cloud.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для проверки AuthController с использованием Mockito.
 * Тестируются методы: login, logout.
 */
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Тесты для login

    /**
     * Тестирует успешную авторизацию пользователя.
     * Входные данные: валидные учетные данные (логин и пароль).
     * Ожидаемый результат: возвращается статус 200 (OK) и токен авторизации.
     */
    @Test
    void login_successful() {
        // Подготовка данных
        User.Credentials credentials = new User.Credentials("testuser", "password");
        User.Token token = new User.Token("valid-token");

        // Мокируем вызовы
        when(authService.login(credentials)).thenReturn(token);

        // Вызываем метод контроллера
        ResponseEntity<?> response = authController.login(credentials);

        // Проверяем результат
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(token, response.getBody());
    }

    /**
     * Тестирует авторизацию с неверными учетными данными.
     * Входные данные: неверные учетные данные (логин и пароль).
     * Ожидаемый результат: возвращается статус 400 (Bad Request) и сообщение об ошибке "Неверные учетные данные".
     */
    @Test
    void login_invalidCredentials_returnsBadRequest() {
        // Подготовка данных
        User.Credentials credentials = new User.Credentials("testuser", "wrong-password");

        // Мокируем вызовы
        when(authService.login(credentials)).thenThrow(new IllegalArgumentException("Неверные учетные данные"));

        // Вызываем метод контроллера
        ResponseEntity<?> response = authController.login(credentials);

        // Проверяем результат
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        assertEquals("Неверные учетные данные", ((ErrorResponse) response.getBody()).getMessage());
    }

    // Тесты для logout

    /**
     * Тестирует успешный выход пользователя из системы.
     * Входные данные: валидный токен авторизации.
     * Ожидаемый результат: возвращается статус 200 (OK), вызывается метод logout сервиса с переданным токеном.
     */
    @Test
    void logout_successful() {
        // Подготовка данных
        String token = "valid-token";

        // Вызываем метод контроллера
        ResponseEntity<?> response = authController.logout(token);

        // Проверяем результат
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).logout(Optional.of(token));
    }

    /**
     * Тестирует выход с невалидным токеном.
     * Входные данные: невалидный токен авторизации.
     * Ожидаемый результат: возвращается статус 400 (Bad Request) и сообщение об ошибке "Неверный токен авторизации".
     */
    @Test
    void logout_invalidToken_returnsBadRequest() {
        // Подготовка данных
        String token = "invalid-token";

        // Мокируем вызовы
        doThrow(new IllegalArgumentException("Неверный токен авторизации"))
                .when(authService).logout(Optional.of(token));

        // Вызываем метод контроллера
        ResponseEntity<?> response = authController.logout(token);

        // Проверяем результат
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        assertEquals("Неверный токен авторизации", ((ErrorResponse) response.getBody()).getMessage());
    }
}
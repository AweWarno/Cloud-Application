package com.example.cloud.service;

import com.example.cloud.model.User;
import com.example.cloud.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для проверки реализации AuthServiceImp.
 * Тестируются методы: login, logout, getUserByToken, validateToken.
 */
class AuthServiceImpTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImp authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Тесты для login

    /**
     * Тестирует успешный вход пользователя.
     * Входные данные: корректные логин и пароль.
     * Ожидаемый результат: новый токен авторизации сгенерирован и сохранён.
     */
    @Test
    void login_successful() {

        User.Credentials credentials = User.Credentials.builder()
                .login("testuser")
                .password("password")
                .build();
        User user = User.builder()
                .credentials(credentials)
                .token(User.Token.builder().authToken("").build())
                .build();
        when(userRepository.findUserByCredentialsLoginIgnoreCase("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User.Token token = authService.login(credentials);

        assertNotNull(token.getAuthToken(), "Токен не должен быть null");
        assertFalse(token.getAuthToken().isEmpty(), "Токен не должен быть пустым");
        assertNotEquals("", token.getAuthToken(), "Новый токен должен отличаться от старого");
        verify(userRepository).save(user);
    }

    /**
     * Тестирует вход с неверным паролем.
     * Входные данные: корректный логин, неверный пароль.
     * Ожидаемый результат: выбрасывается исключение с сообщением "Неверные учетные данные".
     */
    @Test
    void login_invalidPassword_throwsException() {
        User.Credentials credentials = User.Credentials.builder()
                .login("testuser")
                .password("wrongpassword")
                .build();
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        when(userRepository.findUserByCredentialsLoginIgnoreCase("testuser")).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.login(credentials));
        assertEquals("Неверные учетные данные", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    /**
     * Тестирует вход с несуществующим пользователем.
     * Входные данные: логин и пароль не зарегистрированного пользователя.
     * Ожидаемый результат: выбрасывается исключение с сообщением "Неверные учетные данные".
     */
    @Test
    void login_userNotFound_throwsException() {
        User.Credentials credentials = User.Credentials.builder()
                .login("testuser")
                .password("password")
                .build();
        when(userRepository.findUserByCredentialsLoginIgnoreCase("testuser")).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.login(credentials));
        assertEquals("Неверные учетные данные", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    // Тесты для logout

    /**
     * Тестирует успешный выход пользователя.
     * Входные данные: валидный токен авторизации.
     * Ожидаемый результат: токен сбрасывается, изменения сохраняются.
     */
    @Test
    void logout_successful() {
        String token = "valid-token";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken(token).build())
                .build();
        when(userRepository.findUserByTokenAuthToken(token)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        authService.logout(Optional.of(token));
        assertEquals("", user.getToken().getAuthToken(), "Токен должен быть сброшен");
        verify(userRepository).save(user);
    }

    /**
     * Тестирует выход с неверным токеном.
     * Входные данные: несуществующий токен.
     * Ожидаемый результат: выбрасывается исключение с сообщением "Неверный токен авторизации".
     */
    @Test
    void logout_invalidToken_throwsException() {
        String token = "invalid-token";
        when(userRepository.findUserByTokenAuthToken(token)).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.logout(Optional.of(token)));
        assertEquals("Неверный токен авторизации", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    /**
     * Тестирует выход с отсутствующим токеном.
     * Входные данные: Optional.empty().
     * Ожидаемый результат: выбрасывается исключение с сообщением "Неверный токен авторизации".
     */
    @Test
    void logout_nullToken_throwsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.logout(Optional.empty()));
        assertEquals("Неверный токен авторизации", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    // Тесты для getUserByToken

    /**
     * Тестирует успешное получение пользователя по токену.
     * Входные данные: валидный токен.
     * Ожидаемый результат: возвращается Optional с пользователем.
     */
    @Test
    void getUserByToken_successful() {
        String token = "valid-token";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken(token).build())
                .build();
        when(userRepository.findUserByTokenAuthToken(token)).thenReturn(Optional.of(user));
        Optional<User> result = authService.getUserByToken(Optional.of(token));
        assertTrue(result.isPresent(), "Пользователь должен быть найден");
        assertEquals("testuser", result.get().getCredentials().getLogin(), "Логин должен совпадать");
    }

    /**
     * Тестирует получение пользователя с отсутствующим токеном.
     * Входные данные: Optional.empty().
     * Ожидаемый результат: возвращается пустой Optional.
     */
    @Test
    void getUserByToken_nullToken_returnsEmpty() {
        Optional<User> result = authService.getUserByToken(Optional.empty());
        assertFalse(result.isPresent(), "Результат должен быть пустым");
        verify(userRepository, never()).findUserByTokenAuthToken(any());
    }

    /**
     * Тестирует получение пользователя с несуществующим токеном.
     * Входные данные: валидный, но не зарегистрированный токен.
     * Ожидаемый результат: возвращается пустой Optional.
     */
    @Test
    void getUserByToken_userNotFound_returnsEmpty() {
        String token = "valid-token";
        when(userRepository.findUserByTokenAuthToken(token)).thenReturn(Optional.empty());
        Optional<User> result = authService.getUserByToken(Optional.of(token));
        assertFalse(result.isPresent(), "Пользователь не должен быть найден");
    }

    // Тесты для validateToken

    /**
     * Тестирует проверку валидного токена.
     * Входные данные: существующий токен.
     * Ожидаемый результат: возвращается true.
     */
    @Test
    void validateToken_validToken_returnsTrue() {
        String token = "valid-token";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken(token).build())
                .build();
        when(userRepository.findUserByTokenAuthToken(token)).thenReturn(Optional.of(user));
        Boolean result = authService.validateToken(token);
        assertTrue(result, "Токен должен быть валидным");
    }

    /**
     * Тестирует проверку невалидного токена.
     * Входные данные: несуществующий токен.
     * Ожидаемый результат: возвращается false.
     */
    @Test
    void validateToken_invalidToken_returnsFalse() {
        String token = "invalid-token";
        when(userRepository.findUserByTokenAuthToken(token)).thenReturn(Optional.empty());
        Boolean result = authService.validateToken(token);
        assertFalse(result, "Токен не должен быть валидным");
    }

    /**
     * Тестирует проверку null-токена.
     * Входные данные: null.
     * Ожидаемый результат: возвращается false.
     */
    @Test
    void validateToken_nullToken_returnsFalse() {
        Boolean result = authService.validateToken(null);
        assertFalse(result, "Токен null не должен быть валидным");
        verify(userRepository, never()).findUserByTokenAuthToken(any());
    }

    /**
     * Тестирует проверку пустого токена.
     * Входные данные: пустая строка.
     * Ожидаемый результат: возвращается false.
     */
    @Test
    void validateToken_emptyToken_returnsFalse() {
        Boolean result = authService.validateToken("");
        assertFalse(result, "Пустой токен не должен быть валидным");
        verify(userRepository, never()).findUserByTokenAuthToken(any());
    }
}
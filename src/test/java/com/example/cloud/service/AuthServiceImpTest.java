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
 * Тестовый класс для проверки реализации AuthServiceImp с использованием Mockito.
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
     * Ожидаемый результат: возвращается новый токен авторизации, изменения сохраняются в репозитории.
     */
    @Test
    void login_successful() {
        // Подготовка: создаем учетные данные и пользователя
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

        // Действие: вызываем метод login
        String oldToken = user.getToken().getAuthToken();
        User.Token newToken = authService.login(credentials);

        // Проверка: убеждаемся, что токен сгенерирован и отличается от старого
        assertNotNull(newToken.getAuthToken(), "Токен не должен быть null");
        assertFalse(newToken.getAuthToken().isEmpty(), "Токен не должен быть пустым");
        assertNotEquals(oldToken, newToken.getAuthToken(), "Новый токен должен отличаться от старого");
        verify(userRepository).save(user);
    }

    /**
     * Тестирует вход с неверным паролем.
     * Входные данные: корректный логин, неверный пароль.
     * Ожидаемый результат: выбрасывается исключение с сообщением "Неверные учетные данные", сохранение не происходит.
     */
    @Test
    void login_invalidPassword_throwsException() {
        // Подготовка: создаем учетные данные с неверным паролем и пользователя
        User.Credentials credentials = User.Credentials.builder()
                .login("testuser")
                .password("wrongpassword")
                .build();
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        when(userRepository.findUserByCredentialsLoginIgnoreCase("testuser")).thenReturn(Optional.of(user));

        // Проверка: ожидаем исключение при вызове login
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.login(credentials));
        assertEquals("Неверные учетные данные", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    /**
     * Тестирует вход с несуществующим пользователем.
     * Входные данные: логин и пароль не зарегистрированного пользователя.
     * Ожидаемый результат: выбрасывается исключение с сообщением "Неверные учетные данные", сохранение не происходит.
     */
    @Test
    void login_userNotFound_throwsException() {
        // Подготовка: создаем учетные данные для несуществующего пользователя
        User.Credentials credentials = User.Credentials.builder()
                .login("testuser")
                .password("password")
                .build();
        when(userRepository.findUserByCredentialsLoginIgnoreCase("testuser")).thenReturn(Optional.empty());

        // Проверка: ожидаем исключение при вызове login
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.login(credentials));
        assertEquals("Неверные учетные данные", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    // Тесты для logout

    /**
     * Тестирует успешный выход пользователя.
     * Входные данные: валидный токен авторизации.
     * Ожидаемый результат: токен сбрасывается на пустую строку, изменения сохраняются в репозитории.
     */
    @Test
    void logout_successful() {
        // Подготовка: создаем пользователя с токеном
        String token = "valid-token";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken(token).build())
                .build();
        when(userRepository.findUserByTokenAuthToken(token)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // Действие: вызываем метод logout
        authService.logout(Optional.of(token));

        // Проверка: убеждаемся, что токен сброшен
        assertEquals("", user.getToken().getAuthToken(), "Токен должен быть сброшен");
        verify(userRepository).save(user);
    }

    /**
     * Тестирует выход с неверным токеном.
     * Входные данные: несуществующий токен.
     * Ожидаемый результат: выбрасывается исключение с сообщением "Неверный токен авторизации", сохранение не происходит.
     */
    @Test
    void logout_invalidToken_throwsException() {
        // Подготовка: задаем несуществующий токен
        String token = "invalid-token";
        when(userRepository.findUserByTokenAuthToken(token)).thenReturn(Optional.empty());

        // Проверка: ожидаем исключение при вызове logout
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.logout(Optional.of(token)));
        assertEquals("Неверный токен авторизации", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    /**
     * Тестирует выход с отсутствующим токеном.
     * Входные данные: Optional.empty().
     * Ожидаемый результат: выбрасывается исключение с сообщением "Неверный токен авторизации", сохранение не происходит.
     */
    @Test
    void logout_nullToken_throwsException() {
        // Проверка: ожидаем исключение при вызове logout с пустым Optional
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.logout(Optional.empty()));
        assertEquals("Неверный токен авторизации", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    // Тесты для getUserByToken

    /**
     * Тестирует успешное получение пользователя по токену.
     * Входные данные: валидный токен.
     * Ожидаемый результат: возвращается Optional с пользователем, содержащим корректный логин.
     */
    @Test
    void getUserByToken_successful() {
        // Подготовка: создаем пользователя с токеном
        String token = "valid-token";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken(token).build())
                .build();
        when(userRepository.findUserByTokenAuthToken(token)).thenReturn(Optional.of(user));

        // Действие: вызываем метод getUserByToken
        Optional<User> result = authService.getUserByToken(Optional.of(token));

        // Проверка: убеждаемся, что пользователь найден и данные корректны
        assertTrue(result.isPresent(), "Пользователь должен быть найден");
        assertEquals("testuser", result.get().getCredentials().getLogin(), "Логин должен совпадать");
    }

    /**
     * Тестирует получение пользователя с отсутствующим токеном.
     * Входные данные: Optional.empty().
     * Ожидаемый результат: возвращается пустой Optional, запрос к репозиторию не выполняется.
     */
    @Test
    void getUserByToken_nullToken_returnsEmpty() {
        // Действие: вызываем метод getUserByToken с пустым Optional
        Optional<User> result = authService.getUserByToken(Optional.empty());

        // Проверка: убеждаемся, что результат пустой
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
        // Подготовка: задаем несуществующий токен
        String token = "valid-token";
        when(userRepository.findUserByTokenAuthToken(token)).thenReturn(Optional.empty());

        // Действие: вызываем метод getUserByToken
        Optional<User> result = authService.getUserByToken(Optional.of(token));

        // Проверка: убеждаемся, что пользователь не найден
        assertFalse(result.isPresent(), "Пользователь не должен быть найден");
    }

    // Тесты для validateToken

    /**
     * Тестирует проверку валидного токена.
     * Входные данные: существующий токен.
     * Ожидаемый результат: возвращается true, токен признан валидным.
     */
    @Test
    void validateToken_validToken_returnsTrue() {
        // Подготовка: создаем пользователя с токеном
        String token = "valid-token";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken(token).build())
                .build();
        when(userRepository.findUserByTokenAuthToken(token)).thenReturn(Optional.of(user));

        // Действие: вызываем метод validateToken
        Boolean result = authService.validateToken(token);

        // Проверка: убеждаемся, что токен валиден
        assertTrue(result, "Токен должен быть валидным");
    }

    /**
     * Тестирует проверку невалидного токена.
     * Входные данные: несуществующий токен.
     * Ожидаемый результат: возвращается false, токен признан невалидным.
     */
    @Test
    void validateToken_invalidToken_returnsFalse() {
        // Подготовка: задаем несуществующий токен
        String token = "invalid-token";
        when(userRepository.findUserByTokenAuthToken(token)).thenReturn(Optional.empty());

        // Действие: вызываем метод validateToken
        Boolean result = authService.validateToken(token);

        // Проверка: убеждаемся, что токен невалиден
        assertFalse(result, "Токен не должен быть валидным");
    }

    /**
     * Тестирует проверку null-токена.
     * Входные данные: null.
     * Ожидаемый результат: возвращается false, запрос к репозиторию не выполняется.
     */
    @Test
    void validateToken_nullToken_returnsFalse() {
        // Действие: вызываем метод validateToken с null
        Boolean result = authService.validateToken(null);

        // Проверка: убеждаемся, что токен невалиден
        assertFalse(result, "Токен null не должен быть валидным");
        verify(userRepository, never()).findUserByTokenAuthToken(any());
    }

    /**
     * Тестирует проверку пустого токена.
     * Входные данные: пустая строка.
     * Ожидаемый результат: возвращается false, запрос к репозиторию не выполняется.
     */
    @Test
    void validateToken_emptyToken_returnsFalse() {
        // Действие: вызываем метод validateToken с пустой строкой
        Boolean result = authService.validateToken("");

        // Проверка: убеждаемся, что токен невалиден
        assertFalse(result, "Пустой токен не должен быть валидным");
        verify(userRepository, never()).findUserByTokenAuthToken(any());
    }
}
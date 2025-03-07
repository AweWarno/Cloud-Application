package com.example.cloud.service;

import com.example.cloud.model.User;
import com.example.cloud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Сервис для аутентификации и управления сессиями пользователей.
 * Реализует методы входа и выхода согласно спецификации OpenAPI.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImp implements AuthService {
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImp.class);

    /**
     * Аутентификация пользователя.
     * Проверяет учетные данные и возвращает токен авторизации.
     *
     * @param credentials Учетные данные пользователя (логин и пароль).
     * @return Токен авторизации при успешной аутентификации.
     * @throws IllegalArgumentException если пользователь не найден или пароль неверный.
     */
    @Override
    public User.Token login(User.Credentials credentials) {
        String login = credentials.getLogin();
        logger.info("Попытка входа для пользователя: {}", login);
        Optional<User> user = userRepository.findUserByCredentialsLoginIgnoreCase(login);

        if (user.isEmpty() || !user.get().getCredentials().getPassword().equals(credentials.getPassword())) {
            logger.warn("Пользователь не найден или неверный пароль для: {}", login);
            throw new IllegalArgumentException("Неверные учетные данные");
        }

        User foundUser = user.get();
        User.Token token = foundUser.getToken();
        if (token.getAuthToken().isEmpty()) {
            String newToken = token.generateNewToken();
            token.setAuthToken(newToken);
            userRepository.save(foundUser);
            logger.info("Сгенерирован новый токен для пользователя: {}", login);
        }

        return token;
    }

    /**
     * Завершение сессии пользователя.
     * Сбрасывает токен авторизации для указанного пользователя.
     *
     * @param token Токен авторизации (может быть null).
     * @throws IllegalArgumentException если токен отсутствует или пользователь не найден.
     */
    @Override
    public void logout(Optional<String> token) {
        logger.info("Попытка выхода с токеном: {}", token.orElse("отсутствует"));
        User user = getUserByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Неверный токен авторизации"));
        user.getToken().invalidateToken();
        userRepository.save(user);
        logger.info("Токен сброшен для пользователя: {}", user.getCredentials().getLogin());
    }

    /**
     * Поиск пользователя по токену авторизации.
     *
     * @param token Токен авторизации (может быть null).
     * @return Optional с пользователем, если найден, или пустой Optional.
     */
    public Optional<User> getUserByToken(Optional<String> token) {
        if (token.isEmpty()) {
            logger.warn("Токен отсутствует");
            return Optional.empty();
        }
        String cleanToken = token.get().replace("Bearer ", "");
        return userRepository.findUserByTokenAuthToken(cleanToken);
    }

    /**
     * Проверка валидности токена.
     *
     * @param token Токен авторизации.
     * @return true, если токен валиден и соответствует пользователю, иначе false.
     */
    public Boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Токен пустой или null");
            return false;
        }
        Optional<User> user = getUserByToken(Optional.of(token));
        boolean isValid = user.isPresent();
        if (!isValid) {
            logger.warn("Токен не найден: {}", token);
        }
        return isValid;
    }
}
package com.example.cloud.repository;

import com.example.cloud.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с таблицей пользователей в базе данных.
 * Предоставляет методы для поиска пользователей по логину и токену авторизации.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Поиск пользователя по логину (игнорируя регистр).
     * Используется для аутентификации при входе в систему.
     *
     * @param login Логин пользователя (поле credentials.login).
     * @return Optional с найденным пользователем или пустой, если пользователь не найден.
     */
    Optional<User> findUserByCredentialsLoginIgnoreCase(String login);

    /**
     * Поиск пользователя по токену авторизации.
     * Используется для проверки сессии или выхода из системы.
     *
     * @param token Токен авторизации (поле token.authToken).
     * @return Optional с найденным пользователем или пустой, если пользователь не найден.
     */
    Optional<User> findUserByTokenAuthToken(String token);
}
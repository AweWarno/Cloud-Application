package com.example.cloud.controller;

import com.example.cloud.error.ErrorResponse;
import com.example.cloud.model.User;
import com.example.cloud.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


/**
 * OpenAPI Спецификация
 * paths:
 *   /login:
 *     post:
 *       description: Authorization method
 *       requestBody:
 *         description: Login and password hash
 *         required: true
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 login:
 *                   type: string
 *                   required: true
 *                 password:
 *                   type: string
 *                   required: true
 *       responses:
 *         '200':
 *           description: Success authorization
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Login'
 *         '400':
 *           description: Bad credentials
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *   /logout:
 *     post:
 *       parameters:
 *         - in: header
 *           name: auth-token
 *           schema:
 *             type: string
 *           required: true
 *       description: Logout
 *       responses:
 *         '200':
 *           description: Success logout
 * components:
 *   schemas:
 *     Login:
 *       type: object
 *       properties:
 *         auth-token:
 *           type: string
 *     Error:
 *       type: object
 *       properties:
 *         message:
 *           type: string
 *           description: Error message
 *         id:
 *           type: integer
 */

@CrossOrigin
@RestController
@RequestMapping("/cloud")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    /**
     * Авторизация пользователя.
     * Принимает логин и пароль, возвращает токен авторизации при успешной аутентификации.
     *
     * @param credentials Объект с логином и паролем пользователя (обязательные поля).
     * @return ResponseEntity с токеном авторизации (200) или ошибкой (400).
     */
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@Valid @RequestBody User.Credentials credentials) {
        try {
            logger.info("Получен запрос на авторизацию: {}", credentials);
            User.Token token = authService.login(credentials);
            return ResponseEntity.ok().body(token);
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка авторизации: неверные данные {}", credentials, e);
            return ResponseEntity.badRequest().body(ErrorResponse.of("Неверные учетные данные", 400));
        } catch (Exception e) {
            logger.warn("Ошибка авторизации: {}", credentials, e);
            return ResponseEntity.badRequest().body(ErrorResponse.of("Неверные учетные данные", 400));
        }
    }

    /**
     * Выход пользователя из системы.
     * Завершает сессию пользователя по переданному токену авторизации.
     *
     * @param authToken Токен авторизации пользователя (обязательный заголовок).
     * @return ResponseEntity с подтверждением успеха (200) или ошибкой (400).
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("auth-token") String authToken) {
        try {
            logger.info("Получен запрос на выход с токеном: {}", authToken);
            authService.logout(Optional.ofNullable(authToken));
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка выхода: неверный токен {}", authToken, e);
            return ResponseEntity.badRequest().body(ErrorResponse.of("Неверный токен авторизации", 400));
        } catch (Exception e) {
            logger.warn("Ошибка выхода с токеном: {}", authToken, e);
            return ResponseEntity.badRequest().body(ErrorResponse.of("Ошибка при выходе из системы", 400));
        }
    }
}
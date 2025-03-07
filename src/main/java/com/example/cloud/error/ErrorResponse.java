package com.example.cloud.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Класс для стандартизированного ответа при возникновении ошибок.
 *
 * OpenAPI спецификация
 * components:
 *   schemas:
 *     Error:
 *       type: object
 *       properties:
 *         message:
 *           type: string
 *           description: Error message
 *         id:
 *           type: integer
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class ErrorResponse {

    /**
     * Человекочитаемое сообщение об ошибке.
     * Описывает, что пошло не так, в понятной для пользователя форме.
     */
    private String message;

    /**
     * HTTP статус-код ошибки.
     * Соответствует стандартным кодам HTTP (например, 400, 404, 500).
     */
    private int status;

    // Создание объекта
    public static ErrorResponse of(String message, int status) {
        return ErrorResponse.builder()
                .message(message)
                .status(status)
                .build();
    }

    // убрать перед отправкой
    @Override
    public String toString() {
        return String.format("ErrorResponse{status=%d, message='%s'}", status, message);
    }
}
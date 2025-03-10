package com.example.cloud.error;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки класса ErrorResponse.
 * Тестируются конструктор, статический метод of и метод toString.
 */
class ErrorResponseTest {

    /**
     * Тестирует создание объекта ErrorResponse через конструктор.
     * Входные данные: сообщение и статус ошибки.
     * Ожидаемый результат: объект создается с указанным сообщением и статусом 400.
     */
    @Test
    void testConstructor() {
        // Подготовка: создаем объект ErrorResponse через конструктор
        ErrorResponse errorResponse = new ErrorResponse("Тестовое сообщение", 400);

        // Проверка: убеждаемся, что поля message и status установлены корректно
        assertEquals("Тестовое сообщение", errorResponse.getMessage());
        assertEquals(400, errorResponse.getStatus());
    }

    /**
     * Тестирует создание объекта ErrorResponse через статический метод of.
     * Входные данные: сообщение и статус ошибки.
     * Ожидаемый результат: объект создается с указанным сообщением и статусом 404.
     */
    @Test
    void testOfMethod() {
        // Подготовка: создаем объект ErrorResponse через статический метод of
        ErrorResponse errorResponse = ErrorResponse.of("Тестовое сообщение", 404);

        // Проверка: убеждаемся, что поля message и status установлены корректно
        assertEquals("Тестовое сообщение", errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
    }

    /**
     * Тестирует метод toString объекта ErrorResponse.
     * Входные данные: сообщение и статус ошибки.
     * Ожидаемый результат: метод toString возвращает строку в формате "ErrorResponse{status=500, message='Тестовое сообщение'}".
     */
    @Test
    void testToString() {
        // Подготовка: создаем объект ErrorResponse с заданными параметрами
        ErrorResponse errorResponse = new ErrorResponse("Тестовое сообщение", 500);

        // Проверка: убеждаемся, что метод toString возвращает ожидаемую строку
        String expectedToString = "ErrorResponse{status=500, message='Тестовое сообщение'}";
        assertEquals(expectedToString, errorResponse.toString());
    }

    /**
     * Тестирует создание объекта ErrorResponse через метод of с null-сообщением.
     * Входные данные: null в качестве сообщения и статус ошибки.
     * Ожидаемый результат: объект создается с сообщением null и статусом 500.
     */
    @Test
    void testOfMethodWithNullMessage() {
        // Подготовка: создаем объект ErrorResponse с null-сообщением
        ErrorResponse errorResponse = ErrorResponse.of(null, 500);

        // Проверка: убеждаемся, что поле message равно null, а status установлен корректно
        assertNull(errorResponse.getMessage());
        assertEquals(500, errorResponse.getStatus());
    }

    /**
     * Тестирует создание объекта ErrorResponse через метод of с пустым сообщением.
     * Входные данные: пустая строка в качестве сообщения и статус ошибки.
     * Ожидаемый результат: объект создается с пустым сообщением и статусом 400.
     */
    @Test
    void testOfMethodWithEmptyMessage() {
        // Подготовка: создаем объект ErrorResponse с пустым сообщением
        ErrorResponse errorResponse = ErrorResponse.of("", 400);

        // Проверка: убеждаемся, что поле message пустое, а status установлен корректно
        assertEquals("", errorResponse.getMessage());
        assertEquals(400, errorResponse.getStatus());
    }
}
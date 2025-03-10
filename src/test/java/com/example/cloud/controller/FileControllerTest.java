package com.example.cloud.controller;

import com.example.cloud.error.ErrorResponse;
import com.example.cloud.model.File;
import com.example.cloud.model.FileName;
import com.example.cloud.model.User;
import com.example.cloud.service.FileService;
import com.example.cloud.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для проверки FileController с использованием Mockito.
 * Тестируются методы: getItemList, saveItem, deleteItem, updateItem, downloadItem.
 */
class FileControllerTest {

    @Mock
    private FileService fileService;

    @Mock
    private AuthService authService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileController fileController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Тестирует успешное получение списка файлов пользователя.
     * Входные данные: валидный токен и лимит элементов.
     * Ожидаемый результат: возвращается статус 200 (OK) и список файлов в формате File.
     */
    @Test
    void getItemList_successful() {
        String token = "valid-token";
        String owner = "testuser";

        User user = User.builder()
                .credentials(User.Credentials.builder().login(owner).password("password").build())
                .build();

        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));

        File file = File.builder()
                .file(File.FileDto.builder().filename("test.txt").size(100).build())
                .build();
        when(fileService.getItemList(Optional.of(10), owner)).thenReturn(List.of(file));

        ResponseEntity<?> response = fileController.getItemList(Optional.of(10), token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        List<?> files = (List<?>) response.getBody();
        assertEquals(1, files.size());
    }

    /**
     * Тестирует получение списка файлов с невалидным токеном.
     * Входные данные: невалидный токен.
     * Ожидаемый результат: возвращается статус 401 (Unauthorized) и сообщение об ошибке "Неавторизован".
     */
    @Test
    void getItemList_invalidToken_returnsUnauthorized() {
        String token = "invalid-token";
        when(authService.validateToken(token)).thenReturn(false);

        ResponseEntity<?> response = fileController.getItemList(Optional.of(10), token);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        assertEquals("Неавторизован", ((ErrorResponse) response.getBody()).getMessage());
    }

    /**
     * Тестирует успешное сохранение файла.
     * Входные данные: валидный токен, имя файла и данные файла.
     * Ожидаемый результат: возвращается статус 200 (OK), вызывается метод сохранения файла в сервисе.
     */
    @Test
    void saveItem_successful() throws Exception {
        String token = "valid-token";
        String owner = "testuser";

        User user = User.builder()
                .credentials(User.Credentials.builder().login(owner).password("password").build())
                .build();

        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));

        ResponseEntity<?> response = fileController.saveItem("test.txt", multipartFile, token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(fileService).saveItem("test.txt", multipartFile, owner);
    }

    /**
     * Тестирует сохранение файла с невалидным токеном.
     * Входные данные: невалидный токен.
     * Ожидаемый результат: возвращается статус 401 (Unauthorized) и сообщение об ошибке "Неавторизован".
     */
    @Test
    void saveItem_invalidToken_returnsUnauthorized() {
        String token = "invalid-token";
        String filename = "test.txt";
        when(authService.validateToken(token)).thenReturn(false);

        ResponseEntity<?> response = fileController.saveItem(filename, multipartFile, token);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        assertEquals("Неавторизован", ((ErrorResponse) response.getBody()).getMessage());
    }

    /**
     * Тестирует успешное удаление файла.
     * Входные данные: валидный токен и имя файла.
     * Ожидаемый результат: возвращается статус 200 (OK), вызывается метод удаления файла в сервисе.
     */
    @Test
    void deleteItem_successful() {
        String token = "valid-token";
        String owner = "testuser";

        User user = User.builder()
                .credentials(User.Credentials.builder().login(owner).password("password").build())
                .build();

        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));

        ResponseEntity<?> response = fileController.deleteItem("test.txt", token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(fileService).deleteItem("test.txt", owner);
    }

    /**
     * Тестирует удаление файла с невалидным токеном.
     * Входные данные: невалидный токен.
     * Ожидаемый результат: возвращается статус 401 (Unauthorized) и сообщение об ошибке "Неавторизован".
     */
    @Test
    void deleteItem_invalidToken_returnsUnauthorized() {
        String token = "invalid-token";
        String filename = "test.txt";
        when(authService.validateToken(token)).thenReturn(false);

        ResponseEntity<?> response = fileController.deleteItem(filename, token);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        assertEquals("Неавторизован", ((ErrorResponse) response.getBody()).getMessage());
    }

    /**
     * Тестирует успешное обновление имени файла.
     * Входные данные: валидный токен, старое и новое имя файла.
     * Ожидаемый результат: возвращается статус 200 (OK), вызывается метод обновления имени файла в сервисе.
     */
    @Test
    void updateItem_successful() {
        String token = "valid-token";
        String owner = "testuser";

        User user = User.builder()
                .credentials(User.Credentials.builder().login(owner).password("password").build())
                .build();

        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));

        FileName newFilenameDto = new FileName("new.txt");

        ResponseEntity<?> response = fileController.updateItem("old.txt", newFilenameDto, token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(fileService).updateItem("old.txt", "new.txt", owner);
    }

    /**
     * Тестирует обновление имени файла с невалидным токеном.
     * Входные данные: невалидный токен.
     * Ожидаемый результат: возвращается статус 401 (Unauthorized) и сообщение об ошибке "Неавторизован".
     */
    @Test
    void updateItem_invalidToken_returnsUnauthorized() {
        String token = "invalid-token";
        String oldFilename = "old.txt";
        FileName newFilenameDto = new FileName("new.txt");
        when(authService.validateToken(token)).thenReturn(false);

        ResponseEntity<?> response = fileController.updateItem(oldFilename, newFilenameDto, token);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        assertEquals("Неавторизован", ((ErrorResponse) response.getBody()).getMessage());
    }

    /**
     * Тестирует успешное скачивание файла.
     * Входные данные: валидный токен и имя файла.
     * Ожидаемый результат: возвращается статус 200 (OK), файл с типом application/octet-stream и заголовком Content-Disposition.
     */
    @Test
    void downloadItem_successful() {
        String token = "valid-token";
        String owner = "testuser";

        User user = User.builder()
                .credentials(User.Credentials.builder().login(owner).password("password").build())
                .build();

        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));

        byte[] fileData = "test data".getBytes();
        when(fileService.downloadItem("test.txt", owner)).thenReturn(fileData);

        ResponseEntity<?> response = fileController.downloadItem("test.txt", token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertEquals("attachment; filename=\"test.txt\"", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertArrayEquals(fileData, (byte[]) response.getBody());
    }

    /**
     * Тестирует скачивание файла с невалидным токеном.
     * Входные данные: невалидный токен.
     * Ожидаемый результат: возвращается статус 401 (Unauthorized) и сообщение об ошибке "Неавторизован".
     */
    @Test
    void downloadItem_invalidToken_returnsUnauthorized() {
        String token = "invalid-token";
        String filename = "test.txt";
        when(authService.validateToken(token)).thenReturn(false);

        ResponseEntity<?> response = fileController.downloadItem(filename, token);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        assertEquals("Неавторизован", ((ErrorResponse) response.getBody()).getMessage());
    }

    /**
     * Тестирует получение пустого списка файлов пользователя.
     * Входные данные: валидный токен и лимит элементов.
     * Ожидаемый результат: возвращается статус 200 (OK) и пустой список файлов.
     */
    @Test
    void getItemList_emptyList_returnsEmptyList() {
        String token = "valid-token";
        String owner = "testuser";

        User user = User.builder()
                .credentials(User.Credentials.builder().login(owner).password("password").build())
                .build();

        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));
        when(fileService.getItemList(Optional.of(10), owner)).thenReturn(List.of());

        ResponseEntity<?> response = fileController.getItemList(Optional.of(10), token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        List<?> files = (List<?>) response.getBody();
        assertTrue(files.isEmpty());
    }

    /**
     * Тестирует удаление несуществующего файла.
     * Входные данные: валидный токен и имя файла.
     * Ожидаемый результат: возвращается статус 400 (Bad Request) и сообщение об ошибке "Ошибка входных данных".
     */
    @Test
    void deleteItem_fileNotFound_returnsBadRequest() {
        String token = "valid-token";
        String owner = "testuser";

        User user = User.builder()
                .credentials(User.Credentials.builder().login(owner).password("password").build())
                .build();

        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));
        doThrow(new IllegalArgumentException("Ошибка входных данных"))
                .when(fileService).deleteItem("test.txt", owner);

        ResponseEntity<?> response = fileController.deleteItem("test.txt", token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        assertEquals("Ошибка входных данных", ((ErrorResponse) response.getBody()).getMessage());
    }

    /**
     * Тестирует обновление имени несуществующего файла.
     * Входные данные: валидный токен, старое и новое имя файла.
     * Ожидаемый результат: возвращается статус 400 (Bad Request) и сообщение об ошибке "Ошибка входных данных".
     */
    @Test
    void updateItem_fileNotFound_returnsBadRequest() {
        String token = "valid-token";
        String owner = "testuser";

        User user = User.builder()
                .credentials(User.Credentials.builder().login(owner).password("password").build())
                .build();

        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));
        doThrow(new IllegalArgumentException("Ошибка входных данных"))
                .when(fileService).updateItem("old.txt", "new.txt", owner);

        FileName newFilenameDto = new FileName("new.txt");

        ResponseEntity<?> response = fileController.updateItem("old.txt", newFilenameDto, token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        assertEquals("Ошибка входных данных", ((ErrorResponse) response.getBody()).getMessage());
    }

    /**
     * Тестирует скачивание несуществующего файла.
     * Входные данные: валидный токен и имя файла.
     * Ожидаемый результат: возвращается статус 400 (Bad Request) и сообщение об ошибке "Файл не найден".
     */
    @Test
    void downloadItem_fileNotFound_returnsBadRequest() {
        String token = "valid-token";
        String owner = "testuser";

        User user = User.builder()
                .credentials(User.Credentials.builder().login(owner).password("password").build())
                .build();

        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));
        when(fileService.downloadItem("test.txt", owner)).thenThrow(new IllegalArgumentException("Файл не найден"));

        ResponseEntity<?> response = fileController.downloadItem("test.txt", token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        assertEquals("Файл не найден", ((ErrorResponse) response.getBody()).getMessage());
    }
}
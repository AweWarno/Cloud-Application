package com.example.cloud.service;

import com.example.cloud.model.File;
import com.example.cloud.model.User;
import com.example.cloud.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для проверки реализации FileServiceImp.
 * Тестируются методы: getItemList, saveItem, deleteItem, updateItem, downloadItem.
 */
class FileServiceImpTest {

    @Mock
    private AuthServiceImp authService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileServiceImp fileService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Тесты для getItemList

    /**
     * Тестирует успешное получение списка файлов пользователя.
     * Входные данные: валидный токен и лимит элементов.
     * Ожидаемый результат: возвращается список файлов, отсортированных по размеру.
     */
    @Test
    void getItemList_successful() {
        String token = "valid-token";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .build();
        File file = File.builder()
                .file(File.FileDto.builder().filename("test.txt").size(100).build())
                .build();
        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));
        when(fileRepository.findItemByOwnerOrderByFileSizeAsc(eq("testuser"), any())).thenReturn(List.of(file));

        List<File.FileDto> files = fileService.getItemList(Optional.of(10), token);

        assertEquals(1, files.size(), "Список должен содержать один файл");
        assertEquals("test.txt", files.get(0).getFilename(), "Имя файла должно совпадать");
    }

    /**
     * Тестирует получение списка файлов с невалидным токеном.
     * Входные данные: невалидный токен.
     * Ожидаемый результат: выбрасывается SecurityException с сообщением "Невалидный токен".
     */
    @Test
    void getItemList_invalidToken_throwsSecurityException() {
        String token = "invalid-token";
        when(authService.validateToken(token)).thenReturn(false);

        SecurityException exception = assertThrows(SecurityException.class,
                () -> fileService.getItemList(Optional.of(10), token));
        assertEquals("Невалидный токен", exception.getMessage());
        verify(fileRepository, never()).findItemByOwnerOrderByFileSizeAsc(any(), any());
    }

    // Тесты для saveItem

    /**
     * Тестирует успешное сохранение файла.
     * Входные данные: валидный токен, имя файла и данные файла.
     * Ожидаемый результат: файл сохраняется в репозитории.
     */
    @Test
    void saveItem_successful() throws IOException {
        String token = "valid-token";
        String filename = "test.txt";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .build();
        File file = File.builder()
                .file(File.FileDto.builder().filename(filename).size(100).build())
                .data("test data".getBytes())
                .owner("testuser")
                .build();
        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));
        when(multipartFile.getSize()).thenReturn(100L);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test data".getBytes()));
        when(fileRepository.save(any(File.class))).thenReturn(file);

        fileService.saveItem(filename, multipartFile, token);

        verify(fileRepository).save(any(File.class));
    }

    /**
     * Тестирует сохранение файла с невалидным токеном.
     * Входные данные: невалидный токен, имя файла и данные файла.
     * Ожидаемый результат: выбрасывается SecurityException с сообщением "Невалидный токен".
     */
    @Test
    void saveItem_invalidToken_throwsSecurityException() throws IOException {
        String token = "invalid-token";
        String filename = "test.txt";
        when(authService.validateToken(token)).thenReturn(false);

        SecurityException exception = assertThrows(SecurityException.class,
                () -> fileService.saveItem(filename, multipartFile, token));
        assertEquals("Невалидный токен", exception.getMessage());
        verify(fileRepository, never()).save(any());
    }

    // Тесты для deleteItem

    /**
     * Тестирует успешное удаление файла.
     * Входные данные: валидный токен и имя файла.
     * Ожидаемый результат: файл удаляется из репозитория.
     */
    @Test
    void deleteItem_successful() {
        String token = "valid-token";
        String filename = "test.txt";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .build();
        File file = File.builder()
                .file(File.FileDto.builder().filename(filename).size(100).build())
                .owner("testuser")
                .build();
        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));
        when(fileRepository.findItemByOwnerAndFileFilename("testuser", filename)).thenReturn(Optional.of(file));

        fileService.deleteItem(filename, token);

        verify(fileRepository).delete(file);
    }

    /**
     * Тестирует удаление несуществующего файла.
     * Входные данные: валидный токен и имя несуществующего файла.
     * Ожидаемый результат: выбрасывается IllegalArgumentException с сообщением "Файл не найден".
     */
    @Test
    void deleteItem_fileNotFound_throwsIllegalArgumentException() {
        String token = "valid-token";
        String filename = "test.txt";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .build();
        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));
        when(fileRepository.findItemByOwnerAndFileFilename("testuser", filename)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileService.deleteItem(filename, token));
        assertEquals("Файл не найден", exception.getMessage());
        verify(fileRepository, never()).delete(any());
    }

    // Тесты для updateItem

    /**
     * Тестирует успешное обновление имени файла.
     * Входные данные: валидный токен, старое и новое имя файла.
     * Ожидаемый результат: имя файла обновляется и сохраняется.
     */
    @Test
    void updateItem_successful() {
        String token = "valid-token";
        String oldFilename = "old.txt";
        String newFilename = "new.txt";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .build();
        File file = File.builder()
                .file(File.FileDto.builder().filename(oldFilename).size(100).build())
                .owner("testuser")
                .build();
        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));
        when(fileRepository.findItemByOwnerAndFileFilename("testuser", oldFilename)).thenReturn(Optional.of(file));
        when(fileRepository.save(file)).thenReturn(file);

        fileService.updateItem(oldFilename, newFilename, Optional.of(token));

        assertEquals(newFilename, file.getFile().getFilename(), "Имя файла должно быть обновлено");
        verify(fileRepository).save(file);
    }

    /**
     * Тестирует обновление несуществующего файла.
     * Входные данные: валидный токен, старое и новое имя несуществующего файла.
     * Ожидаемый результат: выбрасывается IllegalArgumentException с сообщением "Файл не найден".
     */
    @Test
    void updateItem_fileNotFound_throwsIllegalArgumentException() {
        String token = "valid-token";
        String oldFilename = "old.txt";
        String newFilename = "new.txt";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .build();
        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));
        when(fileRepository.findItemByOwnerAndFileFilename("testuser", oldFilename)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileService.updateItem(oldFilename, newFilename, Optional.of(token)));
        assertEquals("Файл не найден", exception.getMessage());
        verify(fileRepository, never()).save(any());
    }

    // Тесты для downloadItem

    /**
     * Тестирует успешное скачивание файла.
     * Входные данные: валидный токен и имя файла.
     * Ожидаемый результат: возвращаются данные файла.
     */
    @Test
    void downloadItem_successful() {
        String token = "valid-token";
        String filename = "test.txt";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .build();
        byte[] fileData = "test data".getBytes();
        File file = File.builder()
                .file(File.FileDto.builder().filename(filename).size(100).build())
                .data(fileData)
                .owner("testuser")
                .build();
        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));
        when(fileRepository.findItemByOwnerAndFileFilename("testuser", filename)).thenReturn(Optional.of(file));

        byte[] result = fileService.downloadItem(filename, token);

        assertArrayEquals(fileData, result, "Данные файла должны совпадать");
    }

    /**
     * Тестирует скачивание несуществующего файла.
     * Входные данные: валидный токен и имя несуществующего файла.
     * Ожидаемый результат: выбрасывается IllegalArgumentException с сообщением "Файл не найден".
     */
    @Test
    void downloadItem_fileNotFound_throwsIllegalArgumentException() {
        String token = "valid-token";
        String filename = "test.txt";
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .build();
        when(authService.validateToken(token)).thenReturn(true);
        when(authService.getUserByToken(Optional.of(token))).thenReturn(Optional.of(user));
        when(fileRepository.findItemByOwnerAndFileFilename("testuser", filename)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileService.downloadItem(filename, token));
        assertEquals("Файл не найден", exception.getMessage());
    }
}
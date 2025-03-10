package com.example.cloud.service;

import com.example.cloud.model.File;
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
 * Тестовый класс для проверки реализации FileServiceImp с использованием Mockito.
 * Тестируются методы: getItemList, saveItem, deleteItem, updateItem, downloadItem.
 */
class FileServiceImpTest {

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
     * Входные данные: валидный owner и лимит элементов.
     * Ожидаемый результат: возвращается список файлов, отсортированных по размеру в порядке возрастания.
     */
    @Test
    void getItemList_successful() {
        // Подготовка: задаем владельца и мок ответа репозитория
        String owner = "testuser";
        File file = File.builder()
                .file(File.FileDto.builder().filename("test.txt").size(100).build())
                .build();
        when(fileRepository.findItemByOwnerOrderByFileSizeAsc(eq(owner), any())).thenReturn(List.of(file));

        // Действие: вызываем метод getItemList
        List<File> files = fileService.getItemList(Optional.of(10), owner);

        // Проверка: убеждаемся, что список содержит ожидаемые данные
        assertEquals(1, files.size(), "Список должен содержать один файл");
        assertEquals("test.txt", files.get(0).getFile().getFilename(), "Имя файла должно совпадать");
    }

    // Тесты для saveItem

    /**
     * Тестирует успешное сохранение файла.
     * Входные данные: валидный owner, имя файла и данные файла из MultipartFile.
     * Ожидаемый результат: файл сохраняется в репозитории с корректными данными.
     */
    @Test
    void saveItem_successful() throws IOException {
        // Подготовка: задаем данные и мок поведения MultipartFile и репозитория
        String owner = "testuser";
        String filename = "test.txt";
        File file = File.builder()
                .file(File.FileDto.builder().filename(filename).size(100).build())
                .data("test data".getBytes())
                .owner(owner)
                .build();
        when(multipartFile.getSize()).thenReturn(100L);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test data".getBytes()));
        when(fileRepository.save(any(File.class))).thenReturn(file);

        // Действие: вызываем метод saveItem
        fileService.saveItem(filename, multipartFile, owner);

        // Проверка: убеждаемся, что файл был сохранен
        verify(fileRepository).save(any(File.class));
    }

    /**
     * Тестирует попытку сохранения файла с пустым именем.
     * Входные данные: пустая строка в качестве имени файла, валидный owner и данные файла.
     * Ожидаемый результат: выбрасывается IllegalArgumentException с сообщением "Имя файла не может быть null или пустым", сохранение не происходит.
     */
    @Test
    void saveItem_emptyFilename_throwsIllegalArgumentException() {
        // Подготовка: задаем пустое имя файла
        String owner = "testuser";
        String filename = "";

        // Проверка: ожидаем исключение при вызове saveItem
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileService.saveItem(filename, multipartFile, owner));
        assertEquals("Имя файла не может быть null или пустым", exception.getMessage());
        verify(fileRepository, never()).save(any());
    }

    /**
     * Тестирует попытку сохранения файла с null в качестве имени.
     * Входные данные: null в качестве имени файла, валидный owner и данные файла.
     * Ожидаемый результат: выбрасывается IllegalArgumentException с сообщением "Имя файла не может быть null или пустым", сохранение не происходит.
     */
    @Test
    void saveItem_nullFilename_throwsIllegalArgumentException() {
        // Подготовка: задаем null в качестве имени файла
        String owner = "testuser";
        String filename = null;

        // Проверка: ожидаем исключение при вызове saveItem
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileService.saveItem(filename, multipartFile, owner));
        assertEquals("Имя файла не может быть null или пустым", exception.getMessage());
        verify(fileRepository, never()).save(any());
    }

    // Тесты для deleteItem

    /**
     * Тестирует успешное удаление файла.
     * Входные данные: валидный owner и имя файла.
     * Ожидаемый результат: файл удаляется из репозитория.
     */
    @Test
    void deleteItem_successful() {
        // Подготовка: задаем данные файла и мок ответа репозитория
        String owner = "testuser";
        String filename = "test.txt";
        File file = File.builder()
                .file(File.FileDto.builder().filename(filename).size(100).build())
                .owner(owner)
                .build();
        when(fileRepository.findItemByOwnerAndFileFilename(owner, filename)).thenReturn(Optional.of(file));

        // Действие: вызываем метод deleteItem
        fileService.deleteItem(filename, owner);

        // Проверка: убеждаемся, что файл был удален
        verify(fileRepository).delete(file);
    }

    /**
     * Тестирует попытку удаления несуществующего файла.
     * Входные данные: валидный owner и имя несуществующего файла.
     * Ожидаемый результат: выбрасывается IllegalArgumentException с сообщением "Файл не найден", удаление не происходит.
     */
    @Test
    void deleteItem_fileNotFound_throwsIllegalArgumentException() {
        // Подготовка: задаем несуществующий файл
        String owner = "testuser";
        String filename = "test.txt";
        when(fileRepository.findItemByOwnerAndFileFilename(owner, filename)).thenReturn(Optional.empty());

        // Проверка: ожидаем исключение при вызове deleteItem
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileService.deleteItem(filename, owner));
        assertEquals("Файл не найден", exception.getMessage());
        verify(fileRepository, never()).delete(any());
    }

    // Тесты для updateItem

    /**
     * Тестирует успешное обновление имени файла.
     * Входные данные: валидный owner, старое и новое имя файла.
     * Ожидаемый результат: имя файла обновляется, изменения сохраняются в репозитории.
     */
    @Test
    void updateItem_successful() {
        // Подготовка: задаем данные файла и мок поведения репозитория
        String owner = "testuser";
        String oldFilename = "old.txt";
        String newFilename = "new.txt";
        File file = File.builder()
                .file(File.FileDto.builder().filename(oldFilename).size(100).build())
                .owner(owner)
                .build();
        when(fileRepository.findItemByOwnerAndFileFilename(owner, oldFilename)).thenReturn(Optional.of(file));
        when(fileRepository.save(file)).thenReturn(file);

        // Действие: вызываем метод updateItem
        fileService.updateItem(oldFilename, newFilename, owner);

        // Проверка: убеждаемся, что имя файла обновлено
        assertEquals(newFilename, file.getFile().getFilename(), "Имя файла должно быть обновлено");
        verify(fileRepository).save(file);
    }

    /**
     * Тестирует попытку обновления имени несуществующего файла.
     * Входные данные: валидный owner, старое и новое имя несуществующего файла.
     * Ожидаемый результат: выбрасывается IllegalArgumentException с сообщением "Файл не найден", сохранение не происходит.
     */
    @Test
    void updateItem_fileNotFound_throwsIllegalArgumentException() {
        // Подготовка: задаем несуществующий файл
        String owner = "testuser";
        String oldFilename = "old.txt";
        String newFilename = "new.txt";
        when(fileRepository.findItemByOwnerAndFileFilename(owner, oldFilename)).thenReturn(Optional.empty());

        // Проверка: ожидаем исключение при вызове updateItem
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileService.updateItem(oldFilename, newFilename, owner));
        assertEquals("Файл не найден", exception.getMessage());
        verify(fileRepository, never()).save(any());
    }

    // Тесты для downloadItem

    /**
     * Тестирует успешное скачивание файла.
     * Входные данные: валидный owner и имя файла.
     * Ожидаемый результат: возвращаются данные файла в виде массива байтов.
     */
    @Test
    void downloadItem_successful() {
        // Подготовка: задаем данные файла и мок ответа репозитория
        String owner = "testuser";
        String filename = "test.txt";
        byte[] fileData = "test data".getBytes();
        File file = File.builder()
                .file(File.FileDto.builder().filename(filename).size(100).build())
                .data(fileData)
                .owner(owner)
                .build();
        when(fileRepository.findItemByOwnerAndFileFilename(owner, filename)).thenReturn(Optional.of(file));

        // Действие: вызываем метод downloadItem
        byte[] result = fileService.downloadItem(filename, owner);

        // Проверка: убеждаемся, что данные файла возвращены корректно
        assertArrayEquals(fileData, result, "Данные файла должны совпадать");
    }

    /**
     * Тестирует попытку скачивания несуществующего файла.
     * Входные данные: валидный owner и имя несуществующего файла.
     * Ожидаемый результат: выбрасывается IllegalArgumentException с сообщением "Файл не найден".
     */
    @Test
    void downloadItem_fileNotFound_throwsIllegalArgumentException() {
        // Подготовка: задаем несуществующий файл
        String owner = "testuser";
        String filename = "test.txt";
        when(fileRepository.findItemByOwnerAndFileFilename(owner, filename)).thenReturn(Optional.empty());

        // Проверка: ожидаем исключение при вызове downloadItem
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileService.downloadItem(filename, owner));
        assertEquals("Файл не найден", exception.getMessage());
    }
}
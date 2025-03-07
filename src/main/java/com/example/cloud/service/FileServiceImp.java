package com.example.cloud.service;

import com.example.cloud.model.File;
import com.example.cloud.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления файлами.
 * Реализует операции получения списка, загрузки, удаления, обновления и скачивания файлов.
 */
@Service
@RequiredArgsConstructor
public class FileServiceImp implements FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileServiceImp.class);
    private final AuthServiceImp authService;
    private final FileRepository fileRepository;

    /**
     * Получение списка файлов пользователя.
     * Возвращает список файлов, отсортированных по размеру, с учетом лимита.
     *
     * @param limit Опциональный лимит количества возвращаемых файлов.
     * @param token Токен авторизации пользователя.
     * @return Список объектов FileDto с именами и размерами файлов.
     * @throws SecurityException если токен невалиден.
     */
    public List<File.FileDto> getItemList(Optional<Integer> limit, String token) {
        logger.info("Получен запрос на список файлов, лимит: {}, токен: {}", limit, token);
        if (!authService.validateToken(token)) {
            logger.warn("Невалидный токен при запросе списка файлов: {}", token);
            throw new SecurityException("Невалидный токен");
        }

        String owner = authService.getUserByToken(Optional.of(token))
                .map(user -> user.getCredentials().getLogin())
                .orElseThrow(() -> new SecurityException("Пользователь не найден"));

        Limit limitValue = limit.filter(l -> l > 0) // Только положительные значения
                .map(Limit::of)
                .orElseGet(Limit::unlimited); // limit=0 или отсутствует -> unlimited
        List<File> items = fileRepository.findItemByOwnerOrderByFileSizeAsc(owner, limitValue);
        return items.stream().map(File::getFile).toList();
    }

    /**
     * Сохранение файла на сервере.
     * Загружает файл в базу данных с указанным именем и владельцем.
     *
     * @param filename Имя файла.
     * @param file Файл в формате MultipartFile.
     * @param token Токен авторизации пользователя.
     * @throws SecurityException если токен невалиден.
     * @throws IllegalArgumentException если не удалось сохранить файл.
     */
    public void saveItem(String filename, MultipartFile file, String token) {
        logger.info("Получен запрос на сохранение файла: {}, токен: {}", filename, token);
        if (!authService.validateToken(token)) {
            logger.warn("Невалидный токен при сохранении файла: {}", token);
            throw new SecurityException("Невалидный токен");
        }
        if (filename == null || filename.trim().isEmpty()) {
            logger.error("Имя файла не указано или пустое");
            throw new IllegalArgumentException("Имя файла не может быть null или пустым");
        }
        if (file == null || file.isEmpty()) {
            logger.error("Файл не передан или пустой");
            throw new IllegalArgumentException("Файл не может быть null или пустым");
        }

        String owner = authService.getUserByToken(Optional.ofNullable(token))
                .map(user -> user.getCredentials().getLogin())
                .orElseThrow(() -> new SecurityException("Пользователь не найден"));

        try {
            File newFile = File.builder()
                    .hash(String.valueOf(file.hashCode()))
                    .owner(owner)
                    .data(file.getInputStream().readAllBytes())
                    .file(File.FileDto.builder()
                            .filename(filename)
                            .size(file.getSize())
                            .build())
                    .build();
            fileRepository.save(newFile);
            logger.info("Файл успешно сохранен: {}", filename);
        } catch (IOException e) {
            logger.error("Ошибка ввода-вывода при сохранении файла: {}", filename, e);
            throw new IllegalArgumentException("Ошибка при чтении файла: " + e.getMessage());
        }
    }

    /**
     * Удаление файла с сервера.
     * Удаляет файл по имени и владельцу.
     *
     * @param filename Имя файла для удаления.
     * @param token Токен авторизации пользователя.
     * @throws SecurityException если токен невалиден.
     * @throws IllegalArgumentException если файл не найден.
     */
    public void deleteItem(String filename, String token) {
        logger.info("Получен запрос на удаление файла: {}, токен: {}", filename, token);
        if (!authService.validateToken(token)) {
            logger.warn("Невалидный токен при удалении файла: {}", token);
            throw new SecurityException("Невалидный токен");
        }

        String owner = authService.getUserByToken(Optional.ofNullable(token))
                .map(user -> user.getCredentials().getLogin())
                .orElseThrow(() -> new SecurityException("Пользователь не найден"));

        Optional<File> item = fileRepository.findItemByOwnerAndFileFilename(owner, filename);
        if (item.isEmpty()) {
            logger.warn("Файл не найден для удаления: {}, владелец: {}", filename, owner);
            throw new IllegalArgumentException("Файл не найден");
        }

        fileRepository.delete(item.get());
        logger.info("Файл успешно удален: {}", filename);
    }

    /**
     * Обновление имени файла.
     * Изменяет имя файла на новое для указанного владельца.
     *
     * @param oldFilename Старое имя файла.
     * @param newFilename Новое имя файла.
     * @param token Токен авторизации пользователя.
     * @throws SecurityException если токен невалиден.
     * @throws IllegalArgumentException если файл не найден.
     */
    public void updateItem(String oldFilename, String newFilename, Optional<String> token) {
        logger.info("Получен запрос на обновление файла: {} -> {}, токен: {}", oldFilename, newFilename, token);
        if (!authService.validateToken(token.orElse(""))) {
            logger.warn("Невалидный токен при обновлении файла: {}", token);
            throw new SecurityException("Невалидный токен");
        }

        String owner = authService.getUserByToken(token)
                .map(user -> user.getCredentials().getLogin())
                .orElseThrow(() -> new SecurityException("Пользователь не найден"));

        Optional<File> file = fileRepository.findItemByOwnerAndFileFilename(owner, oldFilename);
        if (file.isEmpty()) {
            logger.warn("Файл не найден для обновления: {}, владелец: {}", oldFilename, owner);
            throw new IllegalArgumentException("Файл не найден");
        }

        file.get().getFile().setFilename(newFilename);
        fileRepository.save(file.get());
        logger.info("Имя файла успешно обновлено: {} -> {}", oldFilename, newFilename);
    }

    /**
     * Скачивание файла с сервера.
     * Возвращает бинарные данные файла по имени и владельцу.
     *
     * @param filename Имя файла для скачивания.
     * @param token    Токен авторизации пользователя.
     * @return Объект File с данными файла.
     * @throws SecurityException        если токен невалиден.
     * @throws IllegalArgumentException если файл не найден.
     */
    public byte[] downloadItem(String filename, String token) {
        logger.info("Получен запрос на скачивание файла: {}, токен: {}", filename, token);
        if (!authService.validateToken(token)) {
            logger.warn("Невалидный токен при скачивании файла: {}", token);
            throw new SecurityException("Невалидный токен");
        }

        String owner = authService.getUserByToken(Optional.of(token))
                .map(user -> user.getCredentials().getLogin())
                .orElseThrow(() -> new SecurityException("Пользователь не найден"));

        Optional<File> item = fileRepository.findItemByOwnerAndFileFilename(owner, filename);
        if (item.isEmpty()) {
            logger.warn("Файл не найден для скачивания: {}, владелец: {}", filename, owner);
            throw new IllegalArgumentException("Файл не найден");
        }

        logger.info("Файл успешно подготовлен для скачивания: {}", filename);
        return item.get().getData();
    }
}
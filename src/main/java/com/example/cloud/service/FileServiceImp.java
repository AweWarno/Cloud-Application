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
     * @param owner Токен авторизации пользователя.
     * @return Список объектов FileDto с именами и размерами файлов.
     * @throws SecurityException если токен невалиден.
     */
    @Override
    public List<File> getItemList(Optional<Integer> limit, String owner) {
        logger.info("Получен запрос на список файлов, лимит: {}, владелец: {}", limit, owner);

        Limit limitValue = limit.filter(l -> l > 0)
                .map(Limit::of)
                .orElseGet(Limit::unlimited);

        return fileRepository.findItemByOwnerOrderByFileSizeAsc(owner, limitValue);
    }

    /**
     * Сохранение файла на сервере.
     * Загружает файл в базу данных с указанным именем и владельцем.
     *
     * @param filename Имя файла.
     * @param file Файл в формате MultipartFile.
     * @param owner Токен авторизации пользователя.
     * @throws SecurityException если токен невалиден.
     * @throws IllegalArgumentException если не удалось сохранить файл.
     */
    @Override
    public void saveItem(String filename, MultipartFile file, String owner) throws IOException {
        logger.info("Получен запрос на сохранение файла: {}, владелец: {}", filename, owner);

        // Валидация входных данных
        if (filename == null || filename.trim().isEmpty()) {
            logger.error("Имя файла не указано или пустое");
            throw new IllegalArgumentException("Имя файла не может быть null или пустым");
        }
        if (file == null || file.isEmpty()) {
            logger.error("Файл не передан или пустой");
            throw new IllegalArgumentException("Файл не может быть null или пустым");
        }

        try {
            // Чтение данных файла
            byte[] fileData = file.getInputStream().readAllBytes();

            // Создание объекта File
            File newFile = File.builder()
                    .hash(String.valueOf(file.hashCode())) // Хэш файла для уникальности
                    .owner(owner) // Владелец файла
                    .data(fileData) // Бинарные данные файла
                    .file(File.FileDto.builder()
                            .filename(filename) // Имя файла
                            .size(file.getSize()) // Размер файла
                            .build())
                    .build();

            // Сохранение файла в репозитории
            fileRepository.save(newFile);
            logger.info("Файл успешно сохранен: {}", filename);
        } catch (IOException e) {
            logger.error("Ошибка ввода-вывода при сохранении файла: {}", filename, e);
            throw new IOException("Ошибка при чтении файла: " + e.getMessage());
        }
    }

    /**
     * Удаление файла с сервера.
     * Удаляет файл по имени и владельцу.
     *
     * @param filename Имя файла для удаления.
     * @param owner Токен авторизации пользователя.
     * @throws SecurityException если токен невалиден.
     * @throws IllegalArgumentException если файл не найден.
     */
    @Override
    public void deleteItem(String filename, String owner) {
        logger.info("Получен запрос на удаление файла: {}, владелец: {}", filename, owner);

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
     * @param owner Токен авторизации пользователя.
     * @throws SecurityException если токен невалиден.
     * @throws IllegalArgumentException если файл не найден.
     */
    @Override
    public void updateItem(String oldFilename, String newFilename, String owner) {
        logger.info("Получен запрос на обновление файла: {} -> {}, владелец: {}", oldFilename, newFilename, owner);

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
     * @param owner    Токен авторизации пользователя.
     * @return Объект File с данными файла.
     * @throws SecurityException        если токен невалиден.
     * @throws IllegalArgumentException если файл не найден.
     */
    @Override
    public byte[] downloadItem(String filename, String owner) {
        logger.info("Получен запрос на скачивание файла: {}, владелец: {}", filename, owner);

        Optional<File> item = fileRepository.findItemByOwnerAndFileFilename(owner, filename);
        if (item.isEmpty()) {
            logger.warn("Файл не найден для скачивания: {}, владелец: {}", filename, owner);
            throw new IllegalArgumentException("Файл не найден");
        }

        logger.info("Файл успешно подготовлен для скачивания: {}", filename);
        return item.get().getData();
    }
}
package com.example.cloud.service;

import com.example.cloud.model.File;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс сервиса для управления файлами пользователей.
 * Предоставляет методы для работы с файлами: получение списка, загрузка, удаление, обновление и скачивание.
 * Все операции требуют валидного токена авторизации для проверки прав доступа.
 */
public interface FileService {

    /**
     * Получение списка файлов пользователя.
     * Возвращает список файлов с их именами и размерами, ограниченный указанным лимитом.
     *
     * @param limit Опциональный параметр, ограничивающий количество возвращаемых файлов.
     *              Если {@link Optional} пустой или содержит значение <= 0, возвращаются все доступные файлы.
     * @param token Токен авторизации пользователя в виде строки. Не должен быть null или пустым.
     * @return Список объектов {@link File.FileDto}, содержащих имя и размер каждого файла.
     * @throws SecurityException если токен невалиден или пользователь не авторизован.
     * @throws IllegalArgumentException если параметры запроса некорректны.
     */
    List<File.FileDto> getItemList(Optional<Integer> limit, String token);

    /**
     * Сохранение файла на сервере.
     * Загружает файл в хранилище, связывая его с авторизованным пользователем.
     *
     * @param filename Имя файла, под которым он будет сохранён. Не должно быть null или пустым.
     * @param file Файл в формате {@link MultipartFile}, содержащий данные для загрузки.
     *             Не должен быть null или пустым.
     * @param token Токен авторизации пользователя в виде строки. Не должен быть null или пустым.
     * @throws SecurityException если токен невалиден или пользователь не авторизован.
     * @throws IllegalArgumentException если имя файла или данные файла некорректны.
     * @throws org.springframework.web.multipart.MaxUploadSizeExceededException если размер файла превышает допустимый лимит.
     */
    void saveItem(String filename, MultipartFile file, String token);

    /**
     * Удаление файла с сервера.
     * Удаляет файл по указанному имени, если он принадлежит авторизованному пользователю.
     *
     * @param filename Имя файла для удаления. Не должно быть null или пустым.
     * @param token Токен авторизации пользователя в виде строки. Не должен быть null или пустым.
     * @throws SecurityException если токен невалиден или пользователь не авторизован.
     * @throws IllegalArgumentException если файл с указанным именем не найден.
     */
    void deleteItem(String filename, String token);

    /**
     * Обновление имени файла.
     * Изменяет имя существующего файла на новое для авторизованного пользователя.
     *
     * @param oldFilename Текущее имя файла, которое нужно изменить. Не должно быть null или пустым.
     * @param newFilename Новое имя файла. Не должно быть null или пустым.
     * @param token Токен авторизации в виде {@link Optional<String>}. Может быть пустым, но валидность проверяется.
     * @throws SecurityException если токен невалиден или пользователь не авторизован.
     * @throws IllegalArgumentException если файл не найден или новое имя некорректно.
     */
    void updateItem(String oldFilename, String newFilename, Optional<String> token);

    /**
     * Скачивание файла с сервера.
     * Возвращает бинарные данные файла по его имени для авторизованного пользователя.
     *
     * @param filename Имя файла для скачивания. Не должно быть null или пустым.
     * @param token Токен авторизации пользователя в виде строки. Не должен быть null или пустым.
     * @return Массив байтов ({@code byte[]}), представляющий содержимое файла.
     * @throws SecurityException если токен невалиден или пользователь не авторизован.
     * @throws IllegalArgumentException если файл с указанным именем не найден.
     */
    byte[] downloadItem(String filename, String token);
}
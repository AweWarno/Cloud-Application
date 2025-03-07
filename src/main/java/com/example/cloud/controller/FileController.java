package com.example.cloud.controller;

import com.example.cloud.error.ErrorResponse;
import com.example.cloud.model.File;
import com.example.cloud.model.FileName;
import com.example.cloud.service.FileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * paths:
 *   /file:
 *     post:
 *       description: Upload file to server
 *       parameters:
 *         - in: header
 *           name: auth-token
 *           schema:
 *             type: string
 *           required: true
 *         - name: filename
 *           in: query
 *           schema:
 *             type: string
 *           description: File name to upload
 *       requestBody:
 *         content:
 *           multipart/form-data:
 *             schema:
 *               $ref: '#/components/schemas/File'
 *       responses:
 *         '200':
 *           description: Success upload
 *         '400':
 *           description: Error input data
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *         '401':
 *           description: Unauthorized error
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *     delete:
 *       description: Delete file
 *       parameters:
 *         - in: header
 *           name: auth-token
 *           schema:
 *             type: string
 *           required: true
 *         - name: filename
 *           in: query
 *           schema:
 *             type: string
 *           description: File name to delete
 *           required: true
 *       responses:
 *         '200':
 *           description: Success deleted
 *         '400':
 *           description: Error input data
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *         '401':
 *           description: Unauthorized error
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *         '500':
 *           description: Error delet file
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *     get:
 *       description: Dowload file from cloud
 *       parameters:
 *         - in: header
 *           name: auth-token
 *           schema:
 *             type: string
 *           required: true
 *         - name: filename
 *           in: query
 *           schema:
 *             type: string
 *           description: File name to download
 *       responses:
 *         '200':
 *           description: Success deleted
 *           content:
 *             multipart/form-data:
 *               schema:
 *                 $ref: '#/components/schemas/File'
 *         '400':
 *           description: Error input data
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *         '401':
 *           description: Unauthorized error
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *         '500':
 *           description: Error upload file
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *     put:
 *       description: Edit file name
 *       parameters:
 *         - in: header
 *           name: auth-token
 *           schema:
 *             type: string
 *           required: true
 *         - name: filename
 *           in: query
 *           schema:
 *             type: string
 *           description: File name to upload
 *       requestBody:
 *         description: Login and password hash
 *         required: true
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 name:
 *                   type: string
 *       responses:
 *         '200':
 *           description: Success upload
 *         '400':
 *           description: Error input data
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *         '401':
 *           description: Unauthorized error
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *         '500':
 *           description: Error upload file
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *   /list:
 *     get:
 *       description: Get all files
 *       parameters:
 *         - in: header
 *           name: auth-token
 *           schema:
 *             type: string
 *           required: true
 *         - name: limit
 *           in: query
 *           schema:
 *             type: integer
 *           description: Number requested items
 *       responses:
 *         '200':
 *           description: Success get list
 *           content:
 *             application/json:
 *               schema:
 *                 type: object
 *                 properties:
 *                   filename:
 *                     type: string
 *                     description: File name
 *                     required: true
 *                   size:
 *                     type: integer
 *                     description: File size in bytes
 *                     required: true
 *         '400':
 *           description: Error input data
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *         '401':
 *           description: Unauthorized error
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 *         '500':
 *           description: Error getting file list
 *           content:
 *             application/json:
 *               schema:
 *                 $ref: '#/components/schemas/Error'
 */


@CrossOrigin
@RestController
@RequestMapping("/cloud")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    /**
     * logger.info: Используется для записи успешных операций.
     * logger.warn: Используется для записи ошибок с указанием причины и контекста.
     */

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    /**
     * Получение списка файлов пользователя.
     * Возвращает список файлов с их именами и размерами, ограниченный параметром limit.
     *
     * @param limit           Опциональный параметр, ограничивающий количество возвращаемых файлов.
     * @param headerAuthToken Токен авторизации пользователя.
     * @return ResponseEntity с успешным списком файлов (200) или ошибкой (400, 401, 500).
     */
    @GetMapping("/list")
    public ResponseEntity<?> getItemList(
            @RequestParam("limit") Optional<Integer> limit,
            @RequestHeader("auth-token") String headerAuthToken) {
        try {
            logger.info("Получен запрос на получение списка файлов с лимитом: {} и токеном: {}", limit, headerAuthToken);
            List<File.FileDto> files = fileService.getItemList(limit, headerAuthToken);
            return ResponseEntity.ok(files);
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка получения списка файлов: неверные данные, limit: {}, token: {}", limit, headerAuthToken, e);
            return ResponseEntity.badRequest().body(ErrorResponse.of("Неверные входные данные", 400));
        } catch (SecurityException e) {
            logger.warn("Ошибка авторизации при получении списка файлов, token: {}", headerAuthToken, e);
            return ResponseEntity.status(401).body(ErrorResponse.of("Неавторизован", 401));
        } catch (Exception e) {
            logger.warn("Неизвестная ошибка при получении списка файлов, limit: {}, token: {}", limit, headerAuthToken, e);
            return ResponseEntity.status(500).body(ErrorResponse.of("Ошибка при получении списка файлов", 500));
        }
    }

    /**
     * Загрузка файла на сервер.
     * Принимает файл в формате multipart/form-data и сохраняет его на сервере.
     *
     * @param filename        Имя файла, передаваемое как параметр запроса.
     * @param file            Файл, загружаемый в формате multipart/form-data.
     * @param headerAuthToken Токен авторизации пользователя.
     * @return ResponseEntity с подтверждением успеха ("ok", 200) или ошибкой (400, 401, 500).
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveItem(
            @RequestParam("filename") String filename,
            @RequestPart("file") MultipartFile file,
            @RequestHeader("auth-token") String headerAuthToken) {
        try {
            logger.info("Получен запрос на загрузку файла: {}, размер: {}, токен: {}", filename, file.getSize(), headerAuthToken);
            fileService.saveItem(filename, file, headerAuthToken);
            return ResponseEntity.ok().body("ok");
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка загрузки файла: неверные данные, filename: {}, token: {}", filename, headerAuthToken, e);
            return ResponseEntity.badRequest().body(ErrorResponse.of("Ошибка входных данных", 400));
        } catch (SecurityException e) {
            logger.warn("Ошибка авторизации при загрузке файла, token: {}", headerAuthToken, e);
            return ResponseEntity.status(401).body(ErrorResponse.of("Неавторизован", 401));
        } catch (MaxUploadSizeExceededException e) {
            logger.warn("Превышен максимальный размер файла: {}, token: {}", filename, headerAuthToken, e);
            return ResponseEntity.badRequest().body(ErrorResponse.of("Превышен максимальный размер файла", 400));
        } catch (Exception e) {
            logger.warn("Неизвестная ошибка при загрузке файла: {}, token: {}", filename, headerAuthToken, e);
            return ResponseEntity.status(500).body(ErrorResponse.of("Ошибка при загрузке файла", 500));
        }
    }

    /**
     * Удаление файла с сервера.
     * Удаляет файл по указанному имени, если пользователь авторизован.
     *
     * @param filename        Имя файла для удаления, передаваемое как параметр запроса.
     * @param headerAuthToken Токен авторизации пользователя.
     * @return ResponseEntity с подтверждением успеха (200) или ошибкой (400, 401, 500).
     */
    @DeleteMapping("/file")
    public ResponseEntity<?> deleteItem(
            @RequestParam("filename") String filename,
            @RequestHeader("auth-token") String headerAuthToken) {
        try {
            logger.info("Получен запрос на удаление файла: {}, токен: {}", filename, headerAuthToken);
            fileService.deleteItem(filename, headerAuthToken);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка удаления файла: неверные данные, filename: {}, token: {}", filename, headerAuthToken, e);
            return ResponseEntity.badRequest().body(ErrorResponse.of("Ошибка входных данных", 400));
        } catch (SecurityException e) {
            logger.warn("Ошибка авторизации при удалении файла, token: {}", headerAuthToken, e);
            return ResponseEntity.status(401).body(ErrorResponse.of("Неавторизован", 401));
        } catch (Exception e) {
            logger.warn("Неизвестная ошибка при удалении файла: {}, token: {}", filename, headerAuthToken, e);
            return ResponseEntity.status(500).body(ErrorResponse.of("Ошибка при удалении файла", 500));
        }
    }

    /**
     * Обновление имени файла.
     * Изменяет имя существующего файла на новое, указанное в теле запроса.
     *
     * @param oldFilename     Старое имя файла, передаваемое как параметр запроса.
     * @param newFilename     Новое имя файла, передаваемое в теле запроса (JSON).
     * @param headerAuthToken Токен авторизации пользователя (может быть null, хотя в спецификации обязателен).
     * @return ResponseEntity с подтверждением успеха (200) или ошибкой (400, 401, 500).
     */
    @PutMapping("/file")
    public ResponseEntity<?> updateItem(
            @RequestParam("filename") String oldFilename,
            @RequestBody FileName newFilename,
            @RequestHeader("auth-token") Optional<String> headerAuthToken) {

        try {
            logger.info("Получен запрос на обновление файла: {} -> {}, токен: {}", oldFilename, newFilename.getName(), headerAuthToken);
            fileService.updateItem(oldFilename, newFilename.getName(), headerAuthToken);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка обновления файла: неверные данные, oldFilename: {}, newFilename: {}, token: {}",
                    oldFilename, newFilename.getName(), headerAuthToken, e);
            return ResponseEntity.badRequest().body(ErrorResponse.of("Ошибка входных данных", 400));
        } catch (SecurityException e) {
            logger.warn("Ошибка авторизации при обновлении файла, token: {}", headerAuthToken, e);
            return ResponseEntity.status(401).body(ErrorResponse.of("Неавторизован", 401));
        } catch (Exception e) {
            logger.warn("Неизвестная ошибка при обновлении файла: {} -> {}, token: {}",
                    oldFilename, newFilename.getName(), headerAuthToken, e);
            return ResponseEntity.status(500).body(ErrorResponse.of("Ошибка при обновлении файла", 500));
        }
    }

    /**
     * Скачивание файла с сервера.
     * Возвращает файл в формате application/octet-stream по указанному имени.
     *
     * @param filename Имя файла для скачивания.
     * @param headerAuthToken Токен авторизации пользователя.
     * @return ResponseEntity с данными файла (200) или ошибкой (400, 401, 500).
     */
    @GetMapping("/file")
    public ResponseEntity<?> downloadItem(
            @RequestParam("filename") String filename,
            @RequestHeader("auth-token") Optional<String> headerAuthToken) {
        try {
            logger.info("Получен запрос на скачивание файла: {}, токен: {}", filename, headerAuthToken);
            byte[] fileData = fileService.downloadItem(filename, headerAuthToken.orElseThrow(() -> new IllegalArgumentException("Токен отсутствует")));
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // Используем octet-stream для бинарных данных
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(fileData);
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка скачивания файла: неверные данные, filename: {}, token: {}", filename, headerAuthToken, e);
            return ResponseEntity.badRequest().body(ErrorResponse.of("Ошибка входных данных", 400));
        } catch (SecurityException e) {
            logger.warn("Ошибка авторизации при скачивании файла, token: {}", headerAuthToken, e);
            return ResponseEntity.status(401).body(ErrorResponse.of("Неавторизован", 401));
        } catch (Exception e) {
            logger.warn("Неизвестная ошибка при скачивании файла: {}, token: {}", filename, headerAuthToken, e);
            return ResponseEntity.status(500).body(ErrorResponse.of("Ошибка при скачивании файла", 500));
        }
    }
}
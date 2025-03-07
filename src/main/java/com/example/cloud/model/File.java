package com.example.cloud.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Модель файла для хранения в базе данных.
 */

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "files")
public class File {
    /**
     * Уникальный идентификатор файла в базе данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    /**
     * Хэш файла.
     */
    @Column(name = "hash", nullable = false)
    private String hash;

    /**
     * Владелец файла.
     */
    @Column(name = "owner", nullable = false)
    private String owner;

    /**
     * Бинарные данные файла.
     */
    @Column(name = "data", nullable = false)
    private byte[] data;

    @Embedded
    private FileDto file;

    /**
     * Вложенная информация о файле (имя и размер).
     * Используется для ответа на запрос `/list`.
     */
    @Embeddable
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class FileDto {
        /**
         * Имя файла.
         * Обязательное поле в схеме OpenAPI для `/list`.
         */
        @Column(name = "filename", nullable = false)
        private String filename;

        /**
         * Размер файла в байтах.
         * Обязательное поле в схеме OpenAPI для `/list`.
         */
        @Column(name = "size", nullable = false)
        private long size;
    }
}
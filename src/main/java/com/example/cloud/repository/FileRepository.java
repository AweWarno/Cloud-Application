package com.example.cloud.repository;

import com.example.cloud.model.File;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с таблицей файлов в базе данных.
 * Предоставляет методы для поиска и удаления файлов.
 */
@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    /**
     * Поиск файла по владельцу и имени файла.
     *
     * @param owner Владелец файла (имя пользователя).
     * @param filename Имя файла.
     * @return Optional с найденным файлом или пустой, если файл не найден.
     */
    Optional<File> findItemByOwnerAndFileFilename(String owner, String filename);

    /**
     * Получение списка файлов владельца, отсортированных по возрастанию размера.
     *
     * @param owner Владелец файлов (имя пользователя).
     * @param limitOf Лимит количества возвращаемых записей.
     * @return Список файлов, соответствующих критериям.
     */
    List<File> findItemByOwnerOrderByFileSizeAsc(String owner, Limit limitOf);
}
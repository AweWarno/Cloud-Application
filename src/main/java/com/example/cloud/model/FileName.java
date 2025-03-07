package com.example.cloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Модель для передачи нового имени файла в запросе на обновление.
 * Соответствует схеме OpenAPI для `PUT /file` с полем `name`.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FileName {

    /**
     * Новое имя файла.
     */
    //@Size(min = 1, max= 100, message = "Имя файла не может быть пустым и быть большим 100 знаков")
    @NotNull
    @JsonProperty("filename")
    private String name;
}
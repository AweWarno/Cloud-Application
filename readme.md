# Cloud Application

Cloud Application — это RESTful веб-приложение, разработанное на Spring Boot, для управления файлами в облаке. 
Оно предоставляет пользователям возможность авторизации, загрузки, скачивания, обновления и удаления файлов, а также получения списка загруженных файлов. 
Приложение использует PostgreSQL в качестве базы данных и Liquibase для управления миграциями. 
Фронтенд реализован на JavaScript с использованием Vue.js и взаимодействует с бэкендом через REST API.

## Основные возможности

- **Аутентификация**: Вход и выход пользователей с использованием токенов авторизации.
- **Управление файлами**:
    - Загрузка файлов (до 100 МБ).
    - Получение списка файлов с лимитом.
    - Удаление файлов.
    - Обновление имени файла.
    - Скачивание файлов.
- **Безопасность**: Токен авторизации требуется для всех операций с файлами.
- **Логирование**: Подробное логирование операций и ошибок через SLF4J.
- **Тестирование**: Юнит-тесты и интеграционные тесты с использованием Testcontainers.

## Технологии

### Бэкенд
- **Язык**: Java 17
- **Фреймворк**: Spring Boot
- **База данных**: PostgreSQL
- **ORM**: Hibernate (Spring Data JPA)
- **Миграции**: Liquibase
- **Тестирование**: JUnit 5, Mockito, Testcontainers
- **Сборка**: Maven
- **Контейнеризация**: Docker, Docker Compose

### Фронтенд
- **Язык**: JavaScript
- **Фреймворк**: Vue.js
- **Сборка**: Node.js (версия 19.7.0 или выше), npm

## Структура проекта

```
src/
├── main/
│   ├── java/com/example/cloud/
│   │   ├── config/         # Конфигурация приложения (CORS)
│   │   ├── controller/     # REST-контроллеры (аутентификация и файлы)
│   │   ├── error/          # Обработка ошибок (ErrorResponse)
│   │   ├── model/          # Модели данных (User, File)
│   │   ├── repository/     # Репозитории JPA
│   │   ├── service/        # Бизнес-логика (AuthService, FileService)
│   │   └── CloudApplication.java  # Точка входа
│   └── resources/
│       ├── db/changelog/   # Миграции Liquibase
│       └── application.yml # Конфигурация Spring Boot
└── test/                   # Тесты (юнит и интеграционные)
```

Фронтенд находится в отдельном репозитории или папке `frontend`, структура которой описана в его `README.md`.

## Установка и запуск

### Требования

- **Бэкенд**:
    - Java 17
    - Maven 3.6+
    - PostgreSQL 15+
    - Docker (опционально, для контейнеризации)
- **Фронтенд**:
    - Node.js (версия 19.7.0 или выше)
    - npm

### Локальный запуск бэкенда

1. **Клонируйте репозиторий**:
   ```bash
   git clone https://github.com/AweWarno/Cloud-Application.git
   cd cloud-application
   ```

2. **Настройте PostgreSQL**:
    - Создайте базу данных `postgres`.
    - Убедитесь, что пользователь и пароль соответствуют настройкам в `application.yml` (по умолчанию: `user`/`user`).

3. **Соберите проект**:
   ```bash
   mvn clean install
   ```

4. **Запустите приложение**:
   ```bash
   java -jar target/cloud-api-0.0.1-SNAPSHOT.jar
   ```
   Бэкенд будет доступен на `http://localhost:8081`.

### Запуск бэкенда через Docker Compose

1. **Соберите и запустите контейнеры**:
   ```bash
   docker-compose up --build
   ```
    - PostgreSQL будет запущен на порту `5432`.
    - Бэкенд будет доступен на `http://localhost:8081`.

2. **Остановите контейнеры**:
   ```bash
   docker-compose down
   ```

### Установка и запуск фронтенда

1. **Установите Node.js**:
    - Установите Node.js версии не ниже 19.7.0, следуя [официальной инструкции](https://nodejs.org/en/download/).

2. **Скачайте фронтенд**:
    - Клонируйте или скачайте репозиторий фронтенда (например, `https://github.com/AweWarno/Cloud-Application.git`).

3. **Перейдите в папку фронтенда**:
   ```bash
   cd frontend
   ```

4. **Установите зависимости**:
   ```bash
   npm install
   ```

5. **Настройте URL бэкенда**:
    - Откройте файл `.env` в корне проекта фронтенда.
    - Измените переменную `VUE_APP_BASE_URL`, указав корневой URL вашего бэкенда, например:
      ```
      VUE_APP_BASE_URL=http://localhost:8081
      ```
    - Frontend будет добавлять пути (например, `/login`) к этому URL.

6. **Запустите фронтенд**:
   ```bash
   npm run serve
   ```
    - По умолчанию фронтенд запускается на `http://localhost:8080`.
    - Если порт 8080 занят, он автоматически выберет следующий доступный (например, 8081). Порт будет указан в терминале после запуска.

7. **Проверка**:
    - Откройте браузер и перейдите по адресу, указанному в терминале (например, `http://localhost:8080`).

### Примечания
- Изменения в `.env` сохраняются для последующих запусков.
- Если бэкенд и фронтенд работают на разных портах, убедитесь, что CORS настроен корректно (по умолчанию в `WebConfig` разрешён `http://localhost:8080`).

## Использование

### Аутентификация

- **Вход (POST /cloud/login)**:
  ```bash
  curl -X POST http://localhost:8081/cloud/login \
  -H "Content-Type: application/json" \
  -d '{"login": "user", "password": "user"}'
  ```
  Ответ: `{"auth-token": "<токен>"}`

- **Выход (POST /cloud/logout)**:
  ```bash
  curl -X POST http://localhost:8081/cloud/logout \
  -H "auth-token: <токен>"
  ```

### Операции с файлами

- **Загрузка файла (POST /cloud/file)**:
  ```bash
  curl -X POST http://localhost:8081/cloud/file \
  -H "auth-token: <токен>" \
  -F "filename=test.txt" \
  -F "file=@/path/to/test.txt"
  ```
  Ответ: `"ok"`

- **Получение списка файлов (GET /cloud/list)**:
  ```bash
  curl http://localhost:8081/cloud/list?limit=10 \
  -H "auth-token: <токен>"
  ```
  Ответ: `[{"filename": "test.txt", "size": 123}]`

- **Удаление файла (DELETE /cloud/file)**:
  ```bash
  curl -X DELETE http://localhost:8081/cloud/file?filename=test.txt \
  -H "auth-token: <токен>"
  ```

- **Обновление имени файла (PUT /cloud/file)**:
  ```bash
  curl -X PUT http://localhost:8081/cloud/file?filename=test.txt \
  -H "auth-token: <токен>" \
  -H "Content-Type: application/json" \
  -d '{"filename": "newname.txt"}'
  ```

- **Скачивание файла (GET /cloud/file)**:
  ```bash
  curl http://localhost:8081/cloud/file?filename=test.txt \
  -H "auth-token: <токен>" \
  -o downloaded.txt
  ```

## Тестирование

- **Юнит-тесты**:
  Выполните:
  ```bash
  mvn test
  ```
  Тестируются сервисы `AuthServiceImp` и `FileServiceImp`.

- **Интеграционные тесты**:
  Используют Testcontainers для запуска PostgreSQL в контейнере:
  ```bash
  mvn verify
  ```
  Проверяют полный жизненный цикл операций с файлами.

## Конфигурация

- Файл `application.yml` содержит настройки:
    - Подключение к базе данных.
    - Предел размера загружаемых файлов (по умолчанию 100 МБ).
    - Уровень логирования.

- Миграции базы данных находятся в `src/main/resources/db/changelog/`.

## Логирование

- Уровень `DEBUG` для пакета `com.example.cloud` позволяет отслеживать все операции.
- Ошибки логируются с указанием контекста (например, токен, имя файла).

## Ограничения и допущения

- CORS настроен для `http://localhost:8080`.
- Пароли хранятся в открытом виде (рекомендуется добавить шифрование в продакшене).
- Токены авторизации генерируются случайным образом с использованием `SecureRandom`.
package com.example.cloud;

import com.example.cloud.model.User;
import com.example.cloud.repository.FileRepository;
import com.example.cloud.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тестовый класс для проверки работы приложения CloudApplication.
 * Тестируются основные операции: вход, выход, загрузка файлов, получение списка, удаление, скачивание и обновление.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CloudApplicationIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @BeforeEach
    void setUp() {
        fileRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Успешные сценарии

    /**
     * Тестирует успешный вход пользователя.
     * Входные данные: существующий пользователь с корректными логином и паролем.
     * Ожидаемый результат: возвращается статус OK и токен авторизации.
     */
    @Test
    void testLogin() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        userRepository.save(user);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String loginBody = "{\"login\":\"testuser\",\"password\":\"password\"}";
        HttpEntity<String> loginRequest = new HttpEntity<>(loginBody, headers);

        // Действие
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/cloud/login", loginRequest, Map.class);

        // Проверка
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode(), "Статус должен быть OK");
        String token = (String) loginResponse.getBody().get("auth-token");
        assertNotNull(token, "Токен не должен быть null");
        assertFalse(token.isEmpty(), "Токен не должен быть пустым");
    }

    /**
     * Тестирует успешный выход пользователя.
     * Входные данные: пользователь с валидным токеном.
     * Ожидаемый результат: статус OK, токен сброшен в базе данных.
     */
    @Test
    void testLogout() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("initial-token").build())
                .build();
        userRepository.save(user);

        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", "initial-token");
        HttpEntity<String> logoutRequest = new HttpEntity<>(headers);

        // Действие
        ResponseEntity<Void> logoutResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/cloud/logout", logoutRequest, Void.class);

        // Проверка
        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode(), "Статус должен быть OK");
        User updatedUser = userRepository.findUserByCredentialsLoginIgnoreCase("testuser").orElseThrow();
        assertEquals("", updatedUser.getToken().getAuthToken(), "Токен должен быть сброшен");
    }

    /**
     * Тестирует загрузку файла и получение списка файлов.
     * Входные данные: пользователь с валидным токеном, файл для загрузки.
     * Ожидаемый результат: файл загружен, список содержит загруженный файл.
     */
    @Test
    void testFileUploadAndGetList() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        userRepository.save(user);
        String token = loginAndGetToken("testuser", "password");
        uploadFile(token, "test.txt", "Test file content");

        HttpHeaders listHeaders = new HttpHeaders();
        listHeaders.set("auth-token", token);
        HttpEntity<Void> listRequest = new HttpEntity<>(listHeaders);

        // Действие
        ResponseEntity<List<Map<String, Object>>> listResponse = restTemplate.exchange(
                "http://localhost:" + port + "/cloud/list?limit=10", HttpMethod.GET, listRequest,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        // Проверка
        assertEquals(HttpStatus.OK, listResponse.getStatusCode(), "Статус должен быть OK");
        List<Map<String, Object>> files = listResponse.getBody();
        assertEquals(1, files.size(), "Список должен содержать один файл");
        assertEquals("test.txt", files.get(0).get("filename"), "Имя файла должно совпадать");
    }

    /**
     * Тестирует удаление файла.
     * Входные данные: пользователь с валидным токеном, загруженный файл.
     * Ожидаемый результат: файл удалён, статус OK.
     */
    @Test
    void testDeleteFile() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        userRepository.save(user);
        String token = loginAndGetToken("testuser", "password");
        uploadFile(token, "test.txt", "Test file content");

        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", token);
        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);

        // Действие
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "http://localhost:" + port + "/cloud/file?filename=test.txt", HttpMethod.DELETE, deleteRequest, Void.class);

        // Проверка
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode(), "Статус должен быть OK");
        assertFalse(fileRepository.findItemByOwnerAndFileFilename("testuser", "test.txt").isPresent(),
                "Файл должен быть удалён из базы");
    }

    /**
     * Тестирует скачивание файла.
     * Входные данные: пользователь с валидным токеном, загруженный файл.
     * Ожидаемый результат: данные файла возвращены, статус OK.
     */
    @Test
    void testDownloadFile() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        userRepository.save(user);
        String token = loginAndGetToken("testuser", "password");
        uploadFile(token, "test.txt", "Test file content");

        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", token);
        HttpEntity<Void> downloadRequest = new HttpEntity<>(headers);

        // Действие
        ResponseEntity<byte[]> downloadResponse = restTemplate.exchange(
                "http://localhost:" + port + "/cloud/file?filename=test.txt", HttpMethod.GET, downloadRequest, byte[].class);

        // Проверка
        assertEquals(HttpStatus.OK, downloadResponse.getStatusCode(), "Статус должен быть OK");
        assertArrayEquals("Test file content".getBytes(), downloadResponse.getBody(), "Содержимое файла должно совпадать");
    }

    // Ошибочные сценарии

    /**
     * Тестирует вход с неверными учетными данными.
     * Входные данные: существующий логин, неверный пароль.
     * Ожидаемый результат: статус BAD_REQUEST, сообщение "Неверные учетные данные".
     */
    @Test
    void testLoginWithInvalidCredentials() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        userRepository.save(user);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String loginBody = "{\"login\":\"testuser\",\"password\":\"wrongpassword\"}";
        HttpEntity<String> loginRequest = new HttpEntity<>(loginBody, headers);

        // Действие
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/cloud/login", loginRequest, Map.class);

        // Проверка
        assertEquals(HttpStatus.BAD_REQUEST, loginResponse.getStatusCode(), "Статус должен быть BAD_REQUEST");
        assertEquals("Неверные учетные данные", loginResponse.getBody().get("message"), "Сообщение об ошибке должно быть точным");
    }

    /**
     * Тестирует выход с невалидным токеном.
     * Входные данные: невалидный токен.
     * Ожидаемый результат: статус BAD_REQUEST, сообщение "Неверный токен авторизации".
     */
    @Test
    void testLogoutWithInvalidToken() {
        // Подготовка
        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", "invalid-token");
        HttpEntity<String> logoutRequest = new HttpEntity<>(headers);

        // Действие
        ResponseEntity<Map> logoutResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/cloud/logout", logoutRequest, Map.class);

        // Проверка
        assertEquals(HttpStatus.BAD_REQUEST, logoutResponse.getStatusCode(), "Статус должен быть BAD_REQUEST");
        assertEquals("Неверный токен авторизации", logoutResponse.getBody().get("message"),
                "Сообщение об ошибке должно быть точным");
    }

    /**
     * Тестирует получение списка файлов с невалидным токеном.
     * Входные данные: невалидный токен.
     * Ожидаемый результат: статус UNAUTHORIZED, сообщение "Неавторизован".
     */
    @Test
    void testGetListWithInvalidToken() {
        // Подготовка
        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", "invalid-token");
        HttpEntity<Void> listRequest = new HttpEntity<>(headers);

        // Действие
        ResponseEntity<Map> listResponse = restTemplate.exchange(
                "http://localhost:" + port + "/cloud/list?limit=10", HttpMethod.GET, listRequest, Map.class);

        // Проверка
        assertEquals(HttpStatus.UNAUTHORIZED, listResponse.getStatusCode(), "Статус должен быть UNAUTHORIZED");
        assertEquals("Неавторизован", listResponse.getBody().get("message"), "Сообщение об ошибке должно быть точным");
    }

    /**
     * Тестирует загрузку пустого файла.
     * Входные данные: валидный токен, пустой файл.
     * Ожидаемый результат: статус BAD_REQUEST, сообщение "Ошибка входных данных".
     */
    @Test
    void testUploadEmptyFile() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        userRepository.save(user);
        String token = loginAndGetToken("testuser", "password");

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.set("auth-token", token);
        fileHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("filename", "empty.txt");
        body.add("file", new ByteArrayResource("".getBytes()) {
            @Override
            public String getFilename() {
                return "empty.txt";
            }
        });
        HttpEntity<MultiValueMap<String, Object>> fileRequest = new HttpEntity<>(body, fileHeaders);

        // Действие
        ResponseEntity<Map> fileResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/cloud/file", fileRequest, Map.class);

        // Проверка
        assertEquals(HttpStatus.BAD_REQUEST, fileResponse.getStatusCode(), "Статус должен быть BAD_REQUEST");
        assertEquals("Ошибка входных данных", fileResponse.getBody().get("message"), "Сообщение об ошибке должно быть точным");
    }

    /**
     * Тестирует удаление несуществующего файла.
     * Входные данные: валидный токен, имя несуществующего файла.
     * Ожидаемый результат: статус BAD_REQUEST, сообщение "Ошибка входных данных".
     */
    @Test
    void testDeleteNonExistentFile() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        userRepository.save(user);
        String token = loginAndGetToken("testuser", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", token);
        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);

        // Действие
        ResponseEntity<Map> deleteResponse = restTemplate.exchange(
                "http://localhost:" + port + "/cloud/file?filename=nonexistent.txt", HttpMethod.DELETE, deleteRequest, Map.class);

        // Проверка
        assertEquals(HttpStatus.BAD_REQUEST, deleteResponse.getStatusCode(), "Статус должен быть BAD_REQUEST");
        assertEquals("Ошибка входных данных", deleteResponse.getBody().get("message"), "Сообщение об ошибке должно быть точным");
    }

    /**
     * Тестирует обновление несуществующего файла.
     * Входные данные: валидный токен, имя несуществующего файла.
     * Ожидаемый результат: статус BAD_REQUEST, сообщение "Ошибка входных данных".
     */
    @Test
    void testUpdateNonExistentFile() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        userRepository.save(user);
        String token = loginAndGetToken("testuser", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String updateBody = "{\"name\":\"new.txt\"}";
        HttpEntity<String> updateRequest = new HttpEntity<>(updateBody, headers);

        // Действие
        ResponseEntity<Map> updateResponse = restTemplate.exchange(
                "http://localhost:" + port + "/cloud/file?filename=nonexistent.txt", HttpMethod.PUT, updateRequest, Map.class);

        // Проверка
        assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatusCode(), "Статус должен быть BAD_REQUEST");
        assertEquals("Ошибка входных данных", updateResponse.getBody().get("message"), "Сообщение об ошибке должно быть точным");
    }

    /**
     * Тестирует скачивание несуществующего файла.
     * Входные данные: валидный токен, имя несуществующего файла.
     * Ожидаемый результат: статус BAD_REQUEST, сообщение "Ошибка входных данных".
     */
    @Test
    void testDownloadNonExistentFile() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        userRepository.save(user);
        String token = loginAndGetToken("testuser", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", token);
        HttpEntity<Void> downloadRequest = new HttpEntity<>(headers);

        // Действие
        ResponseEntity<Map> downloadResponse = restTemplate.exchange(
                "http://localhost:" + port + "/cloud/file?filename=nonexistent.txt", HttpMethod.GET, downloadRequest, Map.class);

        // Проверка
        assertEquals(HttpStatus.BAD_REQUEST, downloadResponse.getStatusCode(), "Статус должен быть BAD_REQUEST");
        assertEquals("Ошибка входных данных", downloadResponse.getBody().get("message"), "Сообщение об ошибке должно быть точным");
    }

    // Крайние случаи

    /**
     * Тестирует загрузку большого файла (10MB).
     * Входные данные: валидный токен, файл размером 10MB.
     * Ожидаемый результат: файл успешно загружен, статус OK.
     */
    @Test
    void testUploadLargeFile() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        userRepository.save(user);
        String token = loginAndGetToken("testuser", "password");

        byte[] largeContent = new byte[10 * 1024 * 1024]; // 10MB
        java.util.Arrays.fill(largeContent, (byte) 'A');

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.set("auth-token", token);
        fileHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("filename", "large.txt");
        body.add("file", new ByteArrayResource(largeContent) {
            @Override
            public String getFilename() {
                return "large.txt";
            }
        });
        HttpEntity<MultiValueMap<String, Object>> fileRequest = new HttpEntity<>(body, fileHeaders);

        // Действие
        ResponseEntity<String> fileResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/cloud/file", fileRequest, String.class);

        // Проверка
        assertEquals(HttpStatus.OK, fileResponse.getStatusCode(), "Статус должен быть OK");
        assertEquals("ok", fileResponse.getBody(), "Ответ должен быть 'ok'");
    }

    /**
     * Тестирует получение списка файлов с нулевым лимитом.
     * Входные данные: валидный токен, загруженный файл, limit=0.
     * Ожидаемый результат: список содержит все файлы, статус OK.
     */
    @Test
    void testGetListWithZeroLimit() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        userRepository.save(user);
        String token = loginAndGetToken("testuser", "password");
        uploadFile(token, "test.txt", "Test file content");

        HttpHeaders listHeaders = new HttpHeaders();
        listHeaders.set("auth-token", token);
        HttpEntity<Void> listRequest = new HttpEntity<>(listHeaders);

        // Действие
        ResponseEntity<List<Map<String, Object>>> listResponse = restTemplate.exchange(
                "http://localhost:" + port + "/cloud/list?limit=0", HttpMethod.GET, listRequest,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        // Проверка
        assertEquals(HttpStatus.OK, listResponse.getStatusCode(), "Статус должен быть OK");
        List<Map<String, Object>> files = listResponse.getBody();
        assertEquals(1, files.size(), "Список должен содержать один файл");
        assertEquals("test.txt", files.get(0).get("filename"), "Имя файла должно совпадать");
    }

    /**
     * Тестирует получение списка файлов без указания лимита.
     * Входные данные: валидный токен, загруженный файл.
     * Ожидаемый результат: список содержит все файлы, статус OK.
     */
    @Test
    void testGetListWithoutLimit() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        userRepository.save(user);
        String token = loginAndGetToken("testuser", "password");
        uploadFile(token, "test.txt", "Test file content");

        HttpHeaders listHeaders = new HttpHeaders();
        listHeaders.set("auth-token", token);
        HttpEntity<Void> listRequest = new HttpEntity<>(listHeaders);

        // Действие
        ResponseEntity<List<Map<String, Object>>> listResponse = restTemplate.exchange(
                "http://localhost:" + port + "/cloud/list", HttpMethod.GET, listRequest,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        // Проверка
        assertEquals(HttpStatus.OK, listResponse.getStatusCode(), "Статус должен быть OK");
        List<Map<String, Object>> files = listResponse.getBody();
        assertEquals(1, files.size(), "Список должен содержать один файл");
        assertEquals("test.txt", files.get(0).get("filename"), "Имя файла должно совпадать");
    }

    // Многократные операции

    /**
     * Тестирует загрузку нескольких файлов и получение списка.
     * Входные данные: валидный токен, три файла для загрузки.
     * Ожидаемый результат: список содержит все три файла, статус OK.
     */
    @Test
    void testMultipleFileUploadsAndList() {
        // Подготовка
        User user = User.builder()
                .credentials(User.Credentials.builder().login("testuser").password("password").build())
                .token(User.Token.builder().authToken("").build())
                .build();
        userRepository.save(user);
        String token = loginAndGetToken("testuser", "password");

        uploadFile(token, "file1.txt", "Content 1");
        uploadFile(token, "file2.txt", "Content 2");
        uploadFile(token, "file3.txt", "Content 3");

        HttpHeaders listHeaders = new HttpHeaders();
        listHeaders.set("auth-token", token);
        HttpEntity<Void> listRequest = new HttpEntity<>(listHeaders);

        // Действие
        ResponseEntity<List<Map<String, Object>>> listResponse = restTemplate.exchange(
                "http://localhost:" + port + "/cloud/list?limit=10", HttpMethod.GET, listRequest,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        // Проверка
        assertEquals(HttpStatus.OK, listResponse.getStatusCode(), "Статус должен быть OK");
        List<Map<String, Object>> files = listResponse.getBody();
        assertEquals(3, files.size(), "Список должен содержать три файла");
        assertTrue(files.stream().anyMatch(f -> "file1.txt".equals(f.get("filename"))), "file1.txt должен быть в списке");
        assertTrue(files.stream().anyMatch(f -> "file2.txt".equals(f.get("filename"))), "file2.txt должен быть в списке");
        assertTrue(files.stream().anyMatch(f -> "file3.txt".equals(f.get("filename"))), "file3.txt должен быть в списке");
    }

    // Вспомогательные методы

    /**
     * Выполняет вход и возвращает токен авторизации.
     * @param login логин пользователя
     * @param password пароль пользователя
     * @return токен авторизации
     */
    private String loginAndGetToken(String login, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String loginBody = String.format("{\"login\":\"%s\",\"password\":\"%s\"}", login, password);
        HttpEntity<String> loginRequest = new HttpEntity<>(loginBody, headers);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/cloud/login", loginRequest, Map.class);
        return (String) loginResponse.getBody().get("auth-token");
    }

    /**
     * Загружает файл на сервер.
     * @param token токен авторизации
     * @param filename имя файла
     * @param content содержимое файла
     */
    private void uploadFile(String token, String filename, String content) {
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.set("auth-token", token);
        fileHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("filename", filename);
        body.add("file", new ByteArrayResource(content.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        HttpEntity<MultiValueMap<String, Object>> fileRequest = new HttpEntity<>(body, fileHeaders);
        restTemplate.postForEntity("http://localhost:" + port + "/cloud/file", fileRequest, String.class);
    }
}
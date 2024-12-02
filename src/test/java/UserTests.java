import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserTests {

    private String email; // будет рандомным
    private String password = "testpassword";
    private String name = "Test User";
    private String accessToken;

    @Before
    public void setup() {
        // Устанавливаем базовый URL для всех запросов
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
    }

    @After
    public void tearDown() {
        // Удаляем пользователя
        if (accessToken != null) {
            deleteUser(accessToken);
            accessToken = null; // Сбрасываем токен
        }
    }

    @Test
    @DisplayName("Create unique user")
    public void createUniqueUser() {
        //Проверяем создание пользователя
        email = generateUniqueEmail(); // Генерируем email
        createUser(email, password, name)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));
    }

    @Test
    @DisplayName("Cannot create two identical users")
    public void createDuplicateUser() {
        //Проверяем создание дубликата пользователя
        email = "duplicate@example.com"; // Фиксированный email

        // Создаем первого пользователя
        createUser(email, password, name);
        // Пытаемся создать того же пользователя повторно
        createUser(email, password, name)
                .then()
                .statusCode(403) // Ошибка "пользователь уже существует"
                .body("message", equalTo("User already exists"));
    }

    @Test
    @DisplayName("Create user with missing Email")
    public void createUserWithMissingEmail() {
        email = null; // Пропускаем поле email
        createUser(email, password, name)
                .then()
                .statusCode(403)
                .body("message", equalTo("Email, password and name are required fields"));
    }

    @Test
    @DisplayName("Create user with missing password")
    public void createUserWithMissingPassword() {
        email = generateUniqueEmail();
        createUser(email, null, name) // Пропускаем поле password
                .then()
                .statusCode(403)
                .body("message", equalTo("Email, password and name are required fields"));
    }

    @Test
    @DisplayName("Create user with missing name")
    public void createUserWithMissingName() {
        email = generateUniqueEmail();
        createUser(email, password, null) // Пропускаем поле name
                .then()
                .statusCode(403)
                .body("message", equalTo("Email, password and name are required fields"));
    }

    // Генерация уникального email
    @Step("Generate unique Email")
    private String generateUniqueEmail() {
        return "user" + System.currentTimeMillis() + "@example.com";
    }

    // Создаем пользователя
    @Step("Create user with email: {email}, password: {password}, name: {name}")
    private Response createUser(String email, String password, String name) {
        return given()
                .header("Content-type", "application/json")
                .body(new User(email, password, name)) // Используем объект для формирования тела запроса
                .when()
                .post("/api/auth/register");
    }

    // Удаляем пользователя
    @Step("Delete user")
    private void deleteUser(String accessToken) {
        given()
                .header("Authorization", accessToken)
                .when()
                .delete("/api/auth/user")
                .then()
                .statusCode(200);
    }

    // Класс для тела запроса создания пользователя
    private static class User {
        @JsonProperty("email")
        private final String email;

        @JsonProperty("password")
        private final String password;

        @JsonProperty("name")
        private final String name;

        public User(String email, String password, String name) {
            this.email = email;
            this.password = password;
            this.name = name;
        }
    }
}


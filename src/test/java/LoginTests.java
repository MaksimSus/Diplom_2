import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.qameta.allure.junit4.DisplayName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class LoginTests {

    private String email; // будет рандомный
    private String password = "validpassword";
    private String invalidEmail = "invalid@example.com";
    private String invalidPassword = "invalidpassword";
    private String accessToken;

    @Before
    public void setup() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
        email = generateUniqueEmail(); // Генерируем рандомный email
        createUser(email, password, "Valid User"); // Создаем пользователя
    }

    @After
    public void tearDown() {
        // Удаляем пользователя
        if (accessToken != null) {
            deleteUser(accessToken);
            accessToken = null;
        }
    }

    @Test
    @DisplayName("Login with valid credentials")
    public void loginWithValidCredentials() {
        //Проверяет успешный логин с корректными данными.
        accessToken = loginUser(email, password)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(email))
                .extract()
                .path("accessToken");
    }

    @Test
    @DisplayName("Login with invalid email")
    public void loginWithInvalidEmail() {
        //Проверяет ошибку логина при неверном email.
        loginUser(invalidEmail, password)
                .then()
                .statusCode(401)
                .body("message", equalTo("email or password are incorrect"));
    }

    @Test
    @DisplayName("Login with invalid password")
    public void loginWithInvalidPassword() {
        // Проверяет ошибку логина при неверном пароле.
        loginUser(email, invalidPassword)
                .then()
                .statusCode(401)
                .body("message", equalTo("email or password are incorrect"));
    }

    @Step("Login user with email: {email} and password: {password}")
    private Response loginUser(String email, String password) {
        // Выполняем авторизацию пользователя
        return given()
                .header("Content-type", "application/json")
                .body(new LoginRequest(email, password))
                .when()
                .post("/api/auth/login");
    }

    @Step("Create user with email: {email}")
    private void createUser(String email, String password, String name) {
        // Создаем пользователя
        Response response = given()
                .header("Content-type", "application/json")
                .body(new User(email, password, name))
                .when()
                .post("/api/auth/register");

        if (response.getStatusCode() != 200) {
            throw new RuntimeException("Failed to create user: " + response.getBody().asString());
        }
    }

    @Step("Delete user with token")
    private void deleteUser(String accessToken) {
        // Удаляем пользователя
        given()
                .header("Authorization", accessToken)
                .when()
                .delete("/api/auth/user")
                .then()
                .statusCode(202);
    }

    private String generateUniqueEmail() {
        // Генерируем уникальную почту
        return "user" + System.currentTimeMillis() + "@example.com";
    }

    private static class User {
        // Класс создания пользователя
        private final String email;
        private final String password;
        private final String name;

        public User(String email, String password, String name) {
            this.email = email;
            this.password = password;
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public String getPassword() {
            return password;
        }

        public String getName() {
            return name;
        }
    }

    // Класс для тела запроса логина
    private static class LoginRequest {
        private final String email;
        private final String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public String getPassword() {
            return password;
        }
    }
}

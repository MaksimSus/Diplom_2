import com.fasterxml.jackson.annotation.JsonProperty;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.qameta.allure.junit4.DisplayName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class UpdateUserTests {

    private String email; // Будет рандомным
    private String password = "validpassword";
    private String name = "Valid User";
    private String accessToken;

    @Before
    public void setup() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
        email = generateUniqueEmail(); // Генерация email
        createUser(email, password, name); // Создаем пользователя
    }

    @After
    public void tearDown() {
        // Очистка данных
        if (accessToken != null) {
            deleteUser(accessToken);
            accessToken = null;
        }
    }

    @Test
    @DisplayName("Update user name with authorization")
    public void updateUserNameWithAuthorization() {
        // Изменение имени авторизованным
        String newName = "Updated User";
        updateUser(accessToken, newName, email, password)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.name", equalTo(newName));
    }

    @Test
    @DisplayName("Update user email with authorization")
    public void updateUserEmailWithAuthorization() {
        // Изменение email авторизованным
        String newEmail = generateUniqueEmail();
        updateUser(accessToken, name, newEmail, password)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(newEmail));
    }

    @Test
    @DisplayName("Update user password with authorization")
    public void updateUserPasswordWithAuthorization() {
        // Изменение пароля авторизованным
        String newPassword = "newpassword";
        updateUser(accessToken, name, email, newPassword)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));
    }

    @Test
    @DisplayName("Fail to update name without authorization")
    public void failToUpdateUserNameWithoutAuthorization() {
        // Попытка изменить имя без токена
        String newName = "Unauthorized User";
        updateUser("null", newName, email, password)
                .then()
                .statusCode(401)
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    @DisplayName("Fail to update email without authorization")
    public void failToUpdateUserEmailWithoutAuthorization() {
        // Попытка изменить email без токена
        String newEmail = generateUniqueEmail();
        updateUser("null", name, newEmail, password)
                .then()
                .statusCode(401)
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    @DisplayName("Fail to update password without authorization")
    public void failToUpdateUserPasswordWithoutAuthorization() {
        // Попытка изменить пароль без токена
        String newPassword = "unauthorizedpassword";
        updateUser("null", name, email, newPassword)
                .then()
                .statusCode(401)
                .body("message", equalTo("You should be authorised"));
    }

    @Step("Create user with email: {email}")
    private void createUser(String email, String password, String name) {
        // Создаем пользователя
        Response response = given()
                .header("Content-type", "application/json")
                .body(new User(email, password, name))
                .when()
                .post("/api/auth/register");

        if (response.getStatusCode() == 200) {
            accessToken = response.jsonPath().getString("accessToken");
        } else {
            throw new RuntimeException("Failed to create user: " + response.getBody().asString());
        }
    }

    @Step("Update user data")
    private Response updateUser(String accessToken, String name, String email, String password) {
        // Обновляем данные пользователя
        return given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken)
                .body(new User(email, password, name))
                .when()
                .patch("/api/auth/user");
    }

    @Step("Delete user with token")
    private void deleteUser(String accessToken) {
        // Удаляем пользователя
        given()
                .header("Authorization", accessToken)
                .when()
                .delete("/api/auth/user")
                .then()
                .statusCode(202); // Код успешного удаления
    }

    private String generateUniqueEmail() {
        // Генерация email
        return "user" + System.currentTimeMillis() + "@example.com";
    }

    private static class User {
        // Класс создания пользователя
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

    private static class LoginRequest {
        // Класс логина
        @JsonProperty("email")
        private final String email;

        @JsonProperty("password")
        private final String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }
}


import com.fasterxml.jackson.annotation.JsonProperty;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.qameta.allure.junit4.DisplayName;

import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class CreateOrderTests {

    private String email; // Будет рандомным
    private String password = "validpassword";
    private String name = "Valid User";
    private String accessToken;
    private String validIngredient = "61c0c5a71d1f82001bdaaa6d";
    private String invalidIngredient = "invalidIngredientHash";

    @Before
    public void setup() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
        email = generateUniqueEmail(); // Генерируем email
        createUser(email, password, name); // Создаем пользователя
        accessToken = loginUser(email, password) // Авторизуемся
                .then()
                .extract()
                .path("accessToken"); // Получаем токен
    }

    @After
    public void tearDown() {
        // Очистка после тестов
        if (accessToken != null) {
            deleteUser(accessToken);
            accessToken = null;
        }
    }

    @Test
    @DisplayName("Create order with authorization and valid ingredients")
    public void createOrderWithAuthorizationAndValidIngredients() {
        // Создаем заказ авторизованным пользователем с валидными ингредиентами
        createOrder(accessToken, List.of(validIngredient))
                .then()
                .statusCode(200)
                .body("success", equalTo(true));
    }

    @Test
    @DisplayName("Fail to create order without authorization")
    public void failToCreateOrderWithoutAuthorization() {
        // Попытка создать заказ без авторизации
        createOrder("", List.of(validIngredient))
                .then()
                .statusCode(401)
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    @DisplayName("Fail to create order without ingredients")
    public void failToCreateOrderWithoutIngredients() {
        // Попытка создать заказ без ингредиентов
        createOrder(accessToken, Collections.emptyList())
                .then()
                .statusCode(400)
                .body("message", equalTo("Ingredient ids must be provided"));
    }

    @Test
    @DisplayName("Fail to create order with invalid ingredient hash")
    public void failToCreateOrderWithInvalidIngredientHash() {
        // Попытка создать заказ с неверным хешем ингредиентов
        createOrder(accessToken, List.of(invalidIngredient))
                .then()
                .statusCode(500);
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

    @Step("Login user with email: {email}")
    private Response loginUser(String email, String password) {
        // Логиним пользователя
        return given()
                .header("Content-type", "application/json")
                .body(new LoginRequest(email, password))
                .when()
                .post("/api/auth/login");
    }

    @Step("Create order with ingredients")
    private Response createOrder(String accessToken, List<String> ingredients) {
        // Создаем заказ
        return given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken == null ? "" : accessToken)
                .body(new OrderRequest(ingredients))
                .when()
                .post("/api/orders");
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
        // Генерация уникального email
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

    private static class OrderRequest {
        // Класс заказа
        @JsonProperty("ingredients")
        private final List<String> ingredients;

        public OrderRequest(List<String> ingredients) {
            this.ingredients = ingredients;
        }
    }
}

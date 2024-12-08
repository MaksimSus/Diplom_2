import com.fasterxml.jackson.annotation.JsonProperty;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.qameta.allure.junit4.DisplayName;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

public class GetOrdersTests {

    private String email; // Будет рандомным
    private String password = "validpassword";
    private String name = "Valid User";
    private String accessToken;

    @Before
    public void setup() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
        email = generateUniqueEmail(); // Генерация email
        createUser(email, password, name); // Создаем пользователя
        createOrder(accessToken, List.of("61c0c5a71d1f82001bdaaa6d")); // Создаем заказ
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
    @DisplayName("Get orders for authorized user")
    public void getOrdersForAuthorizedUser() {
        // Получение заказов авторизованным пользователем
        getOrders(accessToken)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("orders", notNullValue());
    }

    @Test
    @DisplayName("Fail to get orders without authorization")
    public void failToGetOrdersWithoutAuthorization() {
        // Попытка получить заказы без авторизации
        getOrders("")
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

    @Step("Get orders for user")
    private Response getOrders(String accessToken) {
        // Получаем заказы пользователя
        return given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken == null ? "" : accessToken)
                .when()
                .get("/api/orders");
    }

    @Step("Delete user with token")
    private void deleteUser(String accessToken) {
        // Удаляем пользователя
        Response response = given()
                .header("Authorization", accessToken)
                .when()
                .delete("/api/auth/user");
        if (response.getStatusCode() != 202) {
            throw new RuntimeException("Failed to delete user: " + response.getBody().asString());
        }
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

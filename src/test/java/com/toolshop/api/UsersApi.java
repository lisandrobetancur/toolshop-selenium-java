package com.toolshop.api;

import com.toolshop.config.Config;
import com.toolshop.data.User;
import com.toolshop.data.UserFactory;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registro e inicio de sesión por API, para no pasar por la UI cuando no es lo que se valida. */
public final class UsersApi {

  private UsersApi() {}

  /** Registra un cliente nuevo y único, listo para iniciar sesión. */
  public static User registerRandomCustomer() {
    User user = UserFactory.random();
    register(user);
    return user;
  }

  public static void register(User user) {
    Map<String, Object> address = new LinkedHashMap<>();
    address.put("street", user.address().street());
    address.put("house_number", user.address().houseNumber());
    address.put("city", user.address().city());
    address.put("state", user.address().state());
    address.put("country", user.address().country());
    address.put("postal_code", user.address().postalCode());

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("first_name", user.firstName());
    body.put("last_name", user.lastName());
    body.put("dob", user.dob());
    body.put("phone", user.phone());
    body.put("email", user.email());
    body.put("password", user.password());
    body.put("address", address);

    ApiClient.anonymous().post("/users/register", body);
  }

  /** Token JWT de acceso para las credenciales dadas. */
  public static String login(String email, String password) {
    return ApiClient.anonymous()
        .post("/users/login", Map.of("email", email, "password", password))
        .path("access_token")
        .asString();
  }

  public static String login(User user) {
    return login(user.email(), user.password());
  }

  public static String loginAsAdmin() {
    Config config = Config.get();
    return login(config.adminEmail(), config.requireAdminPassword());
  }

  /** Estado HTTP de un intento de inicio de sesión, sin lanzar excepción si es rechazado. */
  public static int loginStatus(String email, String password) {
    return ApiClient.anonymous()
        .status("POST", "/users/login", Map.of("email", email, "password", password));
  }
}

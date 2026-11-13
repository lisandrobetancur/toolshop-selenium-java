package com.toolshop.api;

import com.toolshop.config.Config;
import io.qameta.allure.Allure;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cliente HTTP mínimo sobre {@link HttpClient} para preparar precondiciones por API. Es inmutable:
 * {@link #withToken} devuelve otro cliente autenticado, así que se puede compartir entre hilos.
 */
public final class ApiClient {

  // HTTP/2 (por defecto): por HTTP/1.1 el servidor responde "Connection: Upgrade, close" y el
  // cliente del JDK intenta reutilizar una conexión que el servidor ya cerró.
  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
  // Reintentos inmediatos ante cortes de conexión, solo para lecturas (GET es idempotente).
  private static final int GET_ATTEMPTS = 3;
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  private final String baseUrl;
  private final @Nullable String token;

  private ApiClient(String baseUrl, @Nullable String token) {
    this.baseUrl = baseUrl;
    this.token = token;
  }

  /** Cliente anónimo contra la API configurada en {@code API_URL}. */
  public static ApiClient anonymous() {
    return new ApiClient(Config.get().apiUrl(), null);
  }

  public ApiClient withToken(String bearerToken) {
    return new ApiClient(baseUrl, bearerToken);
  }

  public JsonNode get(String path) {
    return send("GET", path, null);
  }

  public JsonNode post(String path, @Nullable Object body) {
    return send("POST", path, body);
  }

  public JsonNode put(String path, @Nullable Object body) {
    return send("PUT", path, body);
  }

  public JsonNode delete(String path) {
    return send("DELETE", path, null);
  }

  /** Estado HTTP de una petición, sin lanzar excepción por estados de error. */
  public int status(String method, String path, @Nullable Object body) {
    return Allure.step(
        "API " + method + " " + withoutQuery(path) + " (estado esperado)",
        () -> exchange(method, path, body).statusCode());
  }

  /** Cada llamada aparece como paso del reporte, para ver qué datos preparó la prueba. */
  private JsonNode send(String method, String path, @Nullable Object body) {
    return Allure.step(
        "API " + method + " " + withoutQuery(path), () -> doSend(method, path, body));
  }

  private static String withoutQuery(String path) {
    int query = path.indexOf('?');
    return query < 0 ? path : path.substring(0, query);
  }

  private JsonNode doSend(String method, String path, @Nullable Object body) {
    HttpResponse<String> response = exchange(method, path, body);
    if (response.statusCode() >= 400) {
      throw new ApiException(
          method + " " + path + " respondió " + response.statusCode() + ": " + response.body(),
          response.statusCode());
    }
    String payload = response.body();
    return payload == null || payload.isBlank() ? JSON.createObjectNode() : JSON.readTree(payload);
  }

  private HttpResponse<String> exchange(String method, String path, @Nullable Object body) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .method(
                method,
                body == null
                    ? BodyPublishers.noBody()
                    : BodyPublishers.ofString(JSON.writeValueAsString(body)));
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    int attempts = "GET".equals(method) ? GET_ATTEMPTS : 1;
    for (int attempt = 1; ; attempt++) {
      try {
        return HTTP.send(request.build(), BodyHandlers.ofString());
      } catch (IOException e) {
        if (attempt >= attempts) {
          throw new ApiException(method + " " + path + " falló: " + e.getMessage(), e);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ApiException(method + " " + path + " fue interrumpida", e);
      }
    }
  }
}

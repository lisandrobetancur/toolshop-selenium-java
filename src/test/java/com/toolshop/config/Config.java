package com.toolshop.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.net.URI;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

/**
 * Configuración de la suite. Cada valor sale de una variable de entorno y, si no existe, del
 * archivo {@code .env} local. No hay credenciales en el código.
 */
public record Config(
    String baseUrl,
    String apiUrl,
    Browser browser,
    boolean headless,
    @Nullable URI gridUrl,
    Duration timeout,
    String adminEmail,
    @Nullable String adminPassword) {

  private static final Config INSTANCE = load();

  public static Config get() {
    return INSTANCE;
  }

  /** Contraseña del administrador; falla con un mensaje claro si no está configurada. */
  public String requireAdminPassword() {
    if (adminPassword == null) {
      throw new IllegalStateException(
          "Falta ADMIN_PASSWORD. Defínela como variable de entorno o en el archivo .env.");
    }
    return adminPassword;
  }

  private static Config load() {
    Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    String grid = blankToNull(env.get("GRID_URL"));
    return new Config(
        stripTrailingSlash(env.get("BASE_URL", "https://practicesoftwaretesting.com")),
        stripTrailingSlash(env.get("API_URL", "https://api.practicesoftwaretesting.com")),
        Browser.from(env.get("BROWSER", "chrome")),
        Boolean.parseBoolean(env.get("HEADLESS", "true")),
        grid == null ? null : URI.create(grid),
        Duration.ofSeconds(Long.parseLong(env.get("TIMEOUT_SECONDS", "15"))),
        env.get("ADMIN_EMAIL", "admin@practicesoftwaretesting.com"),
        blankToNull(env.get("ADMIN_PASSWORD")));
  }

  private static @Nullable String blankToNull(@Nullable String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String stripTrailingSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}

package com.toolshop.session;

import com.toolshop.config.Config;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Sesión del navegador. Toolshop guarda el JWT en {@code localStorage['auth-token']} y el carrito
 * en {@code sessionStorage}; inyectarlos evita repetir el login por UI en cada prueba.
 */
public final class Session {

  private static final String TOKEN_KEY = "auth-token";

  private Session() {}

  /** Deja el navegador autenticado con el token dado. La siguiente navegación ya tiene sesión. */
  public static void authenticate(WebDriver driver, String token) {
    openOrigin(driver);
    js(driver)
        .executeScript(
            "window.localStorage.setItem(arguments[0], arguments[1]);", TOKEN_KEY, token);
  }

  /** Asocia al navegador un carrito creado por API, con su cantidad total de unidades. */
  public static void useCart(WebDriver driver, String cartId, int totalQuantity) {
    openOrigin(driver);
    js(driver)
        .executeScript(
            "window.sessionStorage.setItem('cart_id', arguments[0]);"
                + "window.sessionStorage.setItem('cart_quantity', arguments[1]);",
            cartId,
            String.valueOf(totalQuantity));
  }

  /** Token guardado por la aplicación, o null si no hay sesión iniciada. */
  public static @Nullable String storedToken(WebDriver driver) {
    Object token =
        js(driver).executeScript("return window.localStorage.getItem(arguments[0]);", TOKEN_KEY);
    return token == null ? null : token.toString();
  }

  /** Identificador del carrito que la aplicación asoció a esta pestaña, o null si no hay. */
  public static @Nullable String cartId(WebDriver driver) {
    Object id = js(driver).executeScript("return window.sessionStorage.getItem('cart_id');");
    return id == null ? null : id.toString();
  }

  /** El almacenamiento web es por origen: hay que estar en la aplicación antes de escribirlo. */
  private static void openOrigin(WebDriver driver) {
    String baseUrl = Config.get().baseUrl();
    String current = driver.getCurrentUrl();
    if (current == null || !current.startsWith(baseUrl)) {
      driver.get(baseUrl + "/favicon.ico");
    }
  }

  private static JavascriptExecutor js(WebDriver driver) {
    return (JavascriptExecutor) driver;
  }
}

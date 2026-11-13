package com.toolshop.components;

import com.toolshop.pages.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Barra de navegación superior, común a todas las páginas. */
public final class Header extends BasePage {

  private static final By SIGN_IN = byTest("nav-sign-in");
  private static final By USER_MENU = byTest("nav-menu");
  private static final By SIGN_OUT = byTest("nav-sign-out");
  private static final By CART_LINK = byTest("nav-cart");
  private static final By CART_QUANTITY = byTest("cart-quantity");

  public Header(WebDriver driver) {
    super(driver);
  }

  /** Nombre que muestra el menú del usuario autenticado. */
  public String signedInUserName() {
    return textOf(USER_MENU);
  }

  public boolean isSignedOut() {
    waits.visible(SIGN_IN);
    return !isDisplayed(USER_MENU);
  }

  @Step("Cerrar sesión")
  public void signOut() {
    click(USER_MENU);
    click(SIGN_OUT);
    waits.visible(SIGN_IN);
  }

  @Step("Abrir la opción \"{dataTest}\" del menú de usuario")
  public void openMenuItem(String dataTest) {
    click(USER_MENU);
    click(byTest(dataTest));
  }

  /** Unidades que muestra el indicador del carrito. */
  public int cartQuantity() {
    return Integer.parseInt(textOf(CART_QUANTITY));
  }

  @Step("Esperar que el carrito del encabezado muestre {expected} unidades")
  public void waitForCartQuantity(int expected) {
    waits.until(
        "que el carrito del encabezado muestre " + expected + " unidades",
        d -> String.valueOf(expected).equals(d.findElement(CART_QUANTITY).getText().trim()));
  }

  @Step("Abrir el carrito desde el encabezado")
  public void openCart() {
    click(CART_LINK);
  }
}

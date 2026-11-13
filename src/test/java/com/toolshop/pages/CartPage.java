package com.toolshop.pages;

import com.toolshop.components.CartTable;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Carrito de compras: primer paso de {@code /checkout}. */
public final class CartPage extends BasePage {

  private static final By TOTAL = byTest("cart-total");
  private static final By PROCEED = byTest("proceed-1");
  private static final By EMPTY_MESSAGE =
      By.xpath("//p[normalize-space()='The cart is empty. Nothing to display.']");

  private final CartTable table;

  public CartPage(WebDriver driver) {
    super(driver);
    this.table = new CartTable(driver);
  }

  /** Abre el carrito y espera a que muestre sus líneas o el aviso de carrito vacío. */
  @Step("Abrir el carrito")
  public CartPage open() {
    navigateTo("/checkout");
    waits.until(
        "que el carrito muestre sus productos o el aviso de carrito vacío",
        d -> isDisplayed(TOTAL) || isDisplayed(EMPTY_MESSAGE));
    return this;
  }

  public CartTable table() {
    return table;
  }

  public boolean isEmpty() {
    return isDisplayed(EMPTY_MESSAGE);
  }

  @Step("Continuar al checkout")
  public void proceedToCheckout() {
    click(PROCEED);
  }
}

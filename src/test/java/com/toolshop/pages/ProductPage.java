package com.toolshop.pages;

import com.toolshop.support.Money;
import io.qameta.allure.Step;
import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Detalle de un producto ({@code /product/:id}). */
public final class ProductPage extends BasePage {

  private static final By NAME = byTest("product-name");
  private static final By UNIT_PRICE = byTest("unit-price");
  private static final By CATEGORY = By.cssSelector("[aria-label='category']");
  private static final By BRAND = By.cssSelector("[aria-label='brand']");
  private static final By QUANTITY = byTest("quantity");
  private static final By INCREASE = byTest("increase-quantity");
  private static final By DECREASE = byTest("decrease-quantity");
  private static final By ADD_TO_CART = byTest("add-to-cart");
  private static final By ADD_TO_FAVORITES = byTest("add-to-favorites");
  private static final By OUT_OF_STOCK = byTest("out-of-stock");
  // Las tarjetas de productos relacionados no tienen data-test: son los únicos enlaces a otro
  // producto en esta página, y el nombre accesible está en el texto alternativo de la imagen.
  private static final By RELATED_IMAGES = By.cssSelector("a[href^='/product/'] img");

  public ProductPage(WebDriver driver) {
    super(driver);
  }

  @Step("Abrir el detalle del producto {productId}")
  public ProductPage open(String productId) {
    navigateTo("/product/" + productId);
    waits.visible(NAME);
    return this;
  }

  public String name() {
    return textOf(NAME);
  }

  public BigDecimal unitPrice() {
    return Money.parse(textOf(UNIT_PRICE));
  }

  public String category() {
    return textOf(CATEGORY);
  }

  public String brand() {
    return textOf(BRAND);
  }

  public int quantity() {
    return Integer.parseInt(valueOf(QUANTITY));
  }

  @Step("Aumentar la cantidad {times} vez/veces")
  public ProductPage increaseQuantity(int times) {
    for (int i = 0; i < times; i++) {
      click(INCREASE);
    }
    return this;
  }

  @Step("Disminuir la cantidad {times} vez/veces")
  public ProductPage decreaseQuantity(int times) {
    for (int i = 0; i < times; i++) {
      click(DECREASE);
    }
    return this;
  }

  /** Escribe la cantidad directamente en el campo, reemplazando la actual. */
  @Step("Escribir la cantidad {quantity}")
  public ProductPage setQuantity(int quantity) {
    overwrite(QUANTITY, String.valueOf(quantity));
    return this;
  }

  @Step("Agregar al carrito")
  public ProductPage addToCart() {
    click(ADD_TO_CART);
    return this;
  }

  @Step("Agregar a favoritos")
  public ProductPage addToFavorites() {
    click(ADD_TO_FAVORITES);
    return this;
  }

  public boolean canAddToCart() {
    return waits.visible(ADD_TO_CART).isEnabled();
  }

  public String outOfStockMessage() {
    return textOf(OUT_OF_STOCK);
  }

  public List<String> relatedProductNames() {
    return driver.findElements(RELATED_IMAGES).stream()
        .map(image -> String.valueOf(image.getDomAttribute("alt")))
        .toList();
  }

  /** Abre el detalle de un producto relacionado y espera a que se muestre. */
  @Step("Abrir el producto relacionado \"{productName}\"")
  public ProductPage openRelated(String productName) {
    click(By.cssSelector("a[href^='/product/'] img[alt='" + productName + "']"));
    waits.until(
        "que se muestre el detalle de '" + productName + "'",
        d -> productName.equals(d.findElement(NAME).getText().trim()));
    return this;
  }
}

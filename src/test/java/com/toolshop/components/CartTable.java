package com.toolshop.components;

import com.toolshop.pages.BasePage;
import com.toolshop.support.Money;
import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Tabla de líneas del carrito, compartida por el carrito y el primer paso del checkout. */
public final class CartTable extends BasePage {

  /** Una línea del carrito tal como la ve el cliente. */
  public record Line(String product, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

  private static final By ROWS = By.cssSelector("tbody tr");
  private static final By TITLE = byTest("product-title");
  private static final By QUANTITY = byTest("product-quantity");
  private static final By UNIT_PRICE = byTest("product-price");
  private static final By LINE_PRICE = byTest("line-price");
  private static final By QUANTITY_LABELS = By.cssSelector("label[for^='quantity-']");
  // El botón de eliminar no tiene data-test ni nombre accesible: es el único enlace de la fila.
  private static final By REMOVE = By.tagName("a");
  private static final By TOTAL = byTest("cart-total");

  public CartTable(WebDriver driver) {
    super(driver);
  }

  /** Líneas del carrito en el orden en que se muestran. */
  public List<Line> lines() {
    return productRows().stream()
        .map(row ->
            new Line(
                titleOf(row),
                Integer.parseInt(String.valueOf(row.findElement(QUANTITY).getDomProperty("value"))),
                Money.parse(row.findElement(UNIT_PRICE).getText()),
                Money.parse(row.findElement(LINE_PRICE).getText())))
        .toList();
  }

  public BigDecimal total() {
    return Money.parse(textOf(TOTAL));
  }

  /** Cambia la cantidad de un producto y confirma el campo, como al salir de él con Tab. */
  public void setQuantity(String product, int quantity) {
    String expectedLabel = "Quantity for " + product;
    String inputId =
        waits.until(
            "el campo de cantidad de '" + product + "'",
            d ->
                d.findElements(QUANTITY_LABELS).stream()
                    .filter(label ->
                        expectedLabel.equals(
                            String.valueOf(label.getDomProperty("textContent")).trim()))
                    .map(label -> label.getDomAttribute("for"))
                    .findFirst()
                    .orElse(null));
    overwrite(By.id(inputId), String.valueOf(quantity) + Keys.TAB);
  }

  public void remove(String product) {
    waits.until(
        "poder eliminar '" + product + "' del carrito",
        d -> {
          for (WebElement row : productRows()) {
            if (product.equals(titleOf(row))) {
              row.findElement(REMOVE).click();
              return true;
            }
          }
          return false;
        });
  }

  private List<WebElement> productRows() {
    return driver.findElements(ROWS).stream()
        .filter(row -> !row.findElements(TITLE).isEmpty())
        .toList();
  }

  private static String titleOf(WebElement row) {
    return row.findElement(TITLE).getText().replace(' ', ' ').trim();
  }
}

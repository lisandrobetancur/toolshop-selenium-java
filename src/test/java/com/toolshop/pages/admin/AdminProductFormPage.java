package com.toolshop.pages.admin;

import com.toolshop.data.NewProduct;
import com.toolshop.pages.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Alta y edición de productos ({@code /admin/products/add} y {@code /admin/products/edit/:id}). */
public final class AdminProductFormPage extends BasePage {

  private static final By NAME = byTest("name");
  private static final By DESCRIPTION = byTest("description");
  private static final By STOCK = byTest("stock");
  private static final By PRICE = byTest("price");
  private static final By SAVE = byTest("product-submit");
  private static final By SAVED = By.cssSelector(".alert-success[role='alert']");

  public AdminProductFormPage(WebDriver driver) {
    super(driver);
  }

  @Step("Abrir el alta de producto")
  public AdminProductFormPage openNew() {
    navigateTo("/admin/products/add");
    waits.visible(SAVE);
    return this;
  }

  /** Abre la edición y espera a que el formulario cargue el producto. */
  @Step("Abrir la edición del producto {productId}")
  public AdminProductFormPage openExisting(String productId) {
    navigateTo("/admin/products/edit/" + productId);
    waits.until(
        "que el formulario cargue el producto",
        d -> !String.valueOf(d.findElement(NAME).getDomProperty("value")).isBlank());
    return this;
  }

  /**
   * Completa todos los campos. En la edición también hace falta: la API de productos no devuelve
   * stock ni los identificadores de marca, categoría e imagen, así que el formulario los carga
   * vacíos y no deja guardar sin volver a indicarlos.
   */
  @Step("Completar el producto \"{product.name}\" a {product.price}")
  public AdminProductFormPage fill(NewProduct product) {
    type(NAME, product.name());
    type(DESCRIPTION, product.description());
    type(STOCK, String.valueOf(product.stock()));
    type(PRICE, product.price().toPlainString());
    selectOption("brand-id", product.brandId());
    selectOption("category-id", product.categoryId());
    selectOption("product-image-id", product.imageId());
    return this;
  }

  /** Guarda y devuelve el mensaje de confirmación. */
  @Step("Guardar el producto")
  public String save() {
    click(SAVE);
    return textOf(SAVED);
  }

  /** Las opciones de marca, categoría e imagen llegan por API: se espera a que existan. */
  private void selectOption(String testId, String value) {
    waits.present(By.cssSelector("[data-test='" + testId + "'] option[value='" + value + "']"));
    selectByValue(byTest(testId), value);
  }
}

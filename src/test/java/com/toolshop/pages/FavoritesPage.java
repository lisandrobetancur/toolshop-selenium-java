package com.toolshop.pages;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Favoritos del cliente ({@code /account/favorites}). */
public final class FavoritesPage extends BasePage {

  private static final By TITLE = byTest("page-title");
  private static final By CARDS = By.cssSelector("[data-test^='favorite-']");
  private static final By PRODUCT_NAME = byTest("product-name");
  private static final By DELETE = byTest("delete");
  private static final By EMPTY_MESSAGE =
      By.xpath("//div[starts-with(normalize-space(), 'There are no favorites yet.')]");

  public FavoritesPage(WebDriver driver) {
    super(driver);
  }

  public FavoritesPage open() {
    navigateTo("/account/favorites");
    waits.visible(TITLE);
    return this;
  }

  /** Productos favoritos que se muestran, en orden. */
  public List<String> productNames() {
    return driver.findElements(CARDS).stream()
        .map(card -> card.findElement(PRODUCT_NAME).getText().trim())
        .toList();
  }

  public void remove(String productName) {
    waits.until(
        "poder quitar '" + productName + "' de favoritos",
        d -> {
          for (WebElement card : d.findElements(CARDS)) {
            if (productName.equals(card.findElement(PRODUCT_NAME).getText().trim())) {
              card.findElement(DELETE).click();
              return true;
            }
          }
          return false;
        });
  }

  public boolean showsEmptyMessage() {
    return isDisplayed(EMPTY_MESSAGE);
  }
}

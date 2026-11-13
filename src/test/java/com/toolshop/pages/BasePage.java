package com.toolshop.pages;

import com.toolshop.config.Config;
import com.toolshop.support.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/** Base de los Page Objects: navegación, localizadores {@code data-test} y acciones con espera. */
public abstract class BasePage {

  protected final WebDriver driver;
  protected final Waits waits;

  protected BasePage(WebDriver driver) {
    this.driver = driver;
    this.waits = new Waits(driver, Config.get().timeout());
  }

  /** Localizador por el atributo {@code data-test}, la convención de Toolshop para pruebas. */
  protected static By byTest(String id) {
    return By.cssSelector("[data-test='" + id + "']");
  }

  protected void open(String path) {
    driver.get(Config.get().baseUrl() + path);
  }

  protected void click(By locator) {
    waits.click(locator);
  }

  protected void type(By locator, CharSequence text) {
    WebElement field = waits.visible(locator);
    field.clear();
    field.sendKeys(text);
  }

  protected void selectByValue(By locator, String value) {
    new Select(waits.visible(locator)).selectByValue(value);
  }

  protected String textOf(By locator) {
    return waits.until(
        "texto visible en " + locator,
        d -> {
          WebElement element = d.findElement(locator);
          return element.isDisplayed() ? element.getText().trim() : null;
        });
  }

  protected String valueOf(By locator) {
    String value = waits.visible(locator).getDomProperty("value");
    return value == null ? "" : value;
  }

  protected boolean isDisplayed(By locator) {
    return driver.findElements(locator).stream().anyMatch(WebElement::isDisplayed);
  }
}

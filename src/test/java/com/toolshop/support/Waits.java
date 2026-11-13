package com.toolshop.support;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Esperas explícitas por condición. Es el único mecanismo de sincronización de la suite: no hay
 * esperas implícitas ni pausas fijas.
 */
public final class Waits {

  private final WebDriver driver;
  private final Duration timeout;

  public Waits(WebDriver driver, Duration timeout) {
    this.driver = driver;
    this.timeout = timeout;
  }

  /** Espera hasta que la condición devuelva un valor distinto de null y de false. */
  public <T> T until(Function<? super WebDriver, ? extends @Nullable T> condition) {
    return newWait().until(condition);
  }

  /** Igual que {@link #until}, con un mensaje que explica qué se esperaba. */
  public <T> T until(
      String expectation, Function<? super WebDriver, ? extends @Nullable T> condition) {
    WebDriverWait wait = newWait();
    wait.withMessage(expectation);
    return wait.until(condition);
  }

  public WebElement visible(By locator) {
    return until(ExpectedConditions.visibilityOfElementLocated(locator));
  }

  public WebElement present(By locator) {
    return until(ExpectedConditions.presenceOfElementLocated(locator));
  }

  public List<WebElement> allVisible(By locator) {
    return until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
  }

  public void gone(By locator) {
    until(ExpectedConditions.invisibilityOfElementLocated(locator));
  }

  public void urlContains(String fragment) {
    until(ExpectedConditions.urlContains(fragment));
  }

  /**
   * Hace clic cuando el elemento está visible y habilitado. Si Angular vuelve a renderizar el
   * elemento o algo lo tapa momentáneamente, se reintenta dentro del mismo tiempo de espera.
   */
  public void click(By locator) {
    until(
        "poder hacer clic en " + locator,
        d -> {
          try {
            WebElement element = d.findElement(locator);
            if (!element.isDisplayed() || !element.isEnabled()) {
              return false;
            }
            element.click();
            return true;
          } catch (ElementNotInteractableException e) {
            return false;
          }
        });
  }

  private WebDriverWait newWait() {
    WebDriverWait wait = new WebDriverWait(driver, timeout);
    wait.ignoring(StaleElementReferenceException.class);
    return wait;
  }
}

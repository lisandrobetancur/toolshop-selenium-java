package com.toolshop.components;

import com.toolshop.pages.BasePage;
import io.qameta.allure.Step;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Notificaciones emergentes (toasts) con las que la aplicación confirma una acción. */
public final class Toast extends BasePage {

  private static final By MESSAGES = By.cssSelector("#toast-container [role='alert']");

  public Toast(WebDriver driver) {
    super(driver);
  }

  /** Espera a que aparezca una notificación con exactamente ese texto. */
  @Step("Esperar la notificación \"{message}\"")
  public void waitFor(String message) {
    waits.until(
        "una notificación con el texto '" + message + "'", d -> messages(d).contains(message));
  }

  private static List<String> messages(WebDriver driver) {
    return driver.findElements(MESSAGES).stream()
        .filter(WebElement::isDisplayed)
        .map(element -> element.getText().trim())
        .toList();
  }
}

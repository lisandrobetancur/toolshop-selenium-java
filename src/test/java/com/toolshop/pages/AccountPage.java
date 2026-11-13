package com.toolshop.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Página principal de la cuenta del cliente ({@code /account}). */
public final class AccountPage extends BasePage {

  private static final By TITLE = byTest("page-title");

  public AccountPage(WebDriver driver) {
    super(driver);
  }

  public AccountPage open() {
    open("/account");
    return this;
  }

  /** Espera a que la aplicación redirija a la cuenta y devuelve el título de la página. */
  public String waitUntilLoaded() {
    waits.urlContains("/account");
    return textOf(TITLE);
  }
}

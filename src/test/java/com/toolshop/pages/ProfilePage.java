package com.toolshop.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Perfil del cliente ({@code /account/profile}). */
public final class ProfilePage extends BasePage {

  private static final By FIRST_NAME = byTest("first-name");
  private static final By LAST_NAME = byTest("last-name");
  private static final By EMAIL = byTest("email");
  private static final By PHONE = byTest("phone");
  private static final By SUBMIT = byTest("update-profile-submit");
  private static final By SUCCESS = By.cssSelector(".alert-success[role='alert']");
  private static final By ERROR = By.cssSelector(".alert-danger[role='alert']");

  public ProfilePage(WebDriver driver) {
    super(driver);
  }

  /** Abre el perfil y espera a que el formulario cargue los datos del cliente. */
  public ProfilePage open() {
    navigateTo("/account/profile");
    waits.until(
        "que el perfil cargue el correo del cliente",
        d -> !String.valueOf(d.findElement(EMAIL).getDomProperty("value")).isBlank());
    return this;
  }

  public String firstName() {
    return valueOf(FIRST_NAME);
  }

  public String lastName() {
    return valueOf(LAST_NAME);
  }

  public String phone() {
    return valueOf(PHONE);
  }

  public ProfilePage changeName(String firstName, String lastName) {
    type(FIRST_NAME, firstName);
    type(LAST_NAME, lastName);
    return this;
  }

  public ProfilePage changePhone(String phone) {
    type(PHONE, phone);
    return this;
  }

  public ProfilePage save() {
    click(SUBMIT);
    return this;
  }

  public String successMessage() {
    return textOf(SUCCESS);
  }

  public String errorMessage() {
    return textOf(ERROR);
  }
}

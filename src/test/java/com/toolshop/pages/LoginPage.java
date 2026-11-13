package com.toolshop.pages;

import io.qameta.allure.Param;
import io.qameta.allure.Step;
import io.qameta.allure.model.Parameter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Página de inicio de sesión ({@code /auth/login}). */
public final class LoginPage extends BasePage {

  private static final By EMAIL = byTest("email");
  private static final By PASSWORD = byTest("password");
  private static final By SUBMIT = byTest("login-submit");
  private static final By EMAIL_ERROR = byTest("email-error");
  private static final By PASSWORD_ERROR = byTest("password-error");
  private static final By LOGIN_ERROR = byTest("login-error");

  public LoginPage(WebDriver driver) {
    super(driver);
  }

  @Step("Abrir la página de login")
  public LoginPage open() {
    navigateTo("/auth/login");
    waits.visible(SUBMIT);
    return this;
  }

  /** Envía el formulario con las credenciales dadas; un valor vacío deja el campo sin llenar. */
  @Step("Iniciar sesión con \"{email}\"")
  public LoginPage submit(
      String email, @Param(name = "password", mode = Parameter.Mode.MASKED) String password) {
    waits.visible(SUBMIT);
    if (!email.isEmpty()) {
      type(EMAIL, email);
    }
    if (!password.isEmpty()) {
      type(PASSWORD, password);
    }
    click(SUBMIT);
    return this;
  }

  public String emailError() {
    return textOf(EMAIL_ERROR);
  }

  public String passwordError() {
    return textOf(PASSWORD_ERROR);
  }

  /** Mensaje de rechazo de las credenciales que devuelve el servidor. */
  public String loginError() {
    return textOf(LOGIN_ERROR);
  }

  public boolean isShowingForm() {
    return isDisplayed(SUBMIT);
  }
}

package com.toolshop.pages;

import com.toolshop.data.Address;
import com.toolshop.data.User;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Página de registro de clientes ({@code /auth/register}). */
public final class RegisterPage extends BasePage {

  private static final By FIRST_NAME = byTest("first-name");
  private static final By LAST_NAME = byTest("last-name");
  private static final By DOB = byTest("dob");
  private static final By COUNTRY = byTest("country");
  private static final By POSTAL_CODE = byTest("postal_code");
  private static final By HOUSE_NUMBER = byTest("house_number");
  private static final By STREET = byTest("street");
  private static final By CITY = byTest("city");
  private static final By STATE = byTest("state");
  private static final By PHONE = byTest("phone");
  private static final By EMAIL = byTest("email");
  private static final By PASSWORD = byTest("password");
  private static final By SUBMIT = byTest("register-submit");
  private static final By REGISTER_ERROR = byTest("register-error");

  public RegisterPage(WebDriver driver) {
    super(driver);
  }

  public RegisterPage open() {
    navigateTo("/auth/register");
    waits.visible(SUBMIT);
    return this;
  }

  /**
   * Llena el formulario completo. Calle, ciudad y estado no se escriben: la aplicación los
   * autocompleta a partir de país, código postal y número, y se espera a que lo haga.
   */
  public RegisterPage fillForm(User user) {
    type(FIRST_NAME, user.firstName());
    type(LAST_NAME, user.lastName());
    type(DOB, user.dob());
    fillAddress(user.address());
    type(PHONE, user.phone());
    type(EMAIL, user.email());
    type(PASSWORD, user.password());
    return this;
  }

  public RegisterPage typePassword(String password) {
    type(PASSWORD, password);
    return this;
  }

  public RegisterPage submit() {
    click(SUBMIT);
    return this;
  }

  /** Tras un registro correcto la aplicación lleva al formulario de inicio de sesión. */
  public LoginPage waitForRedirectToLogin() {
    waits.urlContains("/auth/login");
    return new LoginPage(driver);
  }

  /** Dirección que la aplicación autocompletó en el formulario. */
  public Address autofilledAddress() {
    return new Address(
        valueOf(STREET),
        valueOf(HOUSE_NUMBER),
        valueOf(CITY),
        valueOf(STATE),
        valueOf(COUNTRY),
        valueOf(POSTAL_CODE));
  }

  /** Mensaje de validación del campo indicado (por ejemplo {@code first-name} o {@code dob}). */
  public String fieldError(String field) {
    return textOf(byTest(field + "-error"));
  }

  /** Error devuelto por el servidor al rechazar el registro. */
  public String registerError() {
    return textOf(REGISTER_ERROR);
  }

  private void fillAddress(Address address) {
    selectByValue(COUNTRY, address.country());
    type(POSTAL_CODE, address.postalCode());
    type(HOUSE_NUMBER, address.houseNumber());
    waits.until(
        "que la búsqueda por código postal autocomplete calle, ciudad y estado",
        d ->
            !isBlank(d.findElement(STREET).getDomProperty("value"))
                && !isBlank(d.findElement(CITY).getDomProperty("value"))
                && !isBlank(d.findElement(STATE).getDomProperty("value")));
  }

  private static boolean isBlank(@Nullable String value) {
    return value == null || value.isBlank();
  }
}

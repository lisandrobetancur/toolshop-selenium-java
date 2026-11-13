package com.toolshop.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.toolshop.api.UsersApi;
import com.toolshop.components.Header;
import com.toolshop.data.User;
import com.toolshop.pages.AccountPage;
import com.toolshop.pages.LoginPage;
import com.toolshop.session.Session;
import io.qameta.allure.Epic;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Epic("Tienda")
@Tag("F02")
@DisplayName("F02 Login")
class F02LoginTest extends UiTest {

  @Test
  @Tag("smoke")
  @DisplayName("Con credenciales válidas el cliente entra a su cuenta")
  void validCredentialsOpenTheAccount() {
    User customer = UsersApi.registerRandomCustomer();

    new LoginPage(driver()).open().submit(customer.email(), customer.password());

    assertThat(new AccountPage(driver()).waitUntilLoaded()).isEqualTo("My account");
    assertThat(new Header(driver()).signedInUserName()).isEqualTo(customer.fullName());
    assertThat(Session.storedToken(driver())).as("token de sesión guardado").isNotBlank();
  }

  @Test
  @Tag("regression")
  @DisplayName("Con contraseña incorrecta se rechaza el acceso y no se crea sesión")
  void wrongPasswordIsRejected() {
    User customer = UsersApi.registerRandomCustomer();
    LoginPage login = new LoginPage(driver()).open();

    login.submit(customer.email(), customer.password() + "x");

    assertThat(login.loginError()).isEqualTo("Invalid email or password");
    assertThat(login.isShowingForm()).isTrue();
    assertThat(driver().getCurrentUrl()).endsWith("/auth/login");
    assertThat(Session.storedToken(driver())).as("no debe haber sesión").isNull();
  }

  @Test
  @Tag("regression")
  @DisplayName("Un correo no registrado se rechaza con el mismo mensaje genérico")
  void unknownEmailIsRejected() {
    LoginPage login = new LoginPage(driver()).open();

    login.submit("nobody." + System.nanoTime() + "@example.test", "Whatever1!");

    assertThat(login.loginError()).isEqualTo("Invalid email or password");
    assertThat(Session.storedToken(driver())).isNull();
  }

  @Test
  @Tag("regression")
  @DisplayName(
      "Tras tres intentos fallidos la cuenta queda bloqueada, incluso con la clave correcta")
  void accountLocksAfterThreeFailedAttempts() {
    User customer = UsersApi.registerRandomCustomer();
    for (int attempt = 1; attempt <= 3; attempt++) {
      assertThat(UsersApi.loginStatus(customer.email(), "Wrong-password-" + attempt))
          .as("intento fallido %s", attempt)
          .isEqualTo(401);
    }
    LoginPage login = new LoginPage(driver()).open();

    login.submit(customer.email(), customer.password());

    assertThat(login.loginError())
        .isEqualTo("Account locked, too many failed attempts. Please contact the administrator.");
    assertThat(Session.storedToken(driver())).isNull();
  }

  @ParameterizedTest(quoteTextArguments = false, name = "correo=[{0}] clave=[{1}]")
  @Tag("regression")
  @DisplayName("El formulario valida los campos antes de enviar")
  @CsvSource(
      delimiter = '|',
      nullValues = "-",
      value = {
        // correo | clave | error de correo | error de clave
        "'' | '' | Email is required | Password is required",
        "'' | Valid-pass1 | Email is required | -",
        "user@example.test | '' | - | Password is required",
        "not-an-email | Valid-pass1 | Email format is invalid | -",
        "user@example.test | ab | - | Password length is invalid",
      })
  void fieldsAreValidated(
      String email, String password, String expectedEmailError, String expectedPasswordError) {
    LoginPage login = new LoginPage(driver()).open();

    login.submit(email, password);

    SoftAssertions.assertSoftly(soft -> {
      if (expectedEmailError != null) {
        soft.assertThat(login.emailError()).isEqualTo(expectedEmailError);
      }
      if (expectedPasswordError != null) {
        soft.assertThat(login.passwordError()).isEqualTo(expectedPasswordError);
      }
      soft.assertThat(driver().getCurrentUrl()).endsWith("/auth/login");
      soft.assertThat(Session.storedToken(driver())).isNull();
    });
  }
}

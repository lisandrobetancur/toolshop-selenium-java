package com.toolshop.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.toolshop.api.ApiClient;
import com.toolshop.api.UsersApi;
import com.toolshop.components.Header;
import com.toolshop.data.User;
import com.toolshop.data.UserFactory;
import com.toolshop.pages.AccountPage;
import com.toolshop.pages.LoginPage;
import com.toolshop.pages.RegisterPage;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

@Tag("F01")
@DisplayName("F01 Registro de cliente")
class F01RegistrationTest extends UiTest {

  @Test
  @Tag("smoke")
  @DisplayName("Un cliente nuevo se registra y entra por primera vez con sus credenciales")
  void newCustomerRegistersAndLogsInForTheFirstTime() {
    User customer = UserFactory.random();
    RegisterPage register = new RegisterPage(driver()).open().fillForm(customer);

    assertThat(register.autofilledAddress())
        .as("dirección autocompletada por la búsqueda de código postal")
        .isEqualTo(customer.address());

    LoginPage login = register.submit().waitForRedirectToLogin();
    login.submit(customer.email(), customer.password());

    assertThat(new AccountPage(driver()).waitUntilLoaded()).isEqualTo("My account");
    assertThat(new Header(driver()).signedInUserName()).isEqualTo(customer.fullName());

    JsonNode profile = ApiClient.anonymous().withToken(UsersApi.login(customer)).get("/users/me");
    SoftAssertions.assertSoftly(soft -> {
      soft.assertThat(profile.path("first_name").asString()).isEqualTo(customer.firstName());
      soft.assertThat(profile.path("last_name").asString()).isEqualTo(customer.lastName());
      soft.assertThat(profile.path("email").asString()).isEqualTo(customer.email());
      soft.assertThat(profile.path("dob").asString()).isEqualTo(customer.dob());
      soft.assertThat(profile.path("phone").asString()).isEqualTo(customer.phone());
      soft.assertThat(profile.path("address").path("city").asString())
          .isEqualTo(customer.address().city());
      soft.assertThat(profile.path("address").path("postal_code").asString())
          .isEqualTo(customer.address().postalCode());
    });
  }

  @Test
  @Tag("regression")
  @DisplayName("No se puede registrar un correo que ya pertenece a otro cliente")
  void duplicateEmailIsRejected() {
    User existing = UsersApi.registerRandomCustomer();
    User duplicate =
        new User(
            "Other",
            "Person",
            existing.email(),
            UserFactory.randomPassword(),
            existing.dob(),
            existing.phone(),
            existing.address());

    RegisterPage register = new RegisterPage(driver()).open().fillForm(duplicate).submit();

    assertThat(register.registerError())
        .isEqualTo("A customer with this email address already exists.");
    assertThat(driver().getCurrentUrl()).endsWith("/auth/register");
    assertThat(UsersApi.loginStatus(duplicate.email(), duplicate.password()))
        .as("la contraseña del segundo registro no debe dar acceso")
        .isEqualTo(401);
  }

  @Test
  @Tag("regression")
  @DisplayName("El formulario vacío muestra los campos obligatorios y no registra")
  void emptyFormShowsRequiredFields() {
    RegisterPage register = new RegisterPage(driver()).open().submit();

    SoftAssertions.assertSoftly(soft -> {
      soft.assertThat(register.fieldError("first-name")).isEqualTo("First name is required");
      soft.assertThat(register.fieldError("last-name")).isEqualTo("Last name is required");
      soft.assertThat(register.fieldError("dob")).contains("Date of Birth is required");
      soft.assertThat(register.fieldError("country")).isEqualTo("Country is required");
      soft.assertThat(register.fieldError("postal_code")).contains("Postcode is required");
      soft.assertThat(register.fieldError("house_number")).isEqualTo("House number is required");
      soft.assertThat(register.fieldError("street")).isEqualTo("Street is required");
      soft.assertThat(register.fieldError("city")).isEqualTo("City is required");
      soft.assertThat(register.fieldError("state")).isEqualTo("State is required");
      soft.assertThat(register.fieldError("phone")).contains("Phone is required.");
      soft.assertThat(register.fieldError("email")).contains("Email is required");
      soft.assertThat(register.fieldError("password")).contains("Password is required");
      soft.assertThat(driver().getCurrentUrl()).endsWith("/auth/register");
    });
  }

  @Test
  @Tag("regression")
  @DisplayName("Una contraseña débil impide el registro")
  void weakPasswordIsRejected() {
    User customer = UserFactory.random();
    RegisterPage register =
        new RegisterPage(driver()).open().fillForm(customer).typePassword("short").submit();

    assertThat(register.fieldError("password"))
        .contains("Password must be minimal 6 characters long.")
        .contains("Password can not include invalid characters.");
    assertThat(driver().getCurrentUrl()).endsWith("/auth/register");
    assertThat(UsersApi.loginStatus(customer.email(), "short"))
        .as("el cliente no debe existir")
        .isEqualTo(401);
  }
}

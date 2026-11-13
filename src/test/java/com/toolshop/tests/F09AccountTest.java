package com.toolshop.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.toolshop.api.ApiClient;
import com.toolshop.api.CartsApi;
import com.toolshop.api.InvoicesApi;
import com.toolshop.api.ProductsApi;
import com.toolshop.api.UsersApi;
import com.toolshop.components.Header;
import com.toolshop.data.Invoice;
import com.toolshop.data.Payment;
import com.toolshop.data.Product;
import com.toolshop.data.User;
import com.toolshop.pages.InvoiceDetailPage;
import com.toolshop.pages.InvoicesPage;
import com.toolshop.pages.ProfilePage;
import com.toolshop.session.Session;
import com.toolshop.support.Money;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

@Tag("F09")
@DisplayName("F09 Cuenta del cliente")
class F09AccountTest extends UiTest {

  @Test
  @Tag("smoke")
  @DisplayName("El cliente actualiza su perfil y el cambio persiste")
  void customerUpdatesProfile() {
    User customer = UsersApi.registerRandomCustomer();
    String token = UsersApi.login(customer);
    Session.authenticate(driver(), token);
    ProfilePage profile = new ProfilePage(driver()).open();
    assertThat(profile.firstName()).isEqualTo(customer.firstName());

    profile.changeName("Renamed", "Customer").changePhone("5559876543").save();

    assertThat(profile.successMessage()).isEqualTo("Your profile is successfully updated!");
    JsonNode me = ApiClient.anonymous().withToken(token).get("/users/me");
    SoftAssertions.assertSoftly(soft -> {
      soft.assertThat(me.path("first_name").asString()).isEqualTo("Renamed");
      soft.assertThat(me.path("last_name").asString()).isEqualTo("Customer");
      soft.assertThat(me.path("phone").asString()).isEqualTo("5559876543");
      soft.assertThat(me.path("email").asString())
          .as("el correo no cambia")
          .isEqualTo(customer.email());
    });

    profile.open();
    assertThat(profile.firstName()).isEqualTo("Renamed");
    assertThat(profile.lastName()).isEqualTo("Customer");
    assertThat(profile.phone()).isEqualTo("5559876543");
    assertThat(new Header(driver()).signedInUserName()).isEqualTo("Renamed Customer");
  }

  @Test
  @Tag("regression")
  @DisplayName("Un teléfono con letras se rechaza y el perfil no cambia")
  void invalidPhoneIsRejected() {
    User customer = UsersApi.registerRandomCustomer();
    String token = UsersApi.login(customer);
    Session.authenticate(driver(), token);
    ProfilePage profile = new ProfilePage(driver()).open();

    profile.changePhone("phone-abc").save();

    assertThat(profile.errorMessage())
        .isEqualTo("Please enter a valid phone number (digits, spaces and ( ) + - only).");
    assertThat(ApiClient.anonymous().withToken(token).get("/users/me").path("phone").asString())
        .isEqualTo(customer.phone());
  }

  @Test
  @Tag("smoke")
  @DisplayName("El pedido del cliente aparece en sus facturas con el detalle correcto")
  void customerSeesTheInvoiceOfTheirOrder() {
    // F09 crea su propio pedido por API para no depender de F06 (independencia de pruebas).
    User customer = UsersApi.registerRandomCustomer();
    String token = UsersApi.login(customer);
    List<Product> products = ProductsApi.plain(2);
    Map<Product, Integer> contents = Map.of(products.get(0), 2, products.get(1), 1);
    Invoice invoice =
        InvoicesApi.create(
            token, CartsApi.createWith(contents), customer.address(), Payment.cashOnDelivery());
    Session.authenticate(driver(), token);

    InvoicesPage invoices = new InvoicesPage(driver()).open();

    eventually(() ->
        assertThat(invoices.rows())
            .singleElement()
            .satisfies(row -> {
              assertThat(row).containsEntry("Invoice Number", invoice.number());
              assertThat(row).containsEntry("Billing Address", customer.address().street());
              assertThat(Money.parse(row.getOrDefault("Total", "")))
                  .isEqualByComparingTo(invoice.total());
            }));

    InvoiceDetailPage detail = invoices.openDetails(invoice.number());

    assertThat(detail.invoiceNumber()).isEqualTo(invoice.number());
    assertThat(detail.total()).isEqualByComparingTo(invoice.total());
    assertThat(detail.billingAddress())
        .usingRecursiveComparison()
        .ignoringFields("houseNumber")
        .isEqualTo(customer.address());
    assertThat(detail.paymentMethod()).isEqualTo("Cash on Delivery");
    assertThat(detail.lines())
        .extracting(line -> line.get("Product") + " x" + line.get("Quantity"))
        .containsExactlyInAnyOrder(products.get(0).name() + " x2", products.get(1).name() + " x1");
  }
}

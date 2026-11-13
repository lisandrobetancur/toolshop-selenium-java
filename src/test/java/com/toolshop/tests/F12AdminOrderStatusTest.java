package com.toolshop.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.toolshop.api.CartsApi;
import com.toolshop.api.InvoicesApi;
import com.toolshop.api.ProductsApi;
import com.toolshop.api.UsersApi;
import com.toolshop.data.Invoice;
import com.toolshop.data.Payment;
import com.toolshop.data.User;
import com.toolshop.pages.admin.AdminOrderPage;
import com.toolshop.session.Session;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * La UI del cliente no muestra el estado del pedido (ni en el listado ni en el detalle de
 * facturas), así que la verificación "desde el cliente" se hace con su propio token contra {@code
 * GET /invoices/{id}}. Se usan estados finales que la tarea programada de la aplicación (cada 10
 * minutos) no vuelve a mover: ON_HOLD y COMPLETED.
 */
@Tag("F12")
@DisplayName("F12 Administrador: estado de pedidos")
class F12AdminOrderStatusTest extends UiTest {

  @ParameterizedTest(name = "a {0}")
  @Tag("smoke")
  @ValueSource(strings = {"ON_HOLD", "COMPLETED"})
  @DisplayName("El administrador cambia el estado y el cliente lo ve en su pedido")
  void adminChangesStatusAndCustomerSeesIt(String newStatus) {
    Order order = Order.create();
    Session.authenticate(driver(), UsersApi.loginAsAdmin());
    AdminOrderPage page = new AdminOrderPage(driver()).open(order.invoice().id());
    assertThat(page.invoiceNumber()).isEqualTo(order.invoice().number());
    assertThat(page.currentStatus()).isEqualTo("AWAITING_FULFILLMENT");

    page.chooseStatus(newStatus).submitStatus();

    assertThat(page.successMessage()).isEqualTo("Status updated!");
    assertThat(InvoicesApi.get(order.customerToken(), order.invoice().id()).status())
        .as("estado visto por el cliente")
        .isEqualTo(newStatus);
  }

  @Test
  @Tag("regression")
  @DisplayName("Solo se puede avanzar de estado y repetir el actual se rechaza")
  void statusOnlyMovesForward() {
    Order order = Order.create();
    Session.authenticate(driver(), UsersApi.loginAsAdmin());
    AdminOrderPage page = new AdminOrderPage(driver()).open(order.invoice().id());

    assertThat(page.availableStatuses())
        .containsExactly("ON_HOLD", "AWAITING_SHIPMENT", "SHIPPED", "COMPLETED");

    page.submitStatus();

    assertThat(page.errorMessage()).isEqualTo("No new status selected.");
    assertThat(InvoicesApi.get(order.customerToken(), order.invoice().id()).status())
        .isEqualTo("AWAITING_FULFILLMENT");
  }

  /** Pedido recién creado por un cliente nuevo, preparado por API. */
  private record Order(String customerToken, Invoice invoice) {

    static Order create() {
      User customer = UsersApi.registerRandomCustomer();
      String token = UsersApi.login(customer);
      String cartId = CartsApi.createWith(Map.of(ProductsApi.plain(1).get(0), 1));
      return new Order(
          token, InvoicesApi.create(token, cartId, customer.address(), Payment.cashOnDelivery()));
    }
  }
}

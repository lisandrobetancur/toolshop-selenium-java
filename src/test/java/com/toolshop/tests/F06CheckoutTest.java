package com.toolshop.tests;

import static com.toolshop.tests.CheckoutAssertions.assertOrderMatches;
import static org.assertj.core.api.Assertions.assertThat;

import com.toolshop.api.InvoicesApi;
import com.toolshop.data.Invoice;
import com.toolshop.data.Payment;
import com.toolshop.pages.CartPage;
import com.toolshop.pages.CheckoutPage;
import com.toolshop.session.Session;
import io.qameta.allure.Epic;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Tienda")
@Tag("F06")
@DisplayName("F06 Checkout como cliente autenticado")
class F06CheckoutTest extends UiTest {

  @Test
  @Tag("smoke")
  @DisplayName("El cliente confirma dirección y pago y recibe un pedido con el total del carrito")
  void authenticatedCustomerCompletesCheckout() {
    CheckoutFixture fixture = CheckoutFixture.prepare(driver(), 2, 1);
    Payment payment = Payment.creditCard();

    new CartPage(driver()).open().proceedToCheckout();
    CheckoutPage checkout = new CheckoutPage(driver());

    assertThat(checkout.signedInGreeting())
        .isEqualTo(
            "Hello "
                + fixture.customer().fullName()
                + ", you are already logged in. You can proceed to checkout.");
    checkout.proceedAsSignedInCustomer();

    assertThat(checkout.billingAddress())
        .as("dirección precargada desde el perfil")
        .isEqualTo(fixture.customer().address());
    checkout.proceedFromAddress().choosePayment(payment);

    assertThat(checkout.checkPayment()).isEqualTo("Payment was successful");
    String invoiceNumber = checkout.confirmOrder();

    assertThat(checkout.confirmationMessage())
        .isEqualTo("Thanks for your order! Your invoice number is " + invoiceNumber + ".");
    List<Invoice> invoices = InvoicesApi.list(fixture.token());
    assertThat(invoices).as("el cliente tiene exactamente un pedido").hasSize(1);
    Invoice invoice = InvoicesApi.get(fixture.token(), invoices.get(0).id());
    assertThat(invoice.number()).isEqualTo(invoiceNumber);
    assertOrderMatches(invoice, fixture.contents(), fixture.customer().address(), payment.method());
    assertThat(invoice.paymentDetails()).containsAllEntriesOf(payment.details());
    assertThat(Session.cartId(driver())).as("el carrito se vacía tras la compra").isNull();
  }
}

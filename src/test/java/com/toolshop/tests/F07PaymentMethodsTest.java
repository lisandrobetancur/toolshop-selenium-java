package com.toolshop.tests;

import static com.toolshop.tests.CheckoutAssertions.assertOrderMatches;
import static org.assertj.core.api.Assertions.assertThat;

import com.toolshop.api.InvoicesApi;
import com.toolshop.data.Invoice;
import com.toolshop.data.Payment;
import com.toolshop.pages.CartPage;
import com.toolshop.pages.CheckoutPage;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("F07")
@DisplayName("F07 Checkout con distintos medios de pago")
class F07PaymentMethodsTest extends UiTest {

  static Stream<Payment> paymentMethods() {
    return Stream.of(
        Payment.bankTransfer(),
        Payment.cashOnDelivery(),
        Payment.creditCard(),
        Payment.buyNowPayLater(),
        Payment.giftCard());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("paymentMethods")
  @Tag("regression")
  @DisplayName("Cada medio de pago genera un pedido con sus datos de pago")
  void eachPaymentMethodCreatesAnOrder(Payment payment) {
    CheckoutFixture fixture = CheckoutFixture.prepare(driver(), 1);
    CheckoutPage checkout = reachPaymentStep();

    checkout.choosePayment(payment);
    assertThat(checkout.checkPayment()).isEqualTo("Payment was successful");
    String invoiceNumber = checkout.confirmOrder();

    List<Invoice> invoices = InvoicesApi.list(fixture.token());
    assertThat(invoices).hasSize(1);
    Invoice invoice = InvoicesApi.get(fixture.token(), invoices.get(0).id());
    assertThat(invoice.number()).isEqualTo(invoiceNumber);
    assertOrderMatches(invoice, fixture.contents(), fixture.customer().address(), payment.method());
    assertThat(invoice.paymentDetails()).containsAllEntriesOf(payment.details());
  }

  @Test
  @Tag("regression")
  @DisplayName("Con datos de tarjeta inválidos no se puede pagar ni se crea el pedido")
  void invalidCardDataBlocksThePayment() {
    CheckoutFixture fixture = CheckoutFixture.prepare(driver(), 1);
    Payment valid = Payment.creditCard();
    Payment invalid =
        new Payment(
            valid.method(),
            "Tarjeta inválida",
            Map.of(
                "credit_card_number", "4111 1111",
                "expiration_date", "01/2020",
                "cvv", "12",
                "card_holder_name", "Ana Tester"));
    CheckoutPage checkout = reachPaymentStep();

    checkout.choosePayment(invalid);

    assertThat(checkout.canSubmitPayment()).as("botón de pago habilitado").isFalse();
    assertThat(InvoicesApi.list(fixture.token())).isEmpty();
  }

  private static CheckoutPage reachPaymentStep() {
    new CartPage(driver()).open().proceedToCheckout();
    CheckoutPage checkout = new CheckoutPage(driver()).proceedAsSignedInCustomer();
    checkout.billingAddress();
    return checkout.proceedFromAddress();
  }
}

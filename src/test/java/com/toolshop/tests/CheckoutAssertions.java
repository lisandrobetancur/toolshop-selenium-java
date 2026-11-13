package com.toolshop.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.toolshop.data.Address;
import com.toolshop.data.Invoice;
import com.toolshop.data.Product;
import com.toolshop.support.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;

/** Verificaciones de negocio sobre el pedido creado por un checkout. */
final class CheckoutAssertions {

  private CheckoutAssertions() {}

  /**
   * Un pedido nace en AWAITING_FULFILLMENT, pero la tarea {@code order:update} de la aplicación
   * corre cada 10 minutos y lo mueve a ON_HOLD (transferencia bancaria) o a AWAITING_SHIPMENT
   * (resto). Si la prueba coincide con esa ejecución, el estado ya avanzó: ambos son correctos.
   */
  static List<String> initialStatuses(String paymentMethod) {
    return List.of(
        "AWAITING_FULFILLMENT",
        "bank-transfer".equals(paymentMethod) ? "ON_HOLD" : "AWAITING_SHIPMENT");
  }

  static void assertOrderMatches(
      Invoice invoice, Map<Product, Integer> contents, Address billing, String paymentMethod) {
    BigDecimal expectedTotal =
        contents.entrySet().stream()
            .map(e -> Money.times(e.getKey().price(), e.getValue()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    SoftAssertions.assertSoftly(soft -> {
      soft.assertThat(invoice.total()).as("total").isEqualByComparingTo(expectedTotal);
      soft.assertThat(invoice.status())
          .as("estado inicial (o el que asigna la tarea programada)")
          .isIn(initialStatuses(paymentMethod));
      soft.assertThat(invoice.paymentMethod()).as("medio de pago").isEqualTo(paymentMethod);
      soft.assertThat(invoice.billingAddress())
          .as("dirección de facturación")
          .usingRecursiveComparison()
          .ignoringFields("houseNumber")
          .isEqualTo(billing);
      soft.assertThat(invoice.lines())
          .as("líneas del pedido")
          .containsExactlyInAnyOrderElementsOf(
              contents.entrySet().stream()
                  .map(e ->
                      new Invoice.Line(
                          e.getKey().id(), e.getKey().name(), e.getValue(), e.getKey().price()))
                  .toList());
    });
    assertThat(invoice.number()).matches("INV-\\d+");
  }
}

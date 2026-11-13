package com.toolshop.data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Factura (pedido) tal como la devuelve la API. */
public record Invoice(
    String id,
    String number,
    String status,
    BigDecimal total,
    Address billingAddress,
    String paymentMethod,
    Map<String, String> paymentDetails,
    List<Line> lines) {

  /** Línea de la factura. */
  public record Line(String productId, String productName, int quantity, BigDecimal unitPrice) {}
}

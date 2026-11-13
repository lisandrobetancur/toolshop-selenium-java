package com.toolshop.api;

import com.toolshop.data.Address;
import com.toolshop.data.Invoice;
import com.toolshop.data.Payment;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/** Pedidos (facturas) por API: creación como precondición y consulta para verificar. */
public final class InvoicesApi {

  private InvoicesApi() {}

  /** Crea un pedido del cliente autenticado con el carrito y la dirección dados. */
  public static Invoice create(String token, String cartId, Address billing, Payment payment) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("billing_street", billing.street());
    body.put("billing_city", billing.city());
    body.put("billing_state", billing.state());
    body.put("billing_country", billing.country());
    body.put("billing_postal_code", billing.postalCode());
    body.put("payment_method", payment.method());
    body.put("payment_details", payment.details());
    body.put("cart_id", cartId);
    String id =
        ApiClient.anonymous().withToken(token).post("/invoices", body).path("id").asString();
    return get(token, id);
  }

  public static Invoice get(String token, String invoiceId) {
    return toInvoice(ApiClient.anonymous().withToken(token).get("/invoices/" + invoiceId));
  }

  /**
   * Pedidos visibles para el token (todos si es administrador), del más reciente al más antiguo.
   */
  public static List<Invoice> list(String token) {
    List<Invoice> invoices = new ArrayList<>();
    for (JsonNode node : ApiClient.anonymous().withToken(token).get("/invoices").path("data")) {
      invoices.add(toInvoice(node));
    }
    return invoices;
  }

  private static Invoice toInvoice(JsonNode node) {
    List<Invoice.Line> lines = new ArrayList<>();
    for (JsonNode line : node.path("invoicelines")) {
      lines.add(
          new Invoice.Line(
              line.path("product_id").asString(),
              line.path("product").path("name").asString(),
              line.path("quantity").asInt(),
              line.path("unit_price").decimalValue().setScale(2, RoundingMode.HALF_UP)));
    }
    Map<String, String> details = new LinkedHashMap<>();
    node.path("payment")
        .path("payment_details")
        .properties()
        .forEach(e -> details.put(e.getKey(), e.getValue().asString()));
    return new Invoice(
        node.path("id").asString(),
        node.path("invoice_number").asString(),
        node.path("status").asString(),
        node.path("total").decimalValue().setScale(2, RoundingMode.HALF_UP),
        new Address(
            node.path("billing_street").asString(),
            "",
            node.path("billing_city").asString(),
            node.path("billing_state").asString(),
            node.path("billing_country").asString(),
            node.path("billing_postal_code").asString()),
        node.path("payment").path("payment_method").asString(),
        details,
        lines);
  }
}

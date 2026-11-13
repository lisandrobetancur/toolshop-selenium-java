package com.toolshop.data;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Medio de pago del checkout. {@code details} usa los mismos nombres que el formulario ({@code
 * data-test}) y que la API ({@code payment_details}).
 */
public record Payment(String method, String label, Map<String, String> details) {

  public static Payment bankTransfer() {
    return new Payment(
        "bank-transfer",
        "Transferencia bancaria",
        ordered(
            "bank_name", "Test Bank", "account_name", "Ana Tester", "account_number", "123456789"));
  }

  public static Payment cashOnDelivery() {
    return new Payment("cash-on-delivery", "Contra entrega", Map.of());
  }

  public static Payment creditCard() {
    String expiration =
        YearMonth.now(ZoneOffset.UTC).plusYears(2).format(DateTimeFormatter.ofPattern("MM/yyyy"));
    return new Payment(
        "credit-card",
        "Tarjeta de crédito",
        ordered(
            "credit_card_number", "4111-1111-1111-1111",
            "expiration_date", expiration,
            "cvv", "123",
            "card_holder_name", "Ana Tester"));
  }

  /** Compra ahora y paga después; {@code monthly_installments} es una lista desplegable. */
  public static Payment buyNowPayLater() {
    return new Payment("buy-now-pay-later", "Paga después", Map.of("monthly_installments", "3"));
  }

  public static Payment giftCard() {
    return new Payment(
        "gift-card",
        "Tarjeta de regalo",
        ordered("gift_card_number", "1234567890ABCDEF", "validation_code", "1A2B"));
  }

  @Override
  public String toString() {
    return label;
  }

  private static Map<String, String> ordered(String... keyValues) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      map.put(keyValues[i], keyValues[i + 1]);
    }
    return map;
  }
}

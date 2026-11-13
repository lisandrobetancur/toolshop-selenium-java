package com.toolshop.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Importes en dólares tal como los muestra Toolshop (por ejemplo {@code $1,234.50}). */
public final class Money {

  private Money() {}

  public static BigDecimal parse(String text) {
    String digits = text.replaceAll("[^0-9.]", "");
    if (digits.isEmpty()) {
      throw new IllegalArgumentException("No hay un importe en: '" + text + "'");
    }
    return new BigDecimal(digits).setScale(2, RoundingMode.HALF_UP);
  }

  public static BigDecimal of(String amount) {
    return new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
  }

  public static BigDecimal times(BigDecimal unitPrice, int quantity) {
    return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
  }
}

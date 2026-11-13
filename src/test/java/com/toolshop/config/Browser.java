package com.toolshop.config;

import java.util.Locale;

/** Navegadores soportados; se eligen con la variable de entorno {@code BROWSER}. */
public enum Browser {
  CHROME,
  FIREFOX,
  EDGE,
  SAFARI;

  public static Browser from(String value) {
    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "BROWSER no soportado: '" + value + "'. Usa chrome, firefox, edge o safari.", e);
    }
  }
}

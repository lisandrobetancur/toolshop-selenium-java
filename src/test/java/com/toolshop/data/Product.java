package com.toolshop.data;

import java.math.BigDecimal;

/** Producto del catálogo tal como lo devuelve la API. */
public record Product(
    String id,
    String name,
    BigDecimal price,
    boolean inStock,
    boolean rental,
    boolean locationOffer,
    boolean ecoFriendly,
    String categoryId,
    String categoryName,
    String brandId,
    String brandName) {

  /** Producto sin descuentos ni reglas especiales: su total es precio por cantidad. */
  public boolean isPlain() {
    return inStock && !rental && !locationOffer && !ecoFriendly;
  }
}

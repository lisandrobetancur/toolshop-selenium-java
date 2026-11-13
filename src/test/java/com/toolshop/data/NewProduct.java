package com.toolshop.data;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/** Datos para dar de alta un producto desde el panel de administración. */
public record NewProduct(
    String name,
    String description,
    BigDecimal price,
    int stock,
    String categoryId,
    String brandId,
    String imageId) {

  /**
   * Nombre único por ejecución. La búsqueda de Toolshop usa FULLTEXT de MySQL en modo booleano, que
   * ignora palabras de menos de 3 caracteres: el sufijo es una palabra de 10 letras.
   */
  public static String uniqueName(String prefix) {
    StringBuilder suffix = new StringBuilder();
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < 10; i++) {
      suffix.append((char) ('a' + random.nextInt(26)));
    }
    return prefix + " " + Character.toUpperCase(suffix.charAt(0)) + suffix.substring(1);
  }

  public NewProduct withName(String newName) {
    return new NewProduct(newName, description, price, stock, categoryId, brandId, imageId);
  }

  public NewProduct withPrice(BigDecimal newPrice) {
    return new NewProduct(name, description, newPrice, stock, categoryId, brandId, imageId);
  }
}

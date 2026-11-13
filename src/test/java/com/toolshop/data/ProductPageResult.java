package com.toolshop.data;

import java.util.List;

/** Una página de resultados del catálogo. */
public record ProductPageResult(List<Product> products, int total, int lastPage) {

  public List<String> names() {
    return products.stream().map(Product::name).toList();
  }
}

package com.toolshop.api;

import com.toolshop.data.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/** Favoritos del cliente autenticado. */
public final class FavoritesApi {

  private FavoritesApi() {}

  public static void add(String token, Product product) {
    ApiClient.anonymous().withToken(token).post("/favorites", Map.of("product_id", product.id()));
  }

  /** Identificadores de los productos marcados como favoritos. */
  public static List<String> productIds(String token) {
    List<String> ids = new ArrayList<>();
    for (JsonNode favorite : ApiClient.anonymous().withToken(token).get("/favorites")) {
      ids.add(favorite.path("product_id").asString());
    }
    return ids;
  }
}

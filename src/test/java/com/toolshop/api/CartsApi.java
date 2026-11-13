package com.toolshop.api;

import com.toolshop.data.CartItem;
import com.toolshop.data.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/** Carritos por API: permite llegar al carrito o al checkout con productos ya cargados. */
public final class CartsApi {

  private CartsApi() {}

  /** Crea un carrito con las cantidades indicadas y devuelve su identificador. */
  public static String createWith(Map<Product, Integer> quantities) {
    ApiClient api = ApiClient.anonymous();
    String cartId = api.post("/carts", null).path("id").asString();
    quantities.forEach((product, quantity) ->
        api.post("/carts/" + cartId, Map.of("product_id", product.id(), "quantity", quantity)));
    return cartId;
  }

  public static List<CartItem> items(String cartId) {
    List<CartItem> items = new ArrayList<>();
    for (JsonNode item : ApiClient.anonymous().get("/carts/" + cartId).path("cart_items")) {
      items.add(
          new CartItem(
              item.path("product_id").asString(),
              item.path("product").path("name").asString(),
              item.path("quantity").asInt()));
    }
    return items;
  }
}

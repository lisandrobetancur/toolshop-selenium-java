package com.toolshop.tests;

import com.toolshop.api.CartsApi;
import com.toolshop.api.ProductsApi;
import com.toolshop.api.UsersApi;
import com.toolshop.data.Product;
import com.toolshop.data.User;
import com.toolshop.session.Session;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.WebDriver;

/**
 * Precondición común de los checkouts: cliente nuevo con sesión inyectada y carrito cargado por
 * API, todo sin pasar por la UI.
 */
record CheckoutFixture(User customer, String token, String cartId, Map<Product, Integer> contents) {

  static CheckoutFixture prepare(WebDriver driver, int... quantities) {
    User customer = UsersApi.registerRandomCustomer();
    String token = UsersApi.login(customer);
    List<Product> products = ProductsApi.plain(quantities.length);
    Map<Product, Integer> contents = new LinkedHashMap<>();
    for (int i = 0; i < quantities.length; i++) {
      contents.put(products.get(i), quantities[i]);
    }
    String cartId = CartsApi.createWith(contents);
    Session.authenticate(driver, token);
    Session.useCart(driver, cartId, contents.values().stream().mapToInt(Integer::intValue).sum());
    return new CheckoutFixture(customer, token, cartId, contents);
  }
}

package com.toolshop.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.toolshop.api.CartsApi;
import com.toolshop.api.ProductsApi;
import com.toolshop.components.CartTable.Line;
import com.toolshop.components.Header;
import com.toolshop.components.Toast;
import com.toolshop.data.CartItem;
import com.toolshop.data.Product;
import com.toolshop.pages.CartPage;
import com.toolshop.session.Session;
import com.toolshop.support.Money;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("F05")
@DisplayName("F05 Carrito")
class F05CartTest extends UiTest {

  @Test
  @Tag("smoke")
  @DisplayName("El carrito muestra cada línea y el total es la suma de precio por cantidad")
  void cartShowsLinesAndTotal() {
    Map<Product, Integer> contents = contents(2, 1, 3);
    CartPage cart = openCartWith(contents);

    eventually(() ->
        assertThat(cart.table().lines()).containsExactlyInAnyOrderElementsOf(lines(contents)));
    assertThat(cart.table().total()).isEqualByComparingTo(total(contents));
    assertThat(new Header(driver()).cartQuantity()).isEqualTo(6);
  }

  @Test
  @Tag("smoke")
  @DisplayName("Al editar una cantidad se recalculan la línea, el total y el indicador del carrito")
  void editingQuantityRecalculatesTotals() {
    Map<Product, Integer> contents = contents(2, 1);
    Product edited = contents.keySet().iterator().next();
    String cartId = CartsApi.createWith(contents);
    CartPage cart = openCart(cartId, contents);

    cart.table().setQuantity(edited.name(), 5);
    contents.put(edited, 5);

    new Toast(driver()).waitFor("Product quantity updated.");
    eventually(() ->
        assertThat(cart.table().lines()).containsExactlyInAnyOrderElementsOf(lines(contents)));
    eventually(() -> assertThat(cart.table().total()).isEqualByComparingTo(total(contents)));
    new Header(driver()).waitForCartQuantity(6);
    assertThat(CartsApi.items(cartId)).containsExactlyInAnyOrderElementsOf(items(contents));
  }

  @Test
  @Tag("regression")
  @DisplayName("La cantidad máxima por producto es 99 y el carrito lo avisa")
  void quantityIsCappedAtTheMaximum() {
    Map<Product, Integer> contents = contents(1);
    Product product = contents.keySet().iterator().next();
    String cartId = CartsApi.createWith(contents);
    CartPage cart = openCart(cartId, contents);

    cart.table().setQuantity(product.name(), 150);
    contents.put(product, 99);

    new Toast(driver()).waitFor("You can order at most 99 of this product.");
    eventually(() -> assertThat(cart.table().lines()).containsExactlyElementsOf(lines(contents)));
    eventually(() -> assertThat(cart.table().total()).isEqualByComparingTo(total(contents)));
    assertThat(CartsApi.items(cartId)).containsExactlyElementsOf(items(contents));
  }

  @Test
  @Tag("smoke")
  @DisplayName("Al eliminar un producto se recalcula el total y al quitar el último queda vacío")
  void removingItemsRecalculatesAndEmptiesTheCart() {
    Map<Product, Integer> contents = contents(2, 3);
    List<Product> products = List.copyOf(contents.keySet());
    String cartId = CartsApi.createWith(contents);
    CartPage cart = openCart(cartId, contents);

    cart.table().remove(products.get(0).name());
    contents.remove(products.get(0));

    new Toast(driver()).waitFor("Product deleted.");
    eventually(() -> assertThat(cart.table().lines()).containsExactlyElementsOf(lines(contents)));
    eventually(() -> assertThat(cart.table().total()).isEqualByComparingTo(total(contents)));
    new Header(driver()).waitForCartQuantity(3);
    assertThat(CartsApi.items(cartId)).containsExactlyElementsOf(items(contents));

    cart.table().remove(products.get(1).name());

    eventually(() -> assertThat(cart.isEmpty()).as("aviso de carrito vacío").isTrue());
    assertThat(CartsApi.items(cartId)).isEmpty();
  }

  /** Productos sin descuentos con las cantidades dadas, en orden. */
  private static Map<Product, Integer> contents(int... quantities) {
    List<Product> products = ProductsApi.plain(quantities.length);
    Map<Product, Integer> contents = new LinkedHashMap<>();
    for (int i = 0; i < quantities.length; i++) {
      contents.put(products.get(i), quantities[i]);
    }
    return contents;
  }

  private static CartPage openCartWith(Map<Product, Integer> contents) {
    return openCart(CartsApi.createWith(contents), contents);
  }

  private static CartPage openCart(String cartId, Map<Product, Integer> contents) {
    int units = contents.values().stream().mapToInt(Integer::intValue).sum();
    Session.useCart(driver(), cartId, units);
    return new CartPage(driver()).open();
  }

  private static List<Line> lines(Map<Product, Integer> contents) {
    return contents.entrySet().stream()
        .map(e ->
            new Line(
                e.getKey().name(),
                e.getValue(),
                e.getKey().price(),
                Money.times(e.getKey().price(), e.getValue())))
        .toList();
  }

  private static List<CartItem> items(Map<Product, Integer> contents) {
    return contents.entrySet().stream()
        .map(e -> new CartItem(e.getKey().id(), e.getKey().name(), e.getValue()))
        .toList();
  }

  private static BigDecimal total(Map<Product, Integer> contents) {
    return contents.entrySet().stream()
        .map(e -> Money.times(e.getKey().price(), e.getValue()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}

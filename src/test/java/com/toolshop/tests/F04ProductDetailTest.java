package com.toolshop.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.toolshop.api.CartsApi;
import com.toolshop.api.ProductsApi;
import com.toolshop.components.Header;
import com.toolshop.components.Toast;
import com.toolshop.data.CartItem;
import com.toolshop.data.Product;
import com.toolshop.pages.ProductPage;
import com.toolshop.session.Session;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("F04")
@DisplayName("F04 Detalle de producto")
class F04ProductDetailTest extends UiTest {

  @Test
  @Tag("smoke")
  @DisplayName("Se ajusta la cantidad y el producto llega al carrito con esa cantidad")
  void changedQuantityIsAddedToTheCart() {
    Product product = ProductsApi.plain(1).get(0);
    ProductPage page = new ProductPage(driver()).open(product.id());

    SoftAssertions.assertSoftly(soft -> {
      soft.assertThat(page.name()).isEqualTo(product.name());
      soft.assertThat(page.unitPrice()).isEqualByComparingTo(product.price());
      soft.assertThat(page.category()).isEqualTo(product.categoryName());
      soft.assertThat(page.brand()).isEqualTo(product.brandName());
      soft.assertThat(page.quantity()).as("cantidad inicial").isEqualTo(1);
    });

    page.increaseQuantity(3);
    assertThat(page.quantity()).isEqualTo(4);
    page.decreaseQuantity(1);
    assertThat(page.quantity()).isEqualTo(3);

    page.addToCart();

    new Toast(driver()).waitFor("Product added to shopping cart.");
    new Header(driver()).waitForCartQuantity(3);
    assertThat(cartItems()).containsExactly(new CartItem(product.id(), product.name(), 3));
  }

  @Test
  @Tag("regression")
  @DisplayName("La cantidad escrita a mano se respeta y agregar dos veces acumula unidades")
  void typedQuantityAccumulatesInTheCart() {
    Product product = ProductsApi.plain(1).get(0);
    ProductPage page = new ProductPage(driver()).open(product.id());
    Header header = new Header(driver());

    page.setQuantity(5).addToCart();
    header.waitForCartQuantity(5);

    page.setQuantity(2).addToCart();
    header.waitForCartQuantity(7);

    assertThat(cartItems()).containsExactly(new CartItem(product.id(), product.name(), 7));
  }

  @Test
  @Tag("regression")
  @DisplayName("La cantidad nunca baja de una unidad")
  void quantityCannotGoBelowOne() {
    Product product = ProductsApi.plain(1).get(0);
    ProductPage page = new ProductPage(driver()).open(product.id());

    page.decreaseQuantity(2);

    assertThat(page.quantity()).isEqualTo(1);
  }

  @Test
  @Tag("regression")
  @DisplayName("Un producto sin stock no se puede agregar al carrito")
  void outOfStockProductCannotBeAdded() {
    Product product = ProductsApi.find(1, p -> !p.inStock() && !p.rental()).get(0);
    ProductPage page = new ProductPage(driver()).open(product.id());

    assertThat(page.outOfStockMessage()).isEqualTo("Out of stock");
    assertThat(page.canAddToCart()).isFalse();
    assertThat(Session.cartId(driver())).as("no debe crearse un carrito").isNull();
  }

  @Test
  @Tag("regression")
  @DisplayName("Los productos relacionados coinciden con el catálogo y llevan a su detalle")
  void relatedProductsLeadToTheirDetail() {
    Product product = ProductsApi.plain(1).get(0);
    List<Product> related = ProductsApi.related(product.id());
    assertThat(related).as("el producto debe tener relacionados").isNotEmpty();
    ProductPage page = new ProductPage(driver()).open(product.id());

    eventually(() ->
        assertThat(page.relatedProductNames())
            .containsExactlyElementsOf(related.stream().map(Product::name).toList()));
    assertThat(related)
        .as("los relacionados comparten categoría con el producto")
        .allSatisfy(r -> assertThat(r.categoryName()).isEqualTo(product.categoryName()));

    Product target = related.get(0);
    page.openRelated(target.name());

    assertThat(driver().getCurrentUrl()).endsWith("/product/" + target.id());
    assertThat(page.unitPrice()).isEqualByComparingTo(target.price());
  }

  private static List<CartItem> cartItems() {
    String cartId = Session.cartId(driver());
    assertThat(cartId).as("carrito asociado al navegador").isNotNull();
    return CartsApi.items(String.valueOf(cartId));
  }
}

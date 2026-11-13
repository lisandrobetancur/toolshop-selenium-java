package com.toolshop.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.toolshop.api.FavoritesApi;
import com.toolshop.api.ProductsApi;
import com.toolshop.api.UsersApi;
import com.toolshop.components.Toast;
import com.toolshop.data.Product;
import com.toolshop.pages.FavoritesPage;
import com.toolshop.pages.ProductPage;
import com.toolshop.session.Session;
import io.qameta.allure.Epic;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Tienda")
@Tag("F08")
@DisplayName("F08 Favoritos")
class F08FavoritesTest extends UiTest {

  @Test
  @Tag("smoke")
  @DisplayName("Se agrega un favorito desde el detalle, se lista y se elimina")
  void addListAndRemoveFavorites() {
    String token = UsersApi.login(UsersApi.registerRandomCustomer());
    List<Product> products = ProductsApi.plain(2);
    Product fromUi = products.get(0);
    Product fromApi = products.get(1);
    FavoritesApi.add(token, fromApi);
    Session.authenticate(driver(), token);

    new ProductPage(driver()).open(fromUi.id()).addToFavorites();
    new Toast(driver()).waitFor("Product added to your favorites list.");

    FavoritesPage favorites = new FavoritesPage(driver()).open();
    eventually(() ->
        assertThat(favorites.productNames())
            .containsExactlyInAnyOrder(fromUi.name(), fromApi.name()));
    assertThat(FavoritesApi.productIds(token)).containsExactlyInAnyOrder(fromUi.id(), fromApi.id());

    favorites.remove(fromUi.name());

    eventually(() -> assertThat(favorites.productNames()).containsExactly(fromApi.name()));
    assertThat(FavoritesApi.productIds(token)).containsExactly(fromApi.id());

    favorites.remove(fromApi.name());

    eventually(() -> assertThat(favorites.showsEmptyMessage()).isTrue());
    assertThat(FavoritesApi.productIds(token)).isEmpty();
  }

  @Test
  @Tag("regression")
  @DisplayName("Un producto ya favorito no se duplica")
  void duplicateFavoriteIsRejected() {
    String token = UsersApi.login(UsersApi.registerRandomCustomer());
    Product product = ProductsApi.plain(1).get(0);
    FavoritesApi.add(token, product);
    Session.authenticate(driver(), token);

    new ProductPage(driver()).open(product.id()).addToFavorites();

    new Toast(driver()).waitFor("Product already in your favorites list.");
    assertThat(FavoritesApi.productIds(token)).containsExactly(product.id());
  }

  @Test
  @Tag("regression")
  @DisplayName("Sin sesión no se pueden agregar favoritos")
  void anonymousVisitorCannotAddFavorites() {
    Product product = ProductsApi.plain(1).get(0);

    new ProductPage(driver()).open(product.id()).addToFavorites();

    new Toast(driver()).waitFor("Unauthorized, can not add product to your favorite list.");
  }
}

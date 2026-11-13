package com.toolshop.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.toolshop.api.ProductsApi;
import com.toolshop.api.UsersApi;
import com.toolshop.data.NewProduct;
import com.toolshop.data.Product;
import com.toolshop.data.ProductPageResult;
import com.toolshop.pages.CatalogPage;
import com.toolshop.pages.ProductPage;
import com.toolshop.pages.admin.AdminProductFormPage;
import com.toolshop.session.Session;
import com.toolshop.support.Money;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Los productos creados quedan en el entorno compartido hasta el reinicio horario de la base de
 * datos; el nombre único por ejecución evita colisiones entre pruebas paralelas.
 */
@Tag("F11")
@DisplayName("F11 Administrador: productos")
class F11AdminProductTest extends UiTest {

  @Test
  @Tag("smoke")
  @DisplayName("El administrador crea un producto y aparece en el catálogo")
  void adminCreatesProductVisibleInCatalog() {
    Map.Entry<String, String> brand = ProductsApi.brands().entrySet().iterator().next();
    NewProduct product =
        new NewProduct(
            NewProduct.uniqueName("Selenium Hammer"),
            "Product created by the Selenium E2E suite.",
            Money.of("42.50"),
            12,
            ProductsApi.categoryId("Hammer"),
            brand.getValue(),
            ProductsApi.imageId("Claw Hammer"));
    Session.authenticate(driver(), UsersApi.loginAsAdmin());

    String saved = new AdminProductFormPage(driver()).openNew().fill(product).save();

    assertThat(saved).isEqualTo("Product saved!");
    ProductPageResult found = ProductsApi.search(product.name());
    assertThat(found.products()).as("producto guardado en la API").hasSize(1);
    Product created = found.products().get(0);
    assertThat(created.price()).isEqualByComparingTo(product.price());
    assertThat(created.categoryName()).isEqualTo("Hammer");
    assertThat(created.brandName()).isEqualTo(brand.getKey());

    CatalogPage catalog = new CatalogPage(driver()).open().searchFor(product.name());

    eventually(() -> assertThat(catalog.productNames()).containsExactly(product.name()));
    assertThat(catalog.productPrices()).containsExactly(product.price());

    ProductPage detail = catalog.openProduct(created.id());
    assertThat(detail.name()).isEqualTo(product.name());
    assertThat(detail.unitPrice()).isEqualByComparingTo(product.price());
    assertThat(detail.category()).isEqualTo("Hammer");
    assertThat(detail.brand()).isEqualTo(brand.getKey());
    assertThat(detail.canAddToCart()).as("producto con stock").isTrue();
  }

  @Test
  @Tag("regression")
  @DisplayName("El administrador edita nombre y precio y el catálogo refleja el cambio")
  void adminEditsProductAndCatalogReflectsIt() {
    String adminToken = UsersApi.loginAsAdmin();
    NewProduct data =
        new NewProduct(
            NewProduct.uniqueName("Selenium Pliers"),
            "Product created by the Selenium E2E suite.",
            Money.of("19.99"),
            5,
            ProductsApi.categoryId("Pliers"),
            ProductsApi.brands().values().iterator().next(),
            ProductsApi.imageId("Combination pliers"));
    Product original = ProductsApi.create(adminToken, data);
    String newName = NewProduct.uniqueName("Selenium Pliers Edited");
    Session.authenticate(driver(), adminToken);

    String saved =
        new AdminProductFormPage(driver())
            .openExisting(original.id())
            .fill(data.withName(newName).withPrice(Money.of("24.75")))
            .save();

    assertThat(saved).isEqualTo("Product saved!");
    Product updated = ProductsApi.get(original.id());
    assertThat(updated.name()).isEqualTo(newName);
    assertThat(updated.price()).isEqualByComparingTo("24.75");

    CatalogPage catalog = new CatalogPage(driver()).open().searchFor(newName);

    eventually(() -> assertThat(catalog.productNames()).containsExactly(newName));
    assertThat(catalog.productPrices()).containsExactly(Money.of("24.75"));
    assertThat(ProductsApi.search(original.name()).products())
        .as("el nombre anterior ya no aparece")
        .isEmpty();
  }
}

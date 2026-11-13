package com.toolshop.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.toolshop.api.ProductsApi;
import com.toolshop.data.Product;
import com.toolshop.data.ProductPageResult;
import com.toolshop.pages.CatalogPage;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("F03")
@DisplayName("F03 Catálogo")
class F03CatalogTest extends UiTest {

  @Test
  @Tag("smoke")
  @DisplayName("La búsqueda muestra solo los productos que coinciden con el término")
  void searchShowsMatchingProducts() {
    String term = "Hammer";
    ProductPageResult expected = ProductsApi.search(term);
    assertThat(expected.products()).as("el catálogo debe tener coincidencias").isNotEmpty();
    CatalogPage catalog = new CatalogPage(driver()).open();

    catalog.searchFor(term);

    eventually(
        () -> assertThat(catalog.productNames()).containsExactlyElementsOf(expected.names()));
    assertThat(catalog.productNames())
        .allSatisfy(name -> assertThat(name).containsIgnoringCase(term));
    assertThat(catalog.searchCaption()).isEqualTo("Searched for: " + term);
    assertThat(catalog.searchResultCount())
        .isEqualTo(expected.total() + " products found for '" + term + "'");
  }

  @Test
  @Tag("regression")
  @DisplayName("Una búsqueda sin coincidencias lo informa y al limpiarla vuelve el catálogo")
  void searchWithoutMatchesShowsEmptyState() {
    CatalogPage catalog = new CatalogPage(driver()).open();

    catalog.searchFor("zzz-no-such-tool");

    assertThat(catalog.noResultsMessage()).isEqualTo("There are no products found.");
    assertThat(catalog.productNames()).isEmpty();

    catalog.resetSearch();

    List<String> firstPage = ProductsApi.storefront(1, Map.of()).names();
    eventually(() -> assertThat(catalog.productNames()).containsExactlyElementsOf(firstPage));
  }

  @Test
  @Tag("smoke")
  @DisplayName("El filtro por categoría muestra solo productos de esa categoría")
  void categoryFilterShowsOnlyThatCategory() {
    String categoryId = ProductsApi.categoryId("Hammer");
    ProductPageResult expected = ProductsApi.storefront(1, Map.of("by_category", categoryId));
    assertThat(expected.products())
        .isNotEmpty()
        .allSatisfy(product -> assertThat(product.categoryName()).isEqualTo("Hammer"));
    List<String> unfiltered = ProductsApi.storefront(1, Map.of()).names();
    CatalogPage catalog = new CatalogPage(driver()).open();

    catalog.toggleCategory(categoryId);

    eventually(
        () -> assertThat(catalog.productNames()).containsExactlyElementsOf(expected.names()));

    catalog.toggleCategory(categoryId);

    eventually(() -> assertThat(catalog.productNames()).containsExactlyElementsOf(unfiltered));
  }

  @Test
  @Tag("regression")
  @DisplayName("El filtro por marca muestra solo productos de esa marca")
  void brandFilterShowsOnlyThatBrand() {
    Map.Entry<String, String> brand = ProductsApi.brands().entrySet().iterator().next();
    ProductPageResult expected = ProductsApi.storefront(1, Map.of("by_brand", brand.getValue()));
    assertThat(expected.products())
        .isNotEmpty()
        .allSatisfy(product -> assertThat(product.brandName()).isEqualTo(brand.getKey()));
    CatalogPage catalog = new CatalogPage(driver()).open();

    catalog.toggleBrand(brand.getValue());

    eventually(
        () -> assertThat(catalog.productNames()).containsExactlyElementsOf(expected.names()));
  }

  @Test
  @Tag("regression")
  @DisplayName("Categoría y marca combinadas muestran la intersección de ambos filtros")
  void categoryAndBrandFiltersCombine() {
    String categoryId = ProductsApi.categoryId("Pliers");
    Product sample = ProductsApi.storefront(1, Map.of("by_category", categoryId)).products().get(0);
    ProductPageResult expected =
        ProductsApi.storefront(1, Map.of("by_category", categoryId, "by_brand", sample.brandId()));
    assertThat(expected.products())
        .isNotEmpty()
        .allSatisfy(product -> {
          assertThat(product.categoryName()).isEqualTo("Pliers");
          assertThat(product.brandName()).isEqualTo(sample.brandName());
        });
    CatalogPage catalog = new CatalogPage(driver()).open();

    catalog.toggleCategory(categoryId).toggleBrand(sample.brandId());

    eventually(
        () -> assertThat(catalog.productNames()).containsExactlyElementsOf(expected.names()));
  }

  @ParameterizedTest(name = "orden {0}")
  @Tag("regression")
  @DisplayName("El catálogo se ordena por nombre y por precio, ascendente y descendente")
  @ValueSource(strings = {"name,asc", "name,desc", "price,asc", "price,desc"})
  void sortingOrdersTheCatalog(String criterion) {
    List<String> expected = ProductsApi.storefront(1, Map.of("sort", criterion)).names();
    CatalogPage catalog = new CatalogPage(driver()).open();

    catalog.sortBy(criterion);

    eventually(() -> assertThat(catalog.productNames()).containsExactlyElementsOf(expected));
    switch (criterion) {
      case "name,asc" ->
          assertThat(catalog.productNames()).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
      case "name,desc" ->
          assertThat(catalog.productNames())
              .isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER.reversed());
      case "price,asc" ->
          assertThat(catalog.productPrices()).isSortedAccordingTo(Comparator.naturalOrder());
      default ->
          assertThat(catalog.productPrices())
              .isSortedAccordingTo(Comparator.<BigDecimal>naturalOrder().reversed());
    }
  }

  @Test
  @Tag("regression")
  @DisplayName("La paginación recorre el catálogo sin repetir productos")
  void paginationWalksThroughTheCatalog() {
    ProductPageResult first = ProductsApi.storefront(1, Map.of());
    ProductPageResult second = ProductsApi.storefront(2, Map.of());
    ProductPageResult third = ProductsApi.storefront(3, Map.of());
    assertThat(first.lastPage()).as("el catálogo debe tener varias páginas").isGreaterThan(3);
    assertThat(second.names()).doesNotContainAnyElementsOf(first.names());
    CatalogPage catalog = new CatalogPage(driver()).open();
    eventually(() -> assertThat(catalog.productNames()).containsExactlyElementsOf(first.names()));

    catalog.pagination().goTo(2);
    eventually(() -> assertThat(catalog.productNames()).containsExactlyElementsOf(second.names()));

    catalog.pagination().next();
    eventually(() -> assertThat(catalog.productNames()).containsExactlyElementsOf(third.names()));

    catalog.pagination().previous();
    eventually(() -> assertThat(catalog.productNames()).containsExactlyElementsOf(second.names()));

    // La última página es donde aparecen los productos que otros usuarios crean en el entorno
    // compartido: se verifica que sea distinta de las anteriores, no su contenido exacto.
    catalog.pagination().goTo(first.lastPage());
    eventually(() ->
        assertThat(catalog.productNames())
            .isNotEmpty()
            .doesNotContainAnyElementsOf(first.names())
            .doesNotContainAnyElementsOf(second.names())
            .doesNotContainAnyElementsOf(third.names()));
  }
}

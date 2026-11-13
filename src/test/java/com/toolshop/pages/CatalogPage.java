package com.toolshop.pages;

import com.toolshop.components.Pagination;
import com.toolshop.support.Money;
import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Portada con el catálogo de productos: búsqueda, filtros, orden y paginación. */
public final class CatalogPage extends BasePage {

  private static final By PRODUCT_NAMES = byTest("product-name");
  private static final By PRODUCT_PRICES = byTest("product-price");
  private static final By SORT = byTest("sort");
  private static final By SEARCH_QUERY = byTest("search-query");
  private static final By SEARCH_SUBMIT = byTest("search-submit");
  private static final By SEARCH_RESET = byTest("search-reset");
  private static final By SEARCH_CAPTION = byTest("search-caption");
  private static final By SEARCH_RESULT_COUNT = byTest("search-result-count");
  private static final By NO_RESULTS = byTest("no-results");

  private final Pagination pagination;

  public CatalogPage(WebDriver driver) {
    super(driver);
    this.pagination = new Pagination(driver);
  }

  public CatalogPage open() {
    navigateTo("/");
    waits.visible(PRODUCT_NAMES);
    return this;
  }

  public CatalogPage searchFor(String term) {
    type(SEARCH_QUERY, term);
    click(SEARCH_SUBMIT);
    return this;
  }

  public CatalogPage resetSearch() {
    click(SEARCH_RESET);
    return this;
  }

  /** Marca o desmarca el filtro de una categoría. */
  public CatalogPage toggleCategory(String categoryId) {
    click(byTest("category-" + categoryId));
    return this;
  }

  /** Marca o desmarca el filtro de una marca. */
  public CatalogPage toggleBrand(String brandId) {
    click(byTest("brand-" + brandId));
    return this;
  }

  /** Ordena con un criterio de la aplicación, por ejemplo {@code name,asc} o {@code price,desc}. */
  public CatalogPage sortBy(String criterion) {
    selectByValue(SORT, criterion);
    return this;
  }

  public Pagination pagination() {
    return pagination;
  }

  public ProductPage openProduct(String productId) {
    click(byTest("product-" + productId));
    return new ProductPage(driver);
  }

  /** Nombres de los productos visibles, en el orden en que se muestran. */
  public List<String> productNames() {
    return driver.findElements(PRODUCT_NAMES).stream().map(e -> e.getText().trim()).toList();
  }

  /** Precios de lista de los productos visibles, en el orden en que se muestran. */
  public List<BigDecimal> productPrices() {
    return driver.findElements(PRODUCT_PRICES).stream().map(e -> Money.parse(e.getText())).toList();
  }

  public String searchCaption() {
    return textOf(SEARCH_CAPTION);
  }

  public String searchResultCount() {
    return textOf(SEARCH_RESULT_COUNT);
  }

  public String noResultsMessage() {
    return textOf(NO_RESULTS);
  }
}

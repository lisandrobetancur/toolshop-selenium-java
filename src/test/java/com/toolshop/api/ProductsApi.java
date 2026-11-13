package com.toolshop.api;

import com.toolshop.data.NewProduct;
import com.toolshop.data.Product;
import com.toolshop.data.ProductPageResult;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;

/** Consulta de productos, categorías y marcas. Los identificadores cambian en cada reinicio. */
public final class ProductsApi {

  private ProductsApi() {}

  /**
   * Página del catálogo con los mismos criterios base que aplica la tienda en la portada: productos
   * que no son de alquiler y con precio entre 1 y 100.
   */
  public static ProductPageResult storefront(int page, Map<String, String> criteria) {
    Map<String, String> query = new LinkedHashMap<>();
    query.put("page", String.valueOf(page));
    query.put("between", "price,1,100");
    query.put("is_rental", "false");
    query.putAll(criteria);
    return list(query);
  }

  /** Resultado del buscador de la tienda para un término. */
  public static ProductPageResult search(String term) {
    return page("/products/search", Map.of("q", term));
  }

  public static ProductPageResult list(Map<String, String> query) {
    return page("/products", query);
  }

  private static ProductPageResult page(String path, Map<String, String> query) {
    String queryString =
        query.entrySet().stream()
            .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    JsonNode body = ApiClient.anonymous().get(path + "?" + queryString);
    return new ProductPageResult(
        toProducts(body.path("data")), body.path("total").asInt(), body.path("last_page").asInt());
  }

  /** Primeros {@code count} productos del catálogo que cumplen la condición. */
  public static List<Product> find(int count, Predicate<Product> condition) {
    List<Product> found = new ArrayList<>();
    int page = 1;
    int lastPage;
    do {
      ProductPageResult result = list(Map.of("page", String.valueOf(page)));
      lastPage = result.lastPage();
      result.products().stream()
          .filter(condition)
          .limit((long) count - found.size())
          .forEach(found::add);
      page++;
    } while (found.size() < count && page <= lastPage);
    if (found.size() < count) {
      throw new IllegalStateException(
          "El catálogo no tiene " + count + " productos que cumplan la condición pedida.");
    }
    return found;
  }

  /** Productos sin descuentos, en stock y que no son de alquiler. */
  public static List<Product> plain(int count) {
    return find(count, Product::isPlain);
  }

  public static Product get(String id) {
    return toProduct(ApiClient.anonymous().get("/products/" + id));
  }

  public static List<Product> related(String productId) {
    return toProducts(ApiClient.anonymous().get("/products/" + productId + "/related"));
  }

  /** Identificador de una categoría (de cualquier nivel) a partir de su nombre. */
  public static String categoryId(String name) {
    for (JsonNode parent : ApiClient.anonymous().get("/categories/tree")) {
      if (name.equals(parent.path("name").asString())) {
        return parent.path("id").asString();
      }
      for (JsonNode child : parent.path("sub_categories")) {
        if (name.equals(child.path("name").asString())) {
          return child.path("id").asString();
        }
      }
    }
    throw new IllegalStateException("No existe la categoría '" + name + "'");
  }

  /** Marcas disponibles: nombre → identificador, en el orden en que las devuelve la API. */
  public static Map<String, String> brands() {
    Map<String, String> brands = new LinkedHashMap<>();
    for (JsonNode brand : ApiClient.anonymous().get("/brands")) {
      brands.put(brand.path("name").asString(), brand.path("id").asString());
    }
    return brands;
  }

  /** Identificador de una imagen del catálogo a partir de su título. */
  public static String imageId(String title) {
    for (JsonNode image : ApiClient.anonymous().get("/images")) {
      if (title.equals(image.path("title").asString())) {
        return image.path("id").asString();
      }
    }
    throw new IllegalStateException("No existe la imagen '" + title + "'");
  }

  /** Crea un producto como administrador y lo devuelve tal como lo guardó la API. */
  public static Product create(String adminToken, NewProduct product) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("name", product.name());
    body.put("description", product.description());
    body.put("price", product.price());
    body.put("stock", product.stock());
    body.put("category_id", product.categoryId());
    body.put("brand_id", product.brandId());
    body.put("product_image_id", product.imageId());
    body.put("is_location_offer", false);
    body.put("is_rental", false);
    String id =
        ApiClient.anonymous().withToken(adminToken).post("/products", body).path("id").asString();
    return get(id);
  }

  private static List<Product> toProducts(JsonNode array) {
    List<Product> products = new ArrayList<>();
    for (JsonNode node : array) {
      products.add(toProduct(node));
    }
    return products;
  }

  static Product toProduct(JsonNode node) {
    return new Product(
        node.path("id").asString(),
        node.path("name").asString(),
        node.path("price").decimalValue().setScale(2, RoundingMode.HALF_UP),
        node.path("in_stock").asBoolean(),
        node.path("is_rental").asBoolean(),
        node.path("is_location_offer").asBoolean(),
        node.path("is_eco_friendly").asBoolean(),
        node.path("category").path("id").asString(),
        node.path("category").path("name").asString(),
        node.path("brand").path("id").asString(),
        node.path("brand").path("name").asString());
  }
}

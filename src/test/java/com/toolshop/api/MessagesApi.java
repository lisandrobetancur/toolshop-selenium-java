package com.toolshop.api;

import java.util.Optional;
import tools.jackson.databind.JsonNode;

/** Mensajes del formulario de contacto, consultados como administrador. */
public final class MessagesApi {

  private MessagesApi() {}

  /** Mensaje enviado desde el correo dado, si existe. Recorre todas las páginas del listado. */
  public static Optional<JsonNode> findByEmail(String adminToken, String email) {
    ApiClient api = ApiClient.anonymous().withToken(adminToken);
    int page = 1;
    int lastPage;
    do {
      JsonNode body = api.get("/messages?page=" + page);
      for (JsonNode message : body.path("data")) {
        if (email.equals(message.path("email").asString())) {
          return Optional.of(message);
        }
      }
      lastPage = body.path("last_page").asInt(1);
      page++;
    } while (page <= lastPage);
    return Optional.empty();
  }
}

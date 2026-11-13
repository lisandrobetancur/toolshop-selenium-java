package com.toolshop.api;

import com.toolshop.data.Address;
import tools.jackson.databind.JsonNode;

/**
 * Toolshop valida que ciudad y estado correspondan al código postal. Este servicio devuelve la
 * dirección que la propia API espera para un país y código postal.
 */
public final class AddressesApi {

  private AddressesApi() {}

  public static Address lookup(String country, String postalCode, String houseNumber) {
    JsonNode found =
        ApiClient.anonymous()
            .get(
                "/postcode-lookup?country="
                    + country
                    + "&postcode="
                    + postalCode
                    + "&house_number="
                    + houseNumber);
    return new Address(
        found.path("street").asString(),
        houseNumber,
        found.path("city").asString(),
        found.path("state").asString(),
        country,
        postalCode);
  }
}

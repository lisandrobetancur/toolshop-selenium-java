package com.toolshop.data;

import com.toolshop.api.AddressesApi;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.datafaker.Faker;

/** Genera clientes únicos por ejecución para que cada prueba sea independiente. */
public final class UserFactory {

  private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";

  private UserFactory() {}

  /** Cliente con datos aleatorios y una dirección que la API considera válida. */
  public static User random() {
    Faker faker = new Faker(Locale.ENGLISH);
    String firstName = lettersOnly(faker.name().firstName());
    String lastName = lettersOnly(faker.name().lastName());
    String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    ThreadLocalRandom random = ThreadLocalRandom.current();
    return new User(
        firstName,
        lastName,
        "qa." + unique + "@example.test",
        randomPassword(),
        LocalDate.of(random.nextInt(1970, 2000), random.nextInt(1, 13), random.nextInt(1, 28))
            .toString(),
        "555" + random.nextInt(1_000_000, 9_999_999),
        AddressesApi.lookup(
            "US",
            String.valueOf(random.nextInt(10_000, 99_999)),
            String.valueOf(random.nextInt(1, 999))));
  }

  /**
   * Contraseña que cumple las reglas de Toolshop: 8+ caracteres, mayúsculas, minúsculas, número y
   * símbolo, y que no aparece en listados de contraseñas filtradas.
   */
  public static String randomPassword() {
    return "Qa7!" + UUID.randomUUID().toString().replace("-", "").substring(0, 14) + "Zx";
  }

  private static String lettersOnly(String value) {
    String cleaned = value.replaceAll("[^A-Za-z]", "");
    return cleaned.length() >= 2 ? cleaned : "Tester" + randomLetters(4);
  }

  private static String randomLetters(int length) {
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(LETTERS.charAt(ThreadLocalRandom.current().nextInt(LETTERS.length())));
    }
    return builder.toString();
  }
}

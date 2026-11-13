package com.toolshop.report;

import io.qameta.allure.Allure;
import io.qameta.allure.SeverityLevel;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Etiquetas de Allure derivadas de lo que la prueba ya declara, para no duplicarlas:
 *
 * <ul>
 *   <li>{@code feature}: el {@code @DisplayName} de la clase ("F07 Checkout con ..."), que agrupa
 *       la pestaña Behaviors por flujo debajo del {@code @Epic} de la clase.
 *   <li>{@code severity}: {@code critical} para {@code @Tag("smoke")}, {@code normal} para el
 *       resto.
 * </ul>
 */
public final class AllureLabelsExtension implements BeforeEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    classContext(context).ifPresent(flow -> Allure.label("feature", flow.getDisplayName()));
    Set<String> tags = context.getTags();
    SeverityLevel severity = tags.contains("smoke") ? SeverityLevel.CRITICAL : SeverityLevel.NORMAL;
    Allure.label("severity", severity.value());
  }

  /**
   * Contexto de la clase de prueba. En las pruebas parametrizadas el padre inmediato es la
   * plantilla del método, así que se sube hasta el contexto que tiene clase pero no método.
   */
  private static Optional<ExtensionContext> classContext(ExtensionContext context) {
    Optional<ExtensionContext> current = context.getParent();
    while (current.isPresent()) {
      ExtensionContext candidate = current.get();
      if (candidate.getTestClass().isPresent() && candidate.getTestMethod().isEmpty()) {
        return current;
      }
      current = candidate.getParent();
    }
    return Optional.empty();
  }
}

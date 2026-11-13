package com.toolshop.driver;

import com.toolshop.config.Config;
import com.toolshop.evidence.FailureEvidence;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.WebDriver;

/**
 * Ciclo de vida del navegador: un WebDriver nuevo por prueba, guardado en un ThreadLocal para que
 * las pruebas puedan ejecutarse en paralelo. Si la prueba falla, adjunta la evidencia al reporte
 * antes de cerrar el navegador.
 */
public final class DriverExtension implements BeforeEachCallback, AfterEachCallback {

  private static final ThreadLocal<WebDriver> CURRENT = new ThreadLocal<>();

  /** WebDriver de la prueba que corre en el hilo actual. */
  public static WebDriver driver() {
    WebDriver driver = CURRENT.get();
    if (driver == null) {
      throw new IllegalStateException(
          "No hay WebDriver en este hilo. ¿La clase de prueba usa DriverExtension?");
    }
    return driver;
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    CURRENT.set(DriverFactory.create(Config.get()));
  }

  @Override
  public void afterEach(ExtensionContext context) {
    WebDriver driver = CURRENT.get();
    if (driver == null) {
      return;
    }
    try {
      if (context.getExecutionException().isPresent()) {
        FailureEvidence.attach(driver);
      }
    } finally {
      CURRENT.remove();
      driver.quit();
    }
  }
}

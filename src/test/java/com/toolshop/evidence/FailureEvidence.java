package com.toolshop.evidence;

import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evidencia que se adjunta al reporte de Allure solo cuando una prueba falla: captura de pantalla,
 * HTML de la página, URL y logs de la consola del navegador.
 */
public final class FailureEvidence {

  private static final Logger LOG = LoggerFactory.getLogger(FailureEvidence.class);

  private FailureEvidence() {}

  public static void attach(WebDriver driver) {
    safely("URL", () -> Allure.attachment("URL", "text/plain", driver.getCurrentUrl()));
    safely(
        "captura",
        () ->
            Allure.attachment(
                "Captura de pantalla",
                "image/png",
                new ByteArrayInputStream(
                    ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)),
                AttachmentOptions.withFileExtension(".png")));
    safely(
        "HTML",
        () ->
            Allure.attachment(
                "HTML de la página",
                "text/html",
                new ByteArrayInputStream(
                    String.valueOf(driver.getPageSource()).getBytes(StandardCharsets.UTF_8)),
                AttachmentOptions.withFileExtension(".html")));
    safely(
        "logs", () -> Allure.attachment("Logs del navegador", "text/plain", browserLogs(driver)));
  }

  /** La consola solo está disponible en navegadores Chromium; en el resto se indica. */
  private static String browserLogs(WebDriver driver) {
    try {
      String logs =
          driver.manage().logs().get(LogType.BROWSER).getAll().stream()
              .map(entry -> entry.getLevel() + " " + entry.getMessage())
              .collect(Collectors.joining(System.lineSeparator()));
      return logs.isEmpty() ? "(consola sin mensajes)" : logs;
    } catch (RuntimeException e) {
      return "Este navegador no expone los logs de la consola por WebDriver: " + e.getMessage();
    }
  }

  private static void safely(String what, Runnable action) {
    try {
      action.run();
    } catch (RuntimeException e) {
      // La evidencia nunca debe ocultar el fallo original de la prueba.
      LOG.warn("No se pudo adjuntar {} al reporte: {}", what, e.getMessage());
    }
  }
}

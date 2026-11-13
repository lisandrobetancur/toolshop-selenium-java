package com.toolshop.driver;

import com.toolshop.config.Browser;
import com.toolshop.config.Config;
import com.toolshop.report.RunInfo;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

/**
 * Crea el WebDriver según la configuración. Los binarios de los drivers los resuelve Selenium
 * Manager; si hay {@code GRID_URL} se usa RemoteWebDriver sin que las pruebas cambien.
 */
public final class DriverFactory {

  private static final int WIDTH = 1440;
  private static final int HEIGHT = 1080;

  private DriverFactory() {}

  public static WebDriver create(Config config) {
    WebDriver driver = config.gridUrl() != null ? remote(config, config.gridUrl()) : local(config);
    RunInfo.recordBrowser(((HasCapabilities) driver).getCapabilities());
    // Safari no tiene modo headless: siempre se le fija el tamaño de ventana.
    if (!config.headless() || config.browser() == Browser.SAFARI) {
      driver.manage().window().setSize(new Dimension(WIDTH, HEIGHT));
    }
    return driver;
  }

  private static WebDriver local(Config config) {
    return switch (config.browser()) {
      case CHROME -> new ChromeDriver(chrome(config.headless()));
      case FIREFOX -> new FirefoxDriver(firefox(config.headless()));
      case EDGE -> new EdgeDriver(edge(config.headless()));
      case SAFARI -> new SafariDriver(new SafariOptions());
    };
  }

  private static WebDriver remote(Config config, URI gridUrl) {
    Capabilities options =
        switch (config.browser()) {
          case CHROME -> chrome(config.headless());
          case FIREFOX -> firefox(config.headless());
          case EDGE -> edge(config.headless());
          case SAFARI -> new SafariOptions();
        };
    try {
      RemoteWebDriver driver = new RemoteWebDriver(gridUrl.toURL(), options);
      // Permite subir archivos locales (F10) cuando el navegador corre en otro nodo.
      driver.setFileDetector(new LocalFileDetector());
      return driver;
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("GRID_URL no es una URL válida: " + gridUrl, e);
    }
  }

  private static ChromeOptions chrome(boolean headless) {
    ChromeOptions options = new ChromeOptions();
    options.addArguments(
        "--window-size=" + WIDTH + "," + HEIGHT, "--lang=en-US", "--disable-dev-shm-usage");
    if (headless) {
      options.addArguments("--headless=new");
    }
    options.setExperimentalOption("prefs", Map.of("intl.accept_languages", "en-US,en"));
    options.setCapability("goog:loggingPrefs", browserLogs());
    return options;
  }

  private static EdgeOptions edge(boolean headless) {
    EdgeOptions options = new EdgeOptions();
    options.addArguments(
        "--window-size=" + WIDTH + "," + HEIGHT, "--lang=en-US", "--disable-dev-shm-usage");
    if (headless) {
      options.addArguments("--headless=new");
    }
    // En los runners Ubuntu 24.04 de GitHub, AppArmor restringe los user namespaces que usa el
    // sandbox de Edge (Chrome trae su propio perfil de AppArmor, Edge no) y el navegador se cierra
    // al arrancar. Solo en CI, que corre en una máquina efímera, se desactiva ese sandbox.
    if (isCi()) {
      options.addArguments("--no-sandbox");
    }
    options.setExperimentalOption("prefs", Map.of("intl.accept_languages", "en-US,en"));
    options.setCapability("ms:loggingPrefs", browserLogs());
    return options;
  }

  private static FirefoxOptions firefox(boolean headless) {
    FirefoxOptions options = new FirefoxOptions();
    options.addArguments("--width=" + WIDTH, "--height=" + HEIGHT);
    if (headless) {
      options.addArguments("-headless");
    }
    options.addPreference("intl.accept_languages", "en-US,en");
    return options;
  }

  private static boolean isCi() {
    String ci = System.getenv("CI");
    return ci != null && !ci.isBlank() && !"false".equalsIgnoreCase(ci);
  }

  private static LoggingPreferences browserLogs() {
    LoggingPreferences prefs = new LoggingPreferences();
    prefs.enable(LogType.BROWSER, Level.ALL);
    return prefs;
  }
}

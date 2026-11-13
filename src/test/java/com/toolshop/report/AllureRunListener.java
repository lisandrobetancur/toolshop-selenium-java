package com.toolshop.report;

import com.toolshop.config.Config;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.openqa.selenium.BuildInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Al terminar la ejecución escribe en los resultados de Allure el entorno (navegador, URLs,
 * versiones), el ejecutor (local o GitHub Actions) y las categorías de fallos.
 */
public final class AllureRunListener implements TestExecutionListener {

  private static final Logger LOG = LoggerFactory.getLogger(AllureRunListener.class);

  @Override
  public void testPlanExecutionFinished(TestPlan testPlan) {
    Path results = Path.of(System.getProperty("allure.results.directory", "build/allure-results"));
    try {
      Files.createDirectories(results);
      writeEnvironment(results);
      writeExecutor(results);
      copyCategories(results);
    } catch (IOException | UncheckedIOException e) {
      // La metadata del reporte nunca debe hacer fallar la ejecución.
      LOG.warn("No se pudo escribir la metadata de Allure: {}", e.getMessage());
    }
  }

  private static void writeEnvironment(Path results) throws IOException {
    Config config = Config.get();
    String browser = RunInfo.browser();
    Map<String, String> env = new LinkedHashMap<>();
    env.put(
        "Navegador", browser == null ? config.browser().name().toLowerCase(Locale.ROOT) : browser);
    env.put("Headless", String.valueOf(config.headless()));
    env.put("Grid", config.gridUrl() == null ? "no (local)" : config.gridUrl().toString());
    env.put("URL.UI", config.baseUrl());
    env.put("URL.API", config.apiUrl());
    env.put("Selenium", new BuildInfo().getReleaseLabel());
    env.put("Java", Runtime.version().toString());
    env.put("SO", System.getProperty("os.name") + " " + System.getProperty("os.version"));
    env.put(
        "Hilos",
        System.getProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "1"));
    String content =
        env.entrySet().stream()
            .map(e -> escape(e.getKey()) + "=" + escape(e.getValue()))
            .collect(Collectors.joining(System.lineSeparator()));
    Files.writeString(results.resolve("environment.properties"), content, StandardCharsets.UTF_8);
  }

  private static void writeExecutor(Path results) throws IOException {
    Map<String, Object> executor = new LinkedHashMap<>();
    if ("true".equals(System.getenv("GITHUB_ACTIONS"))) {
      String runUrl =
          System.getenv("GITHUB_SERVER_URL")
              + "/"
              + System.getenv("GITHUB_REPOSITORY")
              + "/actions/runs/"
              + System.getenv("GITHUB_RUN_ID");
      executor.put("name", "GitHub Actions");
      executor.put("type", "github");
      executor.put(
          "buildName",
          System.getenv("GITHUB_WORKFLOW")
              + " #"
              + System.getenv("GITHUB_RUN_NUMBER")
              + " ("
              + Config.get().browser().name().toLowerCase(Locale.ROOT)
              + ")");
      executor.put("buildUrl", runUrl);
      executor.put(
          "buildOrder", Long.parseLong(System.getenv().getOrDefault("GITHUB_RUN_NUMBER", "0")));
    } else {
      executor.put("name", "Ejecución local");
      executor.put("type", "local");
      executor.put(
          "buildName", "Local · " + Config.get().browser().name().toLowerCase(Locale.ROOT));
    }
    Files.writeString(
        results.resolve("executor.json"),
        JsonMapper.builder().build().writeValueAsString(executor),
        StandardCharsets.UTF_8);
  }

  private static void copyCategories(Path results) throws IOException {
    try (InputStream categories =
        AllureRunListener.class.getResourceAsStream("/allure/categories.json")) {
      if (categories != null) {
        Files.copy(
            categories, results.resolve("categories.json"), StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  /** Escapa separadores de un archivo .properties. */
  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("=", "\\=").replace(":", "\\:");
  }
}

package com.toolshop.report;

import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.Capabilities;

/** Datos de la ejecución que solo se conocen al abrir el primer navegador. */
public final class RunInfo {

  private static final AtomicReference<@Nullable String> BROWSER = new AtomicReference<>();

  private RunInfo() {}

  /** Registra nombre y versión reales del navegador (solo la primera vez). */
  public static void recordBrowser(Capabilities capabilities) {
    BROWSER.compareAndSet(
        null, capabilities.getBrowserName() + " " + capabilities.getBrowserVersion());
  }

  public static @Nullable String browser() {
    return BROWSER.get();
  }
}

package com.toolshop.tests;

import com.toolshop.config.Config;
import com.toolshop.driver.DriverExtension;
import com.toolshop.support.Waits;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

/** Base de las pruebas de UI: cada prueba recibe su propio navegador. */
@ExtendWith(DriverExtension.class)
abstract class UiTest {

  protected static WebDriver driver() {
    return DriverExtension.driver();
  }

  /**
   * Verifica un resultado que la aplicación actualiza de forma asíncrona: reintenta la aserción
   * hasta que pase o se agote el tiempo de espera configurado.
   */
  protected static void eventually(Runnable assertion) {
    new Waits(driver(), Config.get().timeout()).untilAsserted(assertion);
  }
}

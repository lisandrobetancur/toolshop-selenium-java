package com.toolshop.tests;

import com.toolshop.driver.DriverExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

/** Base de las pruebas de UI: cada prueba recibe su propio navegador. */
@ExtendWith(DriverExtension.class)
abstract class UiTest {

  protected static WebDriver driver() {
    return DriverExtension.driver();
  }
}

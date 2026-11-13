package com.toolshop.components;

import com.toolshop.pages.BasePage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Tabla de datos genérica: lee las filas como mapas encabezado → texto y permite hacer clic en un
 * enlace o botón de la fila que contenga un valor.
 */
public final class DataTable extends BasePage {

  private final By table;

  public DataTable(WebDriver driver, By table) {
    super(driver);
    this.table = table;
  }

  public List<Map<String, String>> rows() {
    WebElement element = waits.visible(table);
    List<String> headers =
        element.findElements(By.cssSelector("thead th")).stream()
            .map(th -> th.getText().trim())
            .toList();
    List<Map<String, String>> rows = new ArrayList<>();
    for (WebElement row : element.findElements(By.cssSelector("tbody tr"))) {
      List<WebElement> cells = row.findElements(By.tagName("td"));
      Map<String, String> values = new LinkedHashMap<>();
      for (int i = 0; i < cells.size() && i < headers.size(); i++) {
        if (!headers.get(i).isEmpty()) {
          values.put(headers.get(i), cells.get(i).getText().trim());
        }
      }
      rows.add(values);
    }
    return rows;
  }

  /** Hace clic en el enlace o botón con ese texto, dentro de la fila que contiene el valor. */
  public void clickInRow(String rowValue, String linkText) {
    waits.until(
        "poder hacer clic en '" + linkText + "' de la fila '" + rowValue + "'",
        d -> {
          for (WebElement row : d.findElement(table).findElements(By.cssSelector("tbody tr"))) {
            boolean matches =
                row.findElements(By.tagName("td")).stream()
                    .anyMatch(td -> rowValue.equals(td.getText().trim()));
            if (matches) {
              for (WebElement action : row.findElements(By.cssSelector("a, button"))) {
                if (linkText.equals(action.getText().trim())) {
                  action.click();
                  return true;
                }
              }
            }
          }
          return false;
        });
  }
}

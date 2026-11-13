package com.toolshop.pages.admin;

import com.toolshop.pages.BasePage;
import io.qameta.allure.Step;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/** Edición de un pedido en el panel de administración ({@code /admin/orders/edit/:id}). */
public final class AdminOrderPage extends BasePage {

  private static final By INVOICE_NUMBER = byTest("invoice-number");
  private static final By STATUS = byTest("order-status");
  private static final By UPDATE_STATUS = byTest("update-status-submit");
  private static final By SUCCESS = By.cssSelector(".alert-success[role='alert']");
  private static final By ERROR = By.cssSelector(".alert-danger[role='alert']");

  public AdminOrderPage(WebDriver driver) {
    super(driver);
  }

  /** Abre el pedido y espera a que cargue su número de factura. */
  @Step("Abrir el pedido {invoiceId} como administrador")
  public AdminOrderPage open(String invoiceId) {
    navigateTo("/admin/orders/edit/" + invoiceId);
    waits.until(
        "que el pedido cargue su número de factura",
        d -> !String.valueOf(d.findElement(INVOICE_NUMBER).getDomProperty("value")).isBlank());
    return this;
  }

  public String invoiceNumber() {
    return valueOf(INVOICE_NUMBER);
  }

  public String currentStatus() {
    return new Select(waits.visible(STATUS)).getFirstSelectedOption().getText().trim();
  }

  /** Estados a los que el pedido todavía puede avanzar (opciones habilitadas). */
  public List<String> availableStatuses() {
    return new Select(waits.visible(STATUS))
        .getOptions().stream().filter(WebElement::isEnabled).map(o -> o.getText().trim()).toList();
  }

  @Step("Elegir el estado {status}")
  public AdminOrderPage chooseStatus(String status) {
    selectByValue(STATUS, status);
    return this;
  }

  @Step("Actualizar el estado del pedido")
  public AdminOrderPage submitStatus() {
    click(UPDATE_STATUS);
    return this;
  }

  public String successMessage() {
    return textOf(SUCCESS);
  }

  public String errorMessage() {
    return textOf(ERROR);
  }
}

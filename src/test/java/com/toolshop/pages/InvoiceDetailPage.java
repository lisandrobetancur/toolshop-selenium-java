package com.toolshop.pages;

import com.toolshop.components.DataTable;
import com.toolshop.data.Address;
import com.toolshop.support.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Detalle de una factura del cliente ({@code /account/invoices/:id}). */
public final class InvoiceDetailPage extends BasePage {

  private static final By INVOICE_NUMBER = byTest("invoice-number");
  private static final By TOTAL = By.id("total");
  private static final By STREET = byTest("street");
  private static final By POSTAL_CODE = byTest("postal_code");
  private static final By CITY = byTest("city");
  private static final By STATE = byTest("state");
  private static final By COUNTRY = byTest("country");
  private static final By PAYMENT_METHOD = byTest("payment-method");

  private final DataTable lines;

  public InvoiceDetailPage(WebDriver driver) {
    super(driver);
    this.lines = new DataTable(driver, By.cssSelector("table"));
  }

  /** Espera a que la factura cargue y devuelve su número. */
  public String invoiceNumber() {
    return waits.until(
        "que se cargue el número de factura",
        d -> {
          String value = String.valueOf(d.findElement(INVOICE_NUMBER).getDomProperty("value"));
          return value.isBlank() ? null : value;
        });
  }

  public BigDecimal total() {
    return Money.parse(valueOf(TOTAL));
  }

  public Address billingAddress() {
    return new Address(
        valueOf(STREET), "", valueOf(CITY), valueOf(STATE), valueOf(COUNTRY), valueOf(POSTAL_CODE));
  }

  public String paymentMethod() {
    return valueOf(PAYMENT_METHOD);
  }

  public List<Map<String, String>> lines() {
    return lines.rows();
  }
}

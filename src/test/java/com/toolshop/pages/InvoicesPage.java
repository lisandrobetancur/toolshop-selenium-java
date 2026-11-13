package com.toolshop.pages;

import com.toolshop.components.DataTable;
import io.qameta.allure.Step;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Listado de facturas del cliente ({@code /account/invoices}). */
public final class InvoicesPage extends BasePage {

  private static final By TABLE = By.cssSelector("table");

  private final DataTable table;

  public InvoicesPage(WebDriver driver) {
    super(driver);
    this.table = new DataTable(driver, TABLE);
  }

  @Step("Abrir las facturas del cliente")
  public InvoicesPage open() {
    navigateTo("/account/invoices");
    return this;
  }

  public List<Map<String, String>> rows() {
    return table.rows();
  }

  @Step("Abrir el detalle de la factura {invoiceNumber}")
  public InvoiceDetailPage openDetails(String invoiceNumber) {
    table.clickInRow(invoiceNumber, "Details");
    return new InvoiceDetailPage(driver);
  }
}

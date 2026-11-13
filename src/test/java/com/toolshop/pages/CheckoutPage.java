package com.toolshop.pages;

import com.toolshop.data.Address;
import com.toolshop.data.Payment;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Pasos 2 a 4 del checkout ({@code /checkout}): identificación, dirección de facturación y pago. El
 * paso 1 (carrito) es {@link CartPage}.
 */
public final class CheckoutPage extends BasePage {

  private static final By PROCEED_SIGNED_IN = byTest("proceed-2");
  private static final By SIGNED_IN_GREETING =
      By.xpath("//p[contains(normalize-space(), 'you are already logged in')]");
  private static final By STREET = byTest("street");
  private static final By HOUSE_NUMBER = byTest("house_number");
  private static final By CITY = byTest("city");
  private static final By STATE = byTest("state");
  private static final By COUNTRY = byTest("country");
  private static final By POSTAL_CODE = byTest("postal_code");
  private static final By PROCEED_ADDRESS = byTest("proceed-3");
  private static final By PAYMENT_METHOD = byTest("payment-method");
  private static final By FINISH = byTest("finish");
  private static final By PAYMENT_SUCCESS = byTest("payment-success-message");
  private static final By PAYMENT_ERROR = byTest("payment-error-message");
  private static final By CONFIRMATION = By.id("order-confirmation");
  // El texto de traducción trae <span id="invoice-number">, pero Angular elimina el id al
  // sanear el innerHTML; el número se extrae del mensaje de confirmación.
  private static final Pattern INVOICE_NUMBER = Pattern.compile("INV-\\d+");

  public CheckoutPage(WebDriver driver) {
    super(driver);
  }

  /** Saludo del paso 2 cuando el cliente ya inició sesión. */
  public String signedInGreeting() {
    // El saludo se pinta antes de que llegue el perfil ("Hello , ..."): se espera al nombre.
    return waits.until(
        "el saludo con el nombre del cliente",
        d -> {
          String text = d.findElement(SIGNED_IN_GREETING).getText().trim();
          return text.startsWith("Hello ,") ? null : text;
        });
  }

  public CheckoutPage proceedAsSignedInCustomer() {
    click(PROCEED_SIGNED_IN);
    return this;
  }

  /** Dirección que el paso 3 precarga desde el perfil del cliente. */
  public Address billingAddress() {
    waits.until(
        "que el paso de dirección precargue los datos del cliente",
        d -> !String.valueOf(d.findElement(CITY).getDomProperty("value")).isBlank());
    return new Address(
        valueOf(STREET),
        valueOf(HOUSE_NUMBER),
        valueOf(CITY),
        valueOf(STATE),
        valueOf(COUNTRY),
        valueOf(POSTAL_CODE));
  }

  public CheckoutPage proceedFromAddress() {
    click(PROCEED_ADDRESS);
    waits.visible(PAYMENT_METHOD);
    return this;
  }

  /** Elige el medio de pago y completa sus campos. */
  public CheckoutPage choosePayment(Payment payment) {
    selectByValue(PAYMENT_METHOD, payment.method());
    for (Map.Entry<String, String> field : payment.details().entrySet()) {
      By locator = byTest(field.getKey());
      WebElement element = waits.visible(locator);
      if ("select".equals(element.getTagName())) {
        selectByValue(locator, field.getValue());
      } else {
        type(locator, field.getValue());
      }
    }
    return this;
  }

  /**
   * Primer clic: la aplicación valida el pago y muestra el resultado. Devuelve el mensaje de
   * validación exitosa.
   */
  public String checkPayment() {
    click(FINISH);
    return textOf(PAYMENT_SUCCESS);
  }

  /** Segundo clic: confirma el pedido y devuelve el número de factura de la confirmación. */
  public String confirmOrder() {
    waits.until(
        "que el botón cambie a 'Confirm'",
        d -> "Confirm".equals(d.findElement(FINISH).getText().trim()));
    click(FINISH);
    String message = textOf(CONFIRMATION);
    Matcher matcher = INVOICE_NUMBER.matcher(message);
    if (!matcher.find()) {
      throw new AssertionError("La confirmación no contiene un número de factura: " + message);
    }
    return matcher.group();
  }

  public String confirmationMessage() {
    return textOf(CONFIRMATION);
  }

  public String paymentError() {
    return textOf(PAYMENT_ERROR);
  }

  public boolean canSubmitPayment() {
    return waits.visible(FINISH).isEnabled();
  }
}

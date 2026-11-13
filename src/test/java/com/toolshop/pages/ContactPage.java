package com.toolshop.pages;

import io.qameta.allure.Param;
import io.qameta.allure.Step;
import io.qameta.allure.model.Parameter;
import java.nio.file.Path;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Formulario de contacto ({@code /contact}). */
public final class ContactPage extends BasePage {

  private static final By FIRST_NAME = byTest("first-name");
  private static final By LAST_NAME = byTest("last-name");
  private static final By EMAIL = byTest("email");
  private static final By SUBJECT = byTest("subject");
  private static final By MESSAGE = byTest("message");
  private static final By ATTACHMENT = byTest("attachment");
  private static final By SUBMIT = byTest("contact-submit");
  private static final By CONFIRMATION = By.cssSelector(".alert-success[role='alert']");

  public ContactPage(WebDriver driver) {
    super(driver);
  }

  @Step("Abrir el formulario de contacto")
  public ContactPage open() {
    navigateTo("/contact");
    waits.visible(SUBMIT);
    return this;
  }

  @Step("Indicar remitente {firstName} {lastName} <{email}>")
  public ContactPage fillSender(String firstName, String lastName, String email) {
    type(FIRST_NAME, firstName);
    type(LAST_NAME, lastName);
    type(EMAIL, email);
    return this;
  }

  /** Asunto por su valor: customer-service, webmaster, return, payments, warranty... */
  @Step("Elegir el asunto \"{subject}\"")
  public ContactPage chooseSubject(String subject) {
    selectByValue(SUBJECT, subject);
    return this;
  }

  /** Escribe el mensaje y sale del campo: la validación del mensaje se dispara al perder foco. */
  @Step("Escribir el mensaje")
  public ContactPage writeMessage(String message) {
    type(MESSAGE, message);
    waits.visible(SUBJECT).click();
    return this;
  }

  /** Adjunta un archivo local; con Grid, RemoteWebDriver lo sube al nodo. */
  public ContactPage attach(Path file) {
    return attachFile(String.valueOf(file.getFileName()), file.toAbsolutePath().toString());
  }

  /**
   * El paso recibe texto y no {@link Path}: Path es iterable de sí mismo y Allure entra en
   * recursión infinita al convertirlo en parámetro del paso.
   */
  @Step("Adjuntar {fileName}")
  private ContactPage attachFile(
      // Lo lee la plantilla del paso ("Adjuntar {fileName}"), no el cuerpo del método.
      @SuppressWarnings("UnusedVariable") String fileName,
      @Param(name = "ruta", mode = Parameter.Mode.HIDDEN) String absolutePath) {
    waits.present(ATTACHMENT).sendKeys(absolutePath);
    return this;
  }

  @Step("Enviar el formulario de contacto")
  public ContactPage submit() {
    click(SUBMIT);
    return this;
  }

  public String confirmation() {
    return textOf(CONFIRMATION);
  }

  public String fieldError(String field) {
    return textOf(byTest(field + "-error"));
  }

  public boolean isShowingForm() {
    return isDisplayed(SUBMIT);
  }
}

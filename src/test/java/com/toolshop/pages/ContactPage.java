package com.toolshop.pages;

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

  public ContactPage open() {
    navigateTo("/contact");
    waits.visible(SUBMIT);
    return this;
  }

  public ContactPage fillSender(String firstName, String lastName, String email) {
    type(FIRST_NAME, firstName);
    type(LAST_NAME, lastName);
    type(EMAIL, email);
    return this;
  }

  /** Asunto por su valor: customer-service, webmaster, return, payments, warranty... */
  public ContactPage chooseSubject(String subject) {
    selectByValue(SUBJECT, subject);
    return this;
  }

  /** Escribe el mensaje y sale del campo: la validación del mensaje se dispara al perder foco. */
  public ContactPage writeMessage(String message) {
    type(MESSAGE, message);
    waits.visible(SUBJECT).click();
    return this;
  }

  /** Adjunta un archivo local; con Grid, RemoteWebDriver lo sube al nodo. */
  public ContactPage attach(Path file) {
    waits.present(ATTACHMENT).sendKeys(file.toAbsolutePath().toString());
    return this;
  }

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

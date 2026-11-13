package com.toolshop.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.toolshop.api.MessagesApi;
import com.toolshop.api.UsersApi;
import com.toolshop.pages.ContactPage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

/**
 * Toolshop solo acepta adjuntos {@code .txt} de 0 bytes (regla deliberada de la aplicación de
 * práctica). El caso feliz usa un archivo vacío; los negativos, uno con contenido y otro de otro
 * tipo.
 */
@Tag("F10")
@DisplayName("F10 Formulario de contacto")
class F10ContactTest extends UiTest {

  private static final String MESSAGE =
      "Hello, I would like to know more about the warranty of the cordless drill I bought.";

  @TempDir Path tempDir;

  @Test
  @Tag("smoke")
  @DisplayName("Un visitante envía un mensaje con adjunto y queda registrado")
  void visitorSendsMessageWithAttachment() throws IOException {
    String email = "contact." + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
    Path attachment = Files.createFile(tempDir.resolve("notes.txt"));
    ContactPage contact = new ContactPage(driver()).open();

    contact
        .fillSender("Ana", "Visitor", email)
        .chooseSubject("warranty")
        .writeMessage(MESSAGE)
        .attach(attachment)
        .submit();

    assertThat(contact.confirmation())
        .isEqualTo("Thanks for your message! We will contact you shortly.");
    assertThat(contact.isShowingForm()).isFalse();
    JsonNode stored =
        MessagesApi.findByEmail(UsersApi.loginAsAdmin(), email)
            .orElseThrow(
                () -> new AssertionError("El mensaje no llegó al buzón del administrador"));
    SoftAssertions.assertSoftly(soft -> {
      soft.assertThat(stored.path("name").asString()).isEqualTo("Ana Visitor");
      soft.assertThat(stored.path("subject").asString()).isEqualTo("warranty");
      soft.assertThat(stored.path("message").asString()).isEqualTo(MESSAGE);
      soft.assertThat(stored.path("status").asString()).isEqualTo("NEW");
    });
  }

  @Test
  @Tag("regression")
  @DisplayName("El formulario vacío muestra los campos obligatorios")
  void emptyFormShowsRequiredFields() {
    ContactPage contact = new ContactPage(driver()).open().submit();

    SoftAssertions.assertSoftly(soft -> {
      soft.assertThat(contact.fieldError("first-name")).isEqualTo("First name is required");
      soft.assertThat(contact.fieldError("last-name")).isEqualTo("Last name is required");
      soft.assertThat(contact.fieldError("email")).isEqualTo("Email is required");
      soft.assertThat(contact.fieldError("subject")).isEqualTo("Subject is required");
      soft.assertThat(contact.fieldError("message")).isEqualTo("Message is required");
      soft.assertThat(contact.isShowingForm()).isTrue();
    });
  }

  @Test
  @Tag("regression")
  @DisplayName("Un mensaje de menos de 50 caracteres y un correo inválido se rechazan")
  void shortMessageAndInvalidEmailAreRejected() {
    ContactPage contact =
        new ContactPage(driver())
            .open()
            .fillSender("Ana", "Visitor", "not-an-email")
            .chooseSubject("payments")
            .writeMessage("Too short")
            .submit();

    assertThat(contact.fieldError("message")).isEqualTo("Message must be minimal 50 characters");
    assertThat(contact.fieldError("email")).isEqualTo("Email format is invalid");
    assertThat(contact.isShowingForm()).isTrue();
  }

  @Test
  @Tag("regression")
  @DisplayName("Un adjunto con contenido se rechaza")
  void nonEmptyAttachmentIsRejected() throws IOException {
    Path attachment =
        Files.writeString(tempDir.resolve("notes.txt"), "not empty", StandardCharsets.UTF_8);
    ContactPage contact = validForm().attach(attachment).submit();

    assertThat(contact.fieldError("attachment")).isEqualTo("File should be empty.");
    assertThat(contact.isShowingForm()).isTrue();
  }

  @Test
  @Tag("regression")
  @DisplayName("Un adjunto que no es de texto se rechaza")
  void wrongAttachmentTypeIsRejected() throws IOException {
    Path attachment = Files.createFile(tempDir.resolve("photo.pdf"));
    ContactPage contact = validForm().attach(attachment).submit();

    assertThat(contact.fieldError("attachment")).isEqualTo("File should have a txt extension.");
    assertThat(contact.isShowingForm()).isTrue();
  }

  private static ContactPage validForm() {
    return new ContactPage(driver())
        .open()
        .fillSender("Ana", "Visitor", "visitor@example.test")
        .chooseSubject("customer-service")
        .writeMessage(MESSAGE);
  }
}

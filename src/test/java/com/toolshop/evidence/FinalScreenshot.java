package com.toolshop.evidence;

import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captura del estado final de una prueba que pasó. Se guarda en JPEG comprimido: con una captura
 * por prueba, PNG llevaría el reporte de un único archivo a decenas de MB. Las capturas de fallos
 * ({@link FailureEvidence}) siguen en PNG a resolución completa.
 */
public final class FinalScreenshot {

  private static final Logger LOG = LoggerFactory.getLogger(FinalScreenshot.class);
  private static final float JPEG_QUALITY = 0.6f;

  private FinalScreenshot() {}

  public static void attach(WebDriver driver) {
    try {
      byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
      Allure.attachment(
          "Captura final",
          "image/jpeg",
          new ByteArrayInputStream(toJpeg(png)),
          AttachmentOptions.withFileExtension(".jpg"));
    } catch (IOException | RuntimeException e) {
      // La captura de una prueba exitosa nunca debe convertirla en fallida.
      LOG.warn("No se pudo adjuntar la captura final: {}", e.getMessage());
    }
  }

  private static byte[] toJpeg(byte[] png) throws IOException {
    BufferedImage source = ImageIO.read(new ByteArrayInputStream(png));
    if (source == null) {
      throw new IOException("la captura no es una imagen legible");
    }
    // JPEG no admite canal alfa: se copia a una imagen RGB.
    BufferedImage rgb =
        new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
    rgb.createGraphics().drawImage(source, 0, 0, null);

    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
    if (!writers.hasNext()) {
      throw new IOException("no hay codificador JPEG disponible");
    }
    ImageWriter writer = writers.next();
    ImageWriteParam params = writer.getDefaultWriteParam();
    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    params.setCompressionQuality(JPEG_QUALITY);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
      writer.setOutput(stream);
      writer.write(null, new IIOImage(rgb, null, null), params);
    } finally {
      writer.dispose();
    }
    return out.toByteArray();
  }
}

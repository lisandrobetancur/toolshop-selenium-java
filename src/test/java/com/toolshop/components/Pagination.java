package com.toolshop.components;

import com.toolshop.pages.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Paginador que comparten el catálogo y los listados. */
public final class Pagination extends BasePage {

  private static final By NEXT = byTest("pagination-next");
  private static final By PREVIOUS = byTest("pagination-prev");

  public Pagination(WebDriver driver) {
    super(driver);
  }

  @Step("Ir a la página {page}")
  public void goTo(int page) {
    click(By.cssSelector("button[aria-label='Page-" + page + "']"));
  }

  @Step("Ir a la página siguiente")
  public void next() {
    click(NEXT);
  }

  @Step("Ir a la página anterior")
  public void previous() {
    click(PREVIOUS);
  }
}

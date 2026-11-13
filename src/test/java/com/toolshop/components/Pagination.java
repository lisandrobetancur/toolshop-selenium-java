package com.toolshop.components;

import com.toolshop.pages.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Paginador que comparten el catálogo y los listados. */
public final class Pagination extends BasePage {

  private static final By NEXT = byTest("pagination-next");
  private static final By PREVIOUS = byTest("pagination-prev");

  public Pagination(WebDriver driver) {
    super(driver);
  }

  public void goTo(int page) {
    click(By.cssSelector("button[aria-label='Page-" + page + "']"));
  }

  public void next() {
    click(NEXT);
  }

  public void previous() {
    click(PREVIOUS);
  }
}

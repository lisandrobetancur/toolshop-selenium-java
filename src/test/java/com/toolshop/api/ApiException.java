package com.toolshop.api;

/** La API respondió con un estado inesperado mientras se preparaban los datos de una prueba. */
public final class ApiException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int status;

  public ApiException(String message, int status) {
    super(message);
    this.status = status;
  }

  public ApiException(String message, Throwable cause) {
    super(message, cause);
    this.status = -1;
  }

  public int status() {
    return status;
  }
}

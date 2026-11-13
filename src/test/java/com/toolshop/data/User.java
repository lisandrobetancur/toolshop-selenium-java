package com.toolshop.data;

/** Cliente de prueba. La fecha de nacimiento va en formato ISO (yyyy-MM-dd). */
public record User(
    String firstName,
    String lastName,
    String email,
    String password,
    String dob,
    String phone,
    Address address) {

  public String fullName() {
    return firstName + " " + lastName;
  }

  /** Sin la contraseña: este texto aparece en los pasos del reporte y en los logs. */
  @Override
  public String toString() {
    return fullName() + " <" + email + ">";
  }
}

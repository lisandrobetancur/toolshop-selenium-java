package com.toolshop.data;

/** Dirección postal. El país es el código ISO de dos letras que usa Toolshop. */
public record Address(
    String street,
    String houseNumber,
    String city,
    String state,
    String country,
    String postalCode) {}

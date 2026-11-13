package com.toolshop.data;

/** Línea de un carrito según la API. */
public record CartItem(String productId, String productName, int quantity) {}

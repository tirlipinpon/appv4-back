package com.appv4.hello.infrastructure.adapter.in.web;

/**
 * Contrat HTTP sortant partagé par {@code /api/ext/hello} et {@code /api/int/hello}.
 */
public record HelloMessageResponse(String text) {
}

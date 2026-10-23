package com.appv4.hello.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contrat HTTP sortant partagé par {@code /api/ext/hello} et {@code /api/int/hello}.
 */
@Schema(name = "HelloMessageResponse")
public record HelloMessageResponse(String text) {
}

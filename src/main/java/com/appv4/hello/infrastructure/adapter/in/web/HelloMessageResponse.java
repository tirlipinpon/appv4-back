package com.appv4.hello.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contrat HTTP sortant partagé par les hello learning, admin, parent et professeur.
 */
@Schema(name = "HelloMessageResponse")
public record HelloMessageResponse(String text) {
}

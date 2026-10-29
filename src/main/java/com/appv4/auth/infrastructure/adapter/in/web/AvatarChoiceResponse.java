package com.appv4.auth.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Avatar proposé sur la fiche du compte.
 *
 * <p><b>Business :</b> l'écran affiche le libellé ; le code est celui qui est enregistré.
 * <p><b>Technique :</b> les six codes sont ceux des enfants (hibou, renard, ours, chat, panda, grenouille).
 * <p><b>Métier / flow :</b> champ {@code avatarChoices} des fiches parent et professeur.
 */
@Schema(name = "AvatarChoice")
public record AvatarChoiceResponse(String code, String label) {
}

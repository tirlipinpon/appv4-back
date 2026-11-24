package com.appv4.ai.application;

import java.time.LocalDate;
import java.util.List;

/**
 * La période UTC renvoyée avec ses lignes (BACK-32).
 *
 * <p><b>Business :</b> sans dates, la période est le jour UTC d'aujourd'hui. Une période vide n'a pas de ligne.
 */
public record LectureUsage(LocalDate du, LocalDate au, List<LigneUsage> lignes) {
}

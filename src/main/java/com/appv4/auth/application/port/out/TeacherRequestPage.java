package com.appv4.auth.application.port.out;

import java.util.List;

/**
 * Page bornée de la file de modération (issue #55).
 *
 * @param items     demandes renvoyées (taille ≤ limite demandée)
 * @param truncated {@code true} s'il reste des lignes au-delà de la limite
 */
public record TeacherRequestPage(List<TeacherRequestView> items, boolean truncated) {
}

package com.appv4.ai.application;

import java.util.List;

import com.appv4.ai.application.port.out.RouteBesoin;

/**
 * Les besoins et les fournisseurs qu'on peut leur brancher (BACK-32).
 *
 * <p><b>Business :</b> la liste des fournisseurs est celle des adaptateurs, pas un catalogue inventé.
 */
public record LectureBesoins(List<RouteBesoin> besoins, List<String> fournisseurs) {
}

package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.ChildSubjectBoardView;

/**
 * Lecture du board matière enfant (BACK-33).
 *
 * <p><b>Business :</b> seule une matière liée et visible s'ouvre.
 * <p><b>Technique :</b> jeton enfant → board + thèmes + progression.
 * <p><b>Métier / flow :</b> {@code GET /api/ext/subjects/{id}} → ce port.
 */
public interface ReadChildSubjectBoardUseCase {

	ChildSubjectBoardView board(String rawToken, long subjectId);
}

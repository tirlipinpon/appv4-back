package com.appv4.auth.application.result;

import java.util.List;

/**
 * Collègues à qui on peut proposer un transfert pour une matière (SCRUM-189).
 *
 * <p><b>Business :</b> professeurs acceptés avec la même école et la même année.
 * <p><b>Technique :</b> tri français sur le nom affiché.
 * <p><b>Métier / flow :</b> {@code GET /api/int/teacher/subjects/{id}/colleagues}.
 */
public record TeacherSubjectColleaguesView(List<ColleagueLine> colleagues) {

	public TeacherSubjectColleaguesView {
		colleagues = colleagues == null ? List.of() : List.copyOf(colleagues);
	}

	public record ColleagueLine(long userId, String displayName) {
	}
}

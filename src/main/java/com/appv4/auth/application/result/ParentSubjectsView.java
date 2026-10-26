package com.appv4.auth.application.result;

import java.util.List;

/**
 * Tous les enfants du parent, avec leurs matières (SCRUM-162).
 *
 * <p><b>Business :</b> la page matières montre chaque élève et ses cours.
 * <p><b>Technique :</b> pas le JSON HTTP.
 */
public record ParentSubjectsView(List<ChildSubjectsView> children) {

	public ParentSubjectsView {
		children = children == null ? List.of() : List.copyOf(children);
	}
}

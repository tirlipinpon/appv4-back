package com.appv4.auth.application.service;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.FollowingChild;
import com.appv4.auth.application.port.out.ChildSubjectPort.SubjectChildLink;

/**
 * Noms des enfants qui suivent encore une matière.
 *
 * <p><b>Business :</b> un enfant de la classe compte, sauf si le parent a retiré
 * le cours. Un enfant d'une autre classe compte s'il a été ajouté à ce cours.
 * Le même calcul sert avant de supprimer des questions et avant de retirer
 * une année ou une école.
 * <p><b>Technique :</b> {@link ChildSubjectPort#activeInClass} puis les liens
 * de la matière. Le tri suit le français, puis l'identifiant.
 * <p><b>Métier / flow :</b> appelé par le service des questions et par l'aperçu
 * du profil professeur. Il ne retire rien.
 */
public final class SubjectFollowers {

	private SubjectFollowers() {
	}

	public static List<String> names(ChildSubjectPort port, long schoolId, String grade, long subjectId) {
		Map<Long, FollowingChild> byId = new LinkedHashMap<>();
		for (FollowingChild child : orEmpty(port.activeInClass(schoolId, grade))) {
			if (child != null) {
				byId.put(child.id(), child);
			}
		}
		for (SubjectChildLink link : orEmpty(port.linksOfSubject(subjectId))) {
			if (link == null || link.child() == null) {
				continue;
			}
			if (link.excluded()) {
				byId.remove(link.child().id());
			} else {
				byId.put(link.child().id(), link.child());
			}
		}
		List<FollowingChild> children = new ArrayList<>();
		for (FollowingChild child : byId.values()) {
			if (!displayName(child).isEmpty()) {
				children.add(child);
			}
		}
		Collator order = Collator.getInstance(Locale.FRENCH);
		order.setStrength(Collator.PRIMARY);
		children.sort(Comparator
				.comparing(SubjectFollowers::displayName, order)
				.thenComparingLong(FollowingChild::id));
		List<String> names = new ArrayList<>();
		for (FollowingChild child : children) {
			names.add(displayName(child));
		}
		return List.copyOf(names);
	}

	private static String displayName(FollowingChild child) {
		String first = child.firstName() == null ? "" : child.firstName().trim();
		String last = child.lastName() == null ? "" : child.lastName().trim();
		if (first.isEmpty()) {
			return last;
		}
		if (last.isEmpty()) {
			return first;
		}
		return first + " " + last;
	}

	private static <T> List<T> orEmpty(List<T> rows) {
		return rows == null ? List.of() : rows;
	}
}

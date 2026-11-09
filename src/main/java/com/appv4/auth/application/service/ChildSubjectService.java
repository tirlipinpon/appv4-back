package com.appv4.auth.application.service;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ChildSubjectUseCase;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildPort.ChildSaved;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.result.ParentSubjectsView;
import com.appv4.auth.application.port.out.SchoolCatalogPort;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.result.ChildSubjectsView;
import com.appv4.auth.application.result.ChildSubjectsView.SubjectOffer;

/**
 * Liste, ajout et retrait des matières d'un enfant (SCRUM-162).
 *
 * <p><b>Business :</b> chaque enfant a déjà les matières visibles de son école et de
 * son année. Le parent en ajoute d'autres, ou retire une matière de la liste.
 * Retirer une matière de la classe ne la remet pas toute seule au prochain passage.
 * <p><b>Technique :</b> le lien de classe retiré passe à {@code excluded}. Un cours
 * hors classe perd seulement sa ligne de lien. La matière du professeur n'est pas effacée.
 * <p><b>Métier / flow :</b> page /parent/matieres → GET /api/int/parent/subjects
 * rattache les matières de la classe → POST ajoute, DELETE retire.
 */
public class ChildSubjectService implements ChildSubjectUseCase {

	public static final String CHILD_UNKNOWN = "Cet enfant est introuvable.";
	public static final String SUBJECT_UNAVAILABLE = "Cette matière n'est pas proposable.";
	public static final String ALREADY_LINKED = "Cette matière est déjà dans la liste.";
	public static final String LINK_MISSING = "Cette matière n'est pas dans la liste.";

	private final ChildPort childPort;
	private final ChildSubjectPort childSubjectPort;
	private final SchoolCatalogPort schoolCatalogPort;
	private final TeacherQuestionPort teacherQuestionPort;

	public ChildSubjectService(
			ChildPort childPort,
			ChildSubjectPort childSubjectPort,
			SchoolCatalogPort schoolCatalogPort,
			TeacherQuestionPort teacherQuestionPort) {
		this.childPort = childPort;
		this.childSubjectPort = childSubjectPort;
		this.schoolCatalogPort = schoolCatalogPort;
		this.teacherQuestionPort = teacherQuestionPort;
	}

	@Override
	@Transactional
	public ParentSubjectsView loadSummary(long parentUserId, String activeRole) {
		requireParent(activeRole);
		List<ChildSubjectsView> result = new ArrayList<>();
		for (ChildSaved child : childPort.findByParent(parentUserId)) {
			ensure(child, classCatalog(child));
			List<Long> ids = linksOf(child.id()).stream().filter(link -> !link.excluded())
					.map(StoredLink::subjectId).toList();
			List<CatalogSubject> subjects = childSubjectPort.visibleByIds(ids);
			List<Long> schoolIds = new ArrayList<>(subjects.stream().map(CatalogSubject::schoolId).distinct().toList());
			if (child.schoolId() != null && !schoolIds.contains(child.schoolId())) {
				schoolIds.add(child.schoolId());
			}
			List<CatalogSchool> schools = schoolCatalogPort.findByIds(schoolIds);
			Map<Long, Long> questionCounts = teacherQuestionPort.countBySubjectIds(
					subjects.stream().map(CatalogSubject::id).toList());
			List<SubjectOffer> chosen = new ArrayList<>();
			for (CatalogSubject subject : subjects) {
				chosen.add(offer(child, subject, schools, questionCounts));
			}
			sort(chosen);
			result.add(new ChildSubjectsView(child.id(), child.firstName(), child.lastName(), child.schoolId(),
					schoolName(child.schoolId() == null ? -1L : child.schoolId(), schools),
					child.grade(), chosen, List.of()));
		}
		return new ParentSubjectsView(result);
	}

	@Override
	@Transactional(readOnly = true)
	public SubjectPage searchOffers(long parentUserId, String activeRole, long childId, String query,
			long schoolId, String grade, long afterId, int limit) {
		requireParent(activeRole);
		ChildSaved child = requireChild(parentUserId, childId);
		String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		String year = grade == null ? "" : grade.trim();
		if (text.length() > 100 || year.length() > 80) {
			throw bad("Recherche trop longue.");
		}
		int size = Math.max(1, Math.min(40, limit));
		List<CatalogSubject> rows = childSubjectPort.searchOffers(childId, text, Math.max(0, schoolId), year,
				child.schoolId() == null ? 0 : child.schoolId(), child.grade() == null ? "" : child.grade(),
				Math.max(0, afterId), size + 1);
		boolean more = rows.size() > size;
		List<CatalogSubject> page = more ? rows.subList(0, size) : rows;
		List<CatalogSchool> schools = schoolCatalogPort.findByIds(page.stream().map(CatalogSubject::schoolId).distinct().toList());
		Map<Long, Long> questionCounts = teacherQuestionPort.countBySubjectIds(
				page.stream().map(CatalogSubject::id).toList());
		List<SubjectOffer> offers = page.stream()
				.map(subject -> offer(child, subject, schools, questionCounts))
				.toList();
		return new SubjectPage(offers, more ? page.get(page.size() - 1).id() : null);
	}

	@Override
	@Transactional
	public SubjectOffer addOne(long parentUserId, String activeRole, long childId, Long subjectId) {
		requireParent(activeRole);
		ChildSaved child = requireChild(parentUserId, childId);
		CatalogSubject subject = subjectId == null ? null : directSubject(subjectId);
		if (subject == null) {
			throw bad(SUBJECT_UNAVAILABLE);
		}
		if (sameClass(child, subject.schoolId(), subject.grade())) {
			childSubjectPort.ensureDefault(childId, subject.id());
		}
		if (activeLink(childId, subject.id())) {
			throw bad(ALREADY_LINKED);
		}
		childSubjectPort.save(childId, subject.id(), false);
		return offer(child, subject, schoolCatalogPort.findByIds(List.of(subject.schoolId())),
				teacherQuestionPort.countBySubjectIds(List.of(subject.id())));
	}

	@Override
	@Transactional
	public SubjectOffer removeOne(long parentUserId, String activeRole, long childId, long subjectId) {
		requireParent(activeRole);
		ChildSaved child = requireChild(parentUserId, childId);
		CatalogSubject subject = directSubject(subjectId);
		if (subject == null) {
			throw missing(LINK_MISSING);
		}
		if (sameClass(child, subject.schoolId(), subject.grade())) {
			childSubjectPort.ensureDefault(childId, subjectId);
		}
		if (!activeLink(childId, subjectId)) {
			throw missing(LINK_MISSING);
		}
		if (sameClass(child, subject.schoolId(), subject.grade())) {
			childSubjectPort.save(childId, subjectId, true);
		} else if (!childSubjectPort.deleteLink(childId, subjectId)) {
			throw missing(LINK_MISSING);
		}
		return offer(child, subject, schoolCatalogPort.findByIds(List.of(subject.schoolId())),
				teacherQuestionPort.countBySubjectIds(List.of(subject.id())));
	}

	private List<CatalogSubject> classCatalog(ChildSaved child) {
		if (child.schoolId() == null || child.grade() == null || child.grade().isBlank()) {
			return List.of();
		}
		return childSubjectPort.visibleInClass(child.schoolId(), child.grade());
	}

	private CatalogSubject directSubject(long subjectId) {
		return childSubjectPort.visibleByIds(List.of(subjectId)).stream().findFirst().orElse(null);
	}

	@Override
	@Transactional
	public ParentSubjectsView loadAll(long parentUserId, String activeRole) {
		requireParent(activeRole);
		return family(parentUserId);
	}

	@Override
	@Transactional
	public ParentSubjectsView add(long parentUserId, String activeRole, long childId, Long subjectId) {
		requireParent(activeRole);
		ChildSaved child = requireChild(parentUserId, childId);
		ensure(child, catalog());
		if (subjectId == null || !visible(subjectId)) {
			throw bad(SUBJECT_UNAVAILABLE);
		}
		StoredLink existing = linkOf(childId, subjectId);
		if (existing != null && !existing.excluded()) {
			throw bad(ALREADY_LINKED);
		}
		childSubjectPort.save(childId, subjectId, false);
		return family(parentUserId);
	}

	@Override
	@Transactional
	public ParentSubjectsView remove(long parentUserId, String activeRole, long childId, long subjectId) {
		requireParent(activeRole);
		ChildSaved child = requireChild(parentUserId, childId);
		ensure(child, catalog());
		CatalogSubject subject = findVisible(subjectId);
		if (subject == null || !activeLink(childId, subjectId)) {
			throw missing(LINK_MISSING);
		}
		if (sameClass(child, subject.schoolId(), subject.grade())) {
			childSubjectPort.save(childId, subjectId, true);
		} else if (!childSubjectPort.deleteLink(childId, subjectId)) {
			throw missing(LINK_MISSING);
		}
		return family(parentUserId);
	}

	/**
	 * Rattache les matières visibles de l'école et de l'année, si le parent ne les a pas retirées.
	 */
	public void ensureDefaults(long parentUserId) {
		for (ChildSaved child : childPort.findByParent(parentUserId)) {
			ensure(child, classCatalog(child));
		}
	}

	private ParentSubjectsView family(long parentUserId) {
		List<CatalogSubject> catalog = catalog();
		List<CatalogSchool> schools = schoolCatalogPort.list();
		Map<Long, Long> questionCounts = teacherQuestionPort.countBySubjectIds(
				catalog.stream().map(CatalogSubject::id).toList());
		List<ChildSubjectsView> children = new ArrayList<>();
		for (ChildSaved child : childPort.findByParent(parentUserId)) {
			ensure(child, catalog);
			children.add(view(child, catalog, schools, questionCounts));
		}
		return new ParentSubjectsView(children);
	}

	private void ensure(ChildSaved child, List<CatalogSubject> catalog) {
		if (child.schoolId() == null || child.grade() == null || child.grade().isBlank()) {
			return;
		}
		Set<Long> known = new HashSet<>();
		for (StoredLink link : linksOf(child.id())) {
			known.add(link.subjectId());
		}
		for (CatalogSubject subject : catalog) {
			if (!sameClass(child, subject.schoolId(), subject.grade()) || known.contains(subject.id())) {
				continue;
			}
			childSubjectPort.ensureDefault(child.id(), subject.id());
		}
	}

	private ChildSubjectsView view(
			ChildSaved child,
			List<CatalogSubject> catalog,
			List<CatalogSchool> schools,
			Map<Long, Long> questionCounts) {
		Set<Long> chosenIds = new HashSet<>();
		for (StoredLink link : linksOf(child.id())) {
			if (!link.excluded()) {
				chosenIds.add(link.subjectId());
			}
		}
		List<SubjectOffer> chosen = new ArrayList<>();
		List<SubjectOffer> proposed = new ArrayList<>();
		for (CatalogSubject subject : catalog) {
			SubjectOffer offer = offer(child, subject, schools, questionCounts);
			if (chosenIds.contains(subject.id())) {
				chosen.add(offer);
			} else {
				proposed.add(offer);
			}
		}
		sort(chosen);
		sort(proposed);
		return new ChildSubjectsView(
				child.id(),
				child.firstName(),
				child.lastName(),
				child.schoolId(),
				schoolName(child.schoolId() == null ? -1L : child.schoolId(), schools),
				child.grade(),
				chosen,
				proposed);
	}

	private SubjectOffer offer(
			ChildSaved child,
			CatalogSubject subject,
			List<CatalogSchool> schools,
			Map<Long, Long> questionCounts) {
		boolean scolaire = sameClass(child, subject.schoolId(), subject.grade());
		return new SubjectOffer(
				subject.id(),
				subject.name(),
				subject.teacherName() == null ? "" : subject.teacherName(),
				subject.schoolId(),
				schoolName(subject.schoolId(), schools),
				subject.grade(),
				scolaire,
				questionCounts.getOrDefault(subject.id(), 0L));
	}

	static boolean sameClass(ChildSaved child, long schoolId, String grade) {
		return child.schoolId() != null
				&& child.grade() != null
				&& !child.grade().isBlank()
				&& child.schoolId() == schoolId
				&& child.grade().equals(grade);
	}

	private static String schoolName(long schoolId, List<CatalogSchool> schools) {
		if (schools == null) {
			return "";
		}
		for (CatalogSchool school : schools) {
			if (school.id() == schoolId) {
				return school.name() == null ? "" : school.name();
			}
		}
		return "";
	}

	private List<CatalogSubject> catalog() {
		List<CatalogSubject> catalog = childSubjectPort.visibleSubjects();
		return catalog == null ? List.of() : catalog;
	}

	private List<StoredLink> linksOf(long childId) {
		List<StoredLink> links = childSubjectPort.linksOf(childId);
		return links == null ? List.of() : links;
	}

	private StoredLink linkOf(long childId, long subjectId) {
		return childSubjectPort.findLink(childId, subjectId).orElse(null);
	}

	private boolean activeLink(long childId, long subjectId) {
		StoredLink link = linkOf(childId, subjectId);
		return link != null && !link.excluded();
	}

	private CatalogSubject findVisible(long subjectId) {
		for (CatalogSubject subject : catalog()) {
			if (subject.id() == subjectId) {
				return subject;
			}
		}
		return null;
	}

	private boolean visible(long subjectId) {
		return findVisible(subjectId) != null;
	}

	private static void sort(List<SubjectOffer> offers) {
		Collator order = Collator.getInstance(Locale.FRENCH);
		order.setStrength(Collator.PRIMARY);
		offers.sort(Comparator
				.comparing(SubjectOffer::name, order)
				.thenComparing(SubjectOffer::teacherName, order));
	}

	private ChildSaved requireChild(long parentUserId, long childId) {
		return childPort.findOwned(parentUserId, childId)
				.orElseThrow(() -> missing(CHILD_UNKNOWN));
	}

	private static void requireParent(String activeRole) {
		if (!ActiveRoles.PARENT.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceHello.DENIED);
		}
	}

	private static AuthDomainException bad(String message) {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, message);
	}

	private static AuthDomainException missing(String message) {
		return new AuthDomainException(AuthErrorCode.NOT_FOUND, message);
	}
}

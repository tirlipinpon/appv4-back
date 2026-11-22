package com.appv4.auth.application.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ParentChildrenUseCase;
import com.appv4.auth.application.port.in.ParentChildrenUseCase.InvitationReceipt;
import com.appv4.auth.application.port.in.ParentChildrenUseCase.PendingInvitation;
import com.appv4.auth.application.port.in.ParentChildrenUseCase.ShareRequestChild;
import com.appv4.auth.application.port.in.ParentChildrenUseCase.ShareRequestView;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.ChildAvatarPort;
import com.appv4.auth.application.port.out.ChildSharePort;
import com.appv4.auth.application.port.out.ChildSharePort.StoredDemande;
import com.appv4.auth.application.port.out.ChildSharePort.StoredInvitation;
import com.appv4.auth.application.port.out.ChildAvatarPort.StoredAvatar;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildPort.ChildChanges;
import com.appv4.auth.application.port.out.ChildPort.ChildLinkView;
import com.appv4.auth.application.port.out.ChildPort.ChildPhoto;
import com.appv4.auth.application.port.out.ChildPort.ChildSaved;
import com.appv4.auth.application.port.out.ChildPort.InterestCopy;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CountedLink;
import com.appv4.auth.application.port.out.ChildPort.NewChild;
import com.appv4.auth.application.port.out.SchoolCatalogPort;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.result.ParentChildrenView;
import com.appv4.auth.application.result.ParentChildrenView.CharacterFace;
import com.appv4.auth.application.result.ParentChildrenView.CharacterPin;
import com.appv4.auth.application.result.ParentChildrenView.ChildCard;
import com.appv4.auth.application.result.ParentChildrenView.GradeList;
import com.appv4.auth.application.result.ParentChildrenView.KinshipMark;
import com.appv4.auth.application.result.ParentChildrenView.SchoolChoice;

/**
 * Enregistre les enfants d'un parent (SCRUM-156).
 *
 * <p><b>Business :</b> prénom, nom, âge, classe et une image sont obligatoires.
 * L'école se choisit dans l'annuaire, ou reste vide. Le code à 4 chiffres est hashé.
 * <p><b>Technique :</b> une écriture. Si une règle refuse, aucune fiche n'est créée.
 * Le pays choisit les noms de classes. Une école choisie limite ces classes aux siennes.
 * <p><b>Métier / flow :</b> port in → hash PIN → port enfant + référentiel d'écoles.
 */
public class ParentChildrenService implements ParentChildrenUseCase {

	public static final String FIRST_NAME_REQUIRED = "Le prénom est obligatoire.";
	public static final String LAST_NAME_REQUIRED = "Le nom est obligatoire.";
	public static final String NAME_TOO_LONG = "Le prénom ou le nom est trop long (maximum 100).";
	public static final String AGE_INVALID = "L'âge doit être un nombre entier entre 2 et 18.";
	public static final String COUNTRY_REQUIRED = "Choisis la norme : France ou Belgique.";
	public static final String GRADE_REQUIRED = "Choisis une classe.";
	public static final String GRADE_NOT_IN_CURRICULUM = "Cette classe n'appartient pas à la norme choisie.";
	public static final String GRADE_NOT_OFFERED = "Cette classe n'est pas proposée par cette école.";
	public static final String SCHOOL_UNKNOWN = "Cette école n'est pas proposable.";
	public static final String IMAGE_REQUIRED = "Choisis une photo ou un avatar.";
	public static final String NOT_AN_IMAGE = "Le fichier n'est pas une image.";
	public static final String IMAGE_TOO_LARGE = "L'image dépasse 5 Mo.";
	public static final String AVATAR_UNKNOWN = "Cet avatar n'est pas proposé.";
	public static final String PIN_FORMAT = "Le code PIN doit faire 4 chiffres.";
	public static final String PIN_MISMATCH = "Les deux codes PIN ne sont pas les mêmes.";
	public static final String CHILD_UNKNOWN = "Cet enfant est introuvable.";
	public static final String INVITATION_SENT = "Invitation envoyée";
	public static final String INVITATION_PENDING = "Une invitation est déjà en attente pour cet email.";
	public static final String ALREADY_HOLDS = "Ce parent a déjà cette fiche.";
	public static final String WRONG_ACCOUNT = "Cette invitation ne s'adresse pas à ce compte.";
	public static final String ONLY_REFERENT = "Seul le parent référent peut faire ça.";
	public static final String EMAIL_REQUIRED = "L'email est manquant.";
	public static final String DEMANDE_SENT = "Demande envoyée";
	public static final String ALREADY_YOURS = "Cette fiche est déjà chez vous.";
	public static final String LINK_UNKNOWN = "Ce lien n'est pas proposé.";
	public static final String LINK_NEEDS_CHILD = "Choisis l'enfant de départ.";
	public static final String LINK_SELF = "Un enfant ne se lie pas à lui-même.";
	public static final String SEX_REQUIRED = "Choisis fille, garçon ou non genré.";
	public static final String SEX_UNKNOWN = "Ce sexe n'est pas proposé.";

	public static final int MIN_AGE = 2;
	public static final int MAX_AGE = 18;
	public static final int MAX_NAME = 100;
	public static final int MAX_PHOTO_BYTES = 5 * 1024 * 1024;

	public static final Set<String> AVATARS = Set.of("owl", "fox", "bear", "cat", "panda", "frog");

	private final ChildPort childPort;
	private final SchoolCatalogPort schoolCatalogPort;
	private final PasswordEncoder passwordEncoder;
	private final ChildSubjectPort childSubjectPort;
	private final ChildSubjectService childSubjectService;
	private final ChildAvatarPort childAvatarPort;
	private final CharacterAvatarCatalog avatarCatalog;
	private final Clock clock;
	private final ChildSharePort childSharePort;
	private final AuthUserAccountPort accounts;

	public ParentChildrenService(
			ChildPort childPort,
			SchoolCatalogPort schoolCatalogPort,
			PasswordEncoder passwordEncoder,
			ChildSubjectPort childSubjectPort,
			ChildSubjectService childSubjectService,
			ChildAvatarPort childAvatarPort,
			CharacterAvatarCatalog avatarCatalog,
			Clock clock,
			ChildSharePort childSharePort,
			AuthUserAccountPort accounts) {
		this.childPort = childPort;
		this.schoolCatalogPort = schoolCatalogPort;
		this.passwordEncoder = passwordEncoder;
		this.childSubjectPort = childSubjectPort;
		this.childSubjectService = childSubjectService;
		this.childAvatarPort = childAvatarPort;
		this.avatarCatalog = avatarCatalog;
		this.clock = clock;
		this.childSharePort = childSharePort;
		this.accounts = accounts;
	}

	/**
	 * Liste les fiches. Le rattachement des matières de la classe se termine avant la lecture.
	 *
	 * <p><b>Business :</b> le parent voit ses enfants et le nombre de matières, sans attendre les photos.
	 * <p><b>Technique :</b> l'insert éventuel commit, puis les lectures. Pas une seule transaction d'écriture.
	 * <p><b>Métier / flow :</b> {@code GET /api/int/parent/children}.
	 */
	@Override
	public ParentChildrenView list(long parentUserId, String activeRole) {
		requireParent(activeRole);
		childSubjectService.ensureDefaults(parentUserId);
		return view(parentUserId);
	}

	/**
	 * Crée la fiche. Si {@code fromChildId} est présent, le formulaire est parti de cet enfant.
	 *
	 * <p><b>Business :</b> l'enfant de départ n'est pas modifié. Son PIN n'est ni relu ni recopié.
	 * Un lien autre qu'Aucun s'affiche ensuite sur les deux fiches. Un refus ne crée rien.
	 * <p><b>Technique :</b> validation avant l'insert. Le hash du nouveau PIN est un nouvel encodage.
	 * <p><b>Métier / flow :</b> même port in que l'ajout vide (SCRUM-156), avec le lien de SCRUM-158.
	 */
	@Override
	@Transactional
	public ChildCard add(long parentUserId, String activeRole, NewChildCommand command) {
		requireParent(activeRole);
		NewChildCommand body = command == null
				? new NewChildCommand(
						null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
				: command;
		ChildSaved source = sourceOf(parentUserId, body.fromChildId());
		Prepared prepared = prepare(body);
		String relation = relationOf(body.link(), source != null);
		List<InterestCopy> interests = ChildInterestList.resolve(body.interests(), List.of(), clock.instant());
		String pinHash = passwordEncoder.encode(prepared.pin());
		ChildSaved saved = childPort.insert(new NewChild(
				parentUserId,
				prepared.firstName(),
				prepared.lastName(),
				prepared.sex(),
				prepared.age(),
				prepared.curriculum(),
				prepared.grade(),
				prepared.schoolId(),
				prepared.avatarCode(),
				prepared.photo(),
				prepared.photoContentType(),
				pinHash));
		if (relation != null && source != null) {
			childPort.insertLink(parentUserId, saved.id(), source.id(), relation);
		}
		childPort.replaceInterests(saved.id(), interests);
		childSubjectService.ensureDefaults(parentUserId);
		Attached attached = attachedFor(parentUserId, saved.id());
		return card(
				saved,
				catalogById(saved.schoolId()),
				attached.phrases(),
				countsFor(parentUserId, saved),
				attached.marks(),
				childAvatarPort.findByChildId(saved.id()).orElse(null),
				ChildInterestList.spellings(interests),
				parentUserId);
	}

	/**
	 * Corrige une fiche déjà affichée. Un PIN vide garde l'ancien code.
	 * Un lien choisi est créé, ou son mot est mis à jour si les deux enfants sont déjà liés.
	 *
	 * <p><b>Business :</b> désactivé ou non, la fiche se corrige. Une fiche retirée de l'affichage est introuvable.
	 * « Aucun » avec un enfant choisi retire ce lien. Sans enfant choisi, les liens restent.
	 * <p><b>Technique :</b> l'image n'est remplacée que si une photo ou un avatar est envoyé.
	 * Le lien est validé avant l'écriture de la fiche.
	 * <p><b>Métier / flow :</b> même formulaire que l'ajout, sur {@code PUT}.
	 */
	@Override
	@Transactional
	public ChildCard update(long parentUserId, String activeRole, long childId, NewChildCommand command) {
		requireParent(activeRole);
		requireHolder(parentUserId, childId);
		NewChildCommand body = command == null
				? new NewChildCommand(
						null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
				: command;
		List<InterestCopy> interests = ChildInterestList.resolve(
				body.interests(), childPort.findInterests(childId), clock.instant());
		String firstName = requiredName(body.firstName(), FIRST_NAME_REQUIRED);
		String lastName = requiredName(body.lastName(), LAST_NAME_REQUIRED);
		String sex = requireSex(body.sex());
		int age = parseAge(body.age());
		String curriculum = curriculumOf(body.country());
		CatalogSchool school = resolveSchool(body.schoolId(), curriculum);
		String grade = requiredGrade(body.grade(), curriculum, school);
		boolean replaceImage = hasPhoto(body) || hasAvatar(body);
		Image image = replaceImage
				? resolveImage(body.avatarCode(), body.photoBase64(), body.photoContentType())
				: new Image(null, null, null);
		String pinHash = blank(body.pin()) && blank(body.pinConfirmation())
				? null
				: passwordEncoder.encode(requirePin(body.pin(), body.pinConfirmation()));
		Long otherId = body.fromChildId();
		LinkEdit linkEdit = linkOnUpdate(parentUserId, childId, otherId, body.link());
		ChildSaved saved = childPort.update(parentUserId, childId, new ChildChanges(
				firstName,
				lastName,
				sex,
				age,
				curriculum,
				grade,
				school == null ? null : school.id(),
				replaceImage,
				image.avatarCode(),
				image.photo(),
				image.contentType(),
				pinHash)).orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN));
		childPort.replaceInterests(saved.id(), interests);
		if (otherId != null && linkEdit.remove()) {
			childPort.deleteLink(parentUserId, saved.id(), otherId);
		} else if (otherId != null && linkEdit.relation() != null) {
			childPort.saveLink(parentUserId, saved.id(), otherId, linkEdit.relation());
		}
		childSubjectService.ensureDefaults(parentUserId);
		Attached attached = attachedFor(parentUserId, saved.id());
		return card(
				saved,
				catalogById(saved.schoolId()),
				attached.phrases(),
				countsFor(parentUserId, saved),
				attached.marks(),
				childAvatarPort.findByChildId(saved.id()).orElse(null),
				ChildInterestList.spellings(interests),
				parentUserId);
	}

	@Override
	@Transactional
	public void deactivate(long parentUserId, String activeRole, long childId) {
		requireParent(activeRole);
		requireReferent(parentUserId, childId);
		if (!childPort.deactivate(parentUserId, childId)) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN);
		}
	}

	@Override
	@Transactional
	public void reactivate(long parentUserId, String activeRole, long childId) {
		requireParent(activeRole);
		requireReferent(parentUserId, childId);
		if (!childPort.reactivate(parentUserId, childId)) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN);
		}
	}

	@Override
	@Transactional
	public void hide(long parentUserId, String activeRole, long childId) {
		requireParent(activeRole);
		requireReferent(parentUserId, childId);
		ChildSaved current = childPort.findOwned(parentUserId, childId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN));
		if (!childPort.hide(parentUserId, childId)) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN);
		}
		childSharePort.cancelPending(childId);
		if (matching(parentUserId, current.firstName(), current.lastName()).isEmpty()) {
			childSharePort.cancelDemandes(parentUserId, current.firstName(), current.lastName());
		}
	}

	@Override
	@Transactional
	public InvitationReceipt invite(long parentUserId, String activeRole, long childId, String email) {
		requireParent(activeRole);
		requireReferent(parentUserId, childId);
		String normalized = normalizeEmail(email);
		if (childSharePort.pending(childId, normalized)) {
			throw bad(INVITATION_PENDING);
		}
		accounts.findByEmail(normalized)
				.filter(account -> holds(account.id(), childId))
				.ifPresent(account -> {
					throw bad(ALREADY_HOLDS);
				});
		childSharePort.invite(childId, normalized);
		return new InvitationReceipt(INVITATION_SENT);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PendingInvitation> pendingInvitations(long parentUserId, String activeRole) {
		requireParent(activeRole);
		String email = accounts.findById(parentUserId).map(AuthUserAccount::email).orElse("");
		if (email.isBlank()) {
			return List.of();
		}
		List<PendingInvitation> found = new java.util.ArrayList<>();
		for (StoredInvitation invitation : childSharePort.pendingForEmail(normalizeEmail(email))) {
			ChildSaved child = childPort.findVisible(invitation.childId()).orElse(null);
			if (child == null) {
				continue;
			}
			found.add(new PendingInvitation(invitation.id(), child.id(), child.firstName(), child.lastName()));
		}
		return List.copyOf(found);
	}

	@Override
	@Transactional
	public ChildCard acceptInvitation(long parentUserId, String activeRole, long invitationId) {
		requireParent(activeRole);
		StoredInvitation invitation = childSharePort.findPending(invitationId)
				.orElseThrow(() -> bad(WRONG_ACCOUNT));
		String email = accounts.findById(parentUserId).map(AuthUserAccount::email).orElse("");
		if (!normalizeEmail(email).equals(invitation.email())) {
			throw bad(WRONG_ACCOUNT);
		}
		childSharePort.accept(invitationId, parentUserId);
		ChildSaved saved = childPort.findOwned(parentUserId, invitation.childId())
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN));
		childSubjectService.ensureDefaults(parentUserId);
		Attached attached = attachedFor(parentUserId, saved.id());
		return card(
				saved,
				catalogById(saved.schoolId()),
				attached.phrases(),
				countsFor(parentUserId, saved),
				attached.marks(),
				childAvatarPort.findByChildId(saved.id()).orElse(null),
				ChildInterestList.spellings(childPort.findInterests(saved.id())),
				parentUserId);
	}

	@Override
	@Transactional
	public InvitationReceipt requestShare(
			long parentUserId, String activeRole, String email, String firstName, String lastName) {
		requireParent(activeRole);
		String normalizedEmail = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
		String first = firstName == null ? "" : firstName.trim();
		String last = lastName == null ? "" : lastName.trim();
		if (normalizedEmail.isBlank() || first.isBlank() || last.isBlank()) {
			return new InvitationReceipt(DEMANDE_SENT);
		}
		var account = accounts.findByEmail(normalizedEmail);
		if (account.isEmpty() || !referentOfAny(account.get().id())) {
			return new InvitationReceipt(DEMANDE_SENT);
		}
		long referentId = account.get().id();
		List<ChildSaved> open = matching(referentId, first, last).stream()
				.filter(child -> !holds(parentUserId, child.id()))
				.toList();
		if (open.isEmpty()) {
			if (matching(referentId, first, last).isEmpty()) {
				return new InvitationReceipt(DEMANDE_SENT);
			}
			throw bad(ALREADY_YOURS);
		}
		if (!childSharePort.pendingDemande(parentUserId, referentId, first, last)) {
			childSharePort.saveDemande(parentUserId, referentId, first, last);
		}
		return new InvitationReceipt(DEMANDE_SENT);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShareRequestView> shareRequests(long parentUserId, String activeRole) {
		requireParent(activeRole);
		List<ShareRequestView> views = new java.util.ArrayList<>();
		for (StoredDemande demande : childSharePort.demandesForReferent(parentUserId)) {
			if (!"pending".equals(demande.status())) {
				continue;
			}
			List<ShareRequestChild> matches = matching(parentUserId, demande.firstName(), demande.lastName()).stream()
					.filter(child -> !holds(demande.senderId(), child.id()))
					.map(child -> new ShareRequestChild(child.id(), child.firstName(), child.lastName()))
					.toList();
			if (matches.isEmpty()) {
				continue;
			}
			views.add(new ShareRequestView(demande.id(), demande.firstName(), demande.lastName(), matches));
		}
		return List.copyOf(views);
	}

	@Override
	@Transactional
	public void acceptShareRequest(long parentUserId, String activeRole, long requestId, long childId) {
		requireParent(activeRole);
		StoredDemande demande = pendingDemande(requestId);
		if (demande.referentId() != parentUserId || !referent(parentUserId, childId)) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN);
		}
		ChildSaved child = childPort.findOwned(parentUserId, childId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN));
		if (!sameName(child, demande.firstName(), demande.lastName()) || holds(demande.senderId(), childId)) {
			throw bad(ALREADY_YOURS);
		}
		childSharePort.grantShare(childId, demande.senderId());
		childSharePort.markDemande(requestId, "accepted");
	}

	@Override
	@Transactional
	public void refuseShareRequest(long parentUserId, String activeRole, long requestId) {
		requireParent(activeRole);
		StoredDemande demande = pendingDemande(requestId);
		if (demande.referentId() != parentUserId) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN);
		}
		childSharePort.markDemande(requestId, "refused");
	}

	@Override
	@Transactional
	public void cancelShareRequest(long parentUserId, String activeRole, long requestId) {
		requireParent(activeRole);
		StoredDemande demande = pendingDemande(requestId);
		if (demande.senderId() != parentUserId) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN);
		}
		childSharePort.markDemande(requestId, "cancelled");
	}

	private StoredDemande pendingDemande(long requestId) {
		StoredDemande demande = childSharePort.findDemande(requestId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN));
		if (!"pending".equals(demande.status())) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN);
		}
		return demande;
	}

	private boolean referentOfAny(long userId) {
		for (ChildSaved child : childPort.findByParent(userId)) {
			if (referent(userId, child.id())) {
				return true;
			}
		}
		return false;
	}

	private List<ChildSaved> matching(long referentId, String firstName, String lastName) {
		List<ChildSaved> found = new java.util.ArrayList<>();
		for (ChildSaved child : childPort.findByParent(referentId)) {
			if (referent(referentId, child.id()) && sameName(child, firstName, lastName)) {
				found.add(child);
			}
		}
		return found;
	}

	private static boolean sameName(ChildSaved child, String firstName, String lastName) {
		return child.firstName().trim().equalsIgnoreCase(firstName.trim())
				&& child.lastName().trim().equalsIgnoreCase(lastName.trim());
	}

	@Override
	@Transactional(readOnly = true)
	public ChildPhoto photo(long parentUserId, String activeRole, long childId) {
		requireParent(activeRole);
		return childPort.findPhoto(parentUserId, childId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN));
	}

	private Prepared prepare(NewChildCommand command) {
		String firstName = requiredName(command.firstName(), FIRST_NAME_REQUIRED);
		String lastName = requiredName(command.lastName(), LAST_NAME_REQUIRED);
		String sex = requireSex(command.sex());
		int age = parseAge(command.age());
		String curriculum = curriculumOf(command.country());
		CatalogSchool school = resolveSchool(command.schoolId(), curriculum);
		String grade = requiredGrade(command.grade(), curriculum, school);
		Image image = resolveImage(command.avatarCode(), command.photoBase64(), command.photoContentType());
		String pin = requirePin(command.pin(), command.pinConfirmation());
		Long schoolId = school == null ? null : school.id();
		return new Prepared(
				firstName,
				lastName,
				sex,
				age,
				curriculum,
				grade,
				schoolId,
				image.avatarCode(),
				image.photo(),
				image.contentType(),
				pin);
	}

	private ChildSaved sourceOf(long parentUserId, Long fromChildId) {
		if (fromChildId == null) {
			return null;
		}
		return childPort.findOwned(parentUserId, fromChildId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN));
	}

	private static String relationOf(String link, boolean hasSource) {
		if (ChildKinship.isNone(link)) {
			return null;
		}
		if (!ChildKinship.isKnown(link)) {
			throw bad(LINK_UNKNOWN);
		}
		if (!hasSource) {
			throw bad(LINK_NEEDS_CHILD);
		}
		return link.trim();
	}

	/**
	 * Lien demandé à la modification.
	 *
	 * <p><b>Business :</b> sans enfant choisi, rien ne change. Avec un enfant et « Aucun », ce lien part.
	 * L'autre enfant doit être à ce parent, et pas la fiche en cours.
	 */
	private LinkEdit linkOnUpdate(long parentUserId, long childId, Long otherId, String link) {
		if (otherId == null) {
			return LinkEdit.KEEP;
		}
		if (otherId.longValue() == childId) {
			throw bad(LINK_SELF);
		}
		sourceOf(parentUserId, otherId);
		if (ChildKinship.isNone(link)) {
			return LinkEdit.REMOVE;
		}
		return new LinkEdit(false, relationOf(link, true));
	}

	private record LinkEdit(boolean remove, String relation) {
		private static final LinkEdit KEEP = new LinkEdit(false, null);
		private static final LinkEdit REMOVE = new LinkEdit(true, null);
	}

	private ParentChildrenView view(long parentUserId) {
		List<ChildSaved> family = childPort.findByParent(parentUserId);
		List<Long> schoolIds = family.stream().map(ChildSaved::schoolId).filter(java.util.Objects::nonNull).distinct().toList();
		Map<Long, CatalogSchool> schools = new LinkedHashMap<>();
		for (CatalogSchool school : schoolCatalogPort.findByIds(schoolIds)) {
			schools.putIfAbsent(school.id(), school);
		}
		Map<Long, Attached> attached = attachedByChild(parentUserId);
		Map<Long, int[]> counts = subjectCounts(parentUserId);
		Map<Long, StoredAvatar> avatars = childAvatarPort.findByChildIds(family.stream().map(ChildSaved::id).toList());
		List<ChildCard> cards = new ArrayList<>();
		for (ChildSaved child : family) {
			Attached links = attached.getOrDefault(child.id(), Attached.NONE);
			int[] pair = counts.getOrDefault(child.id(), new int[] {0, 0});
			cards.add(card(
					child, schools, links.phrases(), pair, links.marks(), avatars.get(child.id()),
					ChildInterestList.spellings(childPort.findInterests(child.id())),
					parentUserId));
		}
		List<SchoolChoice> choices = new ArrayList<>();
		for (CatalogSchool school : schools.values()) {
			choices.add(new SchoolChoice(
					school.id(),
					school.name(),
					school.country(),
					SchoolCurricula.offeredBy(school.country(), school.teachingTypes())));
		}
		List<GradeList> curricula = SchoolCurricula.options().stream()
				.map(choice -> new GradeList(countryOf(choice.code()), choice.grades()))
				.toList();
		return new ParentChildrenView(cards, choices, curricula);
	}

	private Map<Long, CatalogSchool> catalogById(Long schoolId) {
		Map<Long, CatalogSchool> schools = new LinkedHashMap<>();
		for (CatalogSchool school : schoolCatalogPort.findByIds(schoolId == null ? List.of() : List.of(schoolId))) {
			schools.putIfAbsent(school.id(), school);
		}
		return schools;
	}

	private Attached attachedFor(long parentUserId, long childId) {
		return attachedByChild(parentUserId).getOrDefault(childId, Attached.NONE);
	}

	/**
	 * Phrase et mot court, sur les deux enfants du lien.
	 *
	 * <p><b>Business :</b> chaque fiche retrouve l'autre, avec sa photo ou son avatar.
	 */
	private Map<Long, Attached> attachedByChild(long parentUserId) {
		Map<Long, List<String>> phrases = new LinkedHashMap<>();
		Map<Long, List<KinshipMark>> marks = new LinkedHashMap<>();
		for (ChildLinkView link : childPort.findLinks(parentUserId)) {
			phrases.computeIfAbsent(link.subjectChildId(), ignored -> new ArrayList<>())
					.add(ChildKinship.phrase(
							link.relatedFirstName(),
							link.relatedSex(),
							link.relation(),
							link.subjectFirstName()));
			phrases.computeIfAbsent(link.relatedChildId(), ignored -> new ArrayList<>())
					.add(ChildKinship.phrase(
							link.subjectFirstName(),
							link.subjectSex(),
							link.relation(),
							link.relatedFirstName()));
			marks.computeIfAbsent(link.subjectChildId(), ignored -> new ArrayList<>())
					.add(new KinshipMark(
							link.relatedChildId(),
							ChildKinship.word(link.relation(), link.relatedSex())));
			marks.computeIfAbsent(link.relatedChildId(), ignored -> new ArrayList<>())
					.add(new KinshipMark(
							link.subjectChildId(),
							ChildKinship.word(link.relation(), link.subjectSex())));
		}
		Map<Long, Attached> attached = new LinkedHashMap<>();
		for (Long childId : phrases.keySet()) {
			attached.put(childId, new Attached(phrases.get(childId), marks.get(childId)));
		}
		return attached;
	}

	private ChildCard card(
			ChildSaved child,
			Map<Long, CatalogSchool> schools,
			List<String> linkPhrases,
			int[] subjectCounts,
			List<KinshipMark> links,
			StoredAvatar avatar,
			List<String> interests,
			long viewerId) {
		CatalogSchool school = child.schoolId() == null ? null : schools.get(child.schoolId());
		boolean manages = referent(viewerId, child.id());
		return new ChildCard(
				child.id(),
				child.firstName(),
				child.lastName(),
				child.sex(),
				child.age(),
				countryOf(child.curriculum()),
				child.curriculum(),
				child.grade(),
				child.schoolId(),
				school == null ? "" : school.name(),
				child.avatarCode(),
				child.hasPhoto(),
				child.active(),
				linkPhrases,
				subjectCounts[0],
				subjectCounts[1],
				links,
				characterOf(avatar),
				interests,
				manages,
				manages ? childSharePort.pendingCount(child.id()) : 0,
				manages ? childSharePort.pendingDemandesForChild(viewerId, child.firstName(), child.lastName()) : 0);
	}

	private CharacterFace characterOf(StoredAvatar saved) {
		var avatar = avatarCatalog.resolve(saved);
		Map<String, Object> options = avatar.preset() == null ? Map.of() : avatar.preset().options();
		List<CharacterPin> pins = avatar.pins().stream()
				.map(pin -> new CharacterPin(pin.zoneCode(), pin.variantCode()))
				.toList();
		return new CharacterFace(avatar.style().code(), options, pins);
	}

	private int[] countsFor(long parentUserId, ChildSaved child) {
		return subjectCounts(parentUserId).getOrDefault(child.id(), new int[] {0, 0});
	}

	private Map<Long, int[]> subjectCounts(long parentUserId) {
		Map<Long, int[]> counts = new LinkedHashMap<>();
		List<CountedLink> links = childSubjectPort.visibleLinksForParent(parentUserId);
		if (links == null) {
			return counts;
		}
		Map<Long, ChildSaved> children = new LinkedHashMap<>();
		for (ChildSaved child : childPort.findByParent(parentUserId)) {
			children.put(child.id(), child);
		}
		for (CountedLink link : links) {
			ChildSaved child = children.get(link.childId());
			if (child == null) {
				continue;
			}
			int[] pair = counts.computeIfAbsent(child.id(), ignored -> new int[] {0, 0});
			if (ChildSubjectService.sameClass(child, link.schoolId(), link.grade())) {
				pair[0]++;
			} else {
				pair[1]++;
			}
		}
		return counts;
	}

	private record Attached(List<String> phrases, List<KinshipMark> marks) {
		private static final Attached NONE = new Attached(List.of(), List.of());
	}

	private static String countryOf(String curriculum) {
		if (SchoolCurricula.FRANCE.equals(curriculum)) {
			return SchoolCurricula.COUNTRY_FRANCE;
		}
		if (SchoolCurricula.BELGIUM_FR.equals(curriculum)) {
			return SchoolCurricula.COUNTRY_BELGIUM;
		}
		return "";
	}

	private CatalogSchool resolveSchool(Long schoolId, String curriculum) {
		if (schoolId == null) {
			return null;
		}
		CatalogSchool school = catalogById(schoolId).get(schoolId);
		if (school == null || !curriculum.equals(SchoolCurricula.forCountry(school.country()))) {
			throw bad(SCHOOL_UNKNOWN);
		}
		return school;
	}

	private static String requiredGrade(String grade, String curriculum, CatalogSchool school) {
		if (grade == null || grade.isBlank()) {
			throw bad(GRADE_REQUIRED);
		}
		String value = grade.trim();
		List<String> allowed = school == null
				? SchoolCurricula.allForCurriculum(curriculum)
				: SchoolCurricula.offeredBy(school.country(), school.teachingTypes());
		if (!allowed.contains(value)) {
			throw bad(school == null ? GRADE_NOT_IN_CURRICULUM : GRADE_NOT_OFFERED);
		}
		return value;
	}

	private static String curriculumOf(String country) {
		String curriculum = SchoolCurricula.forCountry(country);
		if (curriculum == null) {
			throw bad(COUNTRY_REQUIRED);
		}
		return curriculum;
	}

	private static int parseAge(String age) {
		if (age == null || age.isBlank()) {
			throw bad(AGE_INVALID);
		}
		try {
			int value = Integer.parseInt(age.trim());
			if (value < MIN_AGE || value > MAX_AGE) {
				throw bad(AGE_INVALID);
			}
			return value;
		} catch (NumberFormatException ex) {
			throw bad(AGE_INVALID);
		}
	}

	private static String requiredName(String value, String message) {
		if (value == null || value.isBlank()) {
			throw bad(message);
		}
		String trimmed = value.trim();
		if (trimmed.length() > MAX_NAME) {
			throw bad(NAME_TOO_LONG);
		}
		return trimmed;
	}

	private static Image resolveImage(String avatarCode, String photoBase64, String contentType) {
		byte[] photo = decodePhoto(photoBase64);
		if (photo != null) {
			String type = imageType(photo, contentType);
			return new Image(null, photo, type);
		}
		if (avatarCode == null || avatarCode.isBlank()) {
			throw bad(IMAGE_REQUIRED);
		}
		String code = avatarCode.trim();
		if (!AVATARS.contains(code)) {
			throw bad(AVATAR_UNKNOWN);
		}
		return new Image(code, null, null);
	}

	private static byte[] decodePhoto(String photoBase64) {
		if (photoBase64 == null || photoBase64.isBlank()) {
			return null;
		}
		String payload = photoBase64.trim();
		int comma = payload.indexOf(',');
		if (payload.startsWith("data:") && comma > 0) {
			payload = payload.substring(comma + 1);
		}
		try {
			byte[] bytes = Base64.getDecoder().decode(payload);
			if (bytes.length == 0) {
				throw bad(NOT_AN_IMAGE);
			}
			if (bytes.length > MAX_PHOTO_BYTES) {
				throw bad(IMAGE_TOO_LARGE);
			}
			return bytes;
		} catch (IllegalArgumentException ex) {
			throw bad(NOT_AN_IMAGE);
		}
	}

	private static String imageType(byte[] bytes, String declared) {
		String detected = sniff(bytes);
		if (detected == null) {
			throw bad(NOT_AN_IMAGE);
		}
		if (declared != null && !declared.isBlank() && !detected.equalsIgnoreCase(declared.trim())) {
			throw bad(NOT_AN_IMAGE);
		}
		return detected;
	}

	private static String sniff(byte[] bytes) {
		if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
			return "image/jpeg";
		}
		if (bytes.length >= 8
				&& (bytes[0] & 0xFF) == 0x89
				&& bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
			return "image/png";
		}
		if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
			return "image/gif";
		}
		if (bytes.length >= 12
				&& bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
				&& bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
			return "image/webp";
		}
		return null;
	}

	private static String requireSex(String raw) {
		if (raw == null || raw.isBlank()) {
			throw bad(SEX_REQUIRED);
		}
		String sex = raw.trim();
		if (!ChildSex.isKnown(sex)) {
			throw bad(SEX_UNKNOWN);
		}
		return sex;
	}

	private static String requirePin(String pin, String confirmation) {
		if (!isFourDigits(pin) || !isFourDigits(confirmation)) {
			throw bad(PIN_FORMAT);
		}
		if (!pin.equals(confirmation)) {
			throw bad(PIN_MISMATCH);
		}
		return pin;
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}

	private static boolean hasPhoto(NewChildCommand command) {
		return !blank(command.photoBase64());
	}

	private static boolean hasAvatar(NewChildCommand command) {
		return !blank(command.avatarCode());
	}

	private static boolean isFourDigits(String value) {
		if (value == null || value.length() != 4) {
			return false;
		}
		for (int i = 0; i < value.length(); i++) {
			if (!Character.isDigit(value.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private void requireHolder(long userId, long childId) {
		if (!holds(userId, childId)) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN);
		}
	}

	private void requireReferent(long userId, long childId) {
		if (!holds(userId, childId)) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, CHILD_UNKNOWN);
		}
		if (!referent(userId, childId)) {
			throw bad(ONLY_REFERENT);
		}
	}

	private boolean holds(long userId, long childId) {
		return childSharePort.holds(userId, childId) || childPort.findOwned(userId, childId).isPresent();
	}

	/**
	 * Un détenteur qui n'est pas référent le reste, même si la fiche est aussi visible par son parent d'origine.
	 */
	private boolean referent(long userId, long childId) {
		if (childSharePort.isReferent(userId, childId)) {
			return true;
		}
		if (childSharePort.holds(userId, childId)) {
			return false;
		}
		return childPort.findOwned(userId, childId).isPresent();
	}

	private static String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw bad(EMAIL_REQUIRED);
		}
		return email.trim().toLowerCase(java.util.Locale.ROOT);
	}

	private static void requireParent(String activeRole) {
		if (!ActiveRoles.PARENT.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceAccess.DENIED);
		}
	}

	private static AuthDomainException bad(String message) {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, message);
	}

	private record Prepared(
			String firstName,
			String lastName,
			String sex,
			int age,
			String curriculum,
			String grade,
			Long schoolId,
			String avatarCode,
			byte[] photo,
			String photoContentType,
			String pin) {
	}

	private record Image(String avatarCode, byte[] photo, String contentType) {
	}
}

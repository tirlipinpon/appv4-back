package com.appv4.auth.application.service;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.RankingGroups;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadChildRankingUseCase;
import com.appv4.auth.application.port.out.ChildAvatarPort;
import com.appv4.auth.application.port.out.ChildAvatarPort.StoredAvatar;
import com.appv4.auth.application.port.out.ChildRankingCasesPort;
import com.appv4.auth.application.port.out.ChildRankingCasesPort.RankingSelection;
import com.appv4.auth.application.port.out.ChildRankingRosterPort;
import com.appv4.auth.application.port.out.ChildRankingRosterPort.ChildPhoto;
import com.appv4.auth.application.port.out.ChildRankingRosterPort.VisibleChild;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.result.ChildRankingView;
import com.appv4.auth.application.result.ChildRankingView.Pin;
import com.appv4.auth.application.result.ChildRankingView.RankingCard;
import com.appv4.auth.application.result.ChildRankingView.ScopeChip;

/**
 * Classe, année, pays, tout le monde et âge, ordonnés par XP (BACK-22).
 *
 * <p><b>Business :</b> l'enfant voit dix camarades, et sa carte en plus s'il est au-delà.
 * Sans école, la classe et le pays le disent. Une case décochée le retire sans fermer la page.
 * <p><b>Technique :</b> même jeton que le profil. Lorelei non enregistré n'est pas un personnage.
 * <p><b>Métier / flow :</b> port in de {@code GET /api/ext/ranking}.
 */
public class ReadChildRankingService implements ReadChildRankingUseCase {

	public static final String INVALID_SESSION_MESSAGE = LoginChildService.INVALID_SESSION_MESSAGE;

	public static final String PHOTO_MISSING_MESSAGE = ReadChildProfileService.PHOTO_MISSING_MESSAGE;

	private static final int HEAD = 10;

	private final ChildSessionTokenPort tokens;
	private final ChildRankingRosterPort roster;
	private final ChildRankingCasesPort cases;
	private final ChildAvatarPort avatars;
	private final CharacterAvatarCatalog catalog;

	public ReadChildRankingService(
			ChildSessionTokenPort tokens,
			ChildRankingRosterPort roster,
			ChildRankingCasesPort cases,
			ChildAvatarPort avatars,
			CharacterAvatarCatalog catalog) {
		this.tokens = tokens;
		this.roster = roster;
		this.cases = cases;
		this.avatars = avatars;
		this.catalog = catalog;
	}

	@Override
	@Transactional(readOnly = true)
	public ChildRankingView read(String rawToken, String scope) {
		String code = scope == null || scope.isBlank() ? "classe" : scope;
		if (!RankingGroups.childGroup(code)) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, RankingGroups.UNKNOWN);
		}
		VisibleChild caller = caller(rawToken);
		List<ScopeChip> chips = chips(code);
		if (needsSchool(code) && caller.schoolId() == null) {
			return new ChildRankingView(chips, code, "", RankingGroups.SCHOOL_PHRASE, List.of());
		}
		List<Placed> members = members(caller, code);
		boolean shown = members.stream().anyMatch(row -> row.child.id() == caller.id());
		List<Placed> page = new ArrayList<>();
		for (Placed row : members) {
			if (page.size() < HEAD) {
				page.add(row);
			}
		}
		boolean pinned = false;
		if (shown && page.stream().noneMatch(row -> row.child.id() == caller.id())) {
			page.add(members.stream().filter(row -> row.child.id() == caller.id()).findFirst().orElseThrow());
			pinned = true;
		}
		String phrase = shown ? null : RankingGroups.HIDDEN_PHRASE;
		Map<Long, StoredAvatar> faces = avatars.findByChildIds(page.stream().map(row -> row.child.id()).toList());
		boolean extra = pinned;
		List<RankingCard> cards = page.stream()
				.map(row -> card(row, caller.id(), extra && row.child.id() == caller.id(), faces.get(row.child.id())))
				.toList();
		return new ChildRankingView(chips, code, groupLabel(caller, code), phrase, cards);
	}

	@Override
	@Transactional(readOnly = true)
	public ChildPhoto photo(String rawToken, String scope, long childId) {
		String code = scope == null || scope.isBlank() ? "classe" : scope;
		if (!RankingGroups.childGroup(code)) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, RankingGroups.UNKNOWN);
		}
		VisibleChild caller = caller(rawToken);
		boolean member = !(needsSchool(code) && caller.schoolId() == null)
				&& members(caller, code).stream().anyMatch(row -> row.child.id() == childId);
		if (!member) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, PHOTO_MISSING_MESSAGE);
		}
		return roster.photo(childId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, PHOTO_MISSING_MESSAGE));
	}

	private VisibleChild caller(String rawToken) {
		long childId = tokens.read(rawToken == null ? "" : rawToken)
				.orElseThrow(this::invalid)
				.childId();
		return roster.visibleChildren().stream()
				.filter(child -> child.id() == childId)
				.findFirst()
				.orElseThrow(this::invalid);
	}

	private List<Placed> members(VisibleChild caller, String code) {
		Map<Long, RankingSelection> stored = cases.selection(
				roster.visibleChildren().stream().map(VisibleChild::id).toList());
		List<VisibleChild> matched = new ArrayList<>();
		for (VisibleChild child : roster.visibleChildren()) {
			RankingSelection selection = stored == null ? null : stored.get(child.id());
			if (selection == null) {
				selection = RankingSelection.shown();
			}
			if (matches(caller, child, code) && selection.on(code)) {
				matched.add(child);
			}
		}
		matched.sort(order());
		List<Placed> placed = new ArrayList<>();
		int place = 0;
		int seen = 0;
		int lastXp = Integer.MIN_VALUE;
		for (VisibleChild child : matched) {
			seen++;
			if (child.xp() != lastXp) {
				place = seen;
				lastXp = child.xp();
			}
			placed.add(new Placed(child, place));
		}
		return placed;
	}

	private static Comparator<VisibleChild> order() {
		Collator french = Collator.getInstance(Locale.FRENCH);
		french.setStrength(Collator.SECONDARY);
		return Comparator
				.comparingInt(VisibleChild::xp).reversed()
				.thenComparing(VisibleChild::firstName, french)
				.thenComparing(VisibleChild::createdAt)
				.thenComparingLong(VisibleChild::id);
	}

	private static boolean matches(VisibleChild caller, VisibleChild child, String code) {
		return switch (code) {
			case "classe" -> caller.schoolId() != null
					&& caller.schoolId().equals(child.schoolId())
					&& caller.grade().equals(child.grade());
			case "annee" -> caller.grade().equals(child.grade());
			case "pays" -> caller.schoolId() != null
					&& child.schoolId() != null
					&& caller.country() != null
					&& caller.country().equals(child.country());
			case "tous" -> true;
			case "age" -> caller.age() == child.age();
			default -> false;
		};
	}

	private static boolean needsSchool(String code) {
		return "classe".equals(code) || "pays".equals(code);
	}

	private static String groupLabel(VisibleChild caller, String code) {
		return switch (code) {
			case "classe" -> caller.grade() + " · " + (caller.schoolName() == null ? "" : caller.schoolName());
			case "annee" -> caller.grade();
			case "pays" -> caller.country() == null ? "" : caller.country();
			case "age" -> caller.age() + " ans";
			default -> "Tout le monde";
		};
	}

	private List<ScopeChip> chips(String selected) {
		return RankingGroups.childGroups().stream()
				.map(group -> new ScopeChip(group.code(), group.label(), group.code().equals(selected)))
				.toList();
	}

	private RankingCard card(Placed row, long callerId, boolean pinned, StoredAvatar saved) {
		VisibleChild child = row.child;
		boolean mine = child.id() == callerId;
		if (saved != null) {
			var resolved = catalog.resolve(saved);
			Map<String, Object> options = resolved.preset() == null ? Map.of() : resolved.preset().options();
			List<Pin> pins = resolved.pins().stream()
					.map(pin -> new Pin(pin.zoneCode(), pin.variantCode()))
					.toList();
			return new RankingCard(
					row.place, child.firstName(), child.xp(), mine, pinned,
					"personnage", null, resolved.style().code(), options, pins, child.id());
		}
		if (child.hasPhoto()) {
			return new RankingCard(
					row.place, child.firstName(), child.xp(), mine, pinned,
					"photo", null, null, Map.of(), List.of(), child.id());
		}
		if (child.avatarCode() != null && !child.avatarCode().isBlank()) {
			return new RankingCard(
					row.place, child.firstName(), child.xp(), mine, pinned,
					"animal", child.avatarCode(), null, Map.of(), List.of(), child.id());
		}
		return new RankingCard(
				row.place, child.firstName(), child.xp(), mine, pinned,
				"aucun", null, null, Map.of(), List.of(), child.id());
	}

	private AuthDomainException invalid() {
		return new AuthDomainException(AuthErrorCode.UNAUTHORIZED, INVALID_SESSION_MESSAGE);
	}

	private record Placed(VisibleChild child, int place) {
	}
}

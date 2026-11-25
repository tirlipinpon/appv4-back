package com.appv4.auth.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.text.Collator;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.AppHours;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase;
import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.application.port.out.AppHoursPort;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.LectureEssai;
import com.appv4.auth.application.service.SuccessWindows.Result;
import com.appv4.auth.application.service.SuccessWindows.Sample;
import com.appv4.auth.application.service.question.QuestionFormRegistry;

/**
 * Quatre lectures du suivi parent (BACK-40).
 *
 * <p><b>Business :</b> le créneau suit les heures de l'admin. La tendance est celle
 * de l'humeur. Les minutes viennent des séjours déjà confirmés.
 * <p><b>Technique :</b> rien n'est écrit. Une fenêtre vide laisse la tendance absente.
 * <p><b>Métier / flow :</b> port in de {@code GET /api/int/parent/children/{childId}/progress}.
 */
public class ReadParentProgressService implements ReadParentProgressUseCase {

	static final String WINDOW_LABEL = "7 jours";

	private static final LocalTime NOON = LocalTime.of(12, 0);
	private static final LocalTime EVENING = LocalTime.of(18, 0);
	private final ChildPort children;
	private final ChildQuestionAttemptPort attempts;
	private final SejourUseCase sejours;
	private final AppHoursPort hours;
	private final QuestionFormRegistry forms;
	private final Clock clock;

	public ReadParentProgressService(
			ChildPort children,
			ChildQuestionAttemptPort attempts,
			SejourUseCase sejours,
			AppHoursPort hours,
			QuestionFormRegistry forms,
			Clock clock) {
		this.children = children;
		this.attempts = attempts;
		this.sejours = sejours;
		this.hours = hours;
		this.forms = forms;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public ParentProgress read(long parentUserId, String activeRole, long childId) {
		if (!ActiveRoles.PARENT.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceAccess.DENIED);
		}
		children.findOwned(parentUserId, childId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, ParentChildrenService.CHILD_UNKNOWN));
		Instant now = clock.instant();
		List<LectureEssai> essais = attempts.lectureEssais(childId);
		if (essais == null) {
			essais = List.of();
		}
		AppHours current = hours.current();
		AppHours window = current == null ? AppHours.defaults() : current;
		return new ParentProgress(
				creneau(essais, window, now),
				minutes(sejours.countableSeconds(childId)),
				tendance(essais, now),
				formes(essais, now));
	}

	private String creneau(List<LectureEssai> essais, AppHours window, Instant now) {
		Slot bestSlot = null;
		Tally best = null;
		for (Slot slot : Slot.values()) {
			Tally tally = tally(essais, window, now, slot);
			if (tally.total == 0) {
				continue;
			}
			if (best == null || tally.beats(best, slot, bestSlot)) {
				best = tally;
				bestSlot = slot;
			}
		}
		return bestSlot == null ? null : bestSlot.label;
	}

	private Tally tally(List<LectureEssai> essais, AppHours window, Instant now, Slot slot) {
		int passed = 0;
		int total = 0;
		for (LectureEssai essai : essais) {
			if (!counts(essai, now) || window.closedAt(essai.createdAt(), zone())) {
				continue;
			}
			if (slotOf(essai.createdAt()) != slot) {
				continue;
			}
			total++;
			if (essai.passed()) {
				passed++;
			}
		}
		return new Tally(passed, total);
	}

	private boolean counts(LectureEssai essai, Instant now) {
		return essai.createdAt() != null && !essai.createdAt().isAfter(now);
	}

	private Slot slotOf(Instant at) {
		LocalTime time = at.atZone(zone()).toLocalTime();
		if (time.isBefore(NOON)) {
			return Slot.MATIN;
		}
		if (time.isBefore(EVENING)) {
			return Slot.APRES_MIDI;
		}
		return Slot.SOIR;
	}

	private Integer minutes(List<Long> seconds) {
		if (seconds == null || seconds.isEmpty()) {
			return null;
		}
		long sum = 0;
		int count = 0;
		for (Long value : seconds) {
			if (value == null || value < 1) {
				continue;
			}
			sum += value;
			count++;
		}
		if (count == 0) {
			return null;
		}
		BigDecimal rounded = BigDecimal.valueOf(sum)
				.divide(BigDecimal.valueOf(count * 60L), 0, RoundingMode.HALF_UP);
		return rounded.intValueExact();
	}

	private Trend tendance(List<LectureEssai> essais, Instant now) {
		List<Sample> samples = new ArrayList<>();
		for (LectureEssai essai : essais) {
			samples.add(new Sample(essai.createdAt(), essai.passed()));
		}
		Result result = SuccessWindows.at(samples, now);
		if (!result.present()) {
			return null;
		}
		return new Trend(result.direction(), result.delta(), WINDOW_LABEL);
	}

	private List<FormeRatee> formes(List<LectureEssai> essais, Instant now) {
		List<TallyForm> groups = new ArrayList<>();
		for (LectureEssai essai : essais) {
			if (!counts(essai, now) || essai.passed() || essai.formCode() == null) {
				continue;
			}
			TallyForm found = null;
			for (TallyForm group : groups) {
				if (group.code.equals(essai.formCode())) {
					found = group;
					break;
				}
			}
			if (found == null) {
				groups.add(new TallyForm(essai.formCode()));
			} else {
				found.count++;
			}
		}
		List<FormeRatee> kept = new ArrayList<>();
		for (TallyForm group : groups) {
			if (group.count < 2) {
				continue;
			}
			String label = forms.knownLabel(group.code);
			if (label == null) {
				continue;
			}
			kept.add(new FormeRatee(group.code, group.count, label));
		}
		Collator labels = collator();
		kept.sort(Comparator.comparingInt(FormeRatee::count).reversed()
				.thenComparing(FormeRatee::label, labels));
		return List.copyOf(kept);
	}

	private static ZoneId zone() {
		return ReadChildProgressService.PROGRESS_ZONE;
	}

	private static Collator collator() {
		Collator collator = Collator.getInstance(Locale.FRANCE);
		collator.setStrength(Collator.PRIMARY);
		return collator;
	}

	private enum Slot {
		MATIN("Matin", 1),
		APRES_MIDI("Après-midi", 2),
		SOIR("Soir", 3);

		private final String label;
		private final int rank;

		Slot(String label, int rank) {
			this.label = label;
			this.rank = rank;
		}
	}

	private record Tally(int passed, int total) {

		private boolean beats(Tally other, Slot slot, Slot otherSlot) {
			long mine = SuccessWindows.rate(passed, total);
			long theirs = SuccessWindows.rate(other.passed, other.total);
			if (mine != theirs) {
				return mine > theirs;
			}
			if (total != other.total) {
				return total > other.total;
			}
			return slot.rank > otherSlot.rank;
		}
	}

	private static final class TallyForm {
		private final String code;
		private int count = 1;

		private TallyForm(String code) {
			this.code = code;
		}
	}
}

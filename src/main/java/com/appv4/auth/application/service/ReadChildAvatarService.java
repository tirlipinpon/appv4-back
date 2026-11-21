package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadChildAvatarUseCase;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildProgressPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.result.ChildAvatarView;
import com.appv4.auth.application.result.ChildAvatarView.PresetCard;
import com.appv4.auth.application.result.ChildAvatarView.StyleCard;
import com.appv4.auth.application.result.ChildAvatarView.VariantCard;
import com.appv4.auth.application.result.ChildAvatarView.ZoneCard;
import com.appv4.auth.application.service.CharacterAvatarCatalog.Preset;
import com.appv4.auth.application.service.CharacterAvatarCatalog.Style;
import com.appv4.auth.application.service.CharacterAvatarCatalog.Zone;

/**
 * Lecture du personnage (BACK-48).
 *
 * <p><b>Business :</b> sans choix enregistré, l'enfant voit Lorelei et le prix de chaque zone.
 * <p><b>Technique :</b> jeton enfant, XP déjà stocké, catalogue Characters 10.6.
 * <p><b>Métier / flow :</b> Profil → ce service → pas d'écriture, pas de second XP.
 */
public class ReadChildAvatarService implements ReadChildAvatarUseCase {

	public static final String INVALID_SESSION_MESSAGE = LoginChildService.INVALID_SESSION_MESSAGE;

	static final String DEFAULT_STYLE = "lorelei";

	private final ChildLoginPort childLoginPort;
	private final ChildSessionTokenPort childSessionTokenPort;
	private final ChildProgressPort childProgressPort;
	private final CharacterAvatarCatalog catalog;

	public ReadChildAvatarService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildProgressPort childProgressPort,
			CharacterAvatarCatalog catalog) {
		this.childLoginPort = childLoginPort;
		this.childSessionTokenPort = childSessionTokenPort;
		this.childProgressPort = childProgressPort;
		this.catalog = catalog;
	}

	@Override
	@Transactional(readOnly = true)
	public ChildAvatarView avatar(String rawToken) {
		long childId = requireActiveChild(rawToken);
		int xp = childProgressPort.findByChildId(childId)
				.map(progress -> progress.xpTotal())
				.orElse(0);
		Style selected = catalog.byCode(DEFAULT_STYLE);
		return new ChildAvatarView(
				Long.toString(childId),
				selected.code(),
				selected.name(),
				null,
				selected.credit(),
				catalog.styles().stream().map(this::styleCard).toList(),
				selected.presets().stream().map(this::presetCard).toList(),
				zonesOf(selected, xp));
	}

	private StyleCard styleCard(Style style) {
		List<String> zoneCodes = style.zones().stream()
				.map(Zone::code)
				.sorted(Comparator.comparingInt(AvatarScale::seuil))
				.toList();
		return new StyleCard(style.code(), style.name(), style.credit(), zoneCodes);
	}

	private PresetCard presetCard(Preset preset) {
		return new PresetCard(preset.id(), preset.name(), preset.options());
	}

	private List<ZoneCard> zonesOf(Style style, int xp) {
		List<Zone> ordered = style.zones().stream()
				.sorted(Comparator.comparingInt(zone -> AvatarScale.seuil(zone.code())))
				.toList();
		List<ZoneCard> cards = new ArrayList<>();
		for (Zone zone : ordered) {
			int seuil = AvatarScale.seuil(zone.code());
			boolean open = xp >= seuil;
			List<VariantCard> variants = new ArrayList<>();
			for (String code : zone.variants()) {
				variants.add(new VariantCard(code, code));
			}
			if (AvatarScale.removable(zone.code())) {
				variants.add(new VariantCard("none", "sans"));
			}
			cards.add(new ZoneCard(
					zone.code(),
					AvatarScale.label(zone.code()),
					seuil,
					open ? null : seuil + " XP",
					open ? null : "encore " + (seuil - xp) + " XP",
					open,
					variants));
		}
		return cards;
	}

	private long requireActiveChild(String rawToken) {
		long childId = childSessionTokenPort.read(rawToken)
				.orElseThrow(this::invalidSession)
				.childId();
		if (childLoginPort.findActiveById(childId).isEmpty()) {
			throw invalidSession();
		}
		return childId;
	}

	private AuthDomainException invalidSession() {
		return new AuthDomainException(AuthErrorCode.UNAUTHORIZED, INVALID_SESSION_MESSAGE);
	}
}

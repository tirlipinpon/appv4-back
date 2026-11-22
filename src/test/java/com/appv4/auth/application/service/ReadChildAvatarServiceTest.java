package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ChildAvatarPort;
import com.appv4.auth.application.port.out.ChildAvatarPort.StoredPin;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildProfile;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildProgressPort;
import com.appv4.auth.application.port.out.ChildProgressPort.StoredProgress;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.result.ChildAvatarView;
import com.appv4.auth.application.result.ChildAvatarView.StyleCard;
import com.appv4.auth.application.result.ChildAvatarView.ZoneCard;

@ExtendWith(MockitoExtension.class)
class ReadChildAvatarServiceTest {

	@Mock
	private ChildLoginPort childLoginPort;

	@Mock
	private ChildSessionTokenPort childSessionTokenPort;

	@Mock
	private ChildProgressPort childProgressPort;

	private final MemoryAvatar avatarPort = new MemoryAvatar();

	private ReadChildAvatarService service;

	@BeforeEach
	void setUp() {
		service = new ReadChildAvatarService(
				childLoginPort,
				childSessionTokenPort,
				childProgressPort,
				avatarPort,
				CharacterAvatarCatalog.load());
	}

	@Test
	void defaultReadIsLoreleiWithClosedMouthAtZeroXp() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.empty());

		ChildAvatarView first = service.avatar("token");
		ChildAvatarView second = service.avatar("token");

		assertThat(first.seed()).isEqualTo("7");
		assertThat(second.seed()).isEqualTo(first.seed());
		assertThat(first.styleCode()).isEqualTo("lorelei");
		assertThat(first.styleName()).isEqualTo("Lorelei");
		assertThat(first.presetId()).isNull();
		assertThat(first.credit()).isNull();
		assertThat(first.presets()).isNotEmpty();
		assertThat(first.presets().get(0).id()).isEqualTo("bare");
		assertThat(first.presets().get(0).name()).isEqualTo("Bare");
		ZoneCard mouth = zone(first, "mouth");
		assertThat(mouth.label()).isEqualTo("bouche");
		assertThat(mouth.seuil()).isEqualTo(20);
		assertThat(mouth.open()).isFalse();
		assertThat(mouth.thresholdLabel()).isEqualTo("20 XP");
		assertThat(mouth.missingLabel()).isEqualTo("encore 20 XP");
		assertThat(first.zones().get(0).code()).isEqualTo("mouth");
		assertThat(zone(first, "eyes").seuil()).isEqualTo(30);
		assertThat(zone(first, "glasses").seuil()).isEqualTo(210);
	}

	@Test
	void anotherChildHasAnotherSeed() {
		when(childSessionTokenPort.read("lea"))
				.thenReturn(Optional.of(new ChildSessionTokenPort.ChildSession(7L)));
		when(childSessionTokenPort.read("akira"))
				.thenReturn(Optional.of(new ChildSessionTokenPort.ChildSession(9L)));
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "lea", "hash")));
		when(childLoginPort.findActiveById(9L))
				.thenReturn(Optional.of(new ChildLoginCandidate(9L, "akira", "hash")));
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.empty());
		when(childProgressPort.findByChildId(9L)).thenReturn(Optional.empty());

		assertThat(service.avatar("lea").seed()).isEqualTo("7");
		assertThat(service.avatar("akira").seed()).isEqualTo("9");
	}

	@Test
	void twentyXpOpensTheMouthAndNineteenLeavesItClosed() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L))
				.thenReturn(Optional.of(progress(7L, 20)))
				.thenReturn(Optional.of(progress(7L, 19)));

		ChildAvatarView opened = service.avatar("token");
		ChildAvatarView closed = service.avatar("token");

		assertThat(zone(opened, "mouth").open()).isTrue();
		assertThat(zone(opened, "mouth").thresholdLabel()).isNull();
		assertThat(zone(opened, "mouth").missingLabel()).isNull();
		assertThat(zone(closed, "mouth").open()).isFalse();
		assertThat(zone(closed, "mouth").thresholdLabel()).isEqualTo("20 XP");
		assertThat(zone(closed, "mouth").missingLabel()).isEqualTo("encore 1 XP");
	}

	@Test
	void hairAtEightyXpShowsTheRemainingPhrase() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.of(progress(7L, 80)));

		ZoneCard hair = zone(service.avatar("token"), "hair");

		assertThat(hair.open()).isFalse();
		assertThat(hair.thresholdLabel()).isEqualTo("140 XP");
		assertThat(hair.missingLabel()).isEqualTo("encore 60 XP");
	}

	@Test
	void catalogKeepsEachStylesOwnZonesAndCredits() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.empty());

		ChildAvatarView view = service.avatar("token");

		assertThat(view.styles()).hasSize(38);
		assertThat(view.styles().get(0).code()).isEqualTo("adventurer");
		assertThat(style(view, "fun-emoji").zoneCodes()).containsExactly("mouth", "eyes");
		assertThat(style(view, "fun-emoji").zones()).extracting(ZoneCard::code).containsExactly("mouth", "eyes");
		assertThat(style(view, "fun-emoji").zones().get(0).open()).isFalse();
		assertThat(style(view, "fun-emoji").zones().get(0).thresholdLabel()).isEqualTo("20 XP");
		assertThat(style(view, "bottts").zones()).extracting(ZoneCard::code).contains("mouth", "eyes").doesNotContain("hair");
		assertThat(style(view, "lorelei").presets().get(0).id()).isEqualTo("bare");
		assertThat(style(view, "lorelei").presets().get(0).options()).containsKey("backgroundColor");
		assertThat(style(view, "fun-emoji").presets().get(0).id()).isEqualTo("plain");
		assertThat(style(view, "fun-emoji").presets().get(0).options()).isNotEmpty();
		assertThat(style(view, "avataaars").zoneCodes()).contains("top").doesNotContain("hair");
		assertThat(style(view, "micah").credit()).isEqualTo("Micah Lanier — CC BY 4.0");
		assertThat(style(view, "lorelei").credit()).isNull();
		assertThat(style(view, "avataaars").credit()).isNull();
		assertThat(style(view, "bottts").credit()).isNull();
		ZoneCard glasses = zone(view, "glasses");
		assertThat(glasses.variants()).anyMatch(variant -> variant.code().equals("none") && variant.label().equals("sans"));
		assertThat(zone(view, "mouth").variants()).noneMatch(variant -> variant.code().equals("none"));
	}

	@Test
	void oldStyleAndVariantCodesFallBackToAPlayableAvatar() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.empty());
		avatarPort.save(7L, "ancien-style", "ancien-preset", List.of(
				new StoredPin("mouth", "unknown"), new StoredPin("glasses", "none")));

		ChildAvatarView view = service.avatar("token");

		assertThat(view.styleCode()).isEqualTo("lorelei");
		assertThat(view.presetId()).isNull();
		assertThat(zone(view, "mouth").selectedCode()).isNull();
		assertThat(zone(view, "glasses").selectedCode()).isEqualTo("none");
	}

	@Test
	void saveStyleAndPresetAtZeroXpThenReadThemBack() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.empty());

		ChildAvatarView saved = service.saveStyle("token", "lorelei", "bare");
		ChildAvatarView again = service.avatar("token");

		assertThat(saved.styleCode()).isEqualTo("lorelei");
		assertThat(saved.presetId()).isEqualTo("bare");
		assertThat(saved.seed()).isEqualTo("7");
		assertThat(again.presetId()).isEqualTo("bare");
		assertThat(again.seed()).isEqualTo(saved.seed());
	}

	@Test
	void saveStyleWithoutPresetClearsThePalette() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.empty());
		service.saveStyle("token", "lorelei", "bare");

		ChildAvatarView cleared = service.saveStyle("token", "fun-emoji", null);

		assertThat(cleared.styleCode()).isEqualTo("fun-emoji");
		assertThat(cleared.presetId()).isNull();
		assertThat(cleared.credit()).isEqualTo("Davis Uche — CC BY 4.0");
		assertThat(cleared.presets().get(0).id()).isEqualTo("plain");
	}

	@Test
	void saveRefusesAPresetFromAnotherStyleAndAnUnknownStyle() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.empty());
		service.saveStyle("token", "lorelei", "bare");

		assertThatThrownBy(() -> service.saveStyle("token", "fun-emoji", "bare"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ReadChildAvatarService.UNKNOWN_PRESET)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.BAD_REQUEST);
		assertThat(service.avatar("token").styleCode()).isEqualTo("lorelei");
		assertThat(service.avatar("token").presetId()).isEqualTo("bare");

		assertThatThrownBy(() -> service.saveStyle("token", "not-a-style", null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ReadChildAvatarService.UNKNOWN_STYLE);
		assertThat(service.avatar("token").styleCode()).isEqualTo("lorelei");
	}

	@Test
	void closedPinDoesNotBlockAPaletteOrAStyleThatCannotKeepIt() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.of(progress(7L, 20)));
		String mouth = zone(service.avatar("token"), "mouth").variants().get(0).code();
		service.saveStyle("token", "lorelei", "bare", List.of(new StoredPin("mouth", mouth)));

		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.empty());
		ChildAvatarView palette = service.saveStyle("token", "lorelei", "sepia", null);

		assertThat(palette.presetId()).isEqualTo("sepia");
		assertThat(zone(palette, "mouth").open()).isFalse();
		assertThat(zone(palette, "mouth").selectedCode()).isEqualTo(mouth);

		ChildAvatarView adventurer = service.saveStyle("token", "adventurer", "bare", null);
		assertThat(adventurer.styleCode()).isEqualTo("adventurer");
		assertThat(adventurer.presetId()).isEqualTo("bare");
		assertThat(zone(adventurer, "mouth").selectedCode()).isNull();
	}

	@Test
	void paletteAndNewOpenVariantKeepAnExistingVariant_whenItsZoneBecameClosed() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.of(progress(7L, 30)));
		ChildAvatarView avatar = service.avatar("token");
		String eyes = zone(avatar, "eyes").variants().getFirst().code();
		String mouth = zone(avatar, "mouth").variants().getFirst().code();
		service.saveStyle("token", "lorelei", "bare", List.of(new StoredPin("eyes", eyes)));

		when(childLoginPort.findActiveProfileById(7L))
				.thenReturn(Optional.of(new ChildProfile(7L, "akira", null, true, 7)));
		ChildAvatarView saved = service.saveStyle("token", "lorelei", "sepia", List.of(new StoredPin("mouth", mouth)));

		assertThat(saved.presetId()).isEqualTo("sepia");
		assertThat(zone(saved, "mouth").selectedCode()).isEqualTo(mouth);
		assertThat(zone(saved, "eyes").open()).isFalse();
		assertThat(zone(saved, "eyes").selectedCode()).isEqualTo(eyes);
	}

	@Test
	void changingStyleDropsVariantsThatDoNotExistThere() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.of(progress(7L, 30)));
		ChildAvatarView bottts = service.saveStyle("token", "bottts", "sepia", List.of(
				new StoredPin("mouth", "diagram"),
				new StoredPin("eyes", "eva")));
		assertThat(zone(bottts, "mouth").selectedCode()).isEqualTo("diagram");
		assertThat(zone(bottts, "eyes").selectedCode()).isEqualTo("eva");

		ChildAvatarView saved = service.saveStyle("token", "adventurer", "sepia", List.of(
				new StoredPin("mouth", "diagram"),
				new StoredPin("eyes", "eva")));

		assertThat(saved.styleCode()).isEqualTo("adventurer");
		assertThat(saved.presetId()).isEqualTo("sepia");
		assertThat(zone(saved, "mouth").selectedCode()).isNull();
		assertThat(zone(saved, "eyes").selectedCode()).isNull();
	}

	@Test
	void openMouthPinStaysWhenThePaletteChanges() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.of(progress(7L, 20)));
		String mouth = zone(service.avatar("token"), "mouth").variants().get(0).code();

		service.saveStyle("token", "lorelei", "bare", List.of(new StoredPin("mouth", mouth)));
		ChildAvatarView afterPalette = service.saveStyle("token", "lorelei", "sepia");

		assertThat(zone(afterPalette, "mouth").selectedCode()).isEqualTo(mouth);
		assertThat(zone(afterPalette, "eyes").open()).isFalse();
		assertThat(zone(afterPalette, "eyes").selectedCode()).isNull();
		assertThat(afterPalette.presetId()).isEqualTo("sepia");
	}

	@Test
	void noneOnOpenGlassesIsStoredAndNoneOnMouthIsRefused() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.of(progress(7L, 210)));
		String mouth = zone(service.avatar("token"), "mouth").variants().get(0).code();
		service.saveStyle("token", "lorelei", "bare", List.of(new StoredPin("mouth", mouth)));

		ChildAvatarView glasses = service.saveStyle(
				"token", "lorelei", "bare", List.of(new StoredPin("glasses", "none")));
		assertThat(zone(glasses, "glasses").selectedCode()).isEqualTo("none");

		assertThatThrownBy(() -> service.saveStyle(
				"token", "lorelei", "bare", List.of(new StoredPin("mouth", "none"))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ReadChildAvatarService.UNKNOWN_VARIANT);
		assertThat(zone(service.avatar("token"), "glasses").selectedCode()).isEqualTo("none");
	}

	@Test
	void closedHairPinIsRefusedAndFunEmojiKeepsOnlyTheMouth() {
		activeChild(7L);
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.of(progress(7L, 20)));
		ChildAvatarView current = service.avatar("token");
		String mouth = zone(current, "mouth").variants().get(0).code();
		String hair = zone(current, "hair").variants().get(0).code();
		service.saveStyle("token", "lorelei", "bare", List.of(new StoredPin("mouth", mouth)));

		assertThatThrownBy(() -> service.saveStyle(
				"token", "lorelei", "bare", List.of(new StoredPin("hair", hair))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ReadChildAvatarService.ZONE_CLOSED);
		assertThat(zone(service.avatar("token"), "mouth").selectedCode()).isEqualTo(mouth);

		String emojiMouth = CharacterAvatarCatalog.load().byCode("fun-emoji").zones().stream()
				.filter(zone -> zone.code().equals("mouth"))
				.findFirst()
				.orElseThrow()
				.variants()
				.get(0);
		ChildAvatarView emoji = service.saveStyle(
				"token", "fun-emoji", "plain", List.of(new StoredPin("mouth", emojiMouth)));
		assertThat(emoji.styleCode()).isEqualTo("fun-emoji");
		assertThat(emoji.zones().stream().map(ZoneCard::code)).doesNotContain("hair");
		assertThat(zone(emoji, "mouth").selectedCode()).isEqualTo(emojiMouth);
	}

	@Test
	void eightYearOldPaysTwentyPercentMoreToOpenAZone() {
		activeChild(7L);
		when(childLoginPort.findActiveProfileById(7L))
				.thenReturn(Optional.of(new ChildProfile(7L, "akira", null, true, 8)));
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.empty());

		ZoneCard mouth = zone(service.avatar("token"), "mouth");

		assertThat(mouth.seuil()).isEqualTo(24);
		assertThat(mouth.thresholdLabel()).isEqualTo("24 XP");
		assertThat(mouth.missingLabel()).isEqualTo("encore 24 XP");
		assertThat(mouth.open()).isFalse();
	}

	@Test
	void missingOrStoppedChildIsRefused() {
		when(childSessionTokenPort.read("bad")).thenReturn(Optional.empty());
		when(childSessionTokenPort.read("stopped"))
				.thenReturn(Optional.of(new ChildSessionTokenPort.ChildSession(7L)));
		when(childLoginPort.findActiveById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.avatar("bad"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ReadChildAvatarService.INVALID_SESSION_MESSAGE)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.UNAUTHORIZED);
		assertThatThrownBy(() -> service.avatar("stopped"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ReadChildAvatarService.INVALID_SESSION_MESSAGE)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.UNAUTHORIZED);
	}

	private void activeChild(long childId) {
		when(childSessionTokenPort.read("token"))
				.thenReturn(Optional.of(new ChildSessionTokenPort.ChildSession(childId)));
		when(childLoginPort.findActiveById(childId))
				.thenReturn(Optional.of(new ChildLoginCandidate(childId, "akira", "hash")));
	}

	private static StoredProgress progress(long childId, int xp) {
		return new StoredProgress(childId, xp, 0, null, null, "0000000");
	}

	private static ZoneCard zone(ChildAvatarView view, String code) {
		return view.zones().stream()
				.filter(zone -> zone.code().equals(code))
				.findFirst()
				.orElseThrow();
	}

	private static final class MemoryAvatar implements ChildAvatarPort {

		private ChildAvatarPort.StoredAvatar stored;

		@Override
		public Map<Long, ChildAvatarPort.StoredAvatar> findByChildIds(List<Long> childIds) {
			return stored != null && childIds.contains(stored.childId()) ? Map.of(stored.childId(), stored) : Map.of();
		}

		@Override
		public void save(long childId, String styleCode, String presetId, List<ChildAvatarPort.StoredPin> nextPins) {
			stored = new ChildAvatarPort.StoredAvatar(childId, styleCode, presetId, nextPins);
		}
	}

	private static StyleCard style(ChildAvatarView view, String code) {
		return view.styles().stream()
				.filter(style -> style.code().equals(code))
				.findFirst()
				.orElseThrow();
	}
}

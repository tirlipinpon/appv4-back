package com.appv4.auth.application.port.in;

import java.math.BigDecimal;
import java.util.List;

/**
 * Séjour de l'enfant : signaux, lecture enfant, lecture parent, lecture professeur
 * (BACK-38, question BACK-79, Nuit BACK-80, popup BACK-81, professeur BACK-82).
 *
 * <p><b>Business :</b> l'enfant envoie des signaux. Le jeu compte sur la question,
 * avec l'énoncé et la matière copiés. Le parent lit le temps confirmé, déjà libellé.
 * De 22 h à 6 h, la route enfant est refusée avec une seule phrase.
 * À vingt minutes sans geste, la lecture envoie la popup. Sans bouton à vingt-cinq, elle refuse.
 * L'écran n'ajoute pas de secondes.
 * <p><b>Technique :</b> un seul cas d'usage. L'horloge est celle du bean, en Europe/Paris.
 * <p><b>Métier / flow :</b> {@code POST /api/ext/sejour/signals}, {@code GET /api/ext/sejour},
 * les autres routes {@code /api/ext/**}, {@code GET /api/int/parent/children/{childId}/sejours}
 * et {@code GET /api/int/teacher/students/{childId}/sejours}.
 */
public interface SejourUseCase {

	/**
	 * Ferme le séjour ouvert à 22 h, puis refuse, quand l'horloge est dans la Nuit.
	 * Hors Nuit, ne fait rien.
	 *
	 * @param rawToken jeton enfant, vide si l'appel n'en a pas
	 */
	void refuseDuringNuit(String rawToken);

	/**
	 * Ferme le séjour oublié à vingt-cinq minutes, puis refuse jusqu'à une nouvelle entrée.
	 *
	 * <p><b>Business :</b> sans « Je continue », l'enfant revient à l'entrée.
	 * Une connexion de jour plus tardive ouvre un autre séjour. Le parent n'est pas fermé.
	 * <p><b>Technique :</b> la phrase est unique. Le jeton émis avant la fin ne passe plus.
	 * Un jeton émis après, si.
	 * <p><b>Métier / flow :</b> routes {@code /api/ext/**} sauf l'entrée, après la Nuit.
	 * La Nuit gagne si les deux tombent ensemble.
	 *
	 * @param rawToken jeton enfant, vide si l'appel n'en a pas
	 */
	void refuseAfterIdle(String rawToken);

	/**
	 * @param questionId identifiant de la question jouée, seulement pour un signal question
	 */
	void signal(String rawToken, String kind, String deviceId, String screen, Long questionId);

	/**
	 * Signal d'écran, de geste, de présence, de départ ou de réactivation, sans question.
	 */
	default void signal(String rawToken, String kind, String deviceId, String screen) {
		signal(rawToken, kind, deviceId, screen, null);
	}

	/**
	 * @param holds vrai si cet appareil tient le séjour ouvert
	 */
	ChildSejour readChild(String rawToken, String deviceId);

	List<ParentSejour> readParent(long parentUserId, String activeRole, long childId);

	/**
	 * Mois lu par la page Temps : une journée par date, et le plafond du jour.
	 *
	 * <p><b>Business :</b> plusieurs séjours du même jour ne font qu'une journée.
	 * La Nuit, de 22 h à 6 h, n'ajoute rien. Un mois futur est refusé.
	 * Le plafond compare toujours aujourd'hui, même si le mois lu est passé.
	 * <p><b>Technique :</b> {@code year} et {@code month} vides veulent dire le mois de l'horloge.
	 * Chaque ligne porte un code d'espace et des secondes entières.
	 * <p><b>Métier / flow :</b> {@code GET /api/int/parent/children/{childId}/sejours}.
	 *
	 * @param year année demandée, vide pour le mois en cours
	 * @param month mois de 1 à 12, vide pour le mois en cours
	 */
	JourneeMonth readParentMonth(long parentUserId, String activeRole, long childId, Integer year, Integer month);

	/**
	 * Secondes confirmées de chaque séjour qui compte déjà.
	 *
	 * <p><b>Business :</b> la moyenne du suivi parent s'appuie sur ces durées,
	 * pas sur une nouvelle façon de compter l'activité.
	 * <p><b>Technique :</b> un séjour sous une seconde est absent. Un séjour ouvert
	 * ne donne que ses secondes déjà confirmées.
	 * <p><b>Métier / flow :</b> lecture du suivi, après le calcul du séjour.
	 */
	List<Long> countableSeconds(long childId);

	/**
	 * Pose ou retire le plafond du parent référent.
	 *
	 * <p><b>Business :</b> un entier de 1 à 960 minutes, ou vide pour retirer la limite.
	 * Le parent en partage voit le même plafond et ne peut pas l'écrire. L'enfant continue.
	 * <p><b>Technique :</b> une fraction, 0, un négatif ou plus de 960 est refusé.
	 * La valeur déjà gardée ne bouge pas.
	 * <p><b>Métier / flow :</b> {@code PUT /api/int/parent/children/{childId}/sejours/plafond}.
	 * La lecture du mois renvoie ensuite le même objet.
	 *
	 * @param limitMinutes minutes entières, ou vide pour retirer le plafond
	 */
	Plafond saveParentPlafond(long parentUserId, String activeRole, long childId, BigDecimal limitMinutes);

	/**
	 * Même détail que {@link #readParent}, une fois la classe contrôlée.
	 *
	 * <p><b>Business :</b> chaque professeur de la classe voit les mêmes séjours,
	 * Accueil compris, et les autres matières. Un élève hors classe est refusé
	 * avec la phrase déjà connue. Le parent continue de lire à toute heure.
	 * <p><b>Technique :</b> pas de second calcul. L'école et l'année sont celles
	 * du profil professeur, déjà vérifiées pour la liste des élèves.
	 * <p><b>Métier / flow :</b> {@code GET /api/int/teacher/students/{childId}/sejours},
	 * après {@link TeacherStudentsUseCase#requireInClass}.
	 */
	List<ParentSejour> readTeacher(long teacherUserId, String activeRole, long childId);

	/**
	 * Même mois que {@link #readParentMonth}, une fois la classe contrôlée.
	 *
	 * <p><b>Business :</b> le professeur voit les mêmes journées, sans plafond.
	 * <p><b>Technique :</b> pas d'autre calcul que la lecture parent.
	 * <p><b>Métier / flow :</b> {@code GET /api/int/teacher/students/{childId}/sejours}.
	 */
	JourneeMonth readTeacherMonth(long teacherUserId, String activeRole, long childId, Integer year, Integer month);

	/**
	 * Ce que cet appareil doit afficher maintenant.
	 *
	 * <p><b>Business :</b> la phrase et le bouton n'apparaissent que lorsque la popup
	 * est due. L'écran les recopie, il ne les invente pas.
	 * <p><b>Technique :</b> {@code message} et {@code button} sont absents tant que
	 * vingt minutes ne se sont pas écoulées depuis le dernier geste.
	 * <p><b>Métier / flow :</b> résultat de {@link #readChild}.
	 *
	 * @param holds vrai si cet appareil tient le séjour ouvert
	 * @param message phrase de la popup, absente sinon
	 * @param button libellé du bouton, absent sinon
	 */
	record ChildSejour(boolean holds, String message, String button) {

		public ChildSejour(boolean holds) {
			this(holds, null, null);
		}
	}

	/**
	 * @param lines une ligne par écran ou par question, dans l'ordre de la première visite
	 */
	record ParentSejour(String dateLabel, String timeLabel, String durationLabel, List<Line> lines) {
	}

	/**
	 * @param label libellé d'écran, ou énoncé de la question
	 * @param subjectName nom de matière de la question, absent pour un écran
	 */
	record Line(String label, String subjectName, String durationLabel) {
	}

	/**
	 * Plafond comparé à la journée d'aujourd'hui.
	 *
	 * <p><b>Business :</b> vide veut dire pas de limite. La phrase n'existe que si
	 * les secondes confirmées d'aujourd'hui dépassent strictement les minutes.
	 * <p><b>Technique :</b> {@code message} est exactement {@code Plafond du jour dépassé.}
	 * ou une chaîne vide. {@code editable} est vrai pour le parent référent.
	 * <p><b>Métier / flow :</b> porté par la lecture parent du mois, et par l'enregistrement.
	 *
	 * @param limitMinutes minutes posées, vide s'il n'y a pas de plafond
	 * @param exceeded vrai seulement au-dessus du plafond
	 * @param message phrase exacte, ou vide
	 * @param editable vrai quand ce parent gère la fiche
	 */
	record Plafond(Integer limitMinutes, boolean exceeded, String message, boolean editable) {
	}

	/**
	 * @param monthLabel libellé du mois, par exemple « novembre 2026 »
	 * @param currentMonth vrai quand ce mois est celui de l'horloge
	 * @param journees journées qui ont au moins une seconde, de la plus récente à la plus ancienne
	 * @param plafond plafond du parent, absent pour le professeur
	 */
	record JourneeMonth(
			String monthLabel,
			int year,
			int month,
			boolean currentMonth,
			List<Journee> journees,
			Plafond plafond) {
	}

	/**
	 * @param today vrai seulement pour la date de l'horloge
	 * @param durationSeconds secondes confirmées de la journée, hors Nuit
	 */
	record Journee(
			int year,
			int month,
			int day,
			boolean today,
			String dateLabel,
			String durationLabel,
			long durationSeconds,
			List<JourneeLine> lines) {
	}

	/**
	 * @param space code d'écran, ou {@code question}
	 * @param label libellé d'écran, ou énoncé copié
	 * @param subjectName nom de matière copié, absent pour un écran
	 */
	record JourneeLine(
			String space, String label, String subjectName, String durationLabel, long durationSeconds) {
	}
}

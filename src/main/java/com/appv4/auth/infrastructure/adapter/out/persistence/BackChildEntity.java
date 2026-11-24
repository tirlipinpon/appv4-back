package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Fiche enfant (SCRUM-156).
 *
 * <p><b>Business :</b> un parent inscrit un enfant avec une classe et un code.
 * {@code active} faux veut dire désactivé : la fiche reste, elle n'est plus proposée.
 * {@code hidden} vrai : elle disparaît de l'affichage, la ligne reste.
 * <p><b>Technique :</b> table {@code back_children}. {@code pin_hash} ouvre la session
 * de l'app enfant. Ce n'est pas un mot de passe adulte, et ce jeton n'ouvre pas l'admin.
 */
@Entity
@Table(name = "back_children")
public class BackChildEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "parent_user_id", nullable = false)
	private long parentUserId;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(nullable = false, length = 16)
	private String sex;

	@Column(nullable = false)
	private short age;

	@Column(nullable = false, length = 32)
	private String curriculum;

	@Column(name = "grade_label", nullable = false, length = 80)
	private String gradeLabel;

	@Column(name = "school_id")
	private Long schoolId;

	@Column(name = "avatar_code", length = 32)
	private String avatarCode;

	@Column(name = "photo")
	private byte[] photo;

	@Column(name = "photo_content_type", length = 40)
	private String photoContentType;

	@Column(name = "pin_hash", nullable = false, length = 255)
	private String pinHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private boolean hidden;

	/**
	 * Plafond de la journée, en minutes. Vide : pas de limite.
	 *
	 * <p><b>Business :</b> posé par le parent référent. La Nuit n'est pas dedans.
	 * <p><b>Technique :</b> colonne nullable, entre 1 et 960 quand elle est remplie.
	 */
	@Column(name = "daily_limit_minutes")
	private Integer dailyLimitMinutes;

	protected BackChildEntity() {
	}

	public BackChildEntity(
			long parentUserId,
			String firstName,
			String lastName,
			String sex,
			int age,
			String curriculum,
			String gradeLabel,
			Long schoolId,
			String avatarCode,
			byte[] photo,
			String photoContentType,
			String pinHash) {
		this.parentUserId = parentUserId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.sex = sex;
		this.age = (short) age;
		this.curriculum = curriculum;
		this.gradeLabel = gradeLabel;
		this.schoolId = schoolId;
		this.avatarCode = avatarCode;
		this.photo = photo;
		this.photoContentType = photoContentType;
		this.pinHash = pinHash;
		this.active = true;
		this.hidden = false;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public long getParentUserId() {
		return parentUserId;
	}

	public String getFirstName() {
		return firstName;
	}

	/** Hash du code. Lu seulement pour comparer un login enfant, jamais renvoyé au front. */
	public String getPinHash() {
		return pinHash;
	}

	public String getLastName() {
		return lastName;
	}

	public String getSex() {
		return sex;
	}

	public short getAge() {
		return age;
	}

	public String getCurriculum() {
		return curriculum;
	}

	public String getGradeLabel() {
		return gradeLabel;
	}

	public Long getSchoolId() {
		return schoolId;
	}

	public String getAvatarCode() {
		return avatarCode;
	}

	public byte[] getPhoto() {
		return photo;
	}

	public String getPhotoContentType() {
		return photoContentType;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isHidden() {
		return hidden;
	}

	/** L'enfant reste dans la liste. Il n'est plus proposé dans le reste de l'application. */
	public void deactivate() {
		this.active = false;
	}

	/** Le rend de nouveau proposable dans le reste de l'application. */
	public void reactivate() {
		this.active = true;
	}

	/** Le retire de l'affichage. La ligne, le hash et la photo restent. */
	public void hide() {
		this.hidden = true;
	}

	/**
	 * Le parent qui gère la fiche change.
	 *
	 * <p><b>Business :</b> après un transfert accepté, ce parent est le parent référent.
	 * <p><b>Technique :</b> {@code parent_user_id} seulement. Le hash du code n'est pas touché.
	 * <p><b>Métier / flow :</b> l'ancien parent reste détenteur, dans une autre ligne.
	 */
	public void assignParent(long parentUserId) {
		this.parentUserId = parentUserId;
	}

	public Integer getDailyLimitMinutes() {
		return dailyLimitMinutes;
	}

	/** Remplace le plafond. Vide le retire. */
	public void setDailyLimitMinutes(Integer dailyLimitMinutes) {
		this.dailyLimitMinutes = dailyLimitMinutes;
	}

	/**
	 * Remplace les champs modifiables. Un hash null garde le PIN. Sans nouvelle image, photo et avatar restent.
	 */
	public void applyChanges(
			String firstName,
			String lastName,
			String sex,
			int age,
			String curriculum,
			String gradeLabel,
			Long schoolId,
			boolean replaceImage,
			String avatarCode,
			byte[] photo,
			String photoContentType,
			String pinHash) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.sex = sex;
		this.age = (short) age;
		this.curriculum = curriculum;
		this.gradeLabel = gradeLabel;
		this.schoolId = schoolId;
		if (replaceImage) {
			this.avatarCode = avatarCode;
			this.photo = photo;
			this.photoContentType = photoContentType;
		}
		if (pinHash != null) {
			this.pinHash = pinHash;
		}
	}
}

package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.SejourPort;

/**
 * Séjours en base (BACK-38).
 *
 * <p><b>Business :</b> un enfant n'a qu'un séjour ouvert. Le temps fermé est un
 * intervalle, pas une ligne par seconde.
 * <p><b>Technique :</b> JDBC sur {@code back_sejours} et {@code back_sejour_intervals}.
 * Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out de {@link SejourPort}.
 */
@Component
@Profile("!test")
public class SejourPersistenceAdapter implements SejourPort {

	private final JdbcClient jdbc;

	public SejourPersistenceAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<OpenSlice> findOpen(long childId) {
		return jdbc.sql("""
				select id, child_id, started_at, holding_device_id, slice_screen, slice_question_id,
				       slice_prompt, slice_subject_name, slice_started_at, last_confirmed_at, last_gesture_at
				from back_sejours
				where child_id = :childId and closed_at is null
				""")
				.param("childId", childId)
				.query((rs, row) -> new OpenSlice(
						rs.getLong("id"),
						rs.getLong("child_id"),
						rs.getTimestamp("started_at").toInstant(),
						rs.getString("holding_device_id"),
						rs.getString("slice_screen"),
						rs.getObject("slice_question_id", Long.class),
						rs.getString("slice_prompt"),
						rs.getString("slice_subject_name"),
						rs.getTimestamp("slice_started_at").toInstant(),
						rs.getTimestamp("last_confirmed_at").toInstant(),
						instant(rs.getTimestamp("last_gesture_at"))))
				.optional();
	}

	@Override
	public OpenSlice open(long childId, Instant startedAt, String deviceId, String screen) {
		long id = jdbc.sql("""
				insert into back_sejours (
				    child_id, started_at, holding_device_id, slice_screen,
				    slice_started_at, last_confirmed_at)
				values (:childId, :startedAt, :deviceId, :screen, :startedAt, :startedAt)
				returning id
				""")
				.param("childId", childId)
				.param("startedAt", Timestamp.from(startedAt))
				.param("deviceId", deviceId)
				.param("screen", screen)
				.query(Long.class)
				.single();
		return new OpenSlice(
				id, childId, startedAt, deviceId, screen, null, null, null, startedAt, startedAt, null);
	}

	@Override
	public void saveOpen(OpenSlice slice) {
		jdbc.sql("""
				update back_sejours
				set holding_device_id = :deviceId,
				    slice_screen = :screen,
				    slice_question_id = :questionId,
				    slice_prompt = :prompt,
				    slice_subject_name = :subjectName,
				    slice_started_at = :sliceStartedAt,
				    last_confirmed_at = :lastConfirmedAt,
				    last_gesture_at = :lastGestureAt
				where id = :id and closed_at is null
				""")
				.param("deviceId", slice.deviceId())
				.param("screen", slice.screen())
				.param("questionId", slice.questionId(), Types.BIGINT)
				.param("prompt", slice.prompt())
				.param("subjectName", slice.subjectName())
				.param("sliceStartedAt", Timestamp.from(slice.sliceStartedAt()))
				.param("lastConfirmedAt", Timestamp.from(slice.lastConfirmedAt()))
				.param("lastGestureAt", timestamp(slice.lastGestureAt()), Types.TIMESTAMP_WITH_TIMEZONE)
				.param("id", slice.id())
				.update();
	}

	@Override
	public void appendInterval(
			long sejourId,
			String screen,
			Long questionId,
			String prompt,
			String subjectName,
			Instant startedAt,
			Instant endedAt) {
		jdbc.sql("""
				insert into back_sejour_intervals (
				    sejour_id, screen, question_id, prompt, subject_name, started_at, ended_at)
				values (
				    :sejourId, :screen, :questionId, :prompt, :subjectName, :startedAt, :endedAt)
				""")
				.param("sejourId", sejourId)
				.param("screen", screen)
				.param("questionId", questionId, Types.BIGINT)
				.param("prompt", prompt)
				.param("subjectName", subjectName)
				.param("startedAt", Timestamp.from(startedAt))
				.param("endedAt", Timestamp.from(endedAt))
				.update();
	}

	@Override
	public void close(long sejourId, Instant closedAt) {
		jdbc.sql("""
				update back_sejours
				set closed_at = :closedAt,
				    holding_device_id = null,
				    slice_screen = null,
				    slice_question_id = null,
				    slice_prompt = null,
				    slice_subject_name = null,
				    slice_started_at = null,
				    last_confirmed_at = null,
				    last_gesture_at = null
				where id = :id
				""")
				.param("closedAt", Timestamp.from(closedAt))
				.param("id", sejourId)
				.update();
	}

	@Override
	public List<StoredSejour> list(long childId) {
		List<SejourRow> rows = jdbc.sql("""
				select id, child_id, started_at, closed_at, holding_device_id, slice_screen,
				       slice_question_id, slice_prompt, slice_subject_name,
				       slice_started_at, last_confirmed_at, last_gesture_at
				from back_sejours
				where child_id = :childId
				order by started_at desc, id desc
				""")
				.param("childId", childId)
				.query((rs, row) -> new SejourRow(
						rs.getLong("id"),
						rs.getLong("child_id"),
						rs.getTimestamp("started_at").toInstant(),
						rs.getTimestamp("closed_at") == null,
						rs.getString("holding_device_id"),
						rs.getString("slice_screen"),
						rs.getObject("slice_question_id", Long.class),
						rs.getString("slice_prompt"),
						rs.getString("slice_subject_name"),
						instant(rs.getTimestamp("slice_started_at")),
						instant(rs.getTimestamp("last_confirmed_at")),
						instant(rs.getTimestamp("last_gesture_at"))))
				.list();
		if (rows.isEmpty()) {
			return List.of();
		}
		Map<Long, List<StoredInterval>> intervals = new LinkedHashMap<>();
		for (SejourRow row : rows) {
			intervals.put(row.id(), new ArrayList<>());
		}
		List<IntervalRow> found = jdbc.sql("""
				select sejour_id, screen, question_id, prompt, subject_name, started_at, ended_at
				from back_sejour_intervals
				where sejour_id in (:ids)
				order by started_at, id
				""")
				.param("ids", intervals.keySet())
				.query((rs, row) -> new IntervalRow(
						rs.getLong("sejour_id"),
						rs.getString("screen"),
						rs.getObject("question_id", Long.class),
						rs.getString("prompt"),
						rs.getString("subject_name"),
						rs.getTimestamp("started_at").toInstant(),
						rs.getTimestamp("ended_at").toInstant()))
				.list();
		for (IntervalRow interval : found) {
			intervals.get(interval.sejourId()).add(new StoredInterval(
					interval.screen(),
					interval.questionId(),
					interval.prompt(),
					interval.subjectName(),
					interval.startedAt(),
					interval.endedAt()));
		}
		List<StoredSejour> stored = new ArrayList<>();
		for (SejourRow row : rows) {
			stored.add(new StoredSejour(
					row.id(),
					row.childId(),
					row.startedAt(),
					row.open(),
					row.deviceId(),
					row.screen(),
					row.questionId(),
					row.prompt(),
					row.subjectName(),
					row.sliceStartedAt(),
					row.lastConfirmedAt(),
					row.lastGestureAt(),
					List.copyOf(intervals.get(row.id()))));
		}
		return List.copyOf(stored);
	}

	private static Timestamp timestamp(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}

	private static Instant instant(Timestamp value) {
		return value == null ? null : value.toInstant();
	}

	private record IntervalRow(
			long sejourId,
			String screen,
			Long questionId,
			String prompt,
			String subjectName,
			Instant startedAt,
			Instant endedAt) {
	}

	private record SejourRow(
			long id,
			long childId,
			Instant startedAt,
			boolean open,
			String deviceId,
			String screen,
			Long questionId,
			String prompt,
			String subjectName,
			Instant sliceStartedAt,
			Instant lastConfirmedAt,
			Instant lastGestureAt) {
	}
}

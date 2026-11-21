package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.service.question.play.PlayableQuestionForm;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

/** Distingue dans le contrat exporté les variantes transportées par les DTO communs. */
@Component
@SuppressWarnings("rawtypes")
public class QuestionOpenApiContract implements OpenApiCustomizer {
	private static final String REF = "#/components/schemas/";
	private static final String FORM = "formCode";

	@Override
	public void customise(OpenAPI openApi) {
		Map<String, Schema> schemas = openApi.getComponents().getSchemas();
		playQuestion(schemas);
		attempt(schemas);
		teacher(schemas);
		require(schemas, "PlayChoice", "choiceId", "label");
		require(schemas, "PlayStatement", "statementId", "label");
		require(schemas, "PlayMemoryCard", "cardId", "label");
		require(schemas, "PlayItem", "itemId", "label");
		require(schemas, "PlayColumn", "columnId", "label");
		require(schemas, "PlayCard", "cardId", "label");
		require(schemas, "PlayLinkOption", "optionId", "label");
		require(schemas, "PlayGapWord", "wordId", "label");
		require(schemas, "PlayClickZone", "zoneId", "shape", "points");
		require(schemas, "PlayClickPoint", "x", "y");
		require(schemas, "PlayClickStep", "zoneId", "prompt");
		require(schemas, "PlayPuzzlePiece", "pieceId", "label", "shape", "points");
		require(schemas, "PlaySimonElement", "label");
		require(schemas, "ChildLearningQuestionsResponse", "subjectId", "subjectName", "questions");
		require(schemas, "ChildLearningQuestionItem", "questionId", FORM, "prompt", "formLabel", "mark", "yellowCount");
		require(schemas, "ChildAttemptResultResponse", "attemptId", "scorePercent", "passed",
				"attemptRank", "attemptsRemaining", "pointsAwarded", "replayAvailable", "mark", "yellowCount");
		require(schemas, "PreviewVerdict", "scorePercent", "passed");
		require(schemas, "ChildLetterLocksResponse", "locks");
		require(schemas, "MemoryPairResponse", "matched");
	}

	private static void playQuestion(Map<String, Schema> schemas) {
		Schema<?> base = schemas.get("PlayQuestionResponse");
		if (base == null) {
			return;
		}
		List<Schema> variants = new ArrayList<>();
		Discriminator discriminator = new Discriminator().propertyName(FORM);
		for (PlayableQuestionForm form : PlayableQuestionForm.values()) {
			String content = QuestionHttpForm.of(form).playContent();
			Schema<?> model = schemas.computeIfAbsent(content, key -> new ObjectSchema());
			if (model.getProperties() != null) {
				model.setRequired(new ArrayList<>(model.getProperties().keySet()));
			}
			Schema<?> variant = new ObjectSchema();
			variant.setProperties(new LinkedHashMap<>(base.getProperties()));
			variant.addProperty(FORM, literal(form));
			variant.addProperty("content", ref(content));
			variant.setRequired(List.of(
					"questionId", "subjectId", FORM, "prompt", "formLabel", "content",
					"maxAttempts", "attemptsRemaining"));
			String name = content + "Question";
			schemas.put(name, variant);
			variants.add(ref(name));
			discriminator.mapping(form.code(), REF + name);
		}
		union(base, variants);
		base.setDiscriminator(discriminator);
	}

	private static void attempt(Map<String, Schema> schemas) {
		Schema<?> base = schemas.get("SubmitChildAttemptRequest");
		if (base == null) {
			return;
		}
		List<Schema> variants = new ArrayList<>();
		Set<String> legacyFields = new HashSet<>();
		List<Schema> previewVariants = new ArrayList<>();
		for (PlayableQuestionForm form : PlayableQuestionForm.values()) {
			QuestionHttpForm descriptor = QuestionHttpForm.of(form);
			Schema<?> modern = answerVariant(base, descriptor.answerField());
			modern.addProperty(FORM, literal(form));
			modern.setRequired(List.of(FORM, descriptor.answerField()));
			String name = descriptor.playContent() + "Attempt";
			schemas.put(name, modern);
			previewVariants.add(ref(name));
			if (form == PlayableQuestionForm.SIMON) {
				Schema<?> child = answerVariant(base, descriptor.answerField());
				child.addProperty(FORM, literal(form));
				copy(base, child, List.of("challengeId"));
				child.setRequired(List.of(FORM, descriptor.answerField(), "challengeId"));
				schemas.put("PlaySimonChildAttempt", child);
				variants.add(ref("PlaySimonChildAttempt"));
			} else {
				variants.add(ref(name));
			}
			String legacyField = descriptor.legacyAnswerField();
			if (legacyFields.add(legacyField)) {
				Schema<?> legacy = answerVariant(base, legacyField);
				if (form == PlayableQuestionForm.SIMON) {
					copy(base, legacy, List.of("challengeId"));
				}
				legacy.setRequired(List.of(legacyField));
				schemas.put(name + "Legacy", legacy);
				variants.add(ref(name + "Legacy"));
			}
		}
		union(base, variants);
		Schema<?> preview = schemas.get("PreviewAttemptRequest");
		if (preview != null) {
			Schema<?> previewAnswer = new Schema<>();
			previewAnswer.setOneOf(previewVariants);
			schemas.put("PreviewPlayAnswer", previewAnswer);
			preview.addProperty("answer", ref("PreviewPlayAnswer"));
		}
	}

	private static Schema<?> answerVariant(Schema<?> base, String field) {
		Schema<?> variant = new ObjectSchema();
		copy(base, variant, List.of("submissionId", "playId", field));
		variant.setAdditionalProperties(false);
		return variant;
	}

	private static void teacher(Map<String, Schema> schemas) {
		Schema<?> base = schemas.get("CreateTeacherSubjectQuestionRequest");
		if (base == null) {
			return;
		}
		List<Schema> variants = new ArrayList<>();
		Discriminator discriminator = new Discriminator().propertyName(FORM);
		for (PlayableQuestionForm form : PlayableQuestionForm.values()) {
			QuestionHttpForm descriptor = QuestionHttpForm.of(form);
			Schema<?> variant = new ObjectSchema();
			copy(base, variant, List.of("prompt", "gamification"));
			copy(base, variant, descriptor.teacherRequired());
			copy(base, variant, descriptor.teacherOptional());
			variant.addProperty(FORM, literal(form));
			List<String> required = new ArrayList<>(descriptor.teacherRequired());
			required.addFirst(FORM);
			variant.setRequired(required);
			variant.setAdditionalProperties(false);
			String name = descriptor.playContent() + "TeacherQuestionInput";
			schemas.put(name, variant);
			variants.add(ref(name));
			discriminator.mapping(form.code(), REF + name);
		}
		union(base, variants);
		base.setDiscriminator(discriminator);
	}

	private static void require(Map<String, Schema> schemas, String name, String... fields) {
		Schema<?> model = schemas.get(name);
		if (model != null) {
			model.setRequired(List.of(fields));
		}
	}

	private static void copy(Schema<?> source, Schema<?> target, List<String> fields) {
		fields.forEach(field -> target.addProperty(field, source.getProperties().get(field)));
	}

	private static void union(Schema<?> base, List<Schema> variants) {
		base.setProperties(null);
		base.setRequired(null);
		base.setType(null);
		base.setOneOf(variants);
	}

	private static StringSchema literal(PlayableQuestionForm form) {
		StringSchema schema = new StringSchema();
		schema.setEnum(List.of(form.code()));
		return schema;
	}

	private static Schema<?> ref(String name) {
		return new Schema<>().$ref(REF + name);
	}
}

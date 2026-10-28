package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ChoiceInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.GapWordInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.GapsInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ItemInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.LinkInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.LinksInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.LegacyInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.PairInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.StatementInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.StatementsInput;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.result.TeacherSubjectFollowersView;
import com.appv4.auth.application.result.TeacherQuestionFormsView;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.ChoiceLine;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.GapWordLine;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.ItemLine;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.LinkLine;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.PairLine;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.QuestionLine;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.StatementLine;
import com.appv4.auth.application.service.TeacherQuestionService;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Questions du professeur connecté (SCRUM-174, SCRUM-175, SCRUM-176, SCRUM-177).
 *
 * <p><b>Business :</b> il voit les formes proposables, puis il prépare un énoncé,
 * des paires, une liste déjà rangée, ou des phrases vraies ou fausses.
 * Il peut corriger une question déjà là. La forme de cette question ne change pas.
 * Une autre forme n'est pas acceptée. Il peut aussi copier une ou plusieurs
 * questions vers une matière à laquelle il a accès. Les originaux restent.
 * Il peut aussi en supprimer une ou plusieurs. Avant ça, l'écran lui montre
 * combien d'enfants suivent le cours et la liste de leurs noms.
 * L'élève ne joue pas ici.
 * <p><b>Technique :</b> {@code /api/int/teacher/question-forms} et
 * {@code /api/int/teacher/subjects/{id}/questions}. JWT obligatoire, espace {@code teacher}.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link TeacherQuestionUseCase}.
 */
@RestController
@RequestMapping("/api/int/teacher")
@Tag(name = "Professeur — questions")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class TeacherQuestionController {

	private final TeacherQuestionUseCase teacherQuestionUseCase;

	public TeacherQuestionController(TeacherQuestionUseCase teacherQuestionUseCase) {
		this.teacherQuestionUseCase = teacherQuestionUseCase;
	}

	@GetMapping("/question-forms")
	@Operation(operationId = "listTeacherQuestionForms", summary = "Lister les formes de questions proposables")
	@ApiResponse(responseCode = "200", description = "Formes que le professeur peut préparer maintenant")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherQuestionFormsResponse> listForms(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(forms(teacherQuestionUseCase.listForms(user.userId(), user.activeRole())));
	}

	@GetMapping("/subjects/{subjectId}/questions")
	@Operation(operationId = "listTeacherSubjectQuestions", summary = "Lister les questions d'une matière")
	@ApiResponse(responseCode = "200", description = "Énoncés de cette matière, du plus ancien au plus récent")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière absente pour ce professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherSubjectQuestionsResponse> list(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long subjectId) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(questions(
				teacherQuestionUseCase.list(user.userId(), user.activeRole(), subjectId)));
	}

	@PostMapping("/subjects/{subjectId}/questions")
	@Operation(operationId = "createTeacherSubjectQuestion", summary = "Ajouter une question à une matière")
	@ApiResponse(responseCode = "200", description = "Question enregistrée")
	@ApiResponse(
			responseCode = "400",
			description = "Forme inconnue, énoncé invalide, ou contenu insuffisant pour cette forme",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière absente pour ce professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherSubjectQuestionItem> create(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long subjectId,
			@RequestBody CreateTeacherSubjectQuestionRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		CreateTeacherSubjectQuestionRequest body = request == null
				? new CreateTeacherSubjectQuestionRequest(null, null, null, null, null, null, null, null, null, null)
				: request;
		QuestionLine line = teacherQuestionUseCase.create(
				user.userId(),
				user.activeRole(),
				subjectId,
				body.formCode(),
				body.prompt(),
				content(body));
		return ResponseEntity.ok(item(line));
	}

	@PutMapping("/subjects/{subjectId}/questions/{questionId}")
	@Operation(operationId = "updateTeacherSubjectQuestion", summary = "Corriger une question déjà enregistrée")
	@ApiResponse(responseCode = "200", description = "Question corrigée, la forme ne change pas")
	@ApiResponse(
			responseCode = "400",
			description = "Forme inconnue ou différente, énoncé invalide, ou contenu insuffisant pour cette forme",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière ou question absente pour ce professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherSubjectQuestionItem> update(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long subjectId,
			@PathVariable long questionId,
			@RequestBody CreateTeacherSubjectQuestionRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		CreateTeacherSubjectQuestionRequest body = request == null
				? new CreateTeacherSubjectQuestionRequest(null, null, null, null, null, null, null, null, null, null)
				: request;
		QuestionLine line = teacherQuestionUseCase.update(
				user.userId(),
				user.activeRole(),
				subjectId,
				questionId,
				body.formCode(),
				body.prompt(),
				content(body));
		return ResponseEntity.ok(item(line));
	}

	@PostMapping("/subjects/{subjectId}/questions/clone")
	@Operation(operationId = "cloneTeacherSubjectQuestions", summary = "Copier des questions vers une matière")
	@ApiResponse(responseCode = "200", description = "Copies créées, les questions d'origine restent")
	@ApiResponse(
			responseCode = "400",
			description = "Aucune question choisie, ou matière d'arrivée absente de la demande",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière ou question absente pour ce professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherSubjectQuestionsResponse> cloneQuestions(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long subjectId,
			@RequestBody CloneTeacherSubjectQuestionsRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		CloneTeacherSubjectQuestionsRequest body = request == null
				? new CloneTeacherSubjectQuestionsRequest(null, null)
				: request;
		long targetSubjectId = body.targetSubjectId() == null ? 0L : body.targetSubjectId();
		return ResponseEntity.ok(questions(teacherQuestionUseCase.cloneQuestions(
				user.userId(),
				user.activeRole(),
				subjectId,
				targetSubjectId,
				body.questionIds())));
	}

	@GetMapping("/subjects/{subjectId}/followers")
	@Operation(operationId = "listTeacherSubjectFollowers", summary = "Voir quels enfants suivent ce cours")
	@ApiResponse(responseCode = "200", description = "Nombre d'enfants et leurs noms, avant une suppression")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière absente pour ce professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherSubjectFollowersResponse> followers(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long subjectId) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		TeacherSubjectFollowersView view = teacherQuestionUseCase.followers(
				user.userId(), user.activeRole(), subjectId);
		return ResponseEntity.ok(new TeacherSubjectFollowersResponse(view.childCount(), view.warning(), view.names()));
	}

	@PostMapping("/subjects/{subjectId}/questions/delete")
	@Operation(operationId = "deleteTeacherSubjectQuestions", summary = "Supprimer une ou plusieurs questions")
	@ApiResponse(responseCode = "200", description = "Questions retirées de la matière")
	@ApiResponse(
			responseCode = "400",
			description = "Aucune question choisie",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière ou question absente pour ce professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<DeleteTeacherSubjectQuestionsResponse> deleteQuestions(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long subjectId,
			@RequestBody DeleteTeacherSubjectQuestionsRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		DeleteTeacherSubjectQuestionsRequest body = request == null
				? new DeleteTeacherSubjectQuestionsRequest(null)
				: request;
		return ResponseEntity.ok(new DeleteTeacherSubjectQuestionsResponse(teacherQuestionUseCase.deleteQuestions(
				user.userId(),
				user.activeRole(),
				subjectId,
				body.questionIds())));
	}

	private static ContentInput content(CreateTeacherSubjectQuestionRequest body) {
		String code = body.formCode() == null ? "" : body.formCode().trim();
		if (TeacherQuestionService.TRUE_FALSE.equals(code)) {
			return new StatementsInput(statementInputs(body.statements()));
		}
		if (TeacherQuestionService.LINKS.equals(code)) {
			return new LinksInput(linkInputs(body.links()));
		}
		if (TeacherQuestionService.CASE_VIDE.equals(code)) {
			return new GapsInput(body.sentence(), wordInputs(body.answers()), wordInputs(body.decoys()));
		}
		return new LegacyInput(inputs(body.choices()), pairInputs(body.pairs()), itemInputs(body.items()));
	}

	private static List<ChoiceInput> inputs(List<TeacherQuestionChoiceInput> choices) {
		if (choices == null) {
			return List.of();
		}
		List<ChoiceInput> inputs = new ArrayList<>();
		for (TeacherQuestionChoiceInput choice : choices) {
			if (choice == null) {
				continue;
			}
			inputs.add(new ChoiceInput(choice.label(), choice.correct()));
		}
		return inputs;
	}

	private static List<PairInput> pairInputs(List<TeacherQuestionPairInput> pairs) {
		if (pairs == null) {
			return List.of();
		}
		List<PairInput> inputs = new ArrayList<>();
		for (TeacherQuestionPairInput pair : pairs) {
			if (pair == null) {
				continue;
			}
			inputs.add(new PairInput(pair.questionSide(), pair.answerSide()));
		}
		return inputs;
	}

	private static List<ItemInput> itemInputs(List<TeacherQuestionItemInput> items) {
		if (items == null) {
			return List.of();
		}
		List<ItemInput> inputs = new ArrayList<>();
		for (TeacherQuestionItemInput item : items) {
			if (item == null) {
				continue;
			}
			inputs.add(new ItemInput(item.label()));
		}
		return inputs;
	}

	private static List<StatementInput> statementInputs(List<TeacherQuestionStatementInput> statements) {
		if (statements == null) {
			return List.of();
		}
		List<StatementInput> inputs = new ArrayList<>();
		for (TeacherQuestionStatementInput statement : statements) {
			if (statement == null) {
				continue;
			}
			inputs.add(new StatementInput(statement.label(), statement.truthful()));
		}
		return inputs;
	}

	private static List<GapWordInput> wordInputs(List<TeacherQuestionGapWordInput> words) {
		if (words == null) {
			return List.of();
		}
		List<GapWordInput> inputs = new ArrayList<>();
		for (TeacherQuestionGapWordInput word : words) {
			if (word == null) {
				continue;
			}
			inputs.add(new GapWordInput(word.label()));
		}
		return inputs;
	}

	private static List<LinkInput> linkInputs(List<TeacherQuestionLinkInput> links) {
		if (links == null) {
			return List.of();
		}
		List<LinkInput> inputs = new ArrayList<>();
		for (TeacherQuestionLinkInput link : links) {
			if (link == null) {
				continue;
			}
			inputs.add(new LinkInput(link.leftLabel(), link.rightLabel()));
		}
		return inputs;
	}

	private static TeacherQuestionFormsResponse forms(TeacherQuestionFormsView view) {
		List<TeacherQuestionFormItem> items = new ArrayList<>();
		for (FormLine form : view.forms()) {
			items.add(new TeacherQuestionFormItem(form.code(), form.label(), form.sortOrder()));
		}
		return new TeacherQuestionFormsResponse(items);
	}

	private static TeacherSubjectQuestionsResponse questions(TeacherSubjectQuestionsView view) {
		List<TeacherSubjectQuestionItem> items = new ArrayList<>();
		for (QuestionLine line : view.questions()) {
			items.add(item(line));
		}
		return new TeacherSubjectQuestionsResponse(items);
	}

	private static TeacherSubjectQuestionItem item(QuestionLine line) {
		List<TeacherQuestionChoiceItem> choices = new ArrayList<>();
		for (ChoiceLine choice : line.choices()) {
			choices.add(new TeacherQuestionChoiceItem(choice.id(), choice.label(), choice.correct()));
		}
		List<TeacherQuestionPairItem> pairs = new ArrayList<>();
		for (PairLine pair : line.pairs()) {
			pairs.add(new TeacherQuestionPairItem(pair.id(), pair.questionSide(), pair.answerSide()));
		}
		List<TeacherQuestionItem> items = new ArrayList<>();
		for (ItemLine orderItem : line.items()) {
			items.add(new TeacherQuestionItem(orderItem.id(), orderItem.label()));
		}
		List<TeacherQuestionStatementItem> statements = new ArrayList<>();
		for (StatementLine statement : line.statements()) {
			statements.add(new TeacherQuestionStatementItem(statement.id(), statement.label(), statement.truthful()));
		}
		List<TeacherQuestionLinkItem> links = new ArrayList<>();
		for (LinkLine link : line.links()) {
			links.add(new TeacherQuestionLinkItem(link.id(), link.leftLabel(), link.rightLabel()));
		}
		List<TeacherQuestionGapWordItem> answers = new ArrayList<>();
		for (GapWordLine answer : line.answers()) {
			answers.add(new TeacherQuestionGapWordItem(answer.id(), answer.label()));
		}
		List<TeacherQuestionGapWordItem> decoys = new ArrayList<>();
		for (GapWordLine decoy : line.decoys()) {
			decoys.add(new TeacherQuestionGapWordItem(decoy.id(), decoy.label()));
		}
		return new TeacherSubjectQuestionItem(
				line.id(), line.formCode(), line.formLabel(), line.prompt(),
				choices, pairs, items, statements, links, line.sentence(), answers, decoys);
	}

	@Schema(name = "TeacherQuestionFormsResponse")
	public record TeacherQuestionFormsResponse(List<TeacherQuestionFormItem> forms) {
	}

	@Schema(name = "TeacherQuestionFormItem")
	public record TeacherQuestionFormItem(String code, String label, int sortOrder) {
	}

	@Schema(name = "TeacherSubjectQuestionsResponse")
	public record TeacherSubjectQuestionsResponse(List<TeacherSubjectQuestionItem> questions) {
	}

	@Schema(name = "TeacherSubjectQuestionItem")
	public record TeacherSubjectQuestionItem(
			long id,
			String formCode,
			String formLabel,
			String prompt,
			List<TeacherQuestionChoiceItem> choices,
			List<TeacherQuestionPairItem> pairs,
			List<TeacherQuestionItem> items,
			List<TeacherQuestionStatementItem> statements,
			List<TeacherQuestionLinkItem> links,
			String sentence,
			List<TeacherQuestionGapWordItem> answers,
			List<TeacherQuestionGapWordItem> decoys) {
	}

	@Schema(name = "TeacherQuestionChoiceItem")
	public record TeacherQuestionChoiceItem(long id, String label, boolean correct) {
	}

	@Schema(name = "TeacherQuestionPairItem")
	public record TeacherQuestionPairItem(long id, String questionSide, String answerSide) {
	}

	@Schema(name = "TeacherQuestionItem")
	public record TeacherQuestionItem(long id, String label) {
	}

	@Schema(name = "TeacherQuestionStatementItem")
	public record TeacherQuestionStatementItem(long id, String label, boolean truthful) {
	}

	@Schema(name = "TeacherQuestionLinkItem")
	public record TeacherQuestionLinkItem(long id, String leftLabel, String rightLabel) {
	}

	@Schema(name = "TeacherQuestionGapWordItem")
	public record TeacherQuestionGapWordItem(long id, String label) {
	}

	@Schema(name = "CloneTeacherSubjectQuestionsRequest")
	public record CloneTeacherSubjectQuestionsRequest(Long targetSubjectId, List<Long> questionIds) {
	}

	@Schema(name = "TeacherSubjectFollowersResponse")
	public record TeacherSubjectFollowersResponse(int childCount, String warning, List<String> names) {
	}

	@Schema(name = "DeleteTeacherSubjectQuestionsRequest")
	public record DeleteTeacherSubjectQuestionsRequest(List<Long> questionIds) {
	}

	@Schema(name = "DeleteTeacherSubjectQuestionsResponse")
	public record DeleteTeacherSubjectQuestionsResponse(List<Long> deletedIds) {
	}

	@Schema(name = "CreateTeacherSubjectQuestionRequest")
	public record CreateTeacherSubjectQuestionRequest(
			String formCode,
			String prompt,
			List<TeacherQuestionChoiceInput> choices,
			List<TeacherQuestionPairInput> pairs,
			List<TeacherQuestionItemInput> items,
			List<TeacherQuestionStatementInput> statements,
			List<TeacherQuestionLinkInput> links,
			String sentence,
			List<TeacherQuestionGapWordInput> answers,
			List<TeacherQuestionGapWordInput> decoys) {
	}

	@Schema(name = "TeacherQuestionChoiceInput")
	public record TeacherQuestionChoiceInput(String label, Boolean correct) {
	}

	@Schema(name = "TeacherQuestionPairInput")
	public record TeacherQuestionPairInput(String questionSide, String answerSide) {
	}

	@Schema(name = "TeacherQuestionItemInput")
	public record TeacherQuestionItemInput(String label) {
	}

	@Schema(name = "TeacherQuestionStatementInput")
	public record TeacherQuestionStatementInput(String label, Boolean truthful) {
	}

	@Schema(name = "TeacherQuestionLinkInput")
	public record TeacherQuestionLinkInput(String leftLabel, String rightLabel) {
	}

	@Schema(name = "TeacherQuestionGapWordInput")
	public record TeacherQuestionGapWordInput(String label) {
	}
}

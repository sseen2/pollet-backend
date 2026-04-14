package com.octagram.pollet.survey.application.service;

import org.springframework.data.domain.Slice;
import com.octagram.pollet.gifticon.application.service.GifticonService;
import com.octagram.pollet.gifticon.domain.model.GifticonProduct;
import com.octagram.pollet.gifticon.domain.status.GifticonErrorCode;
import com.octagram.pollet.global.exception.BusinessException;
import com.octagram.pollet.member.application.service.MemberService;
import com.octagram.pollet.member.application.mapper.MemberTagMapper;
import com.octagram.pollet.member.domain.model.Member;
import com.octagram.pollet.member.domain.model.MemberTag;
import com.octagram.pollet.member.presentation.dto.response.MemberTagResponse;
import com.octagram.pollet.survey.application.mapper.QuestionMapper;
import com.octagram.pollet.survey.application.mapper.QuestionOptionSubmissionMapper;
import com.octagram.pollet.survey.application.mapper.QuestionSubmissionMapper;
import com.octagram.pollet.survey.application.mapper.SurveyMapper;
import com.octagram.pollet.survey.application.mapper.SurveySubmissionMapper;
import com.octagram.pollet.survey.application.mapper.TagMapper;
import com.octagram.pollet.survey.domain.model.Question;
import com.octagram.pollet.survey.domain.model.QuestionOption;
import com.octagram.pollet.survey.domain.model.QuestionOptionSubmission;
import com.octagram.pollet.survey.domain.model.QuestionSubmission;
import com.octagram.pollet.survey.domain.model.Survey;
import com.octagram.pollet.survey.domain.model.SurveyPointHistory;
import com.octagram.pollet.survey.domain.model.SurveySubmission;
import com.octagram.pollet.survey.domain.model.SurveyTag;
import com.octagram.pollet.survey.domain.model.Tag;
import com.octagram.pollet.survey.domain.model.type.EndCondition;
import com.octagram.pollet.survey.domain.model.type.RewardType;
import com.octagram.pollet.survey.domain.repository.QuestionOptionSubmissionRepository;
import com.octagram.pollet.survey.domain.repository.QuestionRepository;
import com.octagram.pollet.survey.domain.repository.QuestionSubmissionRepository;
import com.octagram.pollet.survey.domain.repository.SurveyPointHistoryRepository;
import com.octagram.pollet.survey.domain.repository.SurveyRepository;
import com.octagram.pollet.survey.domain.repository.SurveySubmissionRepository;
import com.octagram.pollet.survey.domain.repository.SurveyTagRepository;
import com.octagram.pollet.survey.domain.repository.TagRepository;
import com.octagram.pollet.survey.domain.status.SurveyErrorCode;
import com.octagram.pollet.survey.domain.status.TagErrorCode;
import com.octagram.pollet.survey.presentation.dto.request.QuestionSubmissionRequest;
import com.octagram.pollet.survey.presentation.dto.request.SurveyCreateQuestionOptionRequest;
import com.octagram.pollet.survey.presentation.dto.request.SurveyCreateQuestionRequest;
import com.octagram.pollet.survey.presentation.dto.request.SurveyCreateRequest;
import com.octagram.pollet.survey.presentation.dto.request.SurveyFilterRequest;
import com.octagram.pollet.survey.presentation.dto.request.SurveySubmissionRequest;
import com.octagram.pollet.survey.presentation.dto.response.ParticipantResultResponse;
import com.octagram.pollet.survey.presentation.dto.response.QuestionOptionListResponse;
import com.octagram.pollet.survey.presentation.dto.response.QuestionStatisticsResponse;
import com.octagram.pollet.survey.presentation.dto.response.SurveyGetRecentResponse;
import com.octagram.pollet.survey.presentation.dto.response.SurveyMetadataResponse;
import com.octagram.pollet.survey.presentation.dto.response.TargetQuestionResponse;
import com.octagram.pollet.survey.presentation.dto.response.standard.QuestionResponse;
import com.octagram.pollet.survey.presentation.dto.response.standard.SurveyResponse;
import com.octagram.pollet.survey.presentation.dto.response.standard.TagResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyService {

	private final GifticonService gifticonService;
	private final QuestionSubmissionMapper questionSubmissionMapper;
	private final QuestionOptionSubmissionRepository questionOptionSubmissionRepository;
	private final QuestionSubmissionRepository questionSubmissionRepository;
	private final SurveySubmissionRepository surveySubmissionRepository;
	private final SurveyPointHistoryRepository surveyPointHistoryRepository;
	private final SurveyRepository surveyRepository;
	private final TagRepository tagRepository;
	private final QuestionRepository questionRepository;
	private final SurveyTagRepository surveyTagRepository;
	private final SurveyMapper surveyMapper;
	private final TagMapper tagMapper;
	private final QuestionMapper questionMapper;
	private final MemberService memberService;
	private final SurveySubmissionMapper surveySubmissionMapper;
	private final QuestionOptionSubmissionMapper questionOptionSubmissionMapper;
	private final MemberTagMapper memberTagMapper;

	@Transactional(readOnly = true)
	public List<TagResponse> getAllTags() {
		return tagRepository.findAll().stream()
			.map(tagMapper::toResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<TagResponse> getAllUsedTags() {
		return surveyTagRepository.findAllUsedTags().stream()
			.map(tagMapper::toResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public SurveyResponse getSurveyById(Long surveyId) {
		Survey survey = surveyRepository.findByIdQueryDsl(surveyId)
			.orElseThrow(() -> new BusinessException(SurveyErrorCode.SURVEY_NOT_FOUND));
		return surveyMapper.toSurveyResponse(survey);
	}

	@Transactional(readOnly = true)
	public List<TagResponse> getTagsBySurveyId(Long surveyId) {
		return surveyTagRepository.findBySurveyId(surveyId).stream()
			.map(surveyTag -> tagMapper.toResponse(surveyTag.getTag()))
			.toList();
	}

	@Transactional(readOnly = true)
	public long getActiveCount() {
		LocalDateTime now = LocalDateTime.now();
		return surveyRepository.getActiveCount(now);
	}

	@Transactional(readOnly = true)
	public List<SurveyGetRecentResponse> getLatest4Surveys() {
		return surveyRepository.findTop4ByOrderByCreatedAt().stream()
				.map(surveyMapper::toGetRecentResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TargetQuestionResponse> getLatest4SurveysTargetQuestions() {
		List<Survey> latestSurveys = surveyRepository.findTop4ByOrderByCreatedAt();

		return latestSurveys.stream().map(survey -> {
			List<Question> questions = questionRepository.findQuestionsBySurveyId(survey.getId());

			Question targetQuestion = questions.stream()
					.filter(Question::getIsCheckTarget)
					.findFirst()
					.orElseThrow(() -> new BusinessException(SurveyErrorCode.QUESTION_NOT_FOUND));

			List<QuestionOptionListResponse> questionOptions = questionRepository.findOptionsByQuestionId(targetQuestion.getId())
					.stream()
					.map(questionMapper::toQuestionOptionListResponse)
					.toList();

			return questionMapper.toTargetQuestionResponse(targetQuestion, questionOptions);
		}).toList();
	}

	@Transactional(readOnly = true)
	public List<SurveyResponse> filterSurveys(SurveyFilterRequest filter, Pageable pageable) {
		List<String> allTags = new ArrayList<>();
		if (filter.genders() != null) allTags.addAll(filter.genders());
		if (filter.ages() != null) allTags.addAll(filter.ages());
		if (filter.jobs() != null) allTags.addAll(filter.jobs());
		if (filter.tags() != null) allTags.addAll(filter.tags());
		List<Survey> surveysResult = surveyRepository.findByFilter(allTags, filter, pageable);
		return surveysResult.stream()
			.map(surveyMapper::toSurveyResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<QuestionResponse> getQuestions(Long surveyId) {
		List<Question> result = questionRepository.findBySurveyId(surveyId);
		return result.stream()
			.map(questionMapper::toResponse)
			.toList();
	}

    @Retryable(
            retryFor = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
	@Transactional
	public void submitSurvey(Long surveyId, Member member, SurveySubmissionRequest request) {
		Survey survey = surveyRepository.findByIdByOptimistic(surveyId)
			.orElseThrow(() -> new BusinessException(SurveyErrorCode.SURVEY_NOT_FOUND));

		validateInProgressSurvey(survey);
		validateOwnSurvey(survey, member);
		validateSurveyPoint(survey);
		validateSurveyNotSubmitted(survey, member);
		validateRequiredQuestionSubmission(request.questionSubmissions());

		SurveySubmission surveySubmission = saveSurveySubmission(request, survey, member);
		List<QuestionSubmission> questionSubmissions = saveQuestionSubmissions(request.questionSubmissions(), surveySubmission);
		saveQuestionOptionSubmissions(request.questionSubmissions(), questionSubmissions);
		survey.submitSurvey();

		if (survey.getRewardType().equals(RewardType.POINT)) {
			long amount = survey.getRewardPoint();
			survey.updateAvailablePoint(-amount);
			memberService.surveyPointHistory(member, survey, amount);
			updateSurveyPointHistory(survey, member, amount);
		}
	}

    @Recover
    public void recoverSubmitSurvey(ObjectOptimisticLockingFailureException e, Long surveyId, Member member, SurveySubmissionRequest request) {
        throw new BusinessException(SurveyErrorCode.TOO_MANY_REQUESTS);
    }

    @Recover
    public void recoverBusinessException(BusinessException e, Long surveyId, Member member, SurveySubmissionRequest request) {
        throw e;
    }

	private void updateSurveyPointHistory(Survey survey, Member member, long amount) {
		SurveyPointHistory history = SurveyPointHistory.builder()
			.survey(survey)
			.member(member)
			.amount(amount)
			.build();
		surveyPointHistoryRepository.save(history);
	}

	private void validateInProgressSurvey(Survey survey) {
		LocalDateTime now = LocalDateTime.now();

		if (now.isBefore(survey.getStartDateTime())) {
			throw new BusinessException(SurveyErrorCode.SURVEY_NOT_STARTED);
		}

		boolean isEndByDate = EndCondition.END_BY_DATE.equals(survey.getEndCondition()) && now.isAfter(survey.getEndDateTime());
		boolean isEndBySubmissionCount =
			EndCondition.END_BY_SUBMISSION_COUNT.equals(survey.getEndCondition())
			&& survey.getCurrentSubmissionCount() >= survey.getRequireSubmissionCount();

		if (isEndByDate || isEndBySubmissionCount) {
			throw new BusinessException(SurveyErrorCode.SURVEY_CLOSED);
		}
	}

	private void validateOwnSurvey(Survey survey, Member member) {
		if (survey.getMember().getId().equals(member.getId())) {
			throw new BusinessException(SurveyErrorCode.SURVEY_SELF_SUBMISSION_NOT_ALLOWED);
		}
	}

	private void validateSurveyPoint(Survey survey) {
		if (survey.getRewardType().equals(RewardType.POINT) && survey.getRewardPoint() > survey.getAvailablePoint()) {
			throw new BusinessException(SurveyErrorCode.SURVEY_NOT_ENOUGH_POINTS);
		}
	}

	private void validateSurveyNotSubmitted(Survey survey, Member member) {
		if (surveySubmissionRepository.existsBySurveyAndMember(survey, member)) {
			throw new BusinessException(SurveyErrorCode.ALREADY_SUBMIT_SURVEY);
		}
	}

	private void validateRequiredQuestionSubmission(List<QuestionSubmissionRequest> requests) {
		for (QuestionSubmissionRequest request : requests) {
			Question question = questionRepository.findById(request.questionId())
				.orElseThrow(() -> new BusinessException(SurveyErrorCode.QUESTION_NOT_FOUND));

			if (question.getIsRequired() && request.questionOptionSubmissions().isEmpty()) {
				throw new BusinessException(SurveyErrorCode.REQUIRED_QUESTION_SUBMISSION);
			}
		}
	}

	private SurveySubmission saveSurveySubmission(SurveySubmissionRequest request, Survey survey, Member member) {
		SurveySubmission surveySubmission = surveySubmissionMapper.toEntityFromSurveySubmissionRequest(request, survey, member);
		return surveySubmissionRepository.save(surveySubmission);
	}

	private List<QuestionSubmission> saveQuestionSubmissions(List<QuestionSubmissionRequest> requests, SurveySubmission surveySubmission) {
		List<QuestionSubmission> questions = requests.stream()
			.map(req -> questionSubmissionMapper.toEntityFromQuestionSubmissionRequest(req, surveySubmission))
			.toList();
		questionSubmissionRepository.saveAll(questions);
		return questions;
	}

	private void saveQuestionOptionSubmissions(List<QuestionSubmissionRequest> requests, List<QuestionSubmission> questionSubmissions) {
		List<QuestionOptionSubmission> optionSubs = new ArrayList<>();
		for (int i = 0; i < questionSubmissions.size(); i++) {
			QuestionSubmission submission = questionSubmissions.get(i);
			QuestionSubmissionRequest submissionRequest = requests.get(i);

			submissionRequest.questionOptionSubmissions().stream()
				.map(request -> questionOptionSubmissionMapper.toEntityFromQuestionOptionSubmissionRequest(request, submission))
				.forEach(optionSubs::add);
		}

		if (!optionSubs.isEmpty()) {
			questionOptionSubmissionRepository.saveAll(optionSubs);
		}
	}

	@Transactional
	public void createSurvey(String memberId, SurveyCreateRequest request) {
		log.info(memberId);
		Member member = memberService.findByMemberId(memberId);

		GifticonProduct gifticonProduct = gifticonService.findProductById(request.rewardGifticonProductId())
			.orElseThrow(() -> new BusinessException(GifticonErrorCode.GIFTICON_PRODUCT_NOT_FOUND));

		Survey survey = Survey.builder()
			.member(member)
			.creatorName(member.getMemberId())
			.coverUrl(request.coverUrl())
			.title(request.title())
			.purpose(request.purpose())
			.startDateTime(request.startDateTime())
			.endDateTime(request.endDateTime())
			.endCondition(request.endCondition())
			.submissionExpireDate(request.submissionExpireDate())
			.description(request.description())
			.privacyType(request.privacyType())
			.privacyPurposeValue(request.privacyPurposeValue())
			.privacyContents(request.privacyContents())
			.privacyExpireDate(request.privacyExpireDate())
			.primaryColor(request.primaryColor())
			.secondaryColor(request.secondaryColor())
			.rewardType(request.rewardType())
			.requireSubmissionCount(request.requireSubmissionCount())
			.estimatedTime(request.estimatedTime())
			.rewardPointPerMinute(request.rewardPointPerMinute())
			.gifticonProduct(gifticonProduct)
			.rewardGifticonProductCount(request.rewardGifticonProductCount())
			.availablePoint(0L)
			.build();

		List<String> tags = new ArrayList<>();
		tags.add(request.targetGender());
		tags.add(request.targetAge());
		tags.add(request.targetJob());
		tags.addAll(request.tags());
		tags.removeIf(StringUtils::isBlank);

		for (String tagString : tags) {
			Tag tag = tagRepository.findByName(tagString)
				.orElseThrow(() -> new BusinessException(TagErrorCode.TAG_NOT_FOUND));
			SurveyTag surveyTag = SurveyTag.builder()
				.tag(tag)
				.build();
			surveyTag.setSurvey(survey);
			survey.addSurveyTag(surveyTag);
		}

		for (SurveyCreateQuestionRequest questionDto : request.questions()) {
			Question question = Question.builder()
				.page(questionDto.page())
				.order(questionDto.order())
				.title(questionDto.title())
				.description(questionDto.description())
				.type(questionDto.type())
				.isRequired(questionDto.isRequired())
				.isCheckTarget(questionDto.isCheckTarget())
				.isCheckDiligent(questionDto.isCheckDiligent())
				.imageUrl(questionDto.imageUrl())
				.build();

			for (SurveyCreateQuestionOptionRequest optionDto : questionDto.options()) {
				QuestionOption option = QuestionOption.builder()
					.order(optionDto.order())
					.content(optionDto.content())
					.imageUrl(optionDto.imageUrl())
					.isCheckTarget(optionDto.isCheckTarget())
					.isCheckDiligent(optionDto.isCheckDiligent())
					.build();

				question.addOption(option);
			}

			survey.addQuestion(question);
		}

		surveyRepository.save(survey);

		if (request.rewardType().equals(RewardType.POINT)) {
			long amount = survey.getRequireSubmissionCount() * survey.getRewardPoint();
			memberService.surveyPointHistory(member, survey, -amount);
			survey.updateAvailablePoint(amount);
		}
	}

	@Transactional(readOnly = true)
	public Slice<QuestionStatisticsResponse> getSurveyResults(String memberId, Long surveyId, Pageable pageable) {
		Survey survey = surveyRepository.findById(surveyId)
				.orElseThrow(() -> new BusinessException(SurveyErrorCode.SURVEY_NOT_FOUND));

		validateSurveyCreator(memberId, survey);

		int respondentCount = surveySubmissionRepository.countBySurvey(survey);

		Slice<Question> questionsPage = questionRepository.findBySurveyId(surveyId, pageable);

		Slice<QuestionStatisticsResponse.QuestionResult> questionResults = questionsPage.map(question -> {
			int answeredCount = questionSubmissionRepository.countByQuestion(question);

			List<QuestionStatisticsResponse.OptionResult> options = question.getOptions().stream()
					.map(option -> {
						int responseCount = questionOptionSubmissionRepository.countByQuestionOption(option);
						double ratio = answeredCount == 0 ? 0 : (double) responseCount / answeredCount;
						return questionMapper.toOptionResult(option, responseCount, ratio);
					})
					.toList();

			List<String> etcAnswers = switch (question.getType()) {
				case SINGLE_CHOICE, MULTIPLE_CHOICE -> List.of();
				case SHORT_ANSWER, LONG_ANSWER -> questionSubmissionRepository.findAnswersByQuestion(question);
			};

			return questionMapper.toQuestionResult(question, options, answeredCount, etcAnswers);
		});

		return questionResults.map(questionResult -> new QuestionStatisticsResponse(
				survey.getId(),
				survey.getTitle(),
				respondentCount,
				List.of(questionResult)
		));
	}

	@Transactional(readOnly = true)
	public Slice<ParticipantResultResponse.QuestionAnswer> getParticipantResult(String memberId, Long surveyId, Long submissionId, Pageable pageable) {
		Survey survey = surveyRepository.findById(surveyId)
				.orElseThrow(() -> new BusinessException(SurveyErrorCode.SURVEY_NOT_FOUND));

		SurveySubmission surveySubmission = surveySubmissionRepository.findBySurveyIdAndId(surveyId, submissionId)
				.orElseThrow(() -> new BusinessException(SurveyErrorCode.INVALID_SUBMISSION));

		validateSurveyCreator(memberId, survey);

		Slice<QuestionSubmission> questionSubmissions = questionSubmissionRepository
				.findBySurveySubmissionId(submissionId, pageable);

		return questionSubmissions.map(qs -> {
			List<QuestionOptionSubmission> optionSubmissions = questionOptionSubmissionRepository
					.findByQuestionSubmissionId(qs.getId());
			List<ParticipantResultResponse.SelectedOption> selectedOptions = optionSubmissions.stream()
					.map(questionSubmissionMapper::toSelectedOption)
					.toList();

			return questionSubmissionMapper.toQuestionAnswer(qs, selectedOptions);
		});
	}

	private void validateSurveyCreator(String memberId, Survey survey) {
		Member member = memberService.findByMemberId(memberId);

		if (!survey.getMember().getId().equals(member.getId())) {
			throw new BusinessException(SurveyErrorCode.UNAUTHORIZED_ACCESS);
		}
	}

	@Transactional(readOnly = true)
	public SurveyMetadataResponse getSurveyMetadata(Long surveyId) {
		Survey survey = surveyRepository.findById(surveyId)
				.orElseThrow(() -> new BusinessException(SurveyErrorCode.SURVEY_NOT_FOUND));
		return surveyMapper.toSurveyMetadataResponse(survey);
	}

	private String resolveSurveyStatus(LocalDateTime start, LocalDateTime end) {
		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(start)) return "대기중";
		if (now.isAfter(end)) return "종료";
		return "진행중";
	}

	@Transactional(readOnly = true)
	public List<SurveyResponse> findMyCreatedSurveys(String memberId, Pageable pageable) {
		Member member = memberService.findByMemberId(memberId);
		List<Survey> surveys = surveyRepository.findByMemberId(member.getId(), pageable);
		return surveys.stream()
			.map(surveyMapper::toSurveyResponse)
			.toList();
	}

	public List<SurveyResponse> findMyParticipatedSurveys(String memberId, Pageable pageable) {
		Member member = memberService.findByMemberId(memberId);
		List<Survey> surveys = surveyRepository.findParticipatedByMemberId(member.getId(), pageable);
		return surveys.stream()
			.map(surveyMapper::toSurveyResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<MemberTagResponse> findTagsFromParticipatedSurveys(String memberId) {
		List<MemberTag> memberTags = memberService.findMemberTags(memberId);
		return memberTags.stream()
			.map(memberTagMapper::toResponse)
			.toList();
	}
}

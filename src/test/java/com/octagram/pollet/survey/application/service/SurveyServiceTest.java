package com.octagram.pollet.survey.application.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import com.octagram.pollet.global.exception.BusinessException;
import com.octagram.pollet.member.domain.model.Member;
import com.octagram.pollet.member.domain.model.type.AuthProvider;
import com.octagram.pollet.member.domain.model.type.Role;
import com.octagram.pollet.member.domain.repository.MemberRepository;
import com.octagram.pollet.support.annotation.ServiceTest;
import com.octagram.pollet.survey.domain.model.Question;
import com.octagram.pollet.survey.domain.model.QuestionOption;
import com.octagram.pollet.survey.domain.model.Survey;
import com.octagram.pollet.survey.domain.model.type.EndCondition;
import com.octagram.pollet.survey.domain.model.type.PrivacyType;
import com.octagram.pollet.survey.domain.model.type.QuestionType;
import com.octagram.pollet.survey.domain.model.type.RewardType;
import com.octagram.pollet.survey.domain.repository.SurveyRepository;
import com.octagram.pollet.survey.domain.status.SurveyErrorCode;
import com.octagram.pollet.survey.presentation.dto.request.QuestionOptionSubmissionRequest;
import com.octagram.pollet.survey.presentation.dto.request.QuestionSubmissionRequest;
import com.octagram.pollet.survey.presentation.dto.request.SurveySubmissionRequest;

@TestPropertySource(properties = {
	"spring.sql.init.mode=never"
})
@ServiceTest
class SurveyServiceTest {

	@Autowired
	private SurveyService surveyService;

	@Autowired
	private SurveyRepository surveyRepository;

	@Autowired
	private MemberRepository memberRepository;

	private Member member1;
	private Member member2;
	private Member member3;
	private Survey survey;
    private Survey survey2;
	private Question question;
	private QuestionOption questionOption;

	@TestConfiguration
	static class TestMvcSupportConfig {
		@Bean
		HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
			return new HandlerMappingIntrospector();
		}

		@Bean
		ClientRegistrationRepository clientRegistrationRepository() {
			ClientRegistration reg = ClientRegistration.withRegistrationId("test")
				.clientId("test-client")
				.clientSecret("test-secret")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
				.authorizationUri("https://example.com/oauth/authorize")
				.tokenUri("https://example.com/oauth/token")
				.userInfoUri("https://example.com/userinfo")
				.userNameAttributeName("id")
				.clientName("test")
				.scope("profile")
				.build();
			return new InMemoryClientRegistrationRepository(reg);
		}
	}

	@BeforeEach
	void setUp() {
		member1 = Member.builder()
			.authProvider(AuthProvider.KAKAO)
			.profileImageUrl("")
			.role(Role.MEMBER)
			.memberId("test_1")
			.email("test1@test.com")
			.build();
		member2 = Member.builder()
			.authProvider(AuthProvider.KAKAO)
			.profileImageUrl("")
			.role(Role.MEMBER)
			.memberId("test_2")
			.email("test2@test.com")
			.build();
		member3 = Member.builder()
			.authProvider(AuthProvider.KAKAO)
			.profileImageUrl("")
			.role(Role.MEMBER)
			.memberId("test_3")
			.email("test3@test.com")
			.build();
		memberRepository.saveAll(List.of(member1, member2, member3));

		survey = Survey.builder()
			.member(member3)
			.title("테스트 설문조사")
			.creatorName(member3.getMemberId())
			.purpose("테스트")
			.startDateTime(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
			.endDateTime(LocalDateTime.of(2040, 12, 31, 0, 0, 0))
			.endCondition(EndCondition.END_BY_DATE)
			.submissionExpireDate(LocalDate.of(2050, 10, 31))
			.privacyType(PrivacyType.AGREEMENT_FOR_COLLECTION_AND_USE)
			.privacyContents(0L)
			.privacyPurposeValue("")
			.privacyExpireDate(LocalDate.of(2050, 10, 31))
			.estimatedTime(3L)
			.requireSubmissionCount(1L)
			.rewardType(RewardType.POINT)
			.rewardPointPerMinute(10L)
			// 지급 가능한 예치 포인트 (1명 가능)
			.availablePoint(30L)
			.build();

        survey2 = Survey.builder()
                .member(member3)
                .title("테스트 설문조사")
                .creatorName(member3.getMemberId())
                .purpose("테스트")
                .startDateTime(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                .endDateTime(LocalDateTime.of(2040, 12, 31, 0, 0, 0))
                .endCondition(EndCondition.END_BY_DATE)
                .submissionExpireDate(LocalDate.of(2050, 10, 31))
                .privacyType(PrivacyType.AGREEMENT_FOR_COLLECTION_AND_USE)
                .privacyContents(0L)
                .privacyPurposeValue("")
                .privacyExpireDate(LocalDate.of(2050, 10, 31))
                .estimatedTime(3L)
                .requireSubmissionCount(1L)
                .rewardType(RewardType.POINT)
                .rewardPointPerMinute(10L)
                .availablePoint(999999L)
                .build();

		question = Question.builder()
			.order(1L)
			.page(1L)
			.title("테스트 문항")
			.type(QuestionType.SINGLE_CHOICE)
			.isRequired(false)
			.isCheckTarget(false)
			.isCheckDiligent(false)
			.build();

		questionOption = QuestionOption.builder()
			.order(1L)
			.content("테스트 문항 선택지")
			.isCheckTarget(false)
			.isCheckDiligent(false)
			.build();

		question.addOption(questionOption);
		survey.addQuestion(question);

		surveyRepository.save(survey);
        surveyRepository.save(survey2);
	}

	@Test
	@Transactional(propagation = Propagation.NEVER)
	void concurrencyControlTest() throws Exception {
		Long surveyId = survey.getId();

		ExecutorService pool = Executors.newFixedThreadPool(2);
		CyclicBarrier startGate = new CyclicBarrier(3);

		SurveySubmissionRequest request1 = buildSurveySubmissionRequest();
		SurveySubmissionRequest request2 = buildSurveySubmissionRequest();

		Future<Boolean> future1 = pool.submit(() -> submitAsync(startGate, surveyId, member1, request1));
		Future<Boolean> future2 = pool.submit(() -> submitAsync(startGate, surveyId, member2, request2));

		startGate.await(3, TimeUnit.SECONDS);

		boolean result1 = getResult(future1);
		boolean result2 = getResult(future2);

		pool.shutdown();

		assertThat(result1 ^ result2).isTrue();
	}

	private SurveySubmissionRequest buildSurveySubmissionRequest() {
		QuestionOptionSubmissionRequest qosRequest = new QuestionOptionSubmissionRequest(questionOption.getId());
		QuestionSubmissionRequest qsRequest = new QuestionSubmissionRequest(question.getId(), null, List.of(qosRequest));
		return new SurveySubmissionRequest(
			LocalDateTime.of(2025, 9, 15, 9, 0, 0),
			LocalDateTime.of(2025, 9, 15, 9, 3, 0),
			List.of(qsRequest)
		);
	}

	private boolean submitAsync(CyclicBarrier startGate, Long surveyId, Member member, SurveySubmissionRequest request) throws Exception {
		try {
			startGate.await(3, TimeUnit.SECONDS);
			surveyService.submitSurvey(surveyId, member, request);
			return true;
		} catch (BusinessException e) {
			e.printStackTrace();

            assertThat(e.getErrorCode()).isIn(
                    SurveyErrorCode.SURVEY_NOT_ENOUGH_POINTS,
                    SurveyErrorCode.TOO_MANY_REQUESTS
            );
			return false;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private boolean getResult(Future<Boolean> future) throws Exception {
		return future.get(5, TimeUnit.SECONDS);
	}

    @Test
    @Transactional(propagation = Propagation.NEVER)
    void concurrencyControlTest_TooManyRequests() throws Exception {
        Long surveyId = survey2.getId();

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier startGate = new CyclicBarrier(threadCount);

        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 1; i <= threadCount; i++) {
            Member testMember = createTestMember("member_" + i);
            SurveySubmissionRequest request = buildSurveySubmissionRequest();

            futures.add(pool.submit(() -> submitAsync_TooManyRequest(startGate, surveyId, testMember, request)));
        }

        int successCount = 0;
        int tooManyRequestCount = 0;

        for (Future<Boolean> future : futures) {
            Boolean result = future.get(10, TimeUnit.SECONDS);

            if (result) {
                successCount++;
            } else {
                tooManyRequestCount++;
            }
        }

        pool.shutdown();

        System.out.println("성공한 요청 수: " + successCount);
        System.out.println("Too Many Requests 요청 수: " + tooManyRequestCount);

        assertThat(tooManyRequestCount).isGreaterThan(0);
    }

    private Member createTestMember(String username) {
        Member member = Member.builder()
                .authProvider(AuthProvider.KAKAO)
                .profileImageUrl("")
                .role(Role.MEMBER)
                .memberId(username)
                .email(username + "@test.com")
                .build();
        return memberRepository.save(member);
    }

    private Boolean submitAsync_TooManyRequest(CyclicBarrier startGate, Long surveyId, Member member, SurveySubmissionRequest request) throws Exception {
        try {
            startGate.await(3, TimeUnit.SECONDS);
            surveyService.submitSurvey(surveyId, member, request);
            return true;
        } catch (BusinessException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

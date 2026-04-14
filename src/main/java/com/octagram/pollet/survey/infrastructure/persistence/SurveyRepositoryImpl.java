package com.octagram.pollet.survey.infrastructure.persistence;

import static com.octagram.pollet.gifticon.domain.model.QGifticonProduct.*;
import static com.octagram.pollet.survey.domain.model.QSurvey.*;
import static com.octagram.pollet.survey.domain.model.QSurveySubmission.*;
import static com.octagram.pollet.survey.domain.model.QSurveyTag.*;
import static com.octagram.pollet.survey.domain.model.QTag.*;

import com.octagram.pollet.survey.domain.model.QSurvey;
import com.octagram.pollet.survey.domain.model.Survey;
import com.octagram.pollet.survey.domain.model.type.FilterSortType;
import com.octagram.pollet.survey.domain.repository.SurveyRepositoryCustom;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.octagram.pollet.survey.presentation.dto.request.SurveyFilterRequest;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SurveyRepositoryImpl implements SurveyRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public long getActiveCount(LocalDateTime now) {
		QSurvey survey = QSurvey.survey;
		Long count = queryFactory
			.select(survey.count())
			.from(survey)
			.where(survey.startDateTime.loe(now).and(survey.endDateTime.goe(now)))
			.fetchOne();
		return Objects.requireNonNullElse(count, 0L);
	}

	@Override
	public Optional<Survey> findByIdQueryDsl(Long id) {
		return Optional.ofNullable(
			queryFactory
				.selectFrom(survey)
				.leftJoin(survey.surveyTags, surveyTag).fetchJoin()
				.leftJoin(surveyTag.tag, tag).fetchJoin()
				.leftJoin(survey.gifticonProduct, gifticonProduct).fetchJoin()
				.where(survey.id.eq(id))
				.fetchOne()
		);
	}

	@Override
	public List<Survey> findByFilter(List<String> allTags, SurveyFilterRequest filter, Pageable pageable) {
		OrderSpecifier<?> orderSpecifier = getOrderSpecifier(filter.sortType());

		return queryFactory
			.selectFrom(survey)
			.leftJoin(survey.surveyTags, surveyTag).fetchJoin()
			.leftJoin(surveyTag.tag, tag).fetchJoin()
			.where(
				keywordCondition(filter.keyword()),
				tagsCondition(allTags),
				pointCondition(filter.minPoint(), filter.maxPoint()),
				estimatedTimeCondition(filter.estimatedTime()),
				gifticonCondition(filter.gifticon())
			)
			.orderBy(orderSpecifier)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
	}

	@Override
	public List<Survey> findByMemberId(Long memberId, Pageable pageable) {
		return queryFactory
			.selectFrom(survey)
			.where(survey.member.id.eq(memberId))
			.orderBy(survey.createdAt.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
	}

	@Override
	public List<Survey> findParticipatedByMemberId(Long memberId, Pageable pageable) {
		return queryFactory
			.select(surveySubmission.survey).distinct()
			.from(surveySubmission)
			.where(surveySubmission.member.id.eq(memberId))
			.orderBy(survey.createdAt.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
	}

	private BooleanExpression tagsCondition(List<String> tags) {
		if (tags == null || tags.isEmpty()) {
			return null;
		}

		return survey.id.in(
			JPAExpressions
				.select(surveyTag.survey.id)
				.from(surveyTag)
				.join(surveyTag.tag, tag)
				.where(tag.name.in(tags))
				// 설정한 태그가 모두 포함되어야 결과로 취급
				// .groupBy(surveyTag.survey.id)
				// .having(tag.count().eq((long) tags.size()))
		);
	}

	private BooleanExpression pointCondition(Long minPoint, Long maxPoint) {
		if (minPoint != null && maxPoint != null) {
			return survey.rewardPointPerMinute.between(minPoint, maxPoint);
		} else if (minPoint != null) {
			return survey.rewardPointPerMinute.goe(minPoint);
		} else if (maxPoint != null) {
			return survey.rewardPointPerMinute.loe(maxPoint);
		} else {
			return null;
		}
	}

	private BooleanExpression estimatedTimeCondition(Long estimatedTime) {
		if (estimatedTime != null) {
			return survey.estimatedTime.loe(estimatedTime);
		} else {
			return null;
		}
	}

	private BooleanExpression gifticonCondition(Boolean includeGifticon) {
		if (includeGifticon != null && includeGifticon) {
			return null;
		} else {
			return survey.gifticonProduct.isNull();
		}
	}

	private OrderSpecifier<?> getOrderSpecifier(FilterSortType sortType) {
		if (sortType == null) {
			return survey.createdAt.desc();
		}

		return switch (sortType) {
			case HIGHEST_POINTS -> survey.rewardPointPerMinute.desc();
			case SHORTEST_TIME -> survey.estimatedTime.asc();
			case LONGEST_TIME -> survey.estimatedTime.desc();
			case FEWEST_SUBMISSIONS -> survey.currentSubmissionCount.asc();
		};
	}

	private BooleanExpression keywordCondition(String keyword) {
		if (keyword != null) {
			return survey.title.contains(keyword);
		} else {
			return null;
		}
	}

	@Override
	public Optional<Survey> findByIdByOptimistic(Long id) {
		QSurvey survey = QSurvey.survey;

		return Optional.ofNullable(
			queryFactory.selectFrom(survey)
				.where(survey.id.eq(id))
				.setLockMode(LockModeType.OPTIMISTIC)
				.fetchOne()
		);
	}
}

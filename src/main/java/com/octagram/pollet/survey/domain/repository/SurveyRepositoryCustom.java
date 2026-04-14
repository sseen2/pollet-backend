package com.octagram.pollet.survey.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import com.octagram.pollet.survey.domain.model.Survey;
import com.octagram.pollet.survey.presentation.dto.request.SurveyFilterRequest;

public interface SurveyRepositoryCustom {

	long getActiveCount(LocalDateTime now);
	Optional<Survey> findByIdQueryDsl(Long id);
	List<Survey> findByFilter(List<String> allTags, SurveyFilterRequest filter, Pageable pageable);
	List<Survey> findByMemberId(Long memberId, Pageable pageable);
	List<Survey> findParticipatedByMemberId(Long memberId, Pageable pageable);
	Optional<Survey> findByIdByOptimistic(Long id);
}

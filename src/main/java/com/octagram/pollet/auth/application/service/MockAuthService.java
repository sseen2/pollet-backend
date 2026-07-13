package com.octagram.pollet.auth.application.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.octagram.pollet.auth.presentation.dto.response.TestJwtGetResponse;
import com.octagram.pollet.global.exception.BusinessException;
import com.octagram.pollet.global.jwt.repository.TokenRedisRepository;
import com.octagram.pollet.global.jwt.service.JwtService;
import com.octagram.pollet.member.domain.model.Member;
import com.octagram.pollet.member.domain.repository.MemberRepository;
import com.octagram.pollet.member.domain.status.MemberErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class MockAuthService {

	private final JwtService jwtService;
	private final TokenRedisRepository tokenRepository;
	private final MemberRepository memberRepository;

	public TestJwtGetResponse getTestJwt(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		String accessToken = jwtService.createAccessToken(member.getMemberId(), member.getEmail(), member.getRole().toString());
		String refreshToken = jwtService.createRefreshToken(member.getMemberId());
		tokenRepository.save(accessToken, refreshToken);

		return new TestJwtGetResponse("Bearer " + accessToken, "RefreshToken=" + refreshToken);
	}
}

package com.octagram.pollet.auth.application.service;

import org.springframework.stereotype.Service;

import com.octagram.pollet.auth.presentation.dto.response.AuthReissueResponse;
import com.octagram.pollet.global.exception.BusinessException;
import com.octagram.pollet.global.jwt.repository.TokenRedisRepository;
import com.octagram.pollet.global.jwt.service.JwtService;
import com.octagram.pollet.global.jwt.status.JwtErrorCode;
import com.octagram.pollet.member.domain.model.Member;
import com.octagram.pollet.member.domain.repository.MemberRepository;
import com.octagram.pollet.member.domain.status.MemberErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final JwtService jwtService;
	private final TokenRedisRepository tokenRepository;
	private final MemberRepository memberRepository;

	public AuthReissueResponse reissueAccessToken(String refreshToken) {
		String memberId = validateRefreshToken(refreshToken);

		Member member = memberRepository.findByMemberId(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		String newAccessToken = jwtService.createAccessToken(memberId, member.getEmail(), member.getRole().toString());

		return new AuthReissueResponse(newAccessToken);
	}

	private String validateRefreshToken(String refreshToken) {
		if (refreshToken == null || refreshToken.isEmpty()) {
			throw new BusinessException(JwtErrorCode.REFRESH_TOKEN_INVALID);
		}

		if (!jwtService.validateRefreshToken(refreshToken)) {
			throw new BusinessException(JwtErrorCode.REFRESH_TOKEN_INVALID);
		}

		String memberId = jwtService.getMemberIdFromToken(refreshToken)
			.orElseThrow(() -> new BusinessException(JwtErrorCode.REFRESH_TOKEN_INVALID));

		if (!tokenRepository.existsToken(memberId, refreshToken)) {
			throw new BusinessException(JwtErrorCode.REFRESH_TOKEN_INVALID);
		}

		return memberId;
	}

	public void logout(HttpServletRequest request, HttpServletResponse response, String memberId) {
		String accessToken = jwtService.extractAccessToken(request)
			.orElseThrow(() -> new BusinessException(JwtErrorCode.TOKEN_INVALID));

		jwtService.validateToken(accessToken);

		String refreshToken = jwtService.extractRefreshToken(request)
			.orElseThrow(() -> new BusinessException(JwtErrorCode.REFRESH_TOKEN_INVALID));

		tokenRepository.delete(memberId, refreshToken);

		jwtService.setExpiredRefreshToken(response);
	}
}

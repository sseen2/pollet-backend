package com.octagram.pollet.auth.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.octagram.pollet.auth.application.service.AuthService;
import com.octagram.pollet.auth.domain.status.AuthSuccessCode;
import com.octagram.pollet.auth.presentation.dto.response.AuthReissueResponse;
import com.octagram.pollet.global.presentation.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@GetMapping("/reissue")
	public ApiResponse<AuthReissueResponse> reissueAccessToken(@CookieValue(value = "RefreshToken", required = false) String refreshToken) {
		AuthReissueResponse authReissueResponse = authService.reissueAccessToken(refreshToken);
		return ApiResponse.success(authReissueResponse);
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response, @AuthenticationPrincipal String memberId) {
		authService.logout(request, response, memberId);
		return ApiResponse.success(AuthSuccessCode.LOGOUT_SUCCESS);
	}
}

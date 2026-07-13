package com.octagram.pollet.auth.presentation.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.octagram.pollet.auth.application.service.MockAuthService;
import com.octagram.pollet.auth.presentation.dto.response.TestJwtGetResponse;
import com.octagram.pollet.global.presentation.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class MockAuthController {

	private final MockAuthService mockAuthService;

	@GetMapping("/test-jwt/{memberId}")
	public ApiResponse<TestJwtGetResponse> getTestJwt(@PathVariable Long memberId) {
		TestJwtGetResponse response = mockAuthService.getTestJwt(memberId);
		return ApiResponse.success(response);
	}
}

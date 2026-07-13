package com.octagram.pollet.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile({"dev", "test"})
public class DevSecurityConfig {

	@Bean
	@Order(0)
	public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
		http
			.securityMatcher("/api/v1/auth/test-jwt/**")
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

		return http.build();
	}
}

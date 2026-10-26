package com.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class CorsConfig {
	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origins}") List<String> allowedOrigins,
			@Value("${app.cors.allowed-methods}") List<String> allowedMethods,
			@Value("${app.cors.max-age}") long maxAge) {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.setAllowedOrigins(allowedOrigins);
		corsConfiguration.setAllowedMethods(allowedMethods);
		corsConfiguration.setAllowedHeaders(List.of("*"));
		corsConfiguration.setMaxAge(maxAge);
		UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
		src.registerCorsConfiguration("/**", corsConfiguration);
		return src;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/error").permitAll()
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers("/api/units/**").permitAll()
						.requestMatchers("/api/items/**").permitAll()
						.requestMatchers("/api/suppliers/**").permitAll()
						.requestMatchers("/api/purchase-vouchers/**").permitAll()
						.requestMatchers("/api/variants/{variantId}/suppliers").permitAll())	// in-use
				.build();
	}
}

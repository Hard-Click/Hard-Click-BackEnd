package com.wanted.backend.global.config;

import com.wanted.backend.global.security.JwtAccessDeniedHandler;
import com.wanted.backend.global.security.JwtAuthenticationEntryPoint;
import com.wanted.backend.global.security.CustomUserDetailsService;
import com.wanted.backend.global.security.filter.JwtAuthenticationFilter;
import com.wanted.backend.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/signup",
                                "/api/auth/logout",
                                "/api/auth/refresh",
                                "/api/auth/check-username",
                                "/api/auth/check-email",
                                "/api/auth/email/send",
                                "/api/auth/email/verify",
                                "/api/auth/account-locks/email",
                                "/api/auth/account-locks/verify",
                                "/api/auth/account-locks/password",
                                "/api/auth/password-reset/**",
                                "/api/auth/account-locks/verify",
                                "/api/auth/account-locks/password",
                                "/api/courses/*/reviews",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/ws-chat/**"
                        ).permitAll()
                        // Prometheus는 같은 Docker 네트워크(브리지 대역) 또는 localhost에서만 스크랩하므로
                        // 외부 인터넷으로 메트릭이 그대로 노출되지 않도록 발신 IP를 제한한다.
                        .requestMatchers("/actuator/prometheus").access(
                                new WebExpressionAuthorizationManager(
                                        "hasIpAddress('127.0.0.1') or hasIpAddress('::1') or hasIpAddress('172.16.0.0/12')"))
                        .requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/*","/api/courses/*/reviews").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/courses").hasRole("INSTRUCTOR")
                        .requestMatchers(HttpMethod.POST, "/api/courses/lessons/*/video").hasRole("INSTRUCTOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/courses/*", "/api/courses/*/status").hasRole("INSTRUCTOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/*").hasRole("INSTRUCTOR")
                        .requestMatchers("/api/instructor/**").hasRole("INSTRUCTOR")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "https://hard-click-front-end.vercel.app"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "PUT", "POST", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(
                Arrays.asList(
                        "Access-Control-Allow-Origin",
                        "Content-Type",
                        "Access-Control-Allow-Headers",
                        "Authorization",
                        "X-Requested-With"
                )
        );
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
        ROLE_ADMIN > ROLE_INSTRUCTOR
        ROLE_INSTRUCTOR > ROLE_STUDENT
        """);
    }
}

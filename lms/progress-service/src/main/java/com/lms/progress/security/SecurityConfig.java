package com.lms.progress.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/test/**").permitAll()                                   // TEST ONLY
                // instructors create quizzes and see the answers
                .requestMatchers(HttpMethod.POST, "/api/quizzes/courses/*").hasRole("INSTRUCTOR")
                .requestMatchers(HttpMethod.GET, "/api/quizzes/courses/*/manage").hasRole("INSTRUCTOR")
                // everything else about quizzes and progress is for students
                .requestMatchers("/api/quizzes/**").hasRole("STUDENT")
                .requestMatchers("/api/progress/**").hasRole("STUDENT")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

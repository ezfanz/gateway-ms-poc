//package com.in28minutes.microservices.currencyconversionservice.Config;
//
//import com.in28minutes.microservices.currencyconversionservice.Security.JwtRequestFilter;
//import jakarta.servlet.Filter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http.csrf().disable()
//                .authorizeRequests(authz -> authz
//                        .requestMatchers("/authenticate").permitAll() // Allow access to authentication endpoint
//                        .requestMatchers("/currency-conversion/**").authenticated() // Require authentication for all currency conversion endpoints
//                        .anyRequest().authenticated() // Require authentication for any other requests
//                )
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                );
//
//        // Add JWT filter
//        http.addFilterBefore(jwtRequestFilter(), UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//
//    @Bean
//    public Filter jwtRequestFilter() {
//        return new JwtRequestFilter();
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//}

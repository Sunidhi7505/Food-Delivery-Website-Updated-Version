package com.foody.config;

import com.foody.security.JwtAuthFilter;
import com.foody.security.RestErrorHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestErrorHandlers errorHandlers;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RestErrorHandlers errorHandlers) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.errorHandlers = errorHandlers;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(errorHandlers)
                .accessDeniedHandler(errorHandlers))

            .authorizeHttpRequests(auth -> auth

                // Swagger FULL ACCESS
                .requestMatchers(
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/swagger-ui/index.html",
                        "/webjars/**",
                        "/v3/api-docs.yaml", 
                        "/swagger-resources",      
                        "/swagger-resources/**"
                ).permitAll()

                // Public APIs
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/restaurant", "/api/restaurant/status").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pricing/config").permitAll()

                // Customer APIs
                .requestMatchers("/api/cart/**").hasAuthority("ROLE_CUSTOMER")
                .requestMatchers("/api/users/**").hasAuthority("ROLE_CUSTOMER")
                .requestMatchers("/api/coupons/**").hasAuthority("ROLE_CUSTOMER")
                .requestMatchers("/api/payments/**").hasAuthority("ROLE_CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/orders/place").hasAuthority("ROLE_CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/api/orders").hasAuthority("ROLE_CUSTOMER")
                .requestMatchers(HttpMethod.PUT, "/api/orders/*/cancel").hasAuthority("ROLE_CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/orders/*/reorder").hasAuthority("ROLE_CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/api/orders/*/invoice").hasAuthority("ROLE_CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/reviews/**").hasAuthority("ROLE_CUSTOMER")

                // Admin APIs
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/orders/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/restaurant").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAuthority("ROLE_ADMIN")

                // Secure everything else
                .anyRequest().authenticated()
            )

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
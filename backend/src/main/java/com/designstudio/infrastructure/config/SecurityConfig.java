package com.designstudio.infrastructure.config;

import com.designstudio.infrastructure.filter.JwtAuthenticationFilter;
import com.designstudio.infrastructure.security.CustomUserDetailsService;
import com.designstudio.infrastructure.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtTokenProvider jwtTokenProvider) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/logout",
                                "/api/auth/check",
                                "/api/products/**",
                                "/uploads/**"
                        ).permitAll()

                        // === КЛИЕНТСКИЕ endpoints (ДО admin!) ===
                        .requestMatchers("/api/marketing/campaigns/my/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/marketing/campaigns/apply/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/marketing/campaigns/calculate-discount").hasAnyRole("USER", "ADMIN")

                        // === ДОБАВИТЬ ЭТИ СТРОКИ ДЛЯ СООБЩЕНИЙ ===
                        .requestMatchers("/api/marketing/messages/my").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/marketing/messages/unread-count").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/marketing/messages/{id}/read").hasAnyRole("USER", "ADMIN")

                        // === ADMIN-only маркетинг ===
                        .requestMatchers("/api/marketing/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/marketing/stats/**").hasRole("ADMIN")
                        .requestMatchers("/api/marketing/templates/**").hasRole("ADMIN")
                        .requestMatchers("/api/marketing/automations/**").hasRole("ADMIN")
                        .requestMatchers("/api/marketing/**").hasRole("ADMIN")

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/crm/**").hasRole("ADMIN")
                        .requestMatchers("/api/analytics/admin", "/api/analytics/visits-by-day", "/api/analytics/sales-by-day")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/analytics/user", "/api/analytics/visit/update").authenticated()
                        .requestMatchers("/api/cart/**", "/api/checkout", "/api/order", "/api/orders",
                                "/api/design-orders", "/api/user/**").authenticated()

                        .anyRequest().permitAll()
                )
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .logout(logout -> logout.disable())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(false);
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
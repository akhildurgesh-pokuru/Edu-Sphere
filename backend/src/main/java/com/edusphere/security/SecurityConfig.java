package com.edusphere.security;

import com.edusphere.common.ApiError;
import com.edusphere.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final AppProperties props;
    private final boolean cookieSecure;

    public SecurityConfig(AppProperties props, @Value("${server.servlet.session.cookie.secure:false}") boolean cookieSecure) {
        this.props = props;
        this.cookieSecure = cookieSecure;
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }

    @Bean SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }

    private void writeJson(jakarta.servlet.http.HttpServletResponse res, int status, String error, String msg, String path) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.writeValue(res.getOutputStream(), ApiError.of(status, error, msg, path, null));
    }

    @Bean AuthenticationEntryPoint authenticationEntryPoint() {
        return (req, res, ex) -> writeJson(res, 401, "Unauthorized", "Please sign in to continue", req.getRequestURI());
    }

    @Bean AccessDeniedHandler accessDeniedHandler() {
        return (req, res, ex) -> writeJson(res, 403, "Forbidden", "You do not have permission to do that", req.getRequestURI());
    }

    @Bean CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(props.frontendUrl()));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Accept", "Range"));
        c.setExposedHeaders(List.of("Content-Range", "Accept-Ranges", "Content-Disposition", "Content-Length"));
        c.setAllowCredentials(true);
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AuthenticationEntryPoint entry, AccessDeniedHandler denied,
                                    SecurityContextRepository ctxRepo, GoogleLoginSuccessHandler googleSuccess,
                                    ObjectProvider<ClientRegistrationRepository> clients) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookieCustomizer(b -> b.sameSite("Lax").secure(cookieSecure));
        http
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .csrf(c -> c.csrfTokenRepository(csrfRepo).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .securityContext(c -> c.securityContextRepository(ctxRepo))
            .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()).maximumSessions(10))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .headers(h -> h
                .frameOptions(f -> f.deny())
                .contentTypeOptions(c -> {})
                .referrerPolicy(r -> r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .httpStrictTransportSecurity(hsts -> { if (cookieSecure) hsts.includeSubDomains(true); else hsts.disable(); }))
            .exceptionHandling(e -> e.authenticationEntryPoint(entry).accessDeniedHandler(denied))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/csrf", "/api/auth/providers", "/api/auth/register", "/api/auth/register-teacher",
                        "/api/auth/login", "/api/auth/logout", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/teacher/**").hasRole("TEACHER")
                .requestMatchers("/api/student/**").hasRole("STUDENT")
                .anyRequest().authenticated());
        if (props.googleEnabled()) {
            // prompt=select_account makes Google always show the account chooser, even when one account is signed in
            DefaultOAuth2AuthorizationRequestResolver resolver =
                    new DefaultOAuth2AuthorizationRequestResolver(clients.getObject(), "/oauth2/authorization");
            resolver.setAuthorizationRequestCustomizer(c -> c.additionalParameters(p -> p.put("prompt", "select_account")));
            http.oauth2Login(o -> o.successHandler(googleSuccess)
                    .authorizationEndpoint(a -> a.authorizationRequestResolver(resolver))
                    .failureHandler((req, res, ex) -> res.sendRedirect(props.frontendUrl() + "/login?error=google")));
        }
        return http.build();
    }
}

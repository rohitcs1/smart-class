package com.elneb.config;

import com.elneb.auth.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.*;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain security(HttpSecurity http, JwtService jwtService) throws Exception {
        http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers("/api/health", "/api/auth/login", "/api/auth/register", "/api/subscriptions/plans",
                                "/api/subscriptions/quote", "/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll().anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new JwtFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean CorsConfigurationSource corsSource() {
        CorsConfiguration c = new CorsConfiguration();
        String allowedOrigins = System.getenv().getOrDefault("CORS_ALLOWED_ORIGINS", "http://localhost:5173");
        c.setAllowedOrigins(List.of(allowedOrigins.split(",")).stream().map(String::trim).toList());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }

    static class JwtFilter extends OncePerRequestFilter {
        private final JwtService jwt;
        JwtFilter(JwtService jwt) { this.jwt = jwt; }
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    Claims claims = jwt.parse(header.substring(7));
                    var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + claims.get("role", String.class))));
                    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (RuntimeException ignored) {
                    // Invalid tokens are treated as unauthenticated by the security chain.
                }
            }
            chain.doFilter(request, response);
        }
    }
}

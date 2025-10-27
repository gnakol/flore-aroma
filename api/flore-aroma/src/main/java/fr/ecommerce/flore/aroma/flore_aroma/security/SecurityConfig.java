package fr.ecommerce.flore.aroma.flore_aroma.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
// SecurityConfig.java
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/account/register",
                                "/account/register-africa",   // <- AJOUTE
                                "/account/login",
                                "/account/refresh",
                                "/account/sync-email"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/account/verify-phone"       // <- AJOUTE
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String path = request.getRequestURI();
                            if (
                                    "/api/admin/confirm-invitation-admin".equals(path)
                                            || "/api/admin/invite".equals(path)
                                            || "/api/company/confirm-invitation-recruiter".equals(path)

                            ) {
                                response.setStatus(HttpServletResponse.SC_OK); // ou NO_CONTENT si tu veux
                            } else {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
                            }
                        })
                );

        return http.build();
    }


    private JwtAuthenticationConverter jwtAuthenticationConverter() {

        Converter<Jwt, Collection<GrantedAuthority>> converter = jwt -> {

            // Extrait les rôles depuis realm_access.roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            List<String> roles = Collections.emptyList();
            if (realmAccess != null && realmAccess.get("roles") instanceof List) {
                roles = (List<String>) realmAccess.get("roles");
            }

            // Extrait les rôles depuis resource_access.<client_name>.roles
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("spring-boot-app");
                if (clientAccess != null && clientAccess.get("roles") instanceof List) {
                    roles.addAll((List<String>) clientAccess.get("roles"));
                }
            }

            // Convertit chaque rôle en une autorité Spring Security avec le préfixe ROLE_
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        };

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtAuthenticationConverter;
    }

}


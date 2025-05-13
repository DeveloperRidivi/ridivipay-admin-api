/******************************************************************************
 * CodeStrux Tech SRL - Copyright (c) 2024.                                   *
 * Alvaro Araya O git:alvaro-araya email:aao@codestrux.tech                   *
 ******************************************************************************/
package com.ridivi.ridivipay.admin.config;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.RequiredArgsConstructor;
import org.keycloak.adapters.authorization.integration.jakarta.ServletPolicyEnforcerFilter;
import org.keycloak.adapters.authorization.spi.ConfigurationResolver;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.util.JsonSerialization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
  @Value("${RIDIVI.CORS.ORIGINS}")
  private String corsOrigins;
  @Value("${RIDIVI.KEYCLOAK.ENFORCER}")
  private String keycloakEnforcer;

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter, @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) throws Exception {
    http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
    // HABILITA CORS
    http.cors(cors -> cors.configurationSource(corsConfigurationSource(corsOrigins.split(","))));
    // SESIONES STATELESS
    http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    // DESABILITA CSRF (SESIONES STATELESS)
    http.csrf(AbstractHttpConfigurer::disable);
    // CAMBIO DE CÓDIGO DE ERROR
    http.exceptionHandling(eh -> eh.authenticationEntryPoint((request, response, authException) -> {
      response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "OAuth realm=\"%s\"".formatted(issuerUri));
      response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase());
    }));
    // HABILITA EL POLICY ENFORCER
    http.addFilterAfter(createPolicyEnforcerFilter(), BearerTokenAuthenticationFilter.class);
    http.authorizeHttpRequests(accessManagement ->
                                 accessManagement
                                   .requestMatchers("/api/version", "/error").permitAll()
                                   .requestMatchers("/actuator/health/readiness", "/actuator/health/liveness", "/v3/api-docs/**").permitAll()
                                   .anyRequest().authenticated());
    return http.build();
  }

  private UrlBasedCorsConfigurationSource corsConfigurationSource(String... origins) {
    final var configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(origins));
    configuration.setAllowedMethods(List.of("*"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("*"));
    final var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private ServletPolicyEnforcerFilter createPolicyEnforcerFilter() {
    return new ServletPolicyEnforcerFilter(new ConfigurationResolver() {
      @Override
      public PolicyEnforcerConfig resolve(HttpRequest request) {
        try {
          return JsonSerialization.readValue(getClass().getResourceAsStream(keycloakEnforcer), PolicyEnforcerConfig.class);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  @RequiredArgsConstructor
  static class JwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<? extends GrantedAuthority>> {
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Collection<? extends GrantedAuthority> convert(Jwt jwt) {
      return Stream.of("$.realm_access.roles", "$.resource_access.*.roles").flatMap(claimPaths -> {
        Object claim;
        try {
          claim = JsonPath.read(jwt.getClaims(), claimPaths);
        } catch (PathNotFoundException e) {
          claim = null;
        }
        if (claim == null) {
          return Stream.empty();
        }
        if (claim instanceof String claimStr) {
          return Stream.of(claimStr.split(","));
        }
        if (claim instanceof String[] claimArr) {
          return Stream.of(claimArr);
        }
        if (Collection.class.isAssignableFrom(claim.getClass())) {
          final var iter = ((Collection) claim).iterator();
          if (!iter.hasNext()) {
            return Stream.empty();
          }
          final var firstItem = iter.next();
          if (firstItem instanceof String) {
            return (Stream<String>) ((Collection) claim).stream();
          }
          if (Collection.class.isAssignableFrom(firstItem.getClass())) {
            return (Stream<String>) ((Collection) claim).stream().flatMap(colItem -> ((Collection) colItem).stream()).map(String.class::cast);
          }
        }
        return Stream.empty();
      }).map(role -> "ROLE_" + role).map(SimpleGrantedAuthority::new).map(GrantedAuthority.class::cast).toList();
    }
  }

  @Component
  @RequiredArgsConstructor
  static class SpringAddonsJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {
    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
      final var authorities = new JwtGrantedAuthoritiesConverter().convert(jwt);
      final String username = JsonPath.read(jwt.getClaims(), "preferred_username");
      return new JwtAuthenticationToken(jwt, authorities, username);
    }
  }
}

package org.dzoka.foundation.security;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean JwtDecoder jwtDecoder(@Value("${dzoka.security.issuer}") String issuer,
            @Value("${dzoka.security.jwk-set-uri}") String jwks,
            @Value("${dzoka.security.audience}") String audience,
            @Value("${dzoka.security.allow-insecure-loopback:false}") boolean testLoopback) {
        requireEndpoint(issuer, testLoopback); requireEndpoint(jwks, testLoopback);
        if (audience.isBlank()) throw new IllegalArgumentException("Audience is required");
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).jwsAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> claims = jwt -> {
            if (jwt.getExpiresAt() == null || jwt.getSubject() == null || jwt.getSubject().isBlank()
                    || jwt.getSubject().length() > 512 || jwt.getSubject().indexOf('\0') >= 0
                    || jwt.getAudience() == null || !jwt.getAudience().contains(audience))
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            return OAuth2TokenValidatorResult.success();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), claims));
        return decoder;
    }
    static void requireEndpoint(String value, boolean testLoopback) {
        URI uri = URI.create(value);
        boolean secure = "https".equals(uri.getScheme()) && uri.getHost() != null;
        boolean local = testLoopback && "http".equals(uri.getScheme())
                && ("127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost()));
        if ((!secure && !local) || uri.getUserInfo() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("A secure identity endpoint is required");
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> {})).build();
    }
}

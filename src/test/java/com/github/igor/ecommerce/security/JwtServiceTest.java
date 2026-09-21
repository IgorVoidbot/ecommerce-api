package com.github.igor.ecommerce.security;

import com.github.igor.ecommerce.entity.User;
import com.github.igor.ecommerce.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-with-more-than-32-bytes!!";
    private static final long EXPIRATION_MINUTES = 120;

    private final JwtService jwtService = new JwtService(SECRET, EXPIRATION_MINUTES);

    @Test
    void generateTokenShouldSetEmailAsSubjectAndRoleAsClaim() {
        String token = jwtService.generateToken(buildUser());

        Claims claims = parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("igor@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void generatedTokenShouldExpireInTwoHours() {
        String token = jwtService.generateToken(buildUser());

        Claims claims = parseClaims(token);
        long lifetimeSeconds = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;

        assertThat(lifetimeSeconds).isEqualTo(EXPIRATION_MINUTES * 60);
    }

    @Test
    void getExpirationSecondsShouldReturnTwoHoursInSeconds() {
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(7200L);
    }

    @Test
    void isValidTokenShouldReturnTrueForValidToken() {
        assertThat(jwtService.isValidToken(jwtService.generateToken(buildUser()))).isTrue();
    }

    @Test
    void isValidTokenShouldReturnFalseWhenSignatureIsTampered() {
        String token = jwtService.generateToken(buildUser());
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThat(jwtService.isValidToken(tampered)).isFalse();
    }

    @Test
    void isValidTokenShouldReturnFalseWhenSignedWithDifferentSecret() {
        JwtService otherService = new JwtService("another-secret-key-with-more-than-32-bytes!", EXPIRATION_MINUTES);

        assertThat(jwtService.isValidToken(otherService.generateToken(buildUser()))).isFalse();
    }

    @Test
    void isValidTokenShouldReturnFalseWhenTokenIsExpired() {
        JwtService expiredService = new JwtService(SECRET, -1);

        assertThat(jwtService.isValidToken(expiredService.generateToken(buildUser()))).isFalse();
    }

    @Test
    void isValidTokenShouldReturnFalseForMalformedToken() {
        assertThat(jwtService.isValidToken("not-a-jwt")).isFalse();
    }

    @Test
    void extractEmailShouldReturnSubject() {
        String token = jwtService.generateToken(buildUser());

        assertThat(jwtService.extractEmail(token)).isEqualTo("igor@example.com");
    }

    @Test
    void extractEmailShouldThrowForInvalidToken() {
        assertThatThrownBy(() -> jwtService.extractEmail("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    private User buildUser() {
        return new User("Igor", "igor@example.com", "encoded-password", Role.ADMIN);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

package org.opencivic.signalos.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 60 * 8; // 8 Hours
    private static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 7; // 7 Days
    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, ACCESS_TOKEN_EXPIRATION, TOKEN_TYPE_ACCESS);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, REFRESH_TOKEN_EXPIRATION, TOKEN_TYPE_REFRESH);
    }

    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration, String tokenType) {
        return Jwts.builder()
                .claims(extraClaims)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    // Same key and subject for both tokens, so the type claim is what stops a 7-day refresh
    // token from being replayed as an 8-hour access token.
    public boolean isAccessToken(String token) {
        return TOKEN_TYPE_ACCESS.equals(extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class)));
    }

    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class)));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

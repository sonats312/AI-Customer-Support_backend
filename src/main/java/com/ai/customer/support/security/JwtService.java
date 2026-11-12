package com.ai.customer.support.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final String secretKey;
	private final long expiration;

	public JwtService(@Value("${security.jwt.secret}") String secretKey,
			@Value("${security.jwt.expiration}") long expiration) {
		this.secretKey = secretKey;
		this.expiration = expiration;
	}

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(UserDetails userDetails) {

		Map<String, Object> claims = new HashMap<>();

		if (userDetails instanceof CustomUserDetails customUserDetails) {
			claims.put("userId", customUserDetails.getUserId());
			claims.put("role", customUserDetails.getRole());
		}

		Date now = new Date();

		Date expiryDate = new Date(now.getTime() + expiration);

		return Jwts.builder().claims(claims).subject(userDetails.getUsername()).issuedAt(now).expiration(expiryDate)
				.signWith(getSigningKey()).compact();
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public Long extractUserId(String token) {
		return extractClaim(token, claims -> claims.get("userId", Long.class));
	}

	public String extractRole(String token) {
		return extractClaim(token, claims -> claims.get("role", String.class));
	}

	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {

		String username = extractUsername(token);
		return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {

		Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
		return claimsResolver.apply(claims);
	}

}

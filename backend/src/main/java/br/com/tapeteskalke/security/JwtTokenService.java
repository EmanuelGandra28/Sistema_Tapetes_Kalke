package br.com.tapeteskalke.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service public class JwtTokenService {
 // Valores de exemplo publicados no repositório: aceitar qualquer um deles permitiria forjar tokens.
 private static final List<String> PLACEHOLDERS=List.of("change-this","replace-with","change_me","changeme");
 private final SecretKey key; private final long expirationSeconds;
 public JwtTokenService(@Value("${app.jwt.secret:}") String secret,@Value("${app.jwt.expiration-seconds}") long expirationSeconds) { validate(secret); this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expirationSeconds=expirationSeconds; }
 static void validate(String secret) { if(secret==null||secret.isBlank()) throw new IllegalStateException("JWT_SECRET não configurado. Defina um valor aleatório com ao menos 32 bytes no .env"); if(secret.getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalStateException("JWT_SECRET precisa ter ao menos 32 bytes"); var lower=secret.toLowerCase(); if(PLACEHOLDERS.stream().anyMatch(lower::contains)) throw new IllegalStateException("JWT_SECRET ainda é o valor de exemplo. Gere um segredo aleatório"); }
 public static final String SESSION_CLAIM="sv";
 public String issue(String userId,String email,String role,long sessionVersion) { Instant now=Instant.now(); return Jwts.builder().subject(userId).claim("email",email).claim("role",role).claim(SESSION_CLAIM,sessionVersion).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds))).signWith(key).compact(); }
 public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
 public long expirationSeconds() { return expirationSeconds; }
}

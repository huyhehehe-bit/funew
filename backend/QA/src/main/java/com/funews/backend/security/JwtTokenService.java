package com.funews.backend.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

/** A restricted HS256 JWT codec using the existing Jackson and JDK dependencies. */
@Service
public class JwtTokenService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String HEADER = ENCODER.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    private final ObjectMapper mapper;
    private final SecretKeySpec key;
    private final long ttlSeconds;
    private final String issuer;
    private final String audience;
    private final Clock clock;
    // Single-instance logout revocations, discarded after the token expires.
    private final Map<String, Instant> revoked = new HashMap<>();
    private final PriorityQueue<Revocation> expirations = new PriorityQueue<>(
            Comparator.comparing(Revocation::expiresAt));

    @Autowired
    public JwtTokenService(ObjectMapper mapper,
            @Value("${app.jwt.secret:${JWT_SECRET:}}") String secret,
            @Value("${app.jwt.ttl-seconds:900}") long ttlSeconds,
            @Value("${app.jwt.issuer:funews}") String issuer,
            @Value("${app.jwt.audience:funews-api}") String audience) {
        this(mapper, secret, ttlSeconds, issuer, audience, Clock.systemUTC());
    }

    public JwtTokenService(ObjectMapper mapper, String secret, long ttlSeconds,
            String issuer, String audience, Clock clock) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret == null ? "" : secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("JWT_SECRET must be a Base64-encoded random key of at least 32 bytes.");
        }
        if (decoded.length < 32 || ttlSeconds < 1 || ttlSeconds > 3600
                || issuer == null || issuer.isBlank() || audience == null || audience.isBlank()) {
            throw new IllegalArgumentException(
                    "Set JWT_SECRET to a Base64-encoded random key of at least 32 bytes; JWT TTL must be 1..3600 seconds.");
        }
        this.mapper = mapper.copy().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.key = new SecretKeySpec(decoded, "HmacSHA256");
        this.ttlSeconds = ttlSeconds;
        this.issuer = issuer;
        this.audience = audience;
        this.clock = clock;
    }

    public IssuedToken issue(AccountPrincipal principal) {
        if (principal.id() == null || principal.id() <= 0 || !principal.isEnabled()) {
            throw invalid();
        }
        long issuedAt = clock.instant().getEpochSecond();
        try {
            String payload = ENCODER.encodeToString(mapper.writeValueAsBytes(Map.of(
                    "iss", issuer, "aud", audience, "sub", principal.id().toString(),
                    "iat", issuedAt, "exp", Math.addExact(issuedAt, ttlSeconds),
                    "jti", UUID.randomUUID().toString(), "pwd", fingerprint(principal))));
            String unsigned = HEADER + "." + payload;
            return new IssuedToken(unsigned + "." + ENCODER.encodeToString(sign(unsigned)), ttlSeconds);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not create access token.", exception);
        }
    }

    public VerifiedToken verify(String token) {
        try {
            if (token == null || token.length() > 8192) {
                throw invalid();
            }
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) {
                throw invalid();
            }
            for (String part : parts) {
                if (part.isEmpty() || !part.matches("[A-Za-z0-9_-]+")) {
                    throw invalid();
                }
            }
            byte[] signature = decode(parts[2]);
            if (signature.length != 32 || !MessageDigest.isEqual(sign(parts[0] + "." + parts[1]), signature)) {
                throw invalid();
            }
            JsonNode header = mapper.readTree(utf8(decode(parts[0])));
            if (header == null || !header.isObject() || header.size() != 2
                    || !"HS256".equals(text(header, "alg")) || !"JWT".equals(text(header, "typ"))) {
                throw invalid();
            }
            JsonNode claims = mapper.readTree(utf8(decode(parts[1])));
            if (claims == null || !claims.isObject() || !issuer.equals(text(claims, "iss"))
                    || !audience.equals(text(claims, "aud"))) {
                throw invalid();
            }
            String subject = text(claims, "sub");
            if (!subject.matches("[1-9][0-9]{0,18}")) {
                throw invalid();
            }
            long accountId = Long.parseLong(subject);
            long issuedAt = number(claims, "iat");
            long expiresAt = number(claims, "exp");
            long now = clock.instant().getEpochSecond();
            if (issuedAt < 0 || issuedAt > now || expiresAt <= now || expiresAt <= issuedAt
                    || Math.subtractExact(expiresAt, issuedAt) > ttlSeconds) {
                throw invalid();
            }
            if (claims.has("nbf") && (number(claims, "nbf") > now || number(claims, "nbf") < 0)) {
                throw invalid();
            }
            String tokenId = text(claims, "jti");
            if (!UUID.fromString(tokenId).toString().equals(tokenId)) {
                throw invalid();
            }
            String credentialFingerprint = text(claims, "pwd");
            if (!credentialFingerprint.matches("[A-Za-z0-9_-]{43}") || decode(credentialFingerprint).length != 32
                    || isRevoked(tokenId)) {
                throw invalid();
            }
            return new VerifiedToken(accountId, tokenId, Instant.ofEpochSecond(issuedAt),
                    Instant.ofEpochSecond(expiresAt), credentialFingerprint);
        } catch (java.io.IOException | IllegalArgumentException | ArithmeticException exception) {
            throw invalid();
        }
    }

    public boolean matchesCredentials(VerifiedToken token, AccountPrincipal principal) {
        return principal.id() != null && token.accountId() == principal.id()
                && MessageDigest.isEqual(token.credentialFingerprint().getBytes(StandardCharsets.US_ASCII),
                        fingerprint(principal).getBytes(StandardCharsets.US_ASCII));
    }

    public synchronized void revoke(VerifiedToken token) {
        removeExpired();
        if (token.expiresAt().isAfter(clock.instant()) && revoked.putIfAbsent(token.tokenId(), token.expiresAt()) == null) {
            expirations.add(new Revocation(token.tokenId(), token.expiresAt()));
        }
    }

    private synchronized boolean isRevoked(String tokenId) {
        removeExpired();
        return revoked.containsKey(tokenId);
    }

    private void removeExpired() {
        Instant now = clock.instant();
        while (!expirations.isEmpty() && !expirations.peek().expiresAt().isAfter(now)) {
            Revocation expired = expirations.remove();
            revoked.remove(expired.tokenId(), expired.expiresAt());
        }
    }

    private String fingerprint(AccountPrincipal principal) {
        return ENCODER.encodeToString(sign("funews:credentials:" + principal.id() + ":" + principal.passwordHash()));
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is unavailable.", exception);
        }
    }

    private static byte[] decode(String value) {
        byte[] decoded = DECODER.decode(value);
        if (!ENCODER.encodeToString(decoded).equals(value)) {
            throw invalid();
        }
        return decoded;
    }

    private static String utf8(byte[] value) throws java.nio.charset.CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(value)).toString();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw invalid();
        }
        return value.textValue();
    }

    private static long number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw invalid();
        }
        return value.longValue();
    }

    private static BadCredentialsException invalid() {
        return new BadCredentialsException("Access token is invalid or expired.");
    }

    public record IssuedToken(String accessToken, long expiresIn) {}
    public record VerifiedToken(long accountId, String tokenId, Instant issuedAt,
                                Instant expiresAt, String credentialFingerprint) {}
    private record Revocation(String tokenId, Instant expiresAt) {}
}

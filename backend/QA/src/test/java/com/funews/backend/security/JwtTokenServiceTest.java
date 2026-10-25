package com.funews.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Security boundaries of the JWT codec, independent of the broken legacy application fixtures. */
class JwtTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-25T04:00:00Z");
    private static final long TTL = 900;
    private static final String ISSUER = "funews";
    private static final String AUDIENCE = "funews-api";
    private static final byte[] KEY = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final String SECRET = Base64.getEncoder().encodeToString(KEY);
    private static final Base64.Encoder URL = Base64.getUrlEncoder().withoutPadding();
    private static final AccountPrincipal ACCOUNT = new AccountPrincipal(
            42L, "editor@fu.edu.vn", "$2a$10$stored-password-hash", "EDITOR", true, false);

    private final ObjectMapper mapper = new ObjectMapper();
    private final JwtTokenService tokens = serviceAt(NOW);

    @Test
    void issuesSignedTokensWithoutExposingPasswordOrTrustingRoleClaims() throws Exception {
        var issued = tokens.issue(ACCOUNT);
        var verified = tokens.verify(issued.accessToken());
        ObjectNode header = part(issued.accessToken(), 0);
        ObjectNode claims = part(issued.accessToken(), 1);

        assertThat(issued.expiresIn()).isEqualTo(TTL);
        assertThat(verified.accountId()).isEqualTo(ACCOUNT.id());
        assertThat(verified.issuedAt()).isEqualTo(NOW);
        assertThat(verified.expiresAt()).isEqualTo(NOW.plusSeconds(TTL));
        assertThat(tokens.matchesCredentials(verified, ACCOUNT)).isTrue();
        assertThat(header.path("alg").asText()).isEqualTo("HS256");
        assertThat(header.path("typ").asText()).isEqualTo("JWT");
        assertThat(claims.path("iss").asText()).isEqualTo(ISSUER);
        assertThat(claims.path("aud").asText()).isEqualTo(AUDIENCE);
        assertThat(claims.toString()).doesNotContain(ACCOUNT.passwordHash(), ACCOUNT.email(), "EDITOR");
        assertThat(claims.has("role")).isFalse();
        assertThat(claims.has("passwordHash")).isFalse();
    }

    @Test
    void refusesMissingWeakOrInvalidSigningConfiguration() {
        for (String secret : List.of("", " ", "not base64!", Base64.getEncoder().encodeToString(new byte[31]))) {
            assertThatThrownBy(() -> configured(secret, TTL, ISSUER, AUDIENCE))
                    .as("signing secret must be explicit Base64 containing at least 32 bytes")
                    .isInstanceOf(RuntimeException.class);
        }
        for (long ttl : new long[]{0, -1, 3601}) {
            assertThatThrownBy(() -> configured(SECRET, ttl, ISSUER, AUDIENCE))
                    .as("access token lifetime is bounded")
                    .isInstanceOf(RuntimeException.class);
        }
        assertThatThrownBy(() -> configured(SECRET, TTL, " ", AUDIENCE)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> configured(SECRET, TTL, ISSUER, "")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsTamperedPayloadAndSignatureEvenWhenTheTokenStillParses() throws Exception {
        String token = tokens.issue(ACCOUNT).accessToken();
        String[] parts = token.split("\\.");
        ObjectNode claims = part(token, 1);
        claims.put("sub", "1");
        String forgedPayload = parts[0] + "." + encode(claims.toString()) + "." + parts[2];
        char replacement = parts[2].charAt(0) == 'A' ? 'B' : 'A';
        String forgedSignature = parts[0] + "." + parts[1] + "." + replacement + parts[2].substring(1);

        reject(forgedPayload);
        reject(forgedSignature);
    }

    @Test
    void rejectsUnsignedOrUnexpectedHeaderAlgorithmsAndExtensions() throws Exception {
        ObjectNode claims = validClaims();
        for (String algorithm : List.of("none", "HS384", "RS256", "hs256")) {
            ObjectNode header = validHeader();
            header.put("alg", algorithm);
            reject(signed(header, claims));
        }
        ObjectNode wrongType = validHeader();
        wrongType.put("typ", "other-token");
        reject(signed(wrongType, claims));
        ObjectNode extraHeader = validHeader();
        extraHeader.put("kid", "untrusted-key");
        reject(signed(extraHeader, claims));
        ObjectNode missingType = validHeader();
        missingType.remove("typ");
        reject(signed(missingType, claims));
    }

    @Test
    void rejectsTokensIssuedForAnotherIssuerOrAudience() throws Exception {
        ObjectNode wrongIssuer = validClaims();
        wrongIssuer.put("iss", "another-issuer");
        reject(signed(validHeader(), wrongIssuer));
        ObjectNode wrongAudience = validClaims();
        wrongAudience.put("aud", "another-api");
        reject(signed(validHeader(), wrongAudience));
    }

    @Test
    void expiryIsEnforcedAtItsBoundaryAndTokensCannotBeUsedBeforeTheyAreIssued() {
        String token = tokens.issue(ACCOUNT).accessToken();
        assertThat(serviceAt(NOW.plusSeconds(TTL - 1)).verify(token).accountId()).isEqualTo(ACCOUNT.id());
        assertThatThrownBy(() -> serviceAt(NOW.plusSeconds(TTL)).verify(token)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> serviceAt(NOW.minusSeconds(1)).verify(token)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsInvalidOrUnboundedTokenTimes() throws Exception {
        ObjectNode future = validClaims();
        future.put("iat", NOW.plusSeconds(1).getEpochSecond());
        reject(signed(validHeader(), future));
        ObjectNode reversed = validClaims();
        reversed.put("exp", NOW.minusSeconds(1).getEpochSecond());
        reject(signed(validHeader(), reversed));
        ObjectNode emptyLifetime = validClaims();
        emptyLifetime.put("exp", NOW.getEpochSecond());
        reject(signed(validHeader(), emptyLifetime));
        ObjectNode excessiveLifetime = validClaims();
        excessiveLifetime.put("exp", NOW.plusSeconds(TTL + 1).getEpochSecond());
        reject(signed(validHeader(), excessiveLifetime));
        ObjectNode fractionalTime = validClaims();
        fractionalTime.put("iat", NOW.getEpochSecond() + 0.5);
        reject(signed(validHeader(), fractionalTime));
    }

    @Test
    void requiresEverySecurityClaimWithItsExpectedJsonType() throws Exception {
        for (String claim : List.of("iss", "aud", "sub", "iat", "exp", "jti", "pwd")) {
            ObjectNode missing = validClaims();
            missing.remove(claim);
            reject(signed(validHeader(), missing));
            ObjectNode nullValue = validClaims();
            nullValue.putNull(claim);
            reject(signed(validHeader(), nullValue));
            ObjectNode wrongType = validClaims();
            if ("iat".equals(claim) || "exp".equals(claim)) wrongType.put(claim, "123");
            else wrongType.put(claim, 123);
            reject(signed(validHeader(), wrongType));
        }
        for (String subject : List.of("0", "-1", "not-an-account", "9223372036854775808")) {
            ObjectNode claims = validClaims();
            claims.put("sub", subject);
            reject(signed(validHeader(), claims));
        }
    }

    @Test
    void rejectsDuplicateJsonKeysTrailingContentAndNonObjectJson() throws Exception {
        String header = validHeader().toString();
        String claims = validClaims().toString();
        reject(signedRaw("{\"alg\":\"HS256\"," + header.substring(1), claims));
        reject(signedRaw(header, "{\"iss\":\"funews\"," + claims.substring(1)));
        reject(signedRaw(header + " {}", claims));
        reject(signedRaw(header, claims + " {}"));
        reject(signedRaw("[]", claims));
        reject(signedRaw(header, "[]"));
    }

    @Test
    void rejectsMalformedOversizedAndPaddedCompactSerializations() throws Exception {
        String token = tokens.issue(ACCOUNT).accessToken();
        for (String malformed : List.of("", ".", "..", "a.b.c", token + ".extra", " " + token,
                token + " ", "a".repeat(20000) + ".e30.signature")) {
            reject(malformed);
        }
        // Correctly sign a token with padded segments, so rejection cannot be due to signature tampering.
        String header = Base64.getUrlEncoder().encodeToString(
                (" " + validHeader()).getBytes(StandardCharsets.UTF_8));
        String claims = Base64.getUrlEncoder().encodeToString(validClaims().toString().getBytes(StandardCharsets.UTF_8));
        assertThat(header).contains("=");
        reject(signSegments(header, claims));
    }

    @Test
    void credentialBindingSurvivesProfileAndRoleChangesButRejectsPasswordChangesAndOtherAccounts() {
        var verified = tokens.verify(tokens.issue(ACCOUNT).accessToken());
        AccountPrincipal changedProfile = new AccountPrincipal(ACCOUNT.id(), "new@fu.edu.vn",
                ACCOUNT.passwordHash(), "CONTRIBUTOR", true, false);
        AccountPrincipal changedPassword = new AccountPrincipal(ACCOUNT.id(), ACCOUNT.email(),
                "replacement-password-hash", ACCOUNT.roleCode(), true, false);
        AccountPrincipal differentAccount = new AccountPrincipal(99L, ACCOUNT.email(),
                ACCOUNT.passwordHash(), ACCOUNT.roleCode(), true, false);

        assertThat(tokens.matchesCredentials(verified, changedProfile)).isTrue();
        assertThat(tokens.matchesCredentials(verified, changedPassword)).isFalse();
        assertThat(tokens.matchesCredentials(verified, differentAccount)).isFalse();
    }

    @Test
    void revocationImmediatelyRejectsReplayWithoutRevokingOtherLoginsAtTheSameTime() {
        String first = tokens.issue(ACCOUNT).accessToken();
        String second = tokens.issue(ACCOUNT).accessToken();
        var firstVerified = tokens.verify(first);
        var secondVerified = tokens.verify(second);
        assertThat(firstVerified.tokenId()).isNotEqualTo(secondVerified.tokenId());

        tokens.revoke(firstVerified);
        tokens.revoke(firstVerified);

        reject(first);
        assertThat(tokens.verify(second).accountId()).isEqualTo(ACCOUNT.id());
    }

    private JwtTokenService configured(String secret, long ttl, String issuer, String audience) {
        return new JwtTokenService(mapper, secret, ttl, issuer, audience, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private JwtTokenService serviceAt(Instant instant) {
        return new JwtTokenService(mapper, SECRET, TTL, ISSUER, AUDIENCE, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private ObjectNode validHeader() {
        return mapper.createObjectNode().put("alg", "HS256").put("typ", "JWT");
    }

    private ObjectNode validClaims() throws Exception {
        return part(tokens.issue(ACCOUNT).accessToken(), 1);
    }

    private ObjectNode part(String token, int index) throws Exception {
        JsonNode json = mapper.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[index]));
        return (ObjectNode) json;
    }

    private void reject(String token) {
        assertThatThrownBy(() -> tokens.verify(token)).isInstanceOf(RuntimeException.class);
    }

    private String signed(ObjectNode header, ObjectNode claims) throws Exception {
        return signedRaw(header.toString(), claims.toString());
    }

    private String signedRaw(String header, String claims) throws Exception {
        return signSegments(encode(header), encode(claims));
    }

    private String signSegments(String header, String claims) throws Exception {
        String content = header + "." + claims;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(KEY, "HmacSHA256"));
        return content + "." + URL.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.US_ASCII)));
    }

    private String encode(String value) {
        return URL.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}

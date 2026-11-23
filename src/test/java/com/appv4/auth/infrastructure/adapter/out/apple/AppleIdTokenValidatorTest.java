package com.appv4.auth.infrastructure.adapter.out.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.appv4.auth.application.exception.AuthDomainException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

class AppleIdTokenValidatorTest {

	private static final String KID = "test-kid";
	private static final String AUD = "com.example.service";

	private AppleIdTokenValidator validator;
	private PrivateKey privateKey;
	private PublicKey publicKey;

	@BeforeEach
	void setUp() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair pair = generator.generateKeyPair();
		privateKey = pair.getPrivate();
		publicKey = pair.getPublic();
		validator = new AppleIdTokenValidator(RestClient.builder(), "http://localhost/unused");
		validator.replaceKeysForTests(Map.of(KID, publicKey));
	}

	@Test
	void acceptsValidToken() {
		String token = signedToken(AUD, Instant.now().plusSeconds(600));
		Claims claims = validator.validate(token, AUD);
		assertThat(claims.getSubject()).isEqualTo("apple-sub-1");
	}

	@Test
	void rejectsWrongAudience() {
		String token = signedToken("other.client", Instant.now().plusSeconds(600));
		assertThatThrownBy(() -> validator.validate(token, AUD))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("invalide");
	}

	@Test
	void rejectsExpired() {
		String token = signedToken(AUD, Instant.now().minusSeconds(600));
		assertThatThrownBy(() -> validator.validate(token, AUD))
				.isInstanceOf(AuthDomainException.class);
	}

	@Test
	void emailOnlyWhenVerified() {
		Claims verified = Jwts.claims()
				.subject("s")
				.add("email", "a@b.com")
				.add("email_verified", true)
				.build();
		assertThat(HttpAppleOAuthClient.toProfile(verified).email()).isEqualTo("a@b.com");

		Claims unverified = Jwts.claims()
				.subject("s")
				.add("email", "a@b.com")
				.add("email_verified", false)
				.build();
		assertThat(HttpAppleOAuthClient.toProfile(unverified).email()).isNull();
	}

	private String signedToken(String audience, Instant exp) {
		return Jwts.builder()
				.header().keyId(KID).and()
				.issuer(AppleIdTokenValidator.ISSUER)
				.subject("apple-sub-1")
				.audience().add(audience).and()
				.expiration(Date.from(exp))
				.issuedAt(Date.from(Instant.now().minusSeconds(30)))
				.signWith(privateKey)
				.compact();
	}
}

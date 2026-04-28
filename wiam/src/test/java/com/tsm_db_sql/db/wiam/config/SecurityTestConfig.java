package com.tsm_db_sql.db.wiam.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * Configurazione di test per i JWT — genera una coppia di chiavi RSA in memoria
 * così i test non devono contattare un auth-server reale.
 *
 * @Primary sovrascrive il JwtDecoder auto-configurato da Spring Boot
 * (che normalmente punta al JWK Set URI dell'auth-server).
 * Il JwtEncoder è disponibile per i test che devono generare JWT di test.
 *
 * Attivo solo con profilo "test" — in runtime usa la configurazione reale
 * che punta all'auth-server tramite jwk-set-uri.
 */
@Configuration
@Profile("test")
public class SecurityTestConfig {

    // Coppia di chiavi RSA generata una sola volta per tutti i test.
    // Usata sia per firmare (encoder) che verificare (decoder) i JWT.
    private static final KeyPair RSA_KEY_PAIR = generateRsaKeyPair();

    private static KeyPair generateRsaKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Errore generazione chiavi RSA per test", e);
        }
    }

    /**
     * JwtDecoder di test — verifica i JWT firmati con la chiave RSA di test.
     * @Primary fa sì che questo bean sovrascriva quello auto-configurato
     * da spring.security.oauth2.resourceserver.jwt.jwk-set-uri,
     * evitando che i test provino a contattare un auth-server inesistente.
     */
    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        var publicKey = (RSAPublicKey) RSA_KEY_PAIR.getPublic();
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    /**
     * JwtEncoder di test — firma JWT con la chiave RSA privata di test.
     * Usato nei test per generare token validi da passare ai controller protetti.
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        var publicKey = (RSAPublicKey) RSA_KEY_PAIR.getPublic();
        var privateKey = (RSAPrivateKey) RSA_KEY_PAIR.getPrivate();
        var rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        var jwkSet = new JWKSet(rsaKey);
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(jwkSet));
    }
}

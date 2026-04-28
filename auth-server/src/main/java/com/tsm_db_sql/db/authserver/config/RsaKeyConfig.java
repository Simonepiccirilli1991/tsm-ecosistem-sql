package com.tsm_db_sql.db.authserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * Configurazione delle chiavi RSA per la firma dei JWT.
 *
 * In sviluppo la coppia di chiavi viene generata in memoria all'avvio dell'applicazione.
 * Questo significa che ogni restart dell'auth-server invalida TUTTI i token emessi in precedenza,
 * perché la chiave privata cambia.
 *
 * ⚠️ In PRODUZIONE bisogna:
 * - Caricare le chiavi da file, KeyStore, o servizio di gestione segreti (Vault, AWS KMS)
 * - Usare la stessa coppia di chiavi tra restart per non invalidare i token attivi
 * - Ruotare le chiavi periodicamente con un meccanismo di key rotation
 */
@Configuration
public class RsaKeyConfig {

    // Coppia di chiavi RSA 2048-bit generata all'avvio.
    // 2048-bit è il minimo raccomandato per RSA; 4096 è più sicuro ma più lento.
    private final KeyPair rsaKeyPair = generateRsaKeyPair();

    // RSAKey in formato JWK (JSON Web Key) — include sia chiave pubblica che privata.
    // Il keyID è un UUID random usato nell'header "kid" del JWT per identificare
    // quale chiave ha firmato il token (utile in scenari di key rotation).
    private final RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) rsaKeyPair.getPublic())
            .privateKey((RSAPrivateKey) rsaKeyPair.getPrivate())
            .keyID(UUID.randomUUID().toString())
            .build();

    private static KeyPair generateRsaKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Errore generazione coppia chiavi RSA", e);
        }
    }

    /**
     * JwtEncoder — firma i JWT con la chiave privata RSA.
     * Usato dal TokenService per creare access_token firmati.
     * NimbusJwtEncoder gestisce automaticamente l'header JWS (alg: RS256, kid).
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        var jwkSet = new JWKSet(rsaKey);
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(jwkSet));
    }

    /**
     * JWKSet — set di chiavi pubbliche in formato standard.
     * Esposto dal JwkController su /.well-known/jwks.json.
     * Contiene SOLO la chiave pubblica (la privata viene rimossa automaticamente
     * da toPublicJWKSet()) — i Resource Server usano questa chiave per verificare
     * la firma dei JWT senza mai conoscere la chiave privata.
     */
    @Bean
    public JWKSet jwkSet() {
        return new JWKSet(rsaKey);
    }
}

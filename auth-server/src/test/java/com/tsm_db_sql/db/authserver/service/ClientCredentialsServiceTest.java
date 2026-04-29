package com.tsm_db_sql.db.authserver.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di integrazione per ClientCredentialsService.
 *
 * Verifica che il servizio generi correttamente un JWT B2B con:
 * - Subject = "auth-server-client" (identifica il client, non un utente)
 * - Scope = "internal" (richiesto da WIAM per gli endpoint interni)
 * - Issuer = "tsm-auth-server" (identifica chi ha emesso il token)
 * - Scadenza nel futuro (il token non è già scaduto)
 *
 * Non serve mockare nulla — il JwtEncoder è un bean reale configurato in RsaKeyConfig.
 * Decodichiamo il token con un JwtDecoder costruito sulla stessa chiave pubblica
 * per verificare che i claim siano corretti.
 */
@SpringBootTest
class ClientCredentialsServiceTest {

    @Autowired
    private ClientCredentialsService clientCredentialsService;

    // Usiamo il JWKSet bean (che contiene la chiave pubblica) per decodificare
    // e validare il token generato dal servizio.
    @Autowired
    private com.nimbusds.jose.jwk.JWKSet jwkSet;

    @Test
    void generaTokenB2B_deveCreareTokenConClaimCorretti() {
        // === ARRANGE ===
        // Nessun setup necessario — il servizio usa il JwtEncoder già configurato.

        // === ACT ===
        // Generiamo il token B2B come farebbe WiamClientService prima di chiamare WIAM.
        String tokenValue = clientCredentialsService.generaTokenB2B();

        // === ASSERT ===
        // Verifichiamo che il token non sia nullo o vuoto
        assertNotNull(tokenValue, "Il token B2B non deve essere nullo");
        assertFalse(tokenValue.isBlank(), "Il token B2B non deve essere vuoto");

        // Decodichiamo il JWT per verificare i claim interni.
        // Usiamo la chiave pubblica dal JWKSet — stessa che WIAM userebbe per validare.
        var rsaKey = jwkSet.getKeys().getFirst();
        JwtDecoder decoder;
        try {
            var publicKey = (RSAPublicKey) rsaKey.toRSAKey().toPublicKey();
            decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        } catch (Exception e) {
            fail("Impossibile costruire JwtDecoder dalla chiave pubblica: " + e.getMessage());
            return;
        }

        // Decodifica e validazione automatica (firma + scadenza)
        Jwt jwt = decoder.decode(tokenValue);

        // Verifica subject — deve essere "auth-server-client", non un username utente
        assertEquals("auth-server-client", jwt.getSubject(),
                "Il subject del token B2B deve essere 'auth-server-client'");

        // Verifica issuer — deve essere "tsm-auth-server"
        assertEquals("tsm-auth-server", jwt.getClaimAsString("iss"),
                "L'issuer deve essere 'tsm-auth-server'");

        // Verifica scope — deve contenere "internal" per accedere a /api/internal/**
        // Spring Security converte questo in authority "SCOPE_internal"
        assertEquals("internal", jwt.getClaimAsString("scope"),
                "Lo scope deve essere 'internal' per accedere agli endpoint interni di WIAM");

        // Verifica che la scadenza sia nel futuro (token non già scaduto)
        assertNotNull(jwt.getExpiresAt(), "Il token deve avere una scadenza");
        assertTrue(jwt.getExpiresAt().isAfter(java.time.Instant.now()),
                "Il token non deve essere già scaduto");

        // Verifica che issuedAt sia presente e nel passato/presente
        assertNotNull(jwt.getIssuedAt(), "Il token deve avere un timestamp di emissione");
    }

    @Test
    void generaTokenB2B_ogniChiamataGeneraTokenDiverso() throws InterruptedException {
        // === CONTESTO ===
        // Ogni chiamata a generaTokenB2B() deve generare un token NUOVO.
        // Non cacheamo i token per semplicità e sicurezza.
        // Due token generati in momenti diversi avranno "iat" diversi,
        // quindi il payload (e la firma) saranno diversi.
        // NOTA: JWT "iat" ha precisione al secondo, quindi aspettiamo 1 secondo
        // per garantire che i timestamp differiscano.

        // === ACT ===
        String token1 = clientCredentialsService.generaTokenB2B();
        Thread.sleep(1000); // Aspettiamo 1 secondo per avere un "iat" diverso
        String token2 = clientCredentialsService.generaTokenB2B();

        // === ASSERT ===
        // I token devono essere diversi (il timestamp "iat" differisce)
        assertNotEquals(token1, token2,
                "Due chiamate successive devono generare token diversi");
    }
}

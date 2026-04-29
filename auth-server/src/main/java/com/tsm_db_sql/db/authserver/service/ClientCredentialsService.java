package com.tsm_db_sql.db.authserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Servizio per la generazione di JWT "Client Credentials" (B2B / machine-to-machine).
 *
 * === CONTESTO ===
 * L'auth-server deve chiamare WIAM per verificare le credenziali degli utenti,
 * ma l'endpoint interno di WIAM è protetto con OAuth2: richiede un JWT con scope "internal".
 * Questo servizio genera quel JWT — un token che identifica l'auth-server COME CLIENT,
 * non come utente finale.
 *
 * === PERCHÉ SELF-ISSUED? ===
 * L'auth-server genera il token per sé stesso (self-issued) perché:
 * 1. È l'unico componente che possiede la chiave privata RSA per firmare JWT
 * 2. WIAM già valida i JWT scaricando la chiave pubblica dall'auth-server (JWK Set)
 * 3. Non serve un endpoint esterno — sarebbe circolare (chi protegge il protettore?)
 *
 * === DIFFERENZA DAL TOKEN UTENTE ===
 * - Token Utente: sub="username", claim custom (ruolo, email), durata 1 ora
 * - Token B2B: sub="auth-server-client", scope="internal", durata 5 minuti
 *
 * La durata breve (5 minuti) è una best practice di sicurezza:
 * il token B2B serve SOLO per la singola operazione di verifica credenziali,
 * quindi non ha senso che viva a lungo. Se qualcuno lo intercettasse,
 * avrebbe una finestra di esposizione molto ridotta.
 *
 * === COME WIAM DISTINGUE I DUE TIPI DI TOKEN ===
 * WIAM usa il claim "scope" nel JWT per decidere l'accesso:
 * - Se il JWT ha scope "internal" → può accedere a /api/internal/**
 * - Se il JWT ha un subject utente (senza scope internal) → accede agli endpoint normali
 * Spring Security gestisce questo automaticamente con hasAuthority("SCOPE_internal").
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClientCredentialsService {

    // Lo stesso JwtEncoder usato per i token utente — firma con la stessa chiave RSA.
    // Questo significa che WIAM può validare ENTRAMBI i tipi di token con lo stesso
    // JWK Set endpoint (/.well-known/jwks.json), senza configurazione aggiuntiva.
    private final JwtEncoder jwtEncoder;

    // Durata del token B2B in minuti.
    // 5 minuti è sufficiente per una singola chiamata HTTP a WIAM.
    // Se la chiamata fallisce e viene ritentata, il token è ancora valido.
    // Ma se venisse rubato, l'attaccante ha solo 5 minuti per usarlo.
    private static final long TOKEN_EXPIRY_MINUTES = 5;

    // Subject (sub) del token B2B — identifica l'auth-server come client.
    // NON è un username di un utente reale — è un identificativo di servizio.
    // WIAM può usare questo valore per loggare "chi" ha fatto la richiesta interna.
    private static final String CLIENT_SUBJECT = "auth-server-client";

    // Issuer — stesso dell'auth-server per i token utente.
    // WIAM verifica che il token sia stato emesso dal suo auth-server trusted.
    private static final String ISSUER = "tsm-auth-server";

    /**
     * Genera un JWT Client Credentials per autenticarsi verso WIAM.
     *
     * Questo token permette all'auth-server di chiamare gli endpoint interni di WIAM
     * che richiedono autenticazione con scope "internal".
     *
     * Il token viene generato OGNI VOLTA che serve una chiamata a WIAM.
     * Non viene cachato perché:
     * 1. La generazione è veloce (firma RSA ~1ms)
     * 2. Ogni token ha una finestra temporale precisa
     * 3. Evita problemi di token scaduti in cache
     *
     * @return stringa JWT firmata, pronta per essere usata come Bearer token
     */
    public String generaTokenB2B() {
        log.debug("Generazione token B2B (Client Credentials) per chiamata a WIAM");

        var now = Instant.now();

        // Costruzione dei claim per il token B2B.
        // La struttura è simile al token utente, ma con claim diversi
        // che identificano questo come token di servizio (machine-to-machine).
        var claims = JwtClaimsSet.builder()

                // "sub" (subject) — identifica il CLIENT (l'auth-server), non un utente.
                // WIAM loggerà questo valore come "chi ha fatto la richiesta interna".
                // Usare un valore fisso e riconoscibile aiuta nel debugging.
                .subject(CLIENT_SUBJECT)

                // "iss" (issuer) — stesso issuer dei token utente.
                // WIAM si aspetta che tutti i JWT provengano da "tsm-auth-server".
                // Se un JWT avesse un issuer diverso, verrebbe rifiutato.
                .issuer(ISSUER)

                // "iat" (issued at) — quando è stato emesso.
                // Utile per audit: se vediamo un token B2B emesso in un orario insolito,
                // potrebbe indicare un problema.
                .issuedAt(now)

                // "exp" (expiration) — scade dopo TOKEN_EXPIRY_MINUTES.
                // Breve durata = minore rischio se il token viene intercettato.
                // L'auth-server ne genera uno nuovo per ogni chiamata, quindi
                // non c'è impatto sulle performance.
                .expiresAt(now.plus(TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES))

                // "scope" — IL CLAIM CHIAVE per l'autorizzazione.
                // Spring Security su WIAM converte questo in authority "SCOPE_internal".
                // L'endpoint /api/internal/** richiede questa authority.
                // Senza questo claim, il token verrebbe validato (firma OK) ma
                // l'accesso sarebbe negato (403 Forbidden - manca la authority richiesta).
                .claim("scope", "internal")

                .build();

        // Firma il JWT con la chiave privata RSA — stessa chiave dei token utente.
        // Il JwtEncoder (NimbusJwtEncoder) aggiunge automaticamente l'header con:
        // - alg: RS256 (algoritmo di firma RSA con SHA-256)
        // - kid: ID della chiave (per identificarla nel JWK Set)
        var jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));

        log.debug("Token B2B generato con successo, scadenza: {}", claims.getExpiresAt());

        return jwt.getTokenValue();
    }
}

package com.tsm_db_sql.db.authserver.service;

import com.tsm_db_sql.db.authserver.model.WiamUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Servizio per la generazione di JWT access_token.
 *
 * Dopo che WiamClientService ha verificato le credenziali e restituito i dati utente,
 * questo servizio crea un JWT firmato con la chiave privata RSA contenente:
 *
 * - sub (subject): lo username dell'utente — identifica CHI è il proprietario del token
 * - ruolo: il ruolo dell'utente (Admin, User, Collaborator) — usato dal Resource Server
 *   per autorizzazione basata su ruoli
 * - email: l'email dell'utente — disponibile nei claim senza dover fare query aggiuntive
 * - iss (issuer): identifica l'auth-server che ha emesso il token
 * - iat (issued at): timestamp di emissione
 * - exp (expiration): timestamp di scadenza — il token non è più valido dopo questa data
 *
 * Il token ha una durata di 1 ora — bilanciamento tra sicurezza (token a breve scadenza)
 * e usabilità (il client non deve ri-autenticarsi troppo spesso).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder jwtEncoder;

    // Durata del token in ore — configurabile per ambiente.
    // 1 ora è standard per access_token OAuth2.
    private static final long TOKEN_EXPIRY_HOURS = 1;

    /**
     * Genera un JWT firmato con i dati dell'utente autenticato.
     *
     * @param userResponse dati utente restituiti da WIAM dopo la verifica credenziali
     * @return stringa JWT codificata e firmata (header.payload.signature)
     */
    public String generaToken(WiamUserResponse userResponse) {
        log.info("Generazione JWT per utente: {}", userResponse.username());

        var now = Instant.now();

        // Costruzione dei claim del JWT.
        // Ogni claim è un'informazione sull'utente o sul token stesso.
        var claims = JwtClaimsSet.builder()
                // "sub" (subject) — identifica il proprietario del token.
                // Il Resource Server (WIAM) può usare questo valore per sapere
                // quale utente ha fatto la richiesta senza query al DB.
                .subject(userResponse.username())

                // "iss" (issuer) — identifica chi ha emesso il token.
                // Il Resource Server può verificare che il token sia stato emesso
                // da un auth-server trusted (non da un attaccante).
                .issuer("tsm-auth-server")

                // "iat" (issued at) — timestamp di emissione.
                // Utile per audit e per implementare policy come "token emessi
                // prima di una certa data sono invalidi" (es. dopo un security breach).
                .issuedAt(now)

                // "exp" (expiration) — il token scade dopo TOKEN_EXPIRY_HOURS.
                // Dopo questa data, il Resource Server rifiuterà il token.
                // Il client deve richiedere un nuovo token all'auth-server.
                .expiresAt(now.plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS))

                // Claim custom: ruolo dell'utente nel sistema TSM.
                // Il Resource Server può leggere questo claim per autorizzazione
                // basata su ruoli (es. solo Admin può accedere a certi endpoint).
                .claim("ruolo", userResponse.ruolo())

                // Claim custom: email dell'utente.
                // Disponibile nei claim così i servizi downstream possono usarla
                // senza dover fare una query al DB.
                .claim("email", userResponse.email())

                .build();

        // Firma il JWT con la chiave privata RSA tramite NimbusJwtEncoder.
        // L'encoder aggiunge automaticamente l'header JWS con:
        // - alg: RS256 (algoritmo di firma)
        // - kid: l'ID della chiave RSA (per key rotation)
        var jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));

        log.info("JWT generato con successo per utente: {}, scadenza: {}",
                userResponse.username(), claims.getExpiresAt());

        return jwt.getTokenValue();
    }
}

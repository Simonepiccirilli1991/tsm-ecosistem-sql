package com.tsm_db_sql.db.authserver.controller;

import com.tsm_db_sql.db.authserver.model.TokenRequest;
import com.tsm_db_sql.db.authserver.model.TokenResponse;
import com.tsm_db_sql.db.authserver.service.TokenService;
import com.tsm_db_sql.db.authserver.service.WiamClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Token Endpoint — emette JWT access_token in cambio di credenziali valide.
 *
 * Flusso:
 * 1. Il client invia POST /oauth2/token con username e password
 * 2. L'auth-server chiama WIAM per verificare le credenziali (BCrypt match)
 * 3. Se valide, genera un JWT firmato con i claim dell'utente
 * 4. Restituisce il JWT al client nel formato standard OAuth2
 *
 * Il client usa poi il JWT come Bearer token nelle richieste a WIAM:
 *   Authorization: Bearer eyJhbGciOiJS...
 *
 * Se le credenziali sono errate, restituisce 401 con messaggio di errore.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class TokenController {

    private final WiamClientService wiamClientService;
    private final TokenService tokenService;

    // Durata token in secondi — deve corrispondere a TOKEN_EXPIRY_HOURS in TokenService
    private static final long TOKEN_EXPIRY_SECONDS = 3600;

    /**
     * Endpoint di autenticazione — riceve username/password, restituisce JWT.
     *
     * Percorso: POST /oauth2/token
     * Segue la convenzione OAuth2 per il token endpoint, anche se il flusso
     * è semplificato (no grant_type, no client_id) rispetto allo standard completo.
     *
     * @param request contiene username e password in chiaro
     * @return TokenResponse con access_token, token_type e expires_in
     */
    @PostMapping("/oauth2/token")
    public ResponseEntity<TokenResponse> token(@RequestBody TokenRequest request) {
        log.info("Richiesta token per utente: {}", request.username());

        try {
            // Step 1: Verifica credenziali chiamando WIAM.
            // WIAM cerca l'utente nel DB, confronta la password con BCrypt,
            // e restituisce i dati utente (username, ruolo, email) se valide.
            var wiamUser = wiamClientService.verificaCredenziali(
                    request.username(), request.password());

            // Step 2: Genera il JWT firmato con i dati dell'utente.
            // Il token contiene i claim (sub, ruolo, email) e viene firmato
            // con la chiave privata RSA. Solo chi ha la chiave pubblica
            // (ottenuta dal JWK endpoint) può verificare la firma.
            var jwt = tokenService.generaToken(wiamUser);

            log.info("Token emesso con successo per utente: {}", request.username());

            // Step 3: Restituisci il token nel formato standard OAuth2.
            // - access_token: il JWT codificato
            // - token_type: "Bearer" — il client lo mette nell'header Authorization
            // - expires_in: secondi rimanenti prima della scadenza
            return ResponseEntity.ok(new TokenResponse(jwt, "Bearer", TOKEN_EXPIRY_SECONDS));

        } catch (RuntimeException e) {
            // Le credenziali sono errate o WIAM non è raggiungibile.
            // Restituiamo 401 con messaggio generico (non rivelare se è
            // l'username o la password ad essere errata — prevenzione enumeration).
            log.error("Autenticazione fallita per utente: {} — {}", request.username(), e.getMessage());
            return ResponseEntity.status(401)
                    .body(new TokenResponse(null, null, 0));
        }
    }
}

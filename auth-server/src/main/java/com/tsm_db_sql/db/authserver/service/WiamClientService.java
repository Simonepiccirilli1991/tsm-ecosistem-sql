package com.tsm_db_sql.db.authserver.service;

import com.tsm_db_sql.db.authserver.model.WiamUserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Client REST che chiama WIAM per verificare le credenziali degli utenti.
 *
 * L'auth-server NON ha accesso diretto al database degli utenti — delega
 * la verifica a WIAM tramite il suo endpoint interno. Questo mantiene
 * la separazione delle responsabilità: WIAM gestisce i dati utente,
 * l'auth-server gestisce solo l'emissione dei token.
 *
 * === SICUREZZA B2B (Client Credentials) ===
 * L'endpoint interno di WIAM è protetto con OAuth2: richiede un JWT con scope "internal".
 * Prima di ogni chiamata, questo servizio ottiene un token B2B dal ClientCredentialsService
 * e lo include nell'header Authorization come Bearer token.
 *
 * Flusso completo:
 * 1. ClientCredentialsService genera un JWT B2B (sub=auth-server-client, scope=internal)
 * 2. Questo servizio aggiunge "Authorization: Bearer <jwt-b2b>" alla richiesta HTTP
 * 3. WIAM riceve la richiesta, valida il JWT (firma + scadenza + scope)
 * 4. Se il JWT è valido E ha scope "internal" → risponde con i dati utente
 * 5. Se il JWT manca o non ha lo scope giusto → risponde 401/403
 *
 * In caso di credenziali utente errate, WIAM risponde con 401 o 404 e questo
 * servizio propaga l'errore come RuntimeException.
 */
@Service
@Slf4j
public class WiamClientService {

    private final RestClient restClient;

    // Servizio che genera i JWT B2B per autenticarsi verso WIAM.
    // Viene iniettato tramite costruttore (constructor injection via argomenti).
    private final ClientCredentialsService clientCredentialsService;

    /**
     * Costruttore — crea il RestClient e inietta il servizio Client Credentials.
     *
     * NOTA: Non usiamo @RequiredArgsConstructor qui perché abbiamo bisogno di
     * @Value per iniettare il base URL dal file di configurazione.
     * Quando un costruttore ha un mix di @Value e dipendenze Spring,
     * è più chiaro scrivere il costruttore esplicitamente.
     *
     * @param wiamBaseUrl URL base di WIAM (da application.yaml: auth-server.wiam.base-url)
     * @param clientCredentialsService servizio per generare token B2B
     */
    public WiamClientService(
            @Value("${auth-server.wiam.base-url}") String wiamBaseUrl,
            ClientCredentialsService clientCredentialsService) {

        // RestClient è l'API moderna di Spring 6.1+ per chiamate HTTP sincrone,
        // sostituisce RestTemplate. Supporta fluent API e serializzazione automatica.
        this.restClient = RestClient.builder()
                .baseUrl(wiamBaseUrl)
                .build();

        this.clientCredentialsService = clientCredentialsService;
    }

    /**
     * Verifica le credenziali chiamando l'endpoint interno di WIAM.
     *
     * Flusso dettagliato:
     * 1. Genera un token B2B (Client Credentials) per autenticarsi verso WIAM
     * 2. Invia POST a WIAM con username/password nel body e Bearer token nell'header
     * 3. WIAM valida il Bearer token (firma RSA + scope "internal")
     * 4. Se il token è valido, WIAM verifica le credenziali utente (BCrypt match)
     * 5. Restituisce i dati utente (username, ruolo, email, nome, cognome)
     *
     * Se le credenziali sono errate, WIAM restituisce un errore HTTP (401/404)
     * che viene intercettato e rilanciato come RuntimeException.
     *
     * @param username lo username dell'utente
     * @param password la password in chiaro (WIAM la confronta con l'hash BCrypt)
     * @return i dati dell'utente autenticato
     * @throws RuntimeException se le credenziali sono errate o WIAM non è raggiungibile
     */
    public WiamUserResponse verificaCredenziali(String username, String password) {
        log.info("Chiamata a WIAM per verifica credenziali utente: {}", username);

        // === STEP 1: Generazione token B2B ===
        // Generiamo un nuovo token per OGNI chiamata. Non lo cacheamo perché:
        // - La generazione è velocissima (~1ms, è solo una firma RSA)
        // - Un token nuovo è sempre valido (nessun rischio di token scaduto)
        // - Semplifica il codice (nessuna logica di cache/refresh)
        // Il token ha durata 5 minuti, ma lo usiamo subito per questa singola chiamata.
        var tokenB2B = clientCredentialsService.generaTokenB2B();
        log.debug("Token B2B generato per la chiamata a WIAM");

        try {
            // === STEP 2: Chiamata POST autenticata all'endpoint interno di WIAM ===
            // Header "Authorization: Bearer <jwt>" — standard OAuth2 per Resource Server.
            // WIAM legge questo header, estrae il JWT, ne verifica:
            // - La firma (con la chiave pubblica dall'auth-server JWK Set)
            // - La scadenza (exp deve essere nel futuro)
            // - Lo scope (deve contenere "internal" per accedere a /api/internal/**)
            var response = restClient.post()
                    .uri("/api/internal/v1/utente/verifica-credenziali")
                    .contentType(MediaType.APPLICATION_JSON)
                    // Header Authorization con il token B2B.
                    // Il formato "Bearer <token>" è lo standard OAuth2 RFC 6750.
                    // WIAM (Resource Server) lo legge automaticamente grazie a
                    // .oauth2ResourceServer(jwt -> {}) nella sua SecurityConfig.
                    .header("Authorization", "Bearer " + tokenB2B)
                    .body(Map.of("username", username, "password", password))
                    .retrieve()
                    .body(WiamUserResponse.class);

            log.info("Credenziali verificate con successo per utente: {}", username);
            return response;

        } catch (HttpClientErrorException e) {
            // WIAM ha risposto con un errore HTTP.
            // Possibili cause:
            // - 401: token B2B invalido o scaduto (non dovrebbe succedere, appena generato)
            // - 403: token valido ma senza scope "internal" (bug di configurazione)
            // - 401/404: credenziali utente errate (utente non trovato o password sbagliata)
            log.error("Verifica credenziali fallita per utente: {} — WIAM ha risposto con status {}",
                    username, e.getStatusCode());
            throw new RuntimeException("Credenziali non valide: " + e.getStatusCode());
        } catch (Exception e) {
            // Errore di rete o WIAM non raggiungibile.
            // Possibili cause: WIAM spento, timeout, DNS non risolvibile.
            log.error("Errore nella comunicazione con WIAM per utente: {} — {}", username, e.getMessage());
            throw new RuntimeException("Errore comunicazione con WIAM: " + e.getMessage());
        }
    }
}

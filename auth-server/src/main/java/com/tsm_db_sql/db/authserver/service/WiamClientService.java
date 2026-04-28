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
 * In caso di credenziali errate, WIAM risponde con 401 o 404 e questo
 * servizio propaga l'errore come RuntimeException.
 */
@Service
@Slf4j
public class WiamClientService {

    private final RestClient restClient;

    /**
     * Costruttore — crea il RestClient puntando al base URL di WIAM.
     * Il base URL è configurabile tramite application.yaml (auth-server.wiam.base-url).
     *
     * RestClient è l'API moderna di Spring 6.1+ per chiamate HTTP sincrone,
     * sostituisce RestTemplate. Supporta fluent API e serializzazione automatica.
     */
    public WiamClientService(@Value("${auth-server.wiam.base-url}") String wiamBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(wiamBaseUrl)
                .build();
    }

    /**
     * Verifica le credenziali chiamando l'endpoint interno di WIAM.
     *
     * Invia username e password a WIAM che:
     * 1. Cerca l'utente nel DB
     * 2. Verifica la password con BCrypt
     * 3. Restituisce i dati utente (username, ruolo, email, nome, cognome)
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

        try {
            // Chiamata POST all'endpoint interno di WIAM.
            // Il body contiene username e password come JSON.
            // WIAM risponde con i dati utente se le credenziali sono valide.
            var response = restClient.post()
                    .uri("/api/internal/v1/utente/verifica-credenziali")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", username, "password", password))
                    .retrieve()
                    .body(WiamUserResponse.class);

            log.info("Credenziali verificate con successo per utente: {}", username);
            return response;

        } catch (HttpClientErrorException e) {
            // WIAM ha risposto con un errore HTTP (es. 401 password errata, 404 utente non trovato).
            // Logghiamo e rilanciamo per far gestire al controller.
            log.error("Verifica credenziali fallita per utente: {} — WIAM ha risposto con status {}",
                    username, e.getStatusCode());
            throw new RuntimeException("Credenziali non valide: " + e.getStatusCode());
        } catch (Exception e) {
            // Errore di rete o WIAM non raggiungibile
            log.error("Errore nella comunicazione con WIAM per utente: {} — {}", username, e.getMessage());
            throw new RuntimeException("Errore comunicazione con WIAM: " + e.getMessage());
        }
    }
}

package com.tsm_db_sql.db.wiam.controller;

import com.tsm_db_sql.db.wiam.model.request.VerificaCredenzialiRequest;
import com.tsm_db_sql.db.wiam.model.response.VerificaCredenzialiResponse;
import com.tsm_db_sql.db.wiam.service.utente.LoginUtenteService;
import com.tsm_db_sql.db.wiam.model.request.LoginUtenteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller per endpoint INTERNI — usati esclusivamente per la comunicazione
 * tra microservizi (auth-server → WIAM). NON sono destinati all'accesso diretto dei client.
 *
 * Il prefisso /api/internal/ segnala che questi endpoint:
 * 1. Sono permit-all in SecurityConfig (l'auth-server non ha un JWT proprio)
 * 2. In produzione vanno protetti a livello di rete (firewall, service mesh, VPN)
 *    per impedire che client esterni li chiamino direttamente
 *
 * Endpoint:
 * - POST /api/internal/v1/utente/verifica-credenziali
 *   Riceve username+password, verifica tramite LoginUtenteService (con BCrypt),
 *   e restituisce i dati utente necessari all'auth-server per generare il JWT.
 */
@RestController
@RequestMapping("api/internal/v1/utente")
@RequiredArgsConstructor
public class InternalUtenteController {

    private final LoginUtenteService loginUtenteService;

    /**
     * Verifica le credenziali di un utente per conto dell'auth-server.
     *
     * L'auth-server chiama questo endpoint quando riceve una richiesta di token
     * con username e password. WIAM verifica le credenziali (BCrypt match) e
     * restituisce i dati utente (username, ruolo, email) che l'auth-server
     * inserirà come claim nel JWT.
     *
     * @param request contiene username e password in chiaro
     * @return dati utente (senza password) se le credenziali sono valide
     * @throws com.tsm_db_sql.db.wiam.exception.UtenteException se utente non trovato (404) o password errata (401)
     */
    @PostMapping("verifica-credenziali")
    public ResponseEntity<VerificaCredenzialiResponse> verificaCredenziali(
            @RequestBody VerificaCredenzialiRequest request) {

        // Riusiamo LoginUtenteService che già implementa la logica di verifica
        // credenziali con BCrypt. Convertiamo la request interna nel formato
        // che il servizio si aspetta (LoginUtenteRequest).
        var loginRequest = new LoginUtenteRequest(request.username(), request.password());
        var loginResponse = loginUtenteService.login(loginRequest);

        // Mappiamo la risposta del login alla risposta interna, aggiungendo
        // il campo username che l'auth-server usa come "sub" claim nel JWT
        var response = new VerificaCredenzialiResponse(
                request.username(),
                loginResponse.nome(),
                loginResponse.cognome(),
                loginResponse.ruolo(),
                loginResponse.email()
        );

        return ResponseEntity.ok(response);
    }
}

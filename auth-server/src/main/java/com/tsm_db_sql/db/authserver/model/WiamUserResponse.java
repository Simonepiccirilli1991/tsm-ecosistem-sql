package com.tsm_db_sql.db.authserver.model;

/**
 * Risposta di WIAM all'endpoint interno di verifica credenziali.
 * Contiene i dati dell'utente autenticato che diventano claim nel JWT.
 *
 * Mappata dal JSON restituito da:
 * POST http://wiam-host/api/internal/v1/utente/verifica-credenziali
 */
public record WiamUserResponse(
        String username,
        String nome,
        String cognome,
        String ruolo,
        String email
) {
}

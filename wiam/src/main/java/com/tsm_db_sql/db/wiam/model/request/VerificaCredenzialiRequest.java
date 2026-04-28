package com.tsm_db_sql.db.wiam.model.request;

/**
 * Request per l'endpoint interno di verifica credenziali.
 * Usata dall'auth-server per autenticare un utente tramite WIAM.
 * Contiene username e password in chiaro — la verifica BCrypt avviene nel servizio.
 */
public record VerificaCredenzialiRequest(
        String username,
        String password
) {
}

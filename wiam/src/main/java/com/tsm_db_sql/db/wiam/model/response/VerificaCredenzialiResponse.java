package com.tsm_db_sql.db.wiam.model.response;

import com.tsm_db_sql.db.wiam.utils.UtenteRoles;

/**
 * Response dell'endpoint interno di verifica credenziali.
 * Restituisce i dati dell'utente autenticato necessari all'auth-server
 * per generare il JWT con i claim corretti (sub, ruolo, email).
 *
 * Nota: NON include la password — l'auth-server non ne ha bisogno
 * dopo la verifica, e non deve mai transitare fuori da WIAM.
 */
public record VerificaCredenzialiResponse(
        String username,
        String nome,
        String cognome,
        UtenteRoles ruolo,
        String email
) {
}

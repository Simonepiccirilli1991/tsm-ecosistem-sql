package com.tsm_db_sql.db.authserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Auth Server — OAuth2 Authorization Server per l'ecosistema TSM Resell.
 *
 * Responsabilità:
 * 1. Riceve richieste di autenticazione (username + password)
 * 2. Verifica le credenziali chiamando WIAM (POST /api/internal/v1/utente/verifica-credenziali)
 * 3. Emette JWT firmati con RSA contenenti i claim dell'utente (sub, ruolo, email)
 * 4. Espone il JWK Set endpoint (/.well-known/jwks.json) con la chiave pubblica RSA
 *    così i Resource Server (WIAM) possono verificare la firma dei token
 *
 * NON gestisce sessioni, cookie o registrazione utenti — queste sono responsabilità di WIAM.
 */
@SpringBootApplication
public class AuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServerApplication.class, args);
    }
}

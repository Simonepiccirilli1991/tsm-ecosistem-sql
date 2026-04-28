package com.tsm_db_sql.db.authserver.model;

/**
 * Request per il token endpoint — il client invia username e password
 * per ottenere un JWT access_token.
 *
 * Nota: in un flusso OAuth2 standard questo sarebbe il "Resource Owner Password Credentials"
 * grant (grant_type=password), deprecato in OAuth 2.1 ma ancora usato in contesti interni
 * dove il client è trusted (app mobile proprietaria, SPA interna).
 */
public record TokenRequest(
        String username,
        String password
) {
}

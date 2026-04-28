package com.tsm_db_sql.db.authserver.model;

/**
 * Response del token endpoint — segue il formato standard OAuth2.
 *
 * @param accessToken  il JWT firmato contenente i claim dell'utente
 * @param tokenType    sempre "Bearer" — indica come il client deve inviare il token
 *                     (header Authorization: Bearer {accessToken})
 * @param expiresIn    durata in secondi del token (es. 3600 = 1 ora)
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}

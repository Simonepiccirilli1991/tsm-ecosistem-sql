package com.tsm_db_sql.db.authserver.controller;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JWK Set Endpoint — espone la chiave pubblica RSA per la verifica dei JWT.
 *
 * Percorso standard: GET /.well-known/jwks.json
 *
 * Questo endpoint è fondamentale per il funzionamento dell'OAuth2 Resource Server.
 * WIAM (e qualsiasi altro Resource Server) scarica da qui la chiave pubblica RSA
 * e la usa per verificare la firma dei JWT ricevuti nelle richieste.
 *
 * Il formato JWK Set è definito da RFC 7517. La risposta contiene SOLO la chiave
 * pubblica (mai la privata) — questo è garantito da toPublicJWKSet() che rimuove
 * automaticamente i componenti privati della chiave.
 *
 * Esempio di risposta:
 * {
 *   "keys": [{
 *     "kty": "RSA",
 *     "kid": "uuid-della-chiave",
 *     "n": "modulo-rsa-base64url",
 *     "e": "AQAB",
 *     "use": "sig"
 *   }]
 * }
 */
@RestController
@RequiredArgsConstructor
public class JwkController {

    private final JWKSet jwkSet;

    /**
     * Restituisce il JWK Set con solo la chiave pubblica.
     *
     * toPublicJWKSet() è CRITICO: rimuove la chiave privata dal set.
     * Senza questa chiamata, esporremmo la chiave privata a chiunque
     * faccia una GET su questo endpoint — compromettendo l'intero sistema
     * (un attaccante potrebbe firmare JWT arbitrari).
     *
     * @return mappa JSON del JWK Set pubblico
     */
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwkSet() {
        // toPublicJWKSet() restituisce una copia del JWK Set contenente
        // SOLO i componenti pubblici delle chiavi. La chiave privata RSA
        // (usata per firmare i token) non viene mai esposta.
        return jwkSet.toPublicJWKSet().toJSONObject();
    }
}

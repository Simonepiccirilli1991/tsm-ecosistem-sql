package com.tsm_db_sql.db.wiam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configurazione Spring Security per WIAM.
 *
 * WIAM opera come OAuth2 Resource Server: valida i JWT emessi dall'auth-server
 * per proteggere gli endpoint. Alcuni endpoint restano pubblici perché servono
 * prima dell'autenticazione (registrazione, login, recupero password).
 *
 * SessionCreationPolicy.STATELESS: non creiamo sessioni HTTP lato server.
 * Ogni richiesta si autentica tramite il Bearer token JWT nell'header Authorization.
 * Questo è il pattern standard per API REST con OAuth2.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Bean BCryptPasswordEncoder — usato da tutti i servizi per hashare e verificare password.
     * BCrypt include automaticamente un salt random nell'hash, quindi lo stesso input
     * produce hash diversi ogni volta (protezione contro rainbow table).
     * Il costo di default (strength=10) bilancia sicurezza e performance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura la catena di filtri di sicurezza HTTP.
     *
     * Endpoint pubblici (permitAll):
     * - /api/v1/utente/registra — registrazione utente (non ancora autenticato)
     * - /api/v1/utente/login — login legacy (usato anche dall'auth-server)
     * - /api/v1/utente/securety/retrivestep* — recupero password (utente non può autenticarsi)
     * - /api/internal/** — endpoint interni per comunicazione tra microservizi
     * - /h2-console/** — console H2 per sviluppo
     *
     * Tutti gli altri endpoint richiedono un JWT valido nel header Authorization.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disabilitiamo CSRF perché l'API è stateless e usa Bearer token.
            // CSRF protegge da attacchi basati su cookie di sessione, che non usiamo.
            .csrf(csrf -> csrf.disable())

            // Disabilitiamo frame options per permettere la H2 console (usa iframe)
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))

            // Nessuna sessione HTTP — ogni richiesta porta il suo JWT
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Regole di autorizzazione per gli endpoint
            .authorizeHttpRequests(auth -> auth
                // Endpoint pubblici — accessibili senza autenticazione
                .requestMatchers(
                    "/api/v1/utente/registra",
                    "/api/v1/utente/login"
                ).permitAll()

                // Recupero password — tutti e 3 gli step sono pubblici
                // perché l'utente non conosce la password e non può autenticarsi
                .requestMatchers(
                    "/api/v1/utente/securety/retrivestep1",
                    "/api/v1/utente/securety/retrivestep2",
                    "/api/v1/utente/securety/retrivestep3"
                ).permitAll()

                // Endpoint interni — usati per comunicazione tra microservizi (auth-server → WIAM).
                // In produzione questi vanno protetti a livello di rete (firewall/service mesh),
                // non tramite JWT, perché l'auth-server stesso non ha ancora un token.
                .requestMatchers("/api/internal/**").permitAll()

                // H2 console — solo per sviluppo, disabilitata in prod tramite application-prod.yaml
                .requestMatchers("/h2-console/**").permitAll()

                // Tutto il resto richiede autenticazione con JWT valido
                .anyRequest().authenticated()
            )

            // Configura WIAM come OAuth2 Resource Server che valida JWT.
            // Il JWK Set URI è configurato in application.yaml e punta all'auth-server
            // per scaricare la chiave pubblica RSA usata per verificare la firma dei token.
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            );

        return http.build();
    }
}

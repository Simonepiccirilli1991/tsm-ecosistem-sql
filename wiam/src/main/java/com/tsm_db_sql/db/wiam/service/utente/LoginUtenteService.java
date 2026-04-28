package com.tsm_db_sql.db.wiam.service.utente;


import com.tsm_db_sql.db.wiam.exception.UtenteException;
import com.tsm_db_sql.db.wiam.model.request.LoginUtenteRequest;
import com.tsm_db_sql.db.wiam.model.response.LoginUtenteResponse;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginUtenteService {


    private final UtenteRepository  utenteRepository;
    // PasswordEncoder (BCrypt) iniettato da Spring — usato per verificare la password
    // senza mai confrontare il testo in chiaro direttamente
    private final PasswordEncoder passwordEncoder;


    public LoginUtenteResponse login(LoginUtenteRequest request) {
        log.info("LoginUtente service started for utente: {}", request.username());

        var utente = utenteRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.error("LoginUtente error, utente not found");
                    return new UtenteException("Utente non trovato", "ERR-UT-404");
                });

        // Verifica password con BCrypt: matches() confronta la password in chiaro
        // con l'hash salvato nel DB. BCrypt estrae automaticamente il salt dall'hash
        // e ri-hasha la password per confrontarla, senza mai memorizzare il testo in chiaro.
        if (!passwordEncoder.matches(request.password(), utente.getPassword())) {
            log.error("LoginUtente error, password non valida per utente: {}", request.username());
            // metto codice errore specifico per rimappare messaggio e non fare enumeration poi su chi rimappa
            throw new UtenteException("Password non valida", "ERR-UT-401-P");
        }


        log.info("LoginUtente service ended successfully for utente: {}", request.username());
        return new LoginUtenteResponse(utente.getNome(), utente.getCognome(), utente.getRuolo(), utente.getEmail());
    }
}

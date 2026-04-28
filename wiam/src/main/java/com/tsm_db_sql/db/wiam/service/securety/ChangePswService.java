package com.tsm_db_sql.db.wiam.service.securety;

import com.tsm_db_sql.db.wiam.exception.UtenteException;
import com.tsm_db_sql.db.wiam.model.request.ChangePswRequest;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import com.tsm_db_sql.db.wiam.model.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ChangePswService {


    private final UtenteRepository utenteRepository;
    // PasswordEncoder (BCrypt) — per verificare la vecchia password e hashare la nuova
    private final PasswordEncoder passwordEncoder;


    public BaseResponse changePsw(ChangePswRequest request){
        log.info("ChangePswService started for utente: {}",request.username());

        // riprendo utente
        var utente = utenteRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.error("ChangePsw service error Utente non trovato: {}", request.username());
                    return new UtenteException("Utente non trovato","ERR-UT-404-P");
                });

        // Verifica vecchia password con BCrypt — matches() confronta la password in chiaro
        // con l'hash BCrypt salvato nel DB senza mai esporre il testo originale
        if(!passwordEncoder.matches(request.oldPassword(), utente.getPassword())){
           log.error("ChangePsw service error password non match: {}", request.username());
           throw new UtenteException("Password attuale non corretta","ERR-UT-401-P");
        }
        // Verifica che la nuova password sia diversa dalla vecchia.
        // Usiamo matches() anche qui perché non possiamo confrontare hash direttamente
        // (BCrypt produce hash diversi per lo stesso input grazie al salt random).
        if(passwordEncoder.matches(request.newPassword(), utente.getPassword())){
            log.error("ChangePsw service error password nuova uguale alla vecchia: {}", request.username());
            throw new UtenteException("La nuova password deve essere diversa da quella attuale","ERR-UT-400-P");
        }

        // Hash della nuova password con BCrypt prima di salvarla
        utente.setPassword(passwordEncoder.encode(request.newPassword()));
        utente.getUtenteSecurety().setLastPaswordChange(LocalDateTime.now());
        utenteRepository.save(utente);
        log.info("ChangePswService ended successfully for utente: {}",request.username());
        return new BaseResponse("Password cambiata con successo");

    }
}

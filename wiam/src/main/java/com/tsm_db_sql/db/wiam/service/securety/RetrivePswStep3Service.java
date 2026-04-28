package com.tsm_db_sql.db.wiam.service.securety;

import com.tsm_db_sql.db.wiam.entity.Utente;
import com.tsm_db_sql.db.wiam.entity.UtenteSecurety;
import com.tsm_db_sql.db.wiam.exception.UtenteException;
import com.tsm_db_sql.db.wiam.model.request.RetrivePswStep3Request;
import com.tsm_db_sql.db.wiam.model.response.BaseResponse;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
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
public class RetrivePswStep3Service {


    private final UtenteRepository utenteRepository;
    // PasswordEncoder (BCrypt) — per hashare la nuova password nel recupero password
    private final PasswordEncoder passwordEncoder;


    public BaseResponse retrivePswStep3(RetrivePswStep3Request request){
        log.info("RetrivePswStep3 service started for utente: {}",request.username());

        // controllo se esiste utente
        var utente = utenteRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.error("RetrivePswStep3 service error Utente non trovato: {}", request.username());
                    return new UtenteException("Utente non trovato","ERR-UT-404");
                });

        var utenteSecurety = utente.getUtenteSecurety();
        // controllo che chiave sia ancora valida
        if(utenteSecurety.getDurataChiaveStep3().isBefore(LocalDateTime.now())){
            log.error("RetrivePswStep3 service error chiave scaduta per utente: {}", request.username());
            // cancello chiave in quanto scaduta
            cancellaStepChiave(utenteSecurety,utente);
            throw new UtenteException("Chiave scaduta, richiedi nuovamente la procedura","ERR-UT-403");
        }

        // controllo che chaive matchino
        if(!utenteSecurety.getChiaveStep3().equals(request.chiaveStep())){
            log.error("RetrivePswStep3 service error chiave non valida per utente: {}", request.username());
            throw new UtenteException("Chiave non valida","ERR-UT-403");
        }

        // Hash della nuova password con BCrypt prima di salvarla nel DB
        utente.setPassword(passwordEncoder.encode(request.password()));
        // cancello stepChiave in quanto ormai usati
        cancellaStepChiave(utenteSecurety,utente);
        log.info("RetrivePswStep3 service completed successfully for utente: {}",request.username());
        return new BaseResponse("Password aggiornata con successo");

    }


    private void cancellaStepChiave(UtenteSecurety utenteSecurety, Utente utente){
        utenteSecurety.setDurataChiaveStep3(null);
        utenteSecurety.setChiaveStep3(null);
        utente.setUtenteSecurety(utenteSecurety);
        utenteRepository.save(utente);
    }
}

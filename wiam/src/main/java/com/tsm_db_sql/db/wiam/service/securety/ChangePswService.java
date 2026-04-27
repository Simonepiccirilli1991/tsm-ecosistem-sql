package com.tsm_db_sql.db.wiam.service.securety;

import com.tsm_db_sql.db.wiam.exception.UtenteException;
import com.tsm_db_sql.db.wiam.model.request.ChangePswRequest;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import com.tsm_db_sql.db.wiam.model.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ChangePswService {


    private final UtenteRepository utenteRepository;


    public BaseResponse changePsw(ChangePswRequest request){
        log.info("ChangePswService started for utente: {}",request.username());

        // riprendo utente
        var utente = utenteRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.error("ChangePsw service error Utente non trovato: {}", request.username());
                    return new UtenteException("Utente non trovato","ERR-UT-404-P");
                });

        // controllo che password matchino
        if(!utente.getPassword().equals(request.oldPassword())){
           log.error("ChangePsw service error password non match: {}", request.username());
           throw new UtenteException("Password attuale non corretta","ERR-UT-401-P");
        }
        // controllo che la nuova psw sia diversa dalla vecchia
        if(utente.getPassword().equals(request.newPassword())){
            log.error("ChangePsw service error password nuova uguale alla vecchia: {}", request.username());
            throw new UtenteException("La nuova password deve essere diversa da quella attuale","ERR-UT-400-P");
        }

        // cambio effettivamente la psw
        utente.setPassword(request.newPassword());
        utente.getUtenteSecurety().setLastPaswordChange(LocalDateTime.now());
        utenteRepository.save(utente);
        log.info("ChangePswService ended successfully for utente: {}",request.username());
        return new BaseResponse("Password cambiata con successo");

    }
}

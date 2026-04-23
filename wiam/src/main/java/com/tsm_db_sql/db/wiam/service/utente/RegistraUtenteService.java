package com.tsm_db_sql.db.wiam.service.utente;


import com.tsm_db_sql.db.wiam.entity.Utente;
import com.tsm_db_sql.db.wiam.exception.UtenteException;
import com.tsm_db_sql.db.wiam.model.request.DeleteUtenteRequest;
import com.tsm_db_sql.db.wiam.model.request.RegistraUtenteRequest;
import com.tsm_db_sql.db.wiam.model.response.RegistrazioneResponse;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistraUtenteService {


    private final UtenteRepository utenteRepository;



    public RegistrazioneResponse registraUtente(RegistraUtenteRequest request) {

        log.info("RegistraUtente service started with raw request: {}",request);

        //valido request
        request.validaRequest();
        // controllo se gia esiste un utente registrato con quell'username
        var utenteAlreadyPresent = utenteRepository.findByUsername(request.username());
        if(utenteAlreadyPresent.isPresent()) {
            log.error("Utente gia registrato con username indicato");
            throw new UtenteException("Utente gia registrato con username indicato","ERR-UT-400");
        }
        // controllo che email non sia gia in uso
        if(utenteRepository.findByEmail(request.email()).isPresent()) {
            log.error("Utente gia registrato con email indicato");
            throw new UtenteException("Utente gia registrato con email indicata","ERR-UT-400");
        }
        // se non esiste lo registro
        var utente = new Utente();
        utente.setUsername(request.username());
        utente.setPassword(request.password());
        utente.setEmail(request.email());
        utente.setNome(request.nome());
        utente.setCognome(request.cognome());
        utente.setDataRegistrazione(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // salvo utente
        utenteRepository.save(utente);
        log.info("RegistraUtente service ended successfully");
        return new RegistrazioneResponse("Utente registrato con successo");
    }


    public RegistrazioneResponse cancellaUtente(DeleteUtenteRequest request) {
        log.info("CancellaUtente service started with raw request: {}",request.username());

        // cerco l'utente
        var utente =  utenteRepository.findByUsername(request.username())
                .orElseThrow( () -> {
                    log.error("Error on cancella utente service, utente not found con username: {}",request.username());
                    return new UtenteException("Utente not found con username: "+request.username(),"ERR-UT-404");
                });

        // controllo match password
        if(!utente.getPassword().equals(request.password())) {
            log.error("Passwords don't match, don't authorized for delete utente");
            throw new UtenteException("Passord don't match for delete utente","ERR-UT-401");
        }
        // cancello utente
        utenteRepository.delete(utente);
        log.info("Cancella utente service ended successuflly");
        return new RegistrazioneResponse("Utente cancellato con successo");
    }
}

package com.tsm_db_sql.db.wiam.service.utente;


import com.tsm_db_sql.db.wiam.entity.Utente;
import com.tsm_db_sql.db.wiam.entity.UtenteSecurety;
import com.tsm_db_sql.db.wiam.exception.UtenteException;
import com.tsm_db_sql.db.wiam.model.request.DeleteUtenteRequest;
import com.tsm_db_sql.db.wiam.model.request.RegistraUtenteRequest;
import com.tsm_db_sql.db.wiam.model.response.RegistrazioneResponse;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import com.tsm_db_sql.db.wiam.utils.UtenteRoles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RegistraUtenteService {


    private final UtenteRepository utenteRepository;
    // PasswordEncoder (BCrypt) — usato per hashare la password prima di salvarla nel DB.
    // Non salviamo MAI la password in chiaro: BCrypt genera un hash con salt random incluso.
    private final PasswordEncoder passwordEncoder;
    @Value("${roles.admin.email}")
    private String emailAdmin;
    @Value("${roles.collaborator.email}")
    private String emailCollaboratore;


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
        // Hash della password con BCrypt prima di salvare — la password in chiaro
        // non viene mai persistita. L'hash include un salt random, quindi registrazioni
        // con la stessa password producono hash diversi (protezione rainbow table).
        utente.setPassword(passwordEncoder.encode(request.password()));
        utente.setEmail(request.email());
        utente.setNome(request.nome());
        utente.setCognome(request.cognome());
        utente.setDataRegistrazione(LocalDateTime.now());
        // devo settare il ruolo
        var ruolo = determinaRuolo(request.email());
        utente.setRuolo(ruolo);
        // var setto utente securety
        var utSec = settaUtenteSecurety(utente);
        utente.setUtenteSecurety(utSec);
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

        // Verifica password con BCrypt — confronta la password in chiaro della request
        // con l'hash BCrypt salvato nel DB. Non usiamo .equals() perché l'hash è diverso ogni volta.
        if(!passwordEncoder.matches(request.password(), utente.getPassword())) {
            log.error("Passwords don't match, don't authorized for delete utente");
            throw new UtenteException("Passord don't match for delete utente","ERR-UT-401");
        }
        // cancello utente
        utenteRepository.delete(utente);
        log.info("Cancella utente service ended successuflly");
        return new RegistrazioneResponse("Utente cancellato con successo");
    }

    // messa qui, perche se uso operatore ternario sonar si incazza perche difficile da leggere, bestie
    private UtenteRoles determinaRuolo(String email){
        if(email.equals(emailAdmin)) {
            return UtenteRoles.Admin;
        }
        return (email.equals(emailCollaboratore)) ? UtenteRoles.Collaborator : UtenteRoles.User;
    }

    /*
    Sì, è buona pratica mantenerlo, per un motivo preciso: la consistenza del grafo oggetti in memoria.
Se durante la stessa sessione JPA qualcuno fa utenteSecurety.getUtente(), senza quel set troverebbe null, anche se nel DB la relazione è corretta. Questo può causare bug sottili in logica che attraversa la relazione dal lato inverso.
La convenzione standard in JPA è sempre sincronizzare entrambi i lati di una relazione bidirezionale:
     */
    private UtenteSecurety settaUtenteSecurety(Utente utente) {
        var utenteSecurety = new UtenteSecurety();
        utenteSecurety.setOtpCounter(0);
        utenteSecurety.setLastPaswordChange(LocalDateTime.now());
        utenteSecurety.setUtente(utente);
        return utenteSecurety;
    }
}

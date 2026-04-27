package com.tsm_db_sql.db.wiam.service;


import com.tsm_db_sql.db.wiam.entity.Utente;
import com.tsm_db_sql.db.wiam.entity.UtenteSecurety;
import com.tsm_db_sql.db.wiam.exception.UtenteException;
import com.tsm_db_sql.db.wiam.model.request.ChangePswRequest;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import com.tsm_db_sql.db.wiam.service.securety.ChangePswService;
import com.tsm_db_sql.db.wiam.service.securety.RetrivePswStep1Service;
import com.tsm_db_sql.db.wiam.service.securety.RetrivePswStep2Service;
import com.tsm_db_sql.db.wiam.service.securety.RetrivePswStep3Service;
import com.tsm_db_sql.db.wiam.utils.UtenteRoles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;

@SpringBootTest
@Profile("test")
public class ChangePswServiceTest {


    @Autowired
    UtenteRepository utenteRepository;
    @Autowired
    RetrivePswStep1Service retrivePswStep1Service;
    @Autowired
    RetrivePswStep2Service retrivePswStep2Service;
    @Autowired
    RetrivePswStep3Service  retrivePswStep3Service;
    @Autowired
    ChangePswService changePswService;


    @BeforeEach
    void setUp() {
        utenteRepository.deleteAll();
    }

    @Test
    void changePswTestOK() {

        var utente = new Utente();
        utente.setUsername("Ajeje");
        utente.setNome("Ajeje");
        utente.setCognome("Brazof");
        utente.setEmail("asd@gmail.com");
        utente.setPassword("BeaMerda");
        utente.setDataRegistrazione(LocalDateTime.now());
        utente.setRuolo(UtenteRoles.User);
        // siccome non chiamo api se non setto si sfascia perche giustamente va in nullpointer
        var sec = new UtenteSecurety();
        sec.setOtpCounter(0);
        utente.setUtenteSecurety(sec);

        utenteRepository.save(utente);

        var request = new ChangePswRequest("Ajeje","BeaMerda","Casaparlante");
        var resp = changePswService.changePsw(request);

        Assertions.assertEquals("Password cambiata con successo",resp.messaggio());
        var ut = utenteRepository.findByUsername("Ajeje");
        Assertions.assertEquals("Casaparlante",ut.get().getPassword());
    }

    @Test
    void changePswTestKO() {

        var utente = new Utente();
        utente.setUsername("Ajeje");
        utente.setNome("Ajeje");
        utente.setCognome("Brazof");
        utente.setEmail("asd@gmail.com");
        utente.setPassword("BeaMerda");
        utente.setDataRegistrazione(LocalDateTime.now());
        utente.setRuolo(UtenteRoles.User);

        utenteRepository.save(utente);

        var request = new ChangePswRequest("Ajeje","ttt","Casaparlante");
        var except  = Assertions.assertThrows(UtenteException.class, () -> {
            changePswService.changePsw(request);
        });

        Assertions.assertEquals("Password attuale non corretta", except.getMessaggio());
        Assertions.assertEquals("ERR-UT-401-P",except.getErrorCode());
    }

    @Test
    void changePswTestKO2() {

        var utente = new Utente();
        utente.setUsername("Ajeje");
        utente.setNome("Ajeje");
        utente.setCognome("Brazof");
        utente.setEmail("asd@gmail.com");
        utente.setPassword("Casaparlante");
        utente.setDataRegistrazione(LocalDateTime.now());
        utente.setRuolo(UtenteRoles.User);

        utenteRepository.save(utente);

        var request = new ChangePswRequest("Ajeje","Casaparlante","Casaparlante");
        var except  = Assertions.assertThrows(UtenteException.class, () -> {
            changePswService.changePsw(request);
        });

        Assertions.assertEquals("La nuova password deve essere diversa da quella attuale", except.getMessaggio());
        Assertions.assertEquals("ERR-UT-400-P",except.getErrorCode());
    }



}

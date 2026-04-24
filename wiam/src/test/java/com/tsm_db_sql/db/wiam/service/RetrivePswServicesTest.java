package com.tsm_db_sql.db.wiam.service;

import com.tsm_db_sql.db.wiam.entity.Utente;
import com.tsm_db_sql.db.wiam.entity.UtenteSecurety;
import com.tsm_db_sql.db.wiam.exception.UtenteSecuretyException;
import com.tsm_db_sql.db.wiam.model.request.RetrivePswStep1Request;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
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
public class RetrivePswServicesTest {


    @Autowired
    UtenteRepository utenteRepository;
    @Autowired
    RetrivePswStep1Service retrivePswStep1Service;
    @Autowired
    RetrivePswStep2Service retrivePswStep2Service;
    @Autowired
    RetrivePswStep3Service retrivePswStep3Service;


    @BeforeEach
    void setup() {
        utenteRepository.deleteAll();
    }

    @Test
    void retrivePswStep1OK() {

        var utente = new Utente();
        utente.setUsername("Ajeje");
        utente.setNome("Ajeje");
        utente.setCognome("Brazof");
        utente.setEmail("asd@gmail.com");
        utente.setPassword("BeaMerda");
        utente.setDataRegistrazione(LocalDateTime.now());
        utente.setRuolo(UtenteRoles.User);

        var utenteSecurety = new UtenteSecurety();
        utenteSecurety.setOtpCounter(0);
        utente.setUtenteSecurety(utenteSecurety);
        utenteRepository.save(utente);

        var request =  new RetrivePswStep1Request("Ajeje");

        var resp = retrivePswStep1Service.retrivePseStep1(request);

        Assertions.assertEquals("asd@gmail.com",resp.email());
        var ut = utenteRepository.findByUsername("Ajeje");
        Assertions.assertEquals(1,ut.get().getUtenteSecurety().getOtpCounter());
    }

    @Test
    void retrivePswStep1KO() {

        var utente = new Utente();
        utente.setUsername("Ajeje");
        utente.setNome("Ajeje");
        utente.setCognome("Brazof");
        utente.setEmail("asd@gmail.com");
        utente.setPassword("BeaMerda");
        utente.setDataRegistrazione(LocalDateTime.now());
        utente.setRuolo(UtenteRoles.User);

        var utenteSecurety = new UtenteSecurety();
        utenteSecurety.setOtpCounter(5);
        utente.setUtenteSecurety(utenteSecurety);
        utenteSecurety.setDataRichiestaUltimoOtp(LocalDateTime.now().minusDays(1));
        utenteRepository.save(utente);

        var request =  new RetrivePswStep1Request("Ajeje");

        var except = Assertions.assertThrows(UtenteSecuretyException.class, () -> {
            retrivePswStep1Service.retrivePseStep1(request);
        });

        Assertions.assertEquals("Error on RetrivePswStep1 service, max otp requested",except.getMessaggio());
        var ut = utenteRepository.findByUsername("Ajeje");
        Assertions.assertEquals(5,ut.get().getUtenteSecurety().getOtpCounter());
    }

    // step2
}

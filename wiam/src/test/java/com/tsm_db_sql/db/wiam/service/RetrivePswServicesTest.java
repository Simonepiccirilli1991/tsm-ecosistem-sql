package com.tsm_db_sql.db.wiam.service;

import com.tsm_db_sql.db.wiam.entity.Utente;
import com.tsm_db_sql.db.wiam.entity.UtenteSecurety;
import com.tsm_db_sql.db.wiam.exception.UtenteException;
import com.tsm_db_sql.db.wiam.exception.UtenteSecuretyException;
import com.tsm_db_sql.db.wiam.model.request.RetrivePswStep1Request;
import com.tsm_db_sql.db.wiam.model.request.RetrivePswStep2Request;
import com.tsm_db_sql.db.wiam.model.request.RetrivePswStep3Request;
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
import java.util.UUID;

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

    @Test
    void retrivePswStep2OK() {

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

        var step2Request = new RetrivePswStep2Request("Ajeje", resp.otp());

        var respStep2 = retrivePswStep2Service.retrivePswStep2(step2Request);

        Assertions.assertNotNull(respStep2.resetCode());
    }

    @Test
    void retrivePswStep2KO() {

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

        retrivePswStep1Service.retrivePseStep1(request);

        var otpCifrato = retrivePswStep1Service.cifraOtp("12345");
        var step2Request = new RetrivePswStep2Request("Ajeje", otpCifrato);

        var except =Assertions.assertThrows(UtenteSecuretyException.class, () -> retrivePswStep2Service.retrivePswStep2(step2Request));

       Assertions.assertEquals("Otp non valido",except.getMessaggio());
    }

    @Test
    void retrivePswStep3OK() {

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

        var step2Request = new RetrivePswStep2Request("Ajeje", resp.otp());

        var respStep2 = retrivePswStep2Service.retrivePswStep2(step2Request);

        Assertions.assertNotNull(respStep2.resetCode());

        var step3Request = new RetrivePswStep3Request("Ajeje",respStep2.resetCode(),"Gigino212");

        var resp3 = retrivePswStep3Service.retrivePswStep3(step3Request);

        Assertions.assertEquals("Password aggiornata con successo",resp3.messaggio());
        var utAggiornato = utenteRepository.findByUsername("Ajeje");

        Assertions.assertEquals("Gigino212",utAggiornato.get().getPassword());
    }

    @Test
    void retrivePswStep3KO() {

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

        var step2Request = new RetrivePswStep2Request("Ajeje", resp.otp());

        var respStep2 = retrivePswStep2Service.retrivePswStep2(step2Request);

        Assertions.assertNotNull(respStep2.resetCode());

        var step3Request = new RetrivePswStep3Request("Ajeje", UUID.randomUUID().toString(),"Gigino212");

        var except3 = Assertions.assertThrows(UtenteException.class, () ->  retrivePswStep3Service.retrivePswStep3(step3Request));

        Assertions.assertEquals("Chiave non valida",except3.getMessaggio());
    }
}

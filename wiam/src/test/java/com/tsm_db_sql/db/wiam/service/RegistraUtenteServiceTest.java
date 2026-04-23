package com.tsm_db_sql.db.wiam.service;


import com.tsm_db_sql.db.wiam.entity.Utente;
import com.tsm_db_sql.db.wiam.exception.UtenteException;
import com.tsm_db_sql.db.wiam.model.request.DeleteUtenteRequest;
import com.tsm_db_sql.db.wiam.model.request.RegistraUtenteRequest;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import com.tsm_db_sql.db.wiam.service.utente.RegistraUtenteService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootTest
@ActiveProfiles("test")
public class RegistraUtenteServiceTest {


    @Autowired
    RegistraUtenteService registraUtenteService;
    @Autowired
    UtenteRepository utenteRepository;

    @BeforeEach
    void cleanUp() {
        utenteRepository.deleteAll();
    }


    @Test
    void registraUtenteServiceTestOK(){


        var request = new RegistraUtenteRequest("Ajeje","Brazof","ajeje-braz","ajeje@gmail.com","BeaMerda");

        var response = registraUtenteService.registraUtente(request);

        Assertions.assertEquals(response.messaggio(),"Utente registrato con successo");

        var utente = utenteRepository.findByUsername(request.username());

        Assertions.assertTrue(utente.isPresent());
        Assertions.assertEquals("Brazof",utente.get().getCognome());
    }

    @Test
    void registraUtenteServiceTestKO(){

        var utente = new Utente();
        utente.setUsername("Ajeje");
        utente.setNome("Ajeje");
        utente.setCognome("Brazof");
        utente.setEmail("asd@gmail.com");
        utente.setPassword("BeaMerda");
        utente.setDataRegistrazione(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        utenteRepository.save(utente);

        // caso utente gia registrato
        var request = new RegistraUtenteRequest("asd","buf","ajeje-braz","asd@gmail.com","BeaMerda");

        var except = Assertions.assertThrows(UtenteException.class ,() -> registraUtenteService.registraUtente(request));

        Assertions.assertEquals("Utente gia registrato con email indicata",except.getMessaggio());

    }

    @Test
    void registraECancellaUtenteServiceTestOK(){

        var utente = new Utente();
        utente.setUsername("Ajeje");
        utente.setNome("Ajeje");
        utente.setCognome("Brazof");
        utente.setEmail("asd@gmail.com");
        utente.setPassword("BeaMerda");
        utente.setDataRegistrazione(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        utenteRepository.save(utente);

        var request = new DeleteUtenteRequest("Ajeje","BeaMerda");

        var resp = registraUtenteService.cancellaUtente(request);

        Assertions.assertEquals("Utente cancellato con successo",resp.messaggio());

        Assertions.assertTrue(utenteRepository.findByUsername("Ajeje").isEmpty());
    }

    @Test
    void registraECancellaUtenteServiceTestKO(){

        var utente = new Utente();
        utente.setUsername("Ajeje");
        utente.setNome("Ajeje");
        utente.setCognome("Brazof");
        utente.setEmail("asd@gmail.com");
        utente.setPassword("BeaMerda");
        utente.setDataRegistrazione(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        utenteRepository.save(utente);

        var request = new DeleteUtenteRequest("Ajeje","BeaMerda123");

        var exception = Assertions.assertThrows(UtenteException.class, () -> registraUtenteService.cancellaUtente(request));

        Assertions.assertEquals("Passord don't match for delete utente",exception.getMessaggio());
        Assertions.assertTrue(utenteRepository.findByUsername("Ajeje").isPresent());
    }
}

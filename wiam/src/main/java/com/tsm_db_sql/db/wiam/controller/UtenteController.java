package com.tsm_db_sql.db.wiam.controller;


import com.tsm_db_sql.db.wiam.model.request.DeleteUtenteRequest;
import com.tsm_db_sql.db.wiam.model.request.RegistraUtenteRequest;
import com.tsm_db_sql.db.wiam.model.response.RegistrazioneResponse;
import com.tsm_db_sql.db.wiam.service.utente.RegistraUtenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/utente")
@RequiredArgsConstructor
public class UtenteController {

    private final RegistraUtenteService registraUtenteService;

    @PostMapping("registra")
    public ResponseEntity<RegistrazioneResponse> registraUtente(@RequestBody RegistraUtenteRequest request) {
        return ResponseEntity.ok(registraUtenteService.registraUtente(request));
    }

    @DeleteMapping("delete")
    public ResponseEntity<RegistrazioneResponse> deleteUtente(@RequestBody DeleteUtenteRequest request) {
        return ResponseEntity.ok(registraUtenteService.cancellaUtente(request));
    }


}

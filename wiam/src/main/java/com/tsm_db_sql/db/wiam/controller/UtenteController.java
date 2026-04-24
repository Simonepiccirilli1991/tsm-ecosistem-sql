package com.tsm_db_sql.db.wiam.controller;


import com.tsm_db_sql.db.wiam.model.request.ChangePswRequest;
import com.tsm_db_sql.db.wiam.model.request.DeleteUtenteRequest;
import com.tsm_db_sql.db.wiam.model.request.LoginUtenteRequest;
import com.tsm_db_sql.db.wiam.model.request.RegistraUtenteRequest;
import com.tsm_db_sql.db.wiam.model.response.LoginUtenteResponse;
import com.tsm_db_sql.db.wiam.model.response.RegistrazioneResponse;
import com.tsm_db_sql.db.wiam.service.securety.ChangePswService;
import com.tsm_db_sql.db.wiam.service.utente.BaseResponse;
import com.tsm_db_sql.db.wiam.service.utente.LoginUtenteService;
import com.tsm_db_sql.db.wiam.service.utente.RegistraUtenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/utente")
@RequiredArgsConstructor
public class UtenteController {

    private final RegistraUtenteService registraUtenteService;
    private final LoginUtenteService loginUtenteService;
    private final ChangePswService changePswService;


    @PostMapping("registra")
    public ResponseEntity<RegistrazioneResponse> registraUtente(@RequestBody RegistraUtenteRequest request) {
        return ResponseEntity.ok(registraUtenteService.registraUtente(request));
    }

    @DeleteMapping("delete")
    public ResponseEntity<RegistrazioneResponse> deleteUtente(@RequestBody DeleteUtenteRequest request) {
        return ResponseEntity.ok(registraUtenteService.cancellaUtente(request));
    }

    @PostMapping("login")
    public ResponseEntity<LoginUtenteResponse> loginUtente(@RequestBody LoginUtenteRequest request){
        return ResponseEntity.ok(loginUtenteService.login(request));
    }

    @PostMapping("changepsw")
    public ResponseEntity<BaseResponse> changePsw(@RequestBody ChangePswRequest request) {
        return ResponseEntity.ok(changePswService.changePsw(request));
    }


}

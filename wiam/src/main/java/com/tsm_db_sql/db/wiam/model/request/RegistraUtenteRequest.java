package com.tsm_db_sql.db.wiam.model.request;

import com.tsm_db_sql.db.wiam.exception.UtenteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

@Slf4j
public record RegistraUtenteRequest(

        String nome,
        String cognome,
        String username,
        String email,
        String password
) {

    public void validaRequest(){

        if(ObjectUtils.isEmpty(nome) || ObjectUtils.isEmpty(cognome) || ObjectUtils.isEmpty(username) || ObjectUtils.isEmpty(email) || ObjectUtils.isEmpty(password)){
            log.error("RegistraUtenteRequest validation failed: missing required fields. Request: {}", this);
            throw new UtenteException("Invalid request, missing requiered field","ERR-UT-400");
        }
    }
}

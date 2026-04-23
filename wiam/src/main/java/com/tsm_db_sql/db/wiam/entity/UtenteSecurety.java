package com.tsm_db_sql.db.wiam.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity(name = "utente_securety")
public class UtenteSecurety {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private Integer otpCounter;
    private String dataRichiestaUltimoOtp;
    private String lastPaswordChange;
}

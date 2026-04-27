package com.tsm_db_sql.db.wiam.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity di sicurezza — contiene dati OTP e tracking cambio password.
 * Legata 1:1 all'entity Utente (un utente ha esattamente un record di sicurezza).
 */
@Data
@EqualsAndHashCode(exclude = {"utente"})
@Entity(name = "utente_securety")
public class UtenteSecurety {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Relazione bidirezionale 1:1 con Utente.
     *
     * - mappedBy = "utenteSecurety": indica che la FK è gestita dall'altro lato
     *   (campo "utenteSecurety" nell'entity Utente). Questo lato è il lato "inverso",
     *   cioè non possiede la colonna FK nel database.
     * - NON mettiamo CascadeType qui: le operazioni a cascata partono sempre dal padre
     *   (Utente → UtenteSecurety), mai il contrario. Se mettessimo cascade qui,
     *   cancellare UtenteSecurety cancellerebbe anche l'Utente — pericoloso.
     */
    @OneToOne(mappedBy = "utenteSecurety")
    private Utente utente;

    @Column(nullable = false)
    private Integer otpCounter;
    private LocalDateTime dataRichiestaUltimoOtp;
    @Column(length = 6)
    private String otp;
    private LocalDateTime otpTimeRequest;

    @Column(length = 36)
    private String chiaveStep3;
    private LocalDateTime durataChiaveStep3;

    private LocalDateTime lastPaswordChange;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

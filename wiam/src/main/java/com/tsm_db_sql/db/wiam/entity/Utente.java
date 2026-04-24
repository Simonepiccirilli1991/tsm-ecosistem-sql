package com.tsm_db_sql.db.wiam.entity;


import com.tsm_db_sql.db.wiam.utils.UtenteRoles;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity principale — rappresenta un utente registrato nel sistema.
 * È l'aggregato radice: possiede sia i dati di sicurezza (UtenteSecurety)
 * che l'inventario degli acquisti (UtenteInventario).
 */
@Data
@Entity(name = "utente")
public class Utente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String cognome;

    @Column(unique = true, nullable = false)
    private String email;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private String dataRegistrazione;
    // gestione ruolo
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UtenteRoles ruolo;

    /*
     * Relazione 1:1 con UtenteSecurety (dati di sicurezza come OTP, cambio password).
     *
     * - CascadeType.ALL: quando salviamo/cancelliamo un Utente, Hibernate fa lo stesso
     *   automaticamente su UtenteSecurety. Non serve salvarlo a parte.
     * - orphanRemoval = true: se mettiamo utenteSecurety = null, Hibernate cancella
     *   il record orfano dal DB. Senza questo flag resterebbe una riga senza padre.
     * - FetchType.LAZY: i dati di sicurezza vengono caricati dal DB solo quando li
     *   accediamo davvero (es. utente.getUtenteSecurety()). Migliora le performance
     *   perché non facciamo JOIN inutili ogni volta che carichiamo un Utente.
     * - @JoinColumn: crea una colonna FK "utente_securety_id" sulla tabella utente
     *   che punta all'id di utente_securety. Il nome descrive il contenuto (un ID).
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_securety_id", referencedColumnName = "id")
    private UtenteSecurety utenteSecurety;

    /*
     * Relazione 1:N bidirezionale con UtenteInventario (lista acquisti dell'utente).
     *
     * - mappedBy = "utente": dice a Hibernate che la FK è gestita dal campo "utente"
     *   nell'entity UtenteInventario (lato "many"). Senza mappedBy, Hibernate
     *   creerebbe una tabella di join intermedia — spreco di spazio e performance.
     * - CascadeType.ALL: salvando l'Utente si salvano anche i suoi item di inventario.
     * - orphanRemoval = true: rimuovendo un item dalla lista, Hibernate lo cancella dal DB.
     * - FetchType.LAZY: la lista di inventario viene caricata solo quando serve.
     *   Senza LAZY, ogni query su Utente farebbe un JOIN con TUTTO l'inventario.
     */
    @OneToMany(mappedBy = "utente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UtenteInventario> inventario = new ArrayList<>();
}


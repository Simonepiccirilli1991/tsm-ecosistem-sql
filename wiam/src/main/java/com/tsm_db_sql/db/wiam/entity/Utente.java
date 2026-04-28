package com.tsm_db_sql.db.wiam.entity;


import com.tsm_db_sql.db.wiam.utils.UtenteRoles;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity principale — rappresenta un utente registrato nel sistema.
 * È l'aggregato radice: possiede sia i dati di sicurezza (UtenteSecurety)
 * che l'inventario degli acquisti (UtenteInventario).
 */
@Data
@EqualsAndHashCode(exclude = {"utenteSecurety", "inventario"})
@Entity(name = "utente")
@Table(indexes = {
        @Index(name = "idx_utente_username", columnList = "username"),
        @Index(name = "idx_utente_email", columnList = "email")
})
public class Utente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;
    @Column(nullable = false, length = 100)
    private String cognome;

    @Column(unique = true, nullable = false, length = 150)
    private String email;
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    // La password è salvata come hash BCrypt (~60 caratteri).
    // Non salviamo MAI la password in chiaro nel database.
    @Column(nullable = false, length = 72)
    private String password;
    @Column(nullable = false)
    private LocalDateTime dataRegistrazione;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UtenteRoles ruolo;

    /*
     * Relazione 1:1 con UtenteSecurety (dati di sicurezza come OTP, cambio password).
     *
     * - CascadeType.ALL: quando salviamo/cancelliamo un Utente, Hibernate fa lo stesso
     *   automaticamente su UtenteSecurety. Non serve salvarlo a parte.
     * - orphanRemoval = true: se mettiamo utenteSecurety = null, Hibernate cancella
     *   il record orfano dal DB. Senza questo flag resterebbe una riga senza padre.
     * - FetchType.EAGER: i dati di sicurezza sono sempre necessari nelle operazioni utente
     *   e l'entity ha pochi campi, quindi EAGER non impatta le performance.
     * - @JoinColumn: crea una colonna FK "utente_securety_id" sulla tabella utente
     *   che punta all'id di utente_securety.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
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

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Helper per sincronizzazione bidirezionale — setta entrambi i lati della relazione 1:1.
     */
    public void impostaUtenteSecurety(UtenteSecurety sec) {
        this.utenteSecurety = sec;
        if (sec != null) {
            sec.setUtente(this);
        }
    }

    /**
     * Helper per sincronizzazione bidirezionale — aggiunge un item e setta il lato owning.
     */
    public void aggiungiInventarioItem(UtenteInventario item) {
        this.inventario.add(item);
        item.setUtente(this);
    }
}

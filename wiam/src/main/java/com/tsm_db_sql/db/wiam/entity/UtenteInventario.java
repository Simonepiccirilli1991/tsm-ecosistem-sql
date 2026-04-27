package com.tsm_db_sql.db.wiam.entity;


import com.tsm_db_sql.db.wiam.utils.BrandAcquisti;
import com.tsm_db_sql.db.wiam.utils.StatoAcq;
import com.tsm_db_sql.db.wiam.utils.StatoProdotto;
import com.tsm_db_sql.db.wiam.utils.TipoProdotto;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity inventario — rappresenta un singolo acquisto/prodotto nell'inventario di un utente.
 * Legata N:1 all'entity Utente (un utente può avere molti item di inventario).
 */
@Data
@Entity(name = "utente_inventario")
@Table(indexes = {
        @Index(name = "idx_inventario_utente_id", columnList = "utente_id")
})
public class UtenteInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Relazione bidirezionale N:1 con Utente — questo è il lato "owning" (proprietario).
     *
     * - @ManyToOne: molti item di inventario appartengono a un singolo Utente.
     * - FetchType.LAZY: non carichiamo l'intero Utente ogni volta che leggiamo un item.
     *   Di default @ManyToOne usa EAGER (carica sempre), che causa problemi di performance
     *   con liste grandi — quindi impostiamo LAZY esplicitamente.
     * - @JoinColumn(name = "utente_id"): crea la colonna FK sulla tabella utente_inventario
     *   che punta all'id dell'utente. Questo è il lato che "possiede" la relazione
     *   e controlla la colonna nel database.
     * - nullable = false: ogni item DEVE avere un utente associato.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;

    @Column(nullable = false, length = 200)
    private String nomeAcquisto;
    @Column(length = 100)
    private String codiceAcquisto;
    @Column(nullable = false)
    private LocalDate dataAcquisto;
    @Column(length = 500)
    private String descrizione;

    /*
     * @Enumerated(EnumType.STRING): salva il NOME dell'enum nel DB (es. "Pokemon", "Lego")
     * invece del numero ordinale (es. 0, 1, 2).
     *
     * Perché è importante: senza STRING, JPA usa l'ordinale di default.
     * Se domani aggiungiamo un nuovo valore in mezzo all'enum (es. "YuGiOh" tra Pokemon e OnePiece),
     * tutti gli ordinali si spostano e i dati nel DB diventano sbagliati.
     * Con STRING, il valore salvato è il testo — non cambia mai anche se riordiniamo l'enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BrandAcquisti brandProdotto;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TipoProdotto tipoProdotto;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StatoProdotto statoProdotto;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StatoAcq statoAcquisto;

    // --- Sezione vendita ---
    @Column(precision = 10, scale = 2)
    private BigDecimal prezzoVendita;
    @Column(length = 100)
    private String piattaformaVendita;
    private LocalDate dataVendita;
    @Column(precision = 10, scale = 2)
    private BigDecimal costiAccessori;
    @Column(precision = 10, scale = 2)
    private BigDecimal prezzoNetto;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

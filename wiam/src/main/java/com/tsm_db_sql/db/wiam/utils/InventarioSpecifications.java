package com.tsm_db_sql.db.wiam.utils;

import com.tsm_db_sql.db.wiam.entity.Utente;
import com.tsm_db_sql.db.wiam.entity.UtenteInventario;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Classe utility che contiene metodi statici per costruire Specification<UtenteInventario>.
 *
 * Ogni metodo rappresenta un singolo filtro (un "predicato") che può essere combinato
 * con altri filtri usando .and(). Se un filtro non è richiesto (valore null nella request),
 * il service semplicemente non lo include nella catena.
 *
 * Come funziona JPA Specification:
 * - Specification è un'interfaccia funzionale con il metodo toPredicate(root, query, criteriaBuilder)
 * - root: rappresenta l'entity su cui stiamo facendo la query (UtenteInventario)
 * - criteriaBuilder: fornisce i metodi per costruire condizioni (equal, between, greaterThan, ecc.)
 * - Il risultato è un Predicate che JPA traduce in una clausola WHERE SQL
 *
 * Esempio di query generata con tutti i filtri:
 *   SELECT * FROM utente_inventario
 *   WHERE utente_id = ?
 *     AND prezzo_acquisto >= ?
 *     AND prezzo_acquisto <= ?
 *     AND data_acquisto >= ?
 *     AND data_acquisto <= ?
 *     AND stato_acquisto = ?
 *     AND brand_prodotto = ?
 *     AND tipo_prodotto = ?
 */
public class InventarioSpecifications {

    // Costruttore privato — classe utility con soli metodi statici, non va istanziata
    private InventarioSpecifications() {}

    /**
     * Filtro per utente proprietario — SEMPRE applicato.
     * Ogni inquiry è scoped sull'inventario di un singolo utente.
     */
    public static Specification<UtenteInventario> perUtente(Utente utente) {
        return (root, query, cb) -> cb.equal(root.get("utente"), utente);
    }

    /**
     * Filtro prezzo acquisto minimo (inclusivo).
     * Usa greaterThanOrEqualTo per includere il valore di soglia.
     */
    public static Specification<UtenteInventario> prezzoAcquistoDa(BigDecimal prezzoMin) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("prezzoAcquisto"), prezzoMin);
    }

    /**
     * Filtro prezzo acquisto massimo (inclusivo).
     * Usa lessThanOrEqualTo per includere il valore di soglia.
     */
    public static Specification<UtenteInventario> prezzoAcquistoA(BigDecimal prezzoMax) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("prezzoAcquisto"), prezzoMax);
    }

    /**
     * Filtro data acquisto inizio range (inclusivo).
     */
    public static Specification<UtenteInventario> dataAcquistoDa(LocalDate dataDa) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dataAcquisto"), dataDa);
    }

    /**
     * Filtro data acquisto fine range (inclusivo).
     */
    public static Specification<UtenteInventario> dataAcquistoA(LocalDate dataA) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dataAcquisto"), dataA);
    }

    /**
     * Filtro per stato dell'acquisto (Acquistato, Venduto, Cancellato).
     */
    public static Specification<UtenteInventario> perStatoAcquisto(StatoAcq stato) {
        return (root, query, cb) -> cb.equal(root.get("statoAcquisto"), stato);
    }

    /**
     * Filtro per brand del prodotto (Pokemon, OnePiece, Lego, Generico).
     */
    public static Specification<UtenteInventario> perBrand(BrandAcquisti brand) {
        return (root, query, cb) -> cb.equal(root.get("brandProdotto"), brand);
    }

    /**
     * Filtro per tipo di prodotto (BoxSealed, CartaSingola, ecc.).
     */
    public static Specification<UtenteInventario> perTipoProdotto(TipoProdotto tipo) {
        return (root, query, cb) -> cb.equal(root.get("tipoProdotto"), tipo);
    }
}

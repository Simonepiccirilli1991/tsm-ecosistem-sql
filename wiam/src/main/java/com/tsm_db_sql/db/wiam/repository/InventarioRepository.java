package com.tsm_db_sql.db.wiam.repository;

import com.tsm_db_sql.db.wiam.entity.Utente;
import com.tsm_db_sql.db.wiam.entity.UtenteInventario;
import com.tsm_db_sql.db.wiam.utils.BrandAcquisti;
import com.tsm_db_sql.db.wiam.utils.StatoAcq;
import com.tsm_db_sql.db.wiam.utils.TipoProdotto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository per UtenteInventario.
 *
 * Estende JpaSpecificationExecutor per supportare query dinamiche multi-criterio
 * tramite Specification (Criteria API). Usato dall'InventarioInquiryService V1.
 *
 * Contiene anche @Query JPQL personalizzate per l'inquiry V2 (switch-case).
 * Ogni query è ottimizzata per uno scenario specifico di filtri.
 */
public interface InventarioRepository extends JpaRepository<UtenteInventario, Long>,
        JpaSpecificationExecutor<UtenteInventario> {

    List<UtenteInventario> findByUtente(Utente utente);

    Page<UtenteInventario> findByUtente(Utente utente, Pageable pageable);

    // ==================== Query V2: scenari singolo filtro ====================

    /**
     * Filtro per utente + stato acquisto.
     * Usata quando l'unico filtro attivo è statoAcquisto.
     */
    @Query("SELECT i FROM utente_inventario i WHERE i.utente = :utente AND i.statoAcquisto = :stato")
    Page<UtenteInventario> findByUtenteAndStato(
            @Param("utente") Utente utente,
            @Param("stato") StatoAcq stato,
            Pageable pageable);

    /**
     * Filtro per utente + brand prodotto.
     * Usata quando l'unico filtro attivo è brandProdotto.
     */
    @Query("SELECT i FROM utente_inventario i WHERE i.utente = :utente AND i.brandProdotto = :brand")
    Page<UtenteInventario> findByUtenteAndBrand(
            @Param("utente") Utente utente,
            @Param("brand") BrandAcquisti brand,
            Pageable pageable);

    /**
     * Filtro per utente + tipo prodotto.
     * Usata quando l'unico filtro attivo è tipoProdotto.
     */
    @Query("SELECT i FROM utente_inventario i WHERE i.utente = :utente AND i.tipoProdotto = :tipo")
    Page<UtenteInventario> findByUtenteAndTipo(
            @Param("utente") Utente utente,
            @Param("tipo") TipoProdotto tipo,
            Pageable pageable);

    /**
     * Filtro per utente + range prezzo acquisto.
     * Usa BETWEEN per includere entrambi gli estremi.
     * Se un estremo è null, il service passa BigDecimal.ZERO o un valore molto alto.
     */
    @Query("SELECT i FROM utente_inventario i WHERE i.utente = :utente " +
            "AND i.prezzoAcquisto >= :prezzoMin AND i.prezzoAcquisto <= :prezzoMax")
    Page<UtenteInventario> findByUtenteAndRangePrezzo(
            @Param("utente") Utente utente,
            @Param("prezzoMin") BigDecimal prezzoMin,
            @Param("prezzoMax") BigDecimal prezzoMax,
            Pageable pageable);

    /**
     * Filtro per utente + range data acquisto.
     */
    @Query("SELECT i FROM utente_inventario i WHERE i.utente = :utente " +
            "AND i.dataAcquisto >= :dataDa AND i.dataAcquisto <= :dataA")
    Page<UtenteInventario> findByUtenteAndRangeData(
            @Param("utente") Utente utente,
            @Param("dataDa") LocalDate dataDa,
            @Param("dataA") LocalDate dataA,
            Pageable pageable);

    // ==================== Query V2: scenari doppio filtro ====================

    /**
     * Filtro per utente + brand + stato.
     */
    @Query("SELECT i FROM utente_inventario i WHERE i.utente = :utente " +
            "AND i.brandProdotto = :brand AND i.statoAcquisto = :stato")
    Page<UtenteInventario> findByUtenteAndBrandAndStato(
            @Param("utente") Utente utente,
            @Param("brand") BrandAcquisti brand,
            @Param("stato") StatoAcq stato,
            Pageable pageable);

    /**
     * Filtro per utente + brand + tipo prodotto.
     */
    @Query("SELECT i FROM utente_inventario i WHERE i.utente = :utente " +
            "AND i.brandProdotto = :brand AND i.tipoProdotto = :tipo")
    Page<UtenteInventario> findByUtenteAndBrandAndTipo(
            @Param("utente") Utente utente,
            @Param("brand") BrandAcquisti brand,
            @Param("tipo") TipoProdotto tipo,
            Pageable pageable);

    // ==================== Query V2: scenario universale ====================

    /**
     * Query universale con parametri opzionali — usata per combinazioni di 3+ filtri
     * o scenari non coperti dalle query dedicate sopra.
     *
     * Tecnica: ogni condizione è wrappata con (:param IS NULL OR colonna = :param).
     * Se il parametro è null, la condizione è sempre true e non filtra.
     * Se il parametro ha un valore, la condizione filtra normalmente.
     *
     * Per i range (prezzo e data) usiamo lo stesso pattern con >= e <=.
     */
    @Query("SELECT i FROM utente_inventario i WHERE i.utente = :utente " +
            "AND (:stato IS NULL OR i.statoAcquisto = :stato) " +
            "AND (:brand IS NULL OR i.brandProdotto = :brand) " +
            "AND (:tipo IS NULL OR i.tipoProdotto = :tipo) " +
            "AND (:prezzoMin IS NULL OR i.prezzoAcquisto >= :prezzoMin) " +
            "AND (:prezzoMax IS NULL OR i.prezzoAcquisto <= :prezzoMax) " +
            "AND (:dataDa IS NULL OR i.dataAcquisto >= :dataDa) " +
            "AND (:dataA IS NULL OR i.dataAcquisto <= :dataA)")
    Page<UtenteInventario> findByUtenteWithFiltriOpzionali(
            @Param("utente") Utente utente,
            @Param("stato") StatoAcq stato,
            @Param("brand") BrandAcquisti brand,
            @Param("tipo") TipoProdotto tipo,
            @Param("prezzoMin") BigDecimal prezzoMin,
            @Param("prezzoMax") BigDecimal prezzoMax,
            @Param("dataDa") LocalDate dataDa,
            @Param("dataA") LocalDate dataA,
            Pageable pageable);
}

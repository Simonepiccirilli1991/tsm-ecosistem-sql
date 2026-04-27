package com.tsm_db_sql.db.wiam.model.request;

import com.tsm_db_sql.db.wiam.exception.InventarioException;
import com.tsm_db_sql.db.wiam.utils.BrandAcquisti;
import com.tsm_db_sql.db.wiam.utils.StatoAcq;
import com.tsm_db_sql.db.wiam.utils.TipoProdotto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Request per la ricerca multi-criterio nell'inventario di un utente.
 *
 * Tutti i filtri sono opzionali tranne username — se un filtro è null, viene ignorato.
 * I filtri si combinano in AND: più filtri si applicano, più il risultato è ristretto.
 *
 * Parametri:
 * - username:           (obbligatorio) identifica l'utente proprietario dell'inventario
 * - prezzoAcquistoDa:   prezzo minimo acquisto (inclusivo)
 * - prezzoAcquistoA:    prezzo massimo acquisto (inclusivo)
 * - dataAcquistoDa:     data inizio range acquisto (inclusiva, formato "yyyy-MM-dd")
 * - dataAcquistoA:      data fine range acquisto (inclusiva, formato "yyyy-MM-dd")
 * - statoAcquisto:      filtro sullo stato dell'item (Acquistato, Venduto, Cancellato)
 * - brandProdotto:      filtro sul brand del prodotto
 * - tipoProdotto:       filtro sul tipo di prodotto
 * - page:               numero pagina (default 0)
 * - size:               dimensione pagina (default 20)
 */
@Slf4j
public record InventarioInquiryRequest(
        String username,
        BigDecimal prezzoAcquistoDa,
        BigDecimal prezzoAcquistoA,
        LocalDate dataAcquistoDa,
        LocalDate dataAcquistoA,
        StatoAcq statoAcquisto,
        BrandAcquisti brandProdotto,
        TipoProdotto tipoProdotto,
        Integer page,
        Integer size
) {

    /**
     * Validazione — solo username è obbligatorio.
     * Tutti gli altri campi sono filtri opzionali.
     */
    public void validaRequest() {
        if (ObjectUtils.isEmpty(username)) {
            log.error("InventarioInquiryRequest validazione fallita: username mancante. Request: {}", this);
            throw new InventarioException("Request non valida, username obbligatorio", "ERR-INV-400");
        }
    }
}

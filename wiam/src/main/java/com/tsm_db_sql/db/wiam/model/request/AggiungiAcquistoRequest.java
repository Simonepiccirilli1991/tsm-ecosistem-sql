package com.tsm_db_sql.db.wiam.model.request;

import com.tsm_db_sql.db.wiam.exception.InventarioException;
import com.tsm_db_sql.db.wiam.utils.BrandAcquisti;
import com.tsm_db_sql.db.wiam.utils.StatoProdotto;
import com.tsm_db_sql.db.wiam.utils.TipoProdotto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

/**
 * Request per aggiungere un nuovo acquisto all'inventario di un utente.
 * Contiene tutti i dati obbligatori per creare un item di inventario.
 */
@Slf4j
public record AggiungiAcquistoRequest(
        String username,
        String nomeAcquisto,
        String codiceAcquisto,
        String dataAcquisto,
        String descrizione,
        BrandAcquisti brandProdotto,
        TipoProdotto tipoProdotto,
        StatoProdotto statoProdotto
) {

    /**
     * Validazione campi obbligatori — segue il pattern del progetto:
     * ObjectUtils.isEmpty() + eccezione di dominio.
     * I campi codiceAcquisto e descrizione sono opzionali.
     */
    public void validaRequest() {
        if (ObjectUtils.isEmpty(username) || ObjectUtils.isEmpty(nomeAcquisto)
                || ObjectUtils.isEmpty(dataAcquisto) || ObjectUtils.isEmpty(brandProdotto)
                || ObjectUtils.isEmpty(tipoProdotto) || ObjectUtils.isEmpty(statoProdotto)) {
            log.error("AggiungiAcquistoRequest validazione fallita: campi obbligatori mancanti. Request: {}", this);
            throw new InventarioException("Request non valida, campi obbligatori mancanti", "ERR-INV-400");
        }
    }
}

package com.tsm_db_sql.db.wiam.model.request;

import com.tsm_db_sql.db.wiam.exception.InventarioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

/**
 * Request per registrare la vendita di un item già acquistato.
 * Richiede l'ID dell'item e i dati della vendita.
 */
@Slf4j
public record AggiungiVenditaRequest(
        String username,
        Long itemId,
        String prezzoVendita,
        String piattaformaVendita,
        String dataVendita,
        String costiAccessori
) {

    /**
     * Validazione campi obbligatori per la vendita.
     * piattaformaVendita e costiAccessori sono opzionali.
     */
    public void validaRequest() {
        if (ObjectUtils.isEmpty(username) || ObjectUtils.isEmpty(itemId)
                || ObjectUtils.isEmpty(prezzoVendita) || ObjectUtils.isEmpty(dataVendita)) {
            log.error("AggiungiVenditaRequest validazione fallita: campi obbligatori mancanti. Request: {}", this);
            throw new InventarioException("Request non valida, campi obbligatori mancanti", "ERR-INV-400");
        }
    }
}

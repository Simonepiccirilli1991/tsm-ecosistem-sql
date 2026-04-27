package com.tsm_db_sql.db.wiam.model.request;

import com.tsm_db_sql.db.wiam.exception.InventarioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

/**
 * Request per annullare la vendita di un item e riportarlo allo stato "Acquistato".
 */
@Slf4j
public record CancellaVenditaRequest(
        String username,
        Long itemId
) {

    /**
     * Validazione — entrambi i campi sono obbligatori.
     */
    public void validaRequest() {
        if (ObjectUtils.isEmpty(username) || ObjectUtils.isEmpty(itemId)) {
            log.error("CancellaVenditaRequest validazione fallita: campi obbligatori mancanti. Request: {}", this);
            throw new InventarioException("Request non valida, campi obbligatori mancanti", "ERR-INV-400");
        }
    }
}

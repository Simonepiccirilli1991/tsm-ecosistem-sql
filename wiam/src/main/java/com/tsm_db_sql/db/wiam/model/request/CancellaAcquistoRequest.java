package com.tsm_db_sql.db.wiam.model.request;

import com.tsm_db_sql.db.wiam.exception.InventarioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

/**
 * Request per cancellare (soft delete) un acquisto dall'inventario.
 */
@Slf4j
public record CancellaAcquistoRequest(
        String username,
        Long itemId
) {

    /**
     * Validazione — entrambi i campi sono obbligatori.
     */
    public void validaRequest() {
        if (ObjectUtils.isEmpty(username) || ObjectUtils.isEmpty(itemId)) {
            log.error("CancellaAcquistoRequest validazione fallita: campi obbligatori mancanti. Request: {}", this);
            throw new InventarioException("Request non valida, campi obbligatori mancanti", "ERR-INV-400");
        }
    }
}

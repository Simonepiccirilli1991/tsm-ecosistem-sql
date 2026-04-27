package com.tsm_db_sql.db.wiam.config;

import com.tsm_db_sql.db.wiam.exception.InventarioException;
import com.tsm_db_sql.db.wiam.exception.UtenteException;
import com.tsm_db_sql.db.wiam.exception.UtenteSecuretyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Gestione centralizzata delle eccezioni — mappa eccezioni di dominio e DB
 * a risposte HTTP strutturate, evitando che errori raw (500) arrivino al client.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UtenteException.class)
    public ResponseEntity<Map<String, String>> handleUtenteException(UtenteException ex) {
        log.error("UtenteException: {} [{}]", ex.getMessaggio(), ex.getErrorCode());
        var status = estraiHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(Map.of("messaggio", ex.getMessaggio(), "errorCode", ex.getErrorCode()));
    }

    @ExceptionHandler(UtenteSecuretyException.class)
    public ResponseEntity<Map<String, String>> handleUtenteSecuretyException(UtenteSecuretyException ex) {
        log.error("UtenteSecuretyException: {} [{}]", ex.getMessaggio(), ex.getErrorCode());
        var status = estraiHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(Map.of("messaggio", ex.getMessaggio(), "errorCode", ex.getErrorCode()));
    }

    @ExceptionHandler(InventarioException.class)
    public ResponseEntity<Map<String, String>> handleInventarioException(InventarioException ex) {
        log.error("InventarioException: {} [{}]", ex.getMessaggio(), ex.getErrorCode());
        var status = estraiHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(Map.of("messaggio", ex.getMessaggio(), "errorCode", ex.getErrorCode()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("DataIntegrityViolationException: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("messaggio", "Violazione vincolo di integrità dati", "errorCode", "ERR-DB-409"));
    }

    /**
     * Estrae lo status HTTP dal codice errore (es. "ERR-UT-400" → 400, "ERR-SEC-500" → 500).
     */
    private HttpStatus estraiHttpStatus(String errorCode) {
        try {
            var parts = errorCode.split("-");
            var statusCode = Integer.parseInt(parts[2]);
            return HttpStatus.valueOf(statusCode);
        } catch (Exception e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}

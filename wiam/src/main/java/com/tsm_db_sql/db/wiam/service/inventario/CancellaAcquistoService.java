package com.tsm_db_sql.db.wiam.service.inventario;

import com.tsm_db_sql.db.wiam.exception.InventarioException;
import com.tsm_db_sql.db.wiam.model.request.CancellaAcquistoRequest;
import com.tsm_db_sql.db.wiam.model.response.BaseResponse;
import com.tsm_db_sql.db.wiam.repository.InventarioRepository;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import com.tsm_db_sql.db.wiam.utils.StatoAcq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servizio per cancellare (soft delete) un acquisto dall'inventario.
 *
 * Flusso:
 * 1. Valida la request
 * 2. Cerca l'utente per username
 * 3. Cerca l'item per ID e verifica appartenenza all'utente
 * 4. Verifica che l'item sia in stato "Acquistato" (non si può cancellare un item venduto)
 * 5. Imposta lo stato a "Cancellato" (soft delete — l'item resta nel DB ma è marcato)
 *
 * Nota: usiamo il soft delete (statoAcquisto = Cancellato) invece della cancellazione fisica
 * per mantenere lo storico degli acquisti e permettere eventuali audit o ripristini futuri.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CancellaAcquistoService {

    private final UtenteRepository utenteRepository;
    private final InventarioRepository inventarioRepository;

    public BaseResponse cancellaAcquisto(CancellaAcquistoRequest request) {

        log.info("CancellaAcquisto service avviato con request: {}", request);

        // Passo 1: validazione dei campi obbligatori
        request.validaRequest();

        // Passo 2: cerco l'utente nel DB — se non esiste, errore 404
        var utente = utenteRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.error("Utente non trovato con username: {}", request.username());
                    return new InventarioException(
                            "Utente non trovato con username: " + request.username(), "ERR-INV-404");
                });

        // Passo 3: cerco l'item e verifico che appartenga all'utente
        var item = inventarioRepository.findById(request.itemId())
                .orElseThrow(() -> {
                    log.error("Item inventario non trovato con id: {}", request.itemId());
                    return new InventarioException(
                            "Item inventario non trovato con id: " + request.itemId(), "ERR-INV-404");
                });

        // Verifica appartenenza: previene cancellazioni non autorizzate
        if (!item.getUtente().getId().equals(utente.getId())) {
            log.error("Item id {} non appartiene all'utente: {}", request.itemId(), request.username());
            throw new InventarioException(
                    "Item non appartiene all'utente indicato", "ERR-INV-403");
        }

        // Passo 4: verifico che l'item sia in stato "Acquistato".
        // Un item venduto non può essere cancellato direttamente — bisogna prima annullare
        // la vendita con CancellaVenditaService, poi si può cancellare l'acquisto.
        // Un item già cancellato non può essere cancellato di nuovo.
        if (item.getStatoAcquisto() != StatoAcq.Acquistato) {
            log.error("Item id {} non è in stato Acquistato, stato attuale: {}",
                    request.itemId(), item.getStatoAcquisto());
            throw new InventarioException(
                    "L'item non è in stato Acquistato, impossibile cancellare. Stato attuale: "
                            + item.getStatoAcquisto(), "ERR-INV-400");
        }

        // Passo 5: imposto lo stato a "Cancellato" — soft delete.
        // L'item rimane fisicamente nel database ma è marcato come cancellato.
        // Questo approccio mantiene lo storico completo degli acquisti.
        item.setStatoAcquisto(StatoAcq.Cancellato);

        // Salvo le modifiche
        inventarioRepository.save(item);

        log.info("CancellaAcquisto service completato con successo per item id: {}", request.itemId());
        return new BaseResponse("Acquisto cancellato con successo");
    }
}

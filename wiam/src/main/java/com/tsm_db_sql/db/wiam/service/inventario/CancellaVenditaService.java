package com.tsm_db_sql.db.wiam.service.inventario;

import com.tsm_db_sql.db.wiam.exception.InventarioException;
import com.tsm_db_sql.db.wiam.model.request.CancellaVenditaRequest;
import com.tsm_db_sql.db.wiam.model.response.BaseResponse;
import com.tsm_db_sql.db.wiam.repository.InventarioRepository;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import com.tsm_db_sql.db.wiam.utils.StatoAcq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servizio per annullare la vendita di un item e riportarlo allo stato "Acquistato".
 *
 * Flusso:
 * 1. Valida la request
 * 2. Cerca l'utente per username
 * 3. Cerca l'item per ID e verifica appartenenza all'utente
 * 4. Verifica che l'item sia in stato "Venduto" (solo un item venduto può essere annullato)
 * 5. Azzera tutti i campi relativi alla vendita (rollback)
 * 6. Riporta lo stato a "Acquistato"
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CancellaVenditaService {

    private final UtenteRepository utenteRepository;
    private final InventarioRepository inventarioRepository;

    public BaseResponse cancellaVendita(CancellaVenditaRequest request) {

        log.info("CancellaVendita service avviato con request: {}", request);

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

        // Verifica appartenenza: previene che un utente annulli vendite di un altro utente
        if (!item.getUtente().getId().equals(utente.getId())) {
            log.error("Item id {} non appartiene all'utente: {}", request.itemId(), request.username());
            throw new InventarioException(
                    "Item non appartiene all'utente indicato", "ERR-INV-403");
        }

        // Passo 4: verifico che l'item sia in stato "Venduto".
        // Non ha senso annullare la vendita di un item che non è stato venduto.
        if (item.getStatoAcquisto() != StatoAcq.Venduto) {
            log.error("Item id {} non è in stato Venduto, stato attuale: {}",
                    request.itemId(), item.getStatoAcquisto());
            throw new InventarioException(
                    "L'item non è in stato Venduto, impossibile annullare vendita. Stato attuale: "
                            + item.getStatoAcquisto(), "ERR-INV-400");
        }

        // Passo 5: azzero tutti i campi relativi alla vendita.
        // Questo è un "rollback" logico: l'item torna come se non fosse mai stato venduto.
        // Impostiamo tutto a null perché i dati della vendita non sono più validi.
        item.setPrezzoVendita(null);
        item.setPiattaformaVendita(null);
        item.setDataVendita(null);
        item.setCostiAccessori(null);
        item.setPrezzoNetto(null);

        // Passo 6: riporto lo stato a "Acquistato" — l'item è di nuovo disponibile per la vendita
        item.setStatoAcquisto(StatoAcq.Acquistato);

        // Salvo le modifiche
        inventarioRepository.save(item);

        log.info("CancellaVendita service completato con successo per item id: {}", request.itemId());
        return new BaseResponse("Vendita annullata con successo, item riportato ad Acquistato");
    }
}
